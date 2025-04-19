package test.main.blockchain;

import main.blockchain.Transaction;
import main.kademlia.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    private Node creator;

    @BeforeEach
    public void setUp() {
        creator = new Node("127.0.0.1", 8050);
    }

    @Test
    public void testTransactionCreation() {
        Transaction tx = new Transaction(Transaction.Type.REWARD, creator, 1, "Auction #123");

        assertNotNull(tx.getSignature(), "Signature should not be null");
        assertNotNull(tx.getTimestamp(), "Timestamp should not be null");
        assertTrue(tx.getSignature().length() > 10, "Signature should have reasonable length");
        assertEquals(Transaction.Type.REWARD, tx.getType());
        assertEquals(1, tx.getAuctionNumber());
        assertEquals("Auction #123", tx.getInformation());
    }
}

