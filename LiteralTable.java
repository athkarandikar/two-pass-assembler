import java.util.ArrayList;

class LiteralTable {
    // (id is the serial number)

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

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Literal Table:\n");
        buffer.append(String.format("%-4s %-10s %-7s\n", "ID", "Literal", "Address"));
        for (LiteralTableEntry entry : table) {
            buffer.append(String.format("%-4d %-10s %-7d\n", entry.id, entry.literal, entry.address));
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
