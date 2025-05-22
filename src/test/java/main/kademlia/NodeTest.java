import main.kademlia.Node;
import main.kademlia.Bucket;
import main.kademlia.RoutingTable;
import main.blockchain.Transaction;
import main.blockchain.Blockchain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.security.PrivateKey;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {

    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Node("127.0.0.1", 8080);
    }

       @Test
    public void testGetters() {
        assertEquals("127.0.0.1", node.getNodeIp());
        assertEquals(8080, node.getNodePort());
        assertNotNull(node.getNodeId());
        assertNotNull(node.getRoutingTable());
        assertNotNull(node.getPublicKey());
        assertNotNull(node.getPrivateKey());
    }

    @Test
    public void testTimeAliveSetterGetter() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        node.setTimeAlive(String.valueOf(now));
        assertEquals(now, node.getTimeAlive());
    }


    @Test
    public void testTransactionPoolOperations() {
        Transaction.Type type = Transaction.Type.REWARD; 
        int auctionNumber = 0;
        String info = "Test info";

        Transaction tx = new Transaction(type, node, auctionNumber, info);

        node.addToTransactionPool(tx);
        assertEquals(1, node.transactionPoolSize());
        assertFalse(node.isTransactionPoolFull()); 
        List<Transaction> pool = node.getTransactionPool();
        assertEquals(1, pool.size());
        assertEquals(tx, pool.get(0));
    }


}

