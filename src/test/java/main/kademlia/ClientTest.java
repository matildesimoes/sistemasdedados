
package test.main.kademlia;

import main.kademlia.Client;
import main.kademlia.Communication;
import main.kademlia.Node;
import main.kademlia.Communication.MessageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    private Node senderNode;
    private Node receiverNode;
    private Client client;

    @BeforeEach
    public void setUp() {
        senderNode = new Node("127.0.0.1", 8030);
        receiverNode = new Node("127.0.0.1", 8031);
        client = new Client(senderNode);
    }

}

