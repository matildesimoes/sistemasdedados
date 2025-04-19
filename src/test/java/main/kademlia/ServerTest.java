
package test.main.kademlia;

import main.kademlia.Node;
import main.kademlia.Server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {

    private Server server;
    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Node("127.0.0.1", 8020);
        server = new Server("127.0.0.1", 8020, node.getRoutingTable(), node);
    }

    @Test
    public void testServerInitialization() {
        assertNotNull(server);
    }

    @Test
    public void testServerAddressBinding() {
        assertEquals("127.0.0.1", node.getNodeIp());
        assertEquals(8020, node.getNodePort());
    }

}

