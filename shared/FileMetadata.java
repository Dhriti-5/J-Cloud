package shared;

public class FileMetadata {

    private int fileId;
    private String fileName;
    private long fileSize;
    private int ownerId;

    public FileMetadata() {}

    public FileMetadata(String fileName, long fileSize, int ownerId) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.ownerId = ownerId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "fileId=" + fileId +
                ", fileName='" + fileName + "'" +
                ", fileSize=" + fileSize +
                ", ownerId=" + ownerId +
                '}';
    }
}