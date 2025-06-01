import java.util.HashMap;

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
