package test.main.blockchain;

import main.blockchain.Block;
import main.blockchain.Chain;
import main.blockchain.Transaction;
import main.kademlia.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ChainTest {

    private Chain chain;
    private Node creator;

    @BeforeEach
    public void setUp() {
        chain = new Chain();
        creator = new Node("127.0.0.1", 8070);
    }

    @Test
    public void testAddAndGetBlock() {
        Transaction tx = new Transaction(Transaction.Type.REWARD, creator, 1, "test");
        Block block = new Block(List.of(tx));
        chain.addCompletedBlock(block);

        assertEquals(1, chain.size());
        assertEquals(block, chain.getBlock(0));
        assertEquals(block, chain.getLatest());
    }

    @Test
    public void testRemoveBlock() {
        Transaction tx1 = new Transaction(Transaction.Type.REWARD, creator, 1, "block1");
        Transaction tx2 = new Transaction(Transaction.Type.REWARD, creator, 2, "block2");

        Block b1 = new Block(List.of(tx1));
        Block b2 = new Block(List.of(tx2));

        chain.addCompletedBlock(b1);
        chain.addCompletedBlock(b2);

        assertEquals(2, chain.size());

        chain.removeBlock(0);

        assertEquals(1, chain.size());
        assertEquals(b2, chain.getBlock(0));
    }

    @Test
    public void testEmptyChainAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            chain.getBlock(0);
        });
    }
}

