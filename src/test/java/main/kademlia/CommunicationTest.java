
package test.main.kademlia;

import main.kademlia.Communication;
import main.kademlia.Communication.MessageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommunicationTest {

    private Communication communication;
    private String[] sender;
    private String[] receiver;

    @BeforeEach
    public void setUp() {
        sender = new String[]{"127.0.0.1", "8001", "nodeID1"};
        receiver = new String[]{"127.0.0.1", "8002", "nodeID2"};
        communication = new Communication(MessageType.PING, "test message", sender, receiver);
    }

    @Test
    public void testGetType() {
        assertEquals(MessageType.PING, communication.getType());
    }

    @Test
    public void testGetInformation() {
        assertEquals("test message", communication.getInformation());
    }

    @Test
    public void testGetSender() {
        assertArrayEquals(sender, communication.getSender());
    }

    @Test
    public void testGetReceiver() {
        assertArrayEquals(receiver, communication.getReceiver());
    }
}

