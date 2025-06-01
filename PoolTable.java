import java.util.HashMap;

class PoolTable {
    // id (serial number) mapped to PoolTableEntry
    public HashMap<Integer, PoolTableEntry> table = new HashMap<>();

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Pool Table:\n");
        buffer.append(String.format("%-4s %-8s %-11s\n", "ID", "Lit. ID", "Pool Length"));
        for (int key : table.keySet()) {
            PoolTableEntry entry = table.get(key);
            buffer.append(String.format("%-4d %-8d %-11d\n", key, entry.literalId, entry.poolLength));
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
