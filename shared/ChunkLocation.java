package shared;

public class ChunkLocation {

    private int chunkId;
    private int nodeId;

    public ChunkLocation() {}

    public ChunkLocation(int chunkId, int nodeId) {
        this.chunkId = chunkId;
        this.nodeId = nodeId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        return "ChunkLocation{" +
                "chunkId=" + chunkId +
                ", nodeId=" + nodeId +
                '}';
    }
}