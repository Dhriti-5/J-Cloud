public class Chunk {

    private int chunkId;
    private int fileId;
    private int chunkIndex;
    private int chunkSize;

    public Chunk(){}

    public Chunk(int fileId,int chunkIndex,int chunkSize){
        this.fileId=fileId;
        this.chunkIndex=chunkIndex;
        this.chunkSize=chunkSize;
    }

}