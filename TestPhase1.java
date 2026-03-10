import utils.DBConnection;
import dao.UserDAO;
import dao.NodeDAO;
import shared.User;
import shared.NodeInfo;

/**
 * Test class to verify Phase 1 implementation
 * Tests database connection, UserDAO, and NodeDAO
 */
public class TestPhase1 {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  J-CLOUD PHASE 1 DATABASE TEST");
        System.out.println("========================================\n");

        // Test 1: Database Connection
        System.out.println("Test 1: Database Connection");
        System.out.println("----------------------------");
        try {
            DBConnection dbConn = DBConnection.getInstance();
            if (dbConn.getConnection() != null) {
                System.out.println("✓ Database connection successful!\n");
            }
        } catch (Exception e) {
            System.err.println("✗ Database connection failed!");
            e.printStackTrace();
            return;
        }

        // Test 2: User Registration
        System.out.println("Test 2: User Registration");
        System.out.println("----------------------------");
        UserDAO userDAO = new UserDAO();
        User testUser = new User("testuser123", "password123", "test@jcloud.com");
        
        if (userDAO.registerUser(testUser)) {
            System.out.println("✓ User registered with ID: " + testUser.getUserId() + "\n");
        } else {
            System.out.println("⚠ User registration failed (may already exist)\n");
        }

        // Test 3: User Authentication
        System.out.println("Test 3: User Authentication");
        System.out.println("----------------------------");
        User authenticatedUser = userDAO.authenticateUser("testuser123", "password123");
        
        if (authenticatedUser != null) {
            System.out.println("✓ Authentication successful!");
            System.out.println("  User ID: " + authenticatedUser.getUserId());
            System.out.println("  Username: " + authenticatedUser.getUsername());
            System.out.println("  Email: " + authenticatedUser.getEmail() + "\n");
        } else {
            System.out.println("✗ Authentication failed\n");
        }

        // Test 4: Node Registration
        System.out.println("Test 4: Node Registration");
        System.out.println("----------------------------");
        NodeDAO nodeDAO = new NodeDAO();
        
        NodeInfo node1 = new NodeInfo("DataNode1", "localhost", 9101, 10737418240L);
        NodeInfo node2 = new NodeInfo("DataNode2", "localhost", 9102, 10737418240L);
        
        nodeDAO.registerNode(node1);
        nodeDAO.registerNode(node2);
        System.out.println();

        // Test 5: Fetch All Nodes
        System.out.println("Test 5: Fetch All Nodes");
        System.out.println("----------------------------");
        var allNodes = nodeDAO.getAllNodes();
        System.out.println("Total nodes in database: " + allNodes.size());
        
        for (NodeInfo node : allNodes) {
            System.out.println("  • " + node.getNodeName() + " (" + node.getIpAddress() + 
                               ":" + node.getPort() + ") - Status: " + node.getStatus());
        }
        System.out.println();

        // Test 6: Update Node Status
        System.out.println("Test 6: Update Node Status");
        System.out.println("----------------------------");
        if (node1.getNodeId() > 0) {
            nodeDAO.updateNodeStatus(node1.getNodeId(), "ACTIVE");
        }
        System.out.println();

        System.out.println("========================================");
        System.out.println("  PHASE 1 TESTS COMPLETED");
        System.out.println("========================================");
    }
}
