import main.kademlia.Node;
import main.kademlia.Bucket;
import main.kademlia.RoutingTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.security.PrivateKey;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {

    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Node("127.0.0.1", 8080);
    }

    @Test
    public void testGetNodeId_NotNull() {
        assertNotNull(node.getNodeId());
    }

    @Test
    public void testGetNodeIp() {
        assertEquals("127.0.0.1", node.getNodeIp());
    }

    @Test
    public void testGetNodePort() {
        assertEquals(8080, node.getNodePort());
    }

    @Test
    public void testGetPublicKey_NotNull() {
        PublicKey publicKey = node.getPublicKey();
        assertNotNull(publicKey);
    }

    @Test
    public void testGetPrivateKey_NotNull() {
        PrivateKey privateKey = node.getPrivateKey();
        assertNotNull(privateKey);
    }

    @Test
    public void testRoutingTable_NotNull() {
        RoutingTable routingTable = node.getRoutingTable();
        assertNotNull(routingTable);
    }

    @Test
    public void testSetAndGetTimeAlive() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        node.setTimeAlive(now);
        assertEquals(now, node.getTimeAlive());
    }

}

