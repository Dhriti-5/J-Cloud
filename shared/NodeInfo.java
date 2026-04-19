package shared;

public class NodeInfo {

    private int nodeId;
    private String nodeName;
    private String ipAddress;
    private int port;
    private String status;
    private long storageCapacity;

    public NodeInfo(){}

    public NodeInfo(String nodeName,String ipAddress,int port,long storageCapacity){
        this.nodeName=nodeName;
        this.ipAddress=ipAddress;
        this.port=port;
        this.storageCapacity=storageCapacity;
    }

    // Getters and Setters
    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStorageCapacity() {
        return storageCapacity;
    }

    public void setStorageCapacity(long storageCapacity) {
        this.storageCapacity = storageCapacity;
    }
}