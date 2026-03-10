package shared;

public class FileMetadata {

    private int fileId;
    private String fileName;
    private long fileSize;
    private int ownerId;

    public FileMetadata(){}

    public FileMetadata(String fileName,long fileSize,int ownerId){
        this.fileName=fileName;
        this.fileSize=fileSize;
        this.ownerId=ownerId;
    }

}