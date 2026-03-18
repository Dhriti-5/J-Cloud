package shared;

public class Chunk {

    private int chunkId;
    private int fileId;
    private int chunkIndex;
    private int chunkSize;

    public Chunk() {}

    public Chunk(int fileId, int chunkIndex, int chunkSize) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "chunkId=" + chunkId +
                ", fileId=" + fileId +
                ", chunkIndex=" + chunkIndex +
                ", chunkSize=" + chunkSize +
                '}';
    }
}