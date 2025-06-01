import java.util.HashMap;

class SymbolTable {
    // symbol mapped to (id, address)
    // (id is the serial number)
    public HashMap<String, SymbolTableEntry> table = new HashMap<>();

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Symbol Table:\n");
        buffer.append(String.format("%-4s %-10s %-7s\n", "ID", "Symbol", "Address"));
        // sort the table in ascending order of id before printing
        table.entrySet().stream()
            .sorted((entry1, entry2) -> entry1.getValue().id - entry2.getValue().id)
            .forEach(entry -> {
                buffer.append(String.format("%-4d %-10s %-7d\n",
                    entry.getValue().id, entry.getKey(), entry.getValue().address));
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
