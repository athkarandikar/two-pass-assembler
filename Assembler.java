import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class Assembler {
    public static int locationCounter = 0;
    private static SymbolTable symbolTable = new SymbolTable();
    private static LiteralTable literalTable = new LiteralTable();
    private static PoolTable poolTable = new PoolTable();

    public static void performPass1(String inputAsmFileName) {
        generateTables(inputAsmFileName);
        generateIntermediateCode(inputAsmFileName);
    }

    // generate symbol, literal, and pool tables
    private static void generateTables(String inputAsmFileName) {
        try {
            CustomFileReader reader = new CustomFileReader(inputAsmFileName);
            String word;
            boolean isStartInstructionSeen = false;

            while ((word = reader.readWord()) != null) {
                word = removeCommaFromEnd(word);
        
                MachineOpcodeTableEntry instructionDetails = MachineOpcodeTable.table.get(word.toLowerCase());

                // if the word is a symbol or a literal
                if (instructionDetails == null) {
                    // if the word is a symbol
                    if (isSymbol(word)) {
                        int symbolId = symbolTable.table.size() + 1;
                        // if symbol is already present, get its id
                        if (symbolTable.table.containsKey(word)) {
                            symbolId = symbolTable.table.get(word).id;
                        }
                        // if the word is a label (symbol appearing first in line)
                        if (reader.isFirstWordInLine) {
                            locationCounter++;
                            symbolTable.table.put(word, new SymbolTableEntry(symbolId, locationCounter));
                        } else if (!symbolTable.table.containsKey(word)) {
                            symbolTable.table.put(word, new SymbolTableEntry(symbolId, locationCounter));
                        }
                    }
                    // if the word is a literal
                    else if (isLiteral(word)) {
                        // if there are no pools and the literal is not present in the literal table
                        if (poolTable.table.size() == 0) {
                            if (!literalTable.contains(word)) {
                                literalTable.table.add(new LiteralTableEntry(literalTable.table.size() + 1, word));
                            }
                        } else if (!literalTable.containsInCurrentUndonePool(word, poolTable)) {
                        // if there are pools and the literal is not present in the current undone pool
                            literalTable.table.add(new LiteralTableEntry(literalTable.table.size() + 1, word));
                        }
                    }
                } else {
                    // if the word is a machine instruction
                    word = word.toLowerCase();
                    if (instructionDetails.instructionClass.equals("AD")) {
                        if (word.equals("start")) {
                            word = reader.readWord();
                            locationCounter = Integer.parseInt(word);
                            isStartInstructionSeen = true;
                        } else if (word.equals("origin")) {
                            word = reader.readWord();
                            // if the address is specified as a symbol
                            if (isSymbol(word)) {
                                // if an offset is specified
                                if (word.contains("+")) {
                                    String[] splitWords = word.split("\\+");
                                    locationCounter = symbolTable.table.get(splitWords[0]).address + Integer.parseInt(splitWords[1]);
                                } else {
                                    locationCounter = symbolTable.table.get(word).address;
                                }
                                locationCounter--;
                            } else {
                                // if the address is specified as a constant
                                locationCounter = Integer.parseInt(word);
                            }
                        } else if (word.equals("ltorg") || word.equals("end")) {
                            // assign addresses to literals in the current undone pool
                            // if the pool table is not empty (there is at least one pool)
                            if (poolTable.table.size() > 0) {
                                for (int i = poolTable.table.get(poolTable.table.size()).poolLength; i < literalTable.table.size(); i++) {
                                    locationCounter++;
                                    literalTable.table.get(i).address = locationCounter;
                                }
                                // make a new entry in the pool table
                                poolTable.table.put(poolTable.table.size() + 1, new PoolTableEntry(literalTable.table.size(), literalTable.table.size() - poolTable.table.get(poolTable.table.size()).poolLength));
                            } else {
                                // if the pool table is empty
                                // assign addresses to all literals in the literal table
                                for (int i = 0; i < literalTable.table.size(); i++) {
                                    locationCounter++;
                                    LiteralTableEntry oldEntry = literalTable.table.get(i);
                                    literalTable.table.set(i, new LiteralTableEntry(oldEntry.id, oldEntry.literal, locationCounter));
                                }
                                // make a new entry in the pool table
                                poolTable.table.put(1, new PoolTableEntry(1, literalTable.table.size()));
                            }

                            if (word.equals("end")) {
                                break;
                            }

                            // skip literals after ltorg, as they are already processed
                            ignoreLiteralsAfterLtorg(reader);
                        } else if (word.equals("equ")) {
                            locationCounter--; // decrement the location counter as the the word before equ is a symbol already present in the symbol table
                            String previousWord = reader.previousWord;
                            word = reader.readWord();
                            symbolTable.table.get(previousWord).address = symbolTable.table.get(word).address;
                        } else if (word.equals("ds")) {
                            locationCounter++;
                            String previousWord = reader.previousWord;
                            // allocate memory to the symbol
                            symbolTable.table.get(previousWord).address = locationCounter;
                            word = reader.readWord();
                        }
                    } else {
                        // if the instruction class is other than AD
                        if (isStartInstructionSeen) { // to correctly start at the location specified by the start instruction
                            locationCounter--;
                            isStartInstructionSeen = false;
                        }

                        if (reader.isFirstWordInLine) {
                            if (instructionDetails.instructionClass.equals("IS")) {
                                locationCounter++;
                            } else if (instructionDetails.instructionClass.equals("DL")) {
                                locationCounter++;
                            }   
                        }
                    }
                }
            }
            reader.close();

            writeToFile("output/literal_table.txt", literalTable.toString());
            writeToFile("output/symbol_table.txt", symbolTable.toString());
            writeToFile("output/pool_table.txt", poolTable.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // generate intermediate code
    private static void generateIntermediateCode(String inputAsmFileName) {
        try {
            CustomFileReader reader = new CustomFileReader(inputAsmFileName);
            String word;
            StringBuffer intermediateCode = new StringBuffer();
            int currentPoolNumber = 1;  // starts from 1 (not zero based)

            while ((word = reader.readWord()) != null) {
                word = removeCommaFromEnd(word);

                if (reader.isFirstWordInLine) intermediateCode.append("\n");
                
                MachineOpcodeTableEntry instructionDetails = MachineOpcodeTable.table.get(word.toLowerCase());

                // if the word is not a machine instruction
                if (instructionDetails == null) {
                    // if the word is a symbol or a label, and is not the first word in line
                    if (isSymbol(word) && !reader.isFirstWordInLine) {
                        int symbolId = symbolTable.table.get(word).id;
                        intermediateCode.append("(S, " + symbolId + ") ");
                    } else if (isLiteral(word)) {
                        int literalId = literalTable.getLiteralId(word, currentPoolNumber, poolTable);
                        intermediateCode.append("(L, " + literalId + ") ");
                    } else if (isConstant(word)) {
                        intermediateCode.append("(C, " + word + ") ");
                    }
                } else {
                    // if the word is a machine instruction
                    word = word.toLowerCase();

                    if (instructionDetails.instructionClass.equals("RG")) {
                        intermediateCode.append("(" + instructionDetails.opcode + ") ");
                    } else if (instructionDetails.instructionClass.equals("AD")) {
                        intermediateCode.append("(" + instructionDetails.instructionClass + ", " + instructionDetails.opcode + ") ");

                        if (word.equals("ltorg")) {
                            currentPoolNumber++; // increment the current pool number so that literals can be searched in the right pool
                            
                            // ignore literals after ltorg
                            ignoreLiteralsAfterLtorg(reader);
                        } else if (word.equals("origin")) {
                            word = reader.readWord();
                        } else if (word.equals("end")) {
                            break;
                        }
                    } else {
                        intermediateCode.append("(" + instructionDetails.instructionClass + ", " + instructionDetails.opcode + ") ");
                    }
                }
            }

            intermediateCode.deleteCharAt(0); // delete the newline added at the beginning
            writeToFile("output/intermediate_code.txt", intermediateCode.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ignoreLiteralsAfterLtorg(CustomFileReader reader) throws IOException {
        // ignore literals after ltorg
        String tempWord = reader.readWord();

        while (tempWord != null && tempWord.startsWith("='")) {
            tempWord = reader.readWord();
        }
        
        // the word after the last literal is read - put it back in the queue beginning, and set isFirstWordInLine to true so that isFirstWordInLine is retained
        reader.addWordToQueueBeginning(tempWord);
        reader.isFirstWordInLine = true;
        reader.updateIsFirstWordInLine = false;
    }

    private static void writeToFile(String filename, String content) {
        try {
            File file = new File(filename);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // create the directory if it does not exist
            }
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isSymbol(String word) {
        if (Character.isLetter(word.charAt(0)) || word.charAt(0) == '_') {
            return true;
        }
        return false;
    }

    private static boolean isLiteral(String word) {
        if (word.startsWith("='") && word.endsWith("'")) {
            return true;
        }
        return false;
    }

    private static boolean isConstant(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isDigit(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String removeCommaFromEnd(String word) {
        if (word.endsWith(",")) {
            word = word.substring(0, word.length() - 1);
        }
        return word;
    }
}
