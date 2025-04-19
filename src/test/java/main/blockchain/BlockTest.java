package test.main.blockchain;

import main.blockchain.Block;
import main.blockchain.Transaction;
import main.kademlia.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockTest {

    private Node creator;

    @BeforeEach
    public void setUp() {
        creator = new Node("127.0.0.1", 8040);
    }

    @Test
    public void testBlockWithoutPrevHash() {
        Transaction t1 = new Transaction(Transaction.Type.REWARD, creator, 0, "item1");
        Transaction t2 = new Transaction(Transaction.Type.REWARD, creator, 0, "item2");

        Block block = new Block(List.of(t1, t2));

        assertNotNull(block.getBlockHeader());
        assertEquals(0, block.getBlockHeader().getNounce());
        assertNull(block.getBlockHeader().getPrevHash());
        assertNotNull(block.getBlockHeader().getMerkleRoot());
    }

    @Test
    public void testBlockWithPrevHashAndNounce() {
        Transaction t = new Transaction(Transaction.Type.REWARD, creator, 1, "itemX");
        String prevHash = "abc123";
        int nounce = 42;

        Block block = new Block(List.of(t), nounce, prevHash);

        assertEquals(prevHash, block.getBlockHeader().getPrevHash());
        assertEquals(nounce, block.getBlockHeader().getNounce());
        assertNotNull(block.getBlockHeader().getMerkleRoot());
    }

    @Test
    public void testGetTransactions() {
        Transaction t = new Transaction(Transaction.Type.REWARD, creator, 2, "x");
        Block block = new Block(List.of(t));

        List<Transaction> tx = block.getTransaction();
        assertEquals(1, tx.size());
        assertEquals(t.getSignature(), tx.get(0).getSignature());
    }
}

