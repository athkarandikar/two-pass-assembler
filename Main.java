import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Assembler.work("input/assembly_code.asm");
    }
}

class Assembler {
    public static int locationCounter = 0;
    private static SymbolTable symbolTable = new SymbolTable();
    private static LiteralTable literalTable = new LiteralTable();
    private static PoolTable poolTable = new PoolTable();

    public static void work(String inputAsmFileName) {
        // generate symbol, literal, and pool tables
        performPass1(inputAsmFileName);
        // generate intermediate code
        performPass2(inputAsmFileName);
    }

    // generate symbol, literal, and pool tables
    private static void performPass1(String inputAsmFileName) {
        try {
            FileWordReader reader = new FileWordReader(inputAsmFileName);
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
    private static void performPass2(String inputAsmFileName) {
        try {
            FileWordReader reader = new FileWordReader(inputAsmFileName);
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

    private static void ignoreLiteralsAfterLtorg(FileWordReader reader) throws IOException {
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

class FileWordReader {
    private BufferedReader reader;
    // private StringTokenizer tokenizer;
    private Queue<String> tokens;
    public String word;
    public String previousWord;
    public boolean isFirstWordInLine = false;
    public boolean updateIsFirstWordInLine = true; // whether to set isFirstWordInLine to true or not. This is required when the word after the last literal is read in ltorg, and put back into the tokens queue. This retains the isFirstWordInLine value.

    // read the input file word by word
    public FileWordReader(String filename) throws IOException {
        reader = new BufferedReader(new java.io.FileReader(filename));
        tokens = new LinkedList<>();
    }

    public String readWord() throws IOException {
        if (tokens.isEmpty()) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return null; // end of file
            }
            isFirstWordInLine = true; // Set to true when a new line is read
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.isBlank()) {
                    tokens.add(word);
                }
            }
            previousWord = null;
            word = tokens.poll();
            return word;
        }
        previousWord = word;
        word = tokens.poll();
        if (updateIsFirstWordInLine && isFirstWordInLine) {
            isFirstWordInLine = false; // Reset after the first word in the line is read
        }
        updateIsFirstWordInLine = true;
        return word;
    }

    public void addWordToQueueBeginning(String word) {
        ((LinkedList<String>) tokens).addFirst(word);
    }

    public void close() throws IOException {
        reader.close();
    }
}

class LiteralTable {
    // // literal mapped to (id, address)
    // // (id is the serial number)
    // public static HashMap<String, LiteralTableEntry> table = new HashMap<>();

    public ArrayList<LiteralTableEntry> table = new ArrayList<>();

    public boolean contains(String literal) {
        for (LiteralTableEntry entry : table) {
            if (entry.literal.equals(literal)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsInCurrentUndonePool(String literal, PoolTable poolTable) {
        int indexLiteralTableSearchStart = poolTable.table.get(poolTable.table.size()).literalId + poolTable.table.get(poolTable.table.size()).poolLength;
        int indexLiteralTableSearchEnd = table.size();

        for (int i = indexLiteralTableSearchStart; i < indexLiteralTableSearchEnd; i++) {
            if (table.get(i-1).literal.equals(literal)) {
                return true;
            }
        }
        return false;
    }

    public int getLiteralId(String literal, int currentPoolNumber, PoolTable poolTable) {
        int literalIdFirstInPool = poolTable.table.get(currentPoolNumber).literalId;
        int lengthCurrentPool = poolTable.table.get(currentPoolNumber).poolLength;

        for (int i = literalIdFirstInPool - 1; i < (literalIdFirstInPool + lengthCurrentPool - 1); i++) {
            LiteralTableEntry entry = table.get(i);
            if (entry.literal.equals(literal)) {
                return entry.id;
            }
        }
        return -1;
    }

    public void print() {
        System.out.println("Literal Table:");
        System.out.println("ID\tLiteral\tAddress");
        for (LiteralTableEntry entry : table) {
            System.out.println(entry.id + "\t" + entry.literal + "\t" + entry.address);
        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Literal Table:\n");
        buffer.append("ID\tLiteral\tAddress\n");
        for (LiteralTableEntry entry : table) {
            buffer.append(entry.id + "\t" + entry.literal + "\t" + entry.address + "\n");
        }
        return buffer.toString();
    }
}

class LiteralTableEntry {
    int id;
    String literal;
    int address;

    public LiteralTableEntry(int id, String literal, int address) {
        this.id = id;
        this.literal = literal;
        this.address = address;
    }

    public LiteralTableEntry(int id, String literal) {
        this.id = id;
        this.literal = literal;
        this.address = -1;
    }
}

class SymbolTable {
    // symbol mapped to (id, address)
    // (id is the serial number)
    public HashMap<String, SymbolTableEntry> table = new HashMap<>();

    public void print() {
        System.out.println("Symbol Table:");
        System.out.println("ID\tSymbol\tAddress");
        // sort the table in ascending order of id before printing
        table.entrySet().stream().sorted((entry1, entry2) -> entry1.getValue().id - entry2.getValue().id).forEach(entry -> {
            System.out.println(entry.getValue().id + "\t" + entry.getKey() + "\t" + entry.getValue().address);
        });
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Symbol Table:\n");
        buffer.append("ID, Symbol, Address\n");
        // sort the table in ascending order of id before printing
        table.entrySet().stream().sorted((entry1, entry2) -> entry1.getValue().id - entry2.getValue().id).forEach(entry -> {
            buffer.append(entry.getValue().id + ", " + entry.getKey() + ", " + entry.getValue().address + "\n");
        });
        return buffer.toString();
    }
}

class SymbolTableEntry {
    int id;
    int address;

    public SymbolTableEntry(int id) {
        this.id = id;
        this.address = -1;
    }

    public SymbolTableEntry(int id, int address) {
        this.id = id;
        this.address = address;
    }
}

class PoolTable {
    // id (serial number) mapped to PoolTableEntry
    public HashMap<Integer, PoolTableEntry> table = new HashMap<>();

    public void print() {
        System.out.println("Pool Table:");
        System.out.println("ID\tLit. ID\tPool Length");
        for (int key : table.keySet()) {
            PoolTableEntry entry = table.get(key);
            System.out.println(key + "\t" + entry.literalId + "\t" + entry.poolLength);
        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Pool Table:\n");
        buffer.append("ID\tLit. ID\tPool Length\n");
        for (int key : table.keySet()) {
            PoolTableEntry entry = table.get(key);
            buffer.append(key + "\t" + entry.literalId + "\t" + entry.poolLength + "\n");
        }
        return buffer.toString();
    }
}

class PoolTableEntry {
    int literalId; // literal id in the literal table
    int poolLength; // number of literals in the pool

    public PoolTableEntry(int literalId, int poolLength) {
        this.literalId = literalId;
        this.poolLength = poolLength;
    }
}

class MachineOpcodeTable {
    /*
        IS - imperative statement
        AD - assembler directive
        DL - declarative statement
        RG - register
        CC - condition code
    */
    public static HashMap<String, MachineOpcodeTableEntry> table = new HashMap<>(29);

    static {
        table.put("stop", new MachineOpcodeTableEntry("IS", 0));
        table.put("add", new MachineOpcodeTableEntry("IS", 1));
        table.put("sub", new MachineOpcodeTableEntry("IS", 2));
        table.put("mult", new MachineOpcodeTableEntry("IS", 3));
        table.put("mover", new MachineOpcodeTableEntry("IS", 4));
        table.put("movem", new MachineOpcodeTableEntry("IS", 5));
        table.put("comp", new MachineOpcodeTableEntry("IS", 6));
        table.put("bc", new MachineOpcodeTableEntry("IS", 7));
        table.put("div", new MachineOpcodeTableEntry("IS", 8));
        table.put("read", new MachineOpcodeTableEntry("IS", 9));
        table.put("print", new MachineOpcodeTableEntry("IS", 10));
        table.put("load", new MachineOpcodeTableEntry("IS", 11));
        table.put("start", new MachineOpcodeTableEntry("AD", 1));
        table.put("end", new MachineOpcodeTableEntry("AD", 2));
        table.put("origin", new MachineOpcodeTableEntry("AD", 3));
        table.put("equ", new MachineOpcodeTableEntry("AD", 4));
        table.put("ltorg", new MachineOpcodeTableEntry("AD", 5));
        table.put("ds", new MachineOpcodeTableEntry("DL", 1));
        table.put("dc", new MachineOpcodeTableEntry("DL", 2));
        table.put("areg", new MachineOpcodeTableEntry("RG", 1));
        table.put("breg", new MachineOpcodeTableEntry("RG", 2));
        table.put("creg", new MachineOpcodeTableEntry("RG", 3));
        table.put("dreg", new MachineOpcodeTableEntry("RG", 4));
        table.put("eq", new MachineOpcodeTableEntry("CC", 1));
        table.put("lt", new MachineOpcodeTableEntry("CC", 2));
        table.put("gt", new MachineOpcodeTableEntry("CC", 3));
        table.put("le", new MachineOpcodeTableEntry("CC", 4));
        table.put("ge", new MachineOpcodeTableEntry("CC", 5));
        table.put("any", new MachineOpcodeTableEntry("CC", 6));
    }
}

class MachineOpcodeTableEntry {
    String instructionClass;
    int opcode;

    public MachineOpcodeTableEntry(String instructionClass, int opcode) {
        this.instructionClass = instructionClass;
        this.opcode = opcode;
    }
}