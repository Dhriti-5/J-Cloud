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

}