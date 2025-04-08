package main.blockchain;

import main.auctions.User;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BlockTest {

    @Test
    public void testBlockWithoutPrevHash() {
        User buyer = new User();
        User seller = new User();

        Transaction t1 = new Transaction(buyer, seller, "item1");
        Transaction t2 = new Transaction(buyer, seller, "item2");

        Block block = new Block(List.of(t1, t2));

        assertNotNull(block.getBlockHeader());
        assertEquals(0, block.getBlockHeader().getNounce());
        assertNull(block.getBlockHeader().getPrevHash());
        assertNotNull(block.getBlockHeader().getMerkleRoot());
    }

    @Test
    public void testBlockWithPrevHashAndNounce() {
        User buyer = new User();
        User seller = new User();

        Transaction t = new Transaction(buyer, seller, "itemX");
        String prevHash = "abc123";
        int nounce = 42;

        Block block = new Block(List.of(t), nounce, prevHash);

        assertEquals(prevHash, block.getBlockHeader().getPrevHash());
        assertEquals(nounce, block.getBlockHeader().getNounce());
        assertNotNull(block.getBlockHeader().getMerkleRoot());
    }

    @Test
    public void testGetTransactions() {
        User buyer = new User();
        User seller = new User();

        Transaction t = new Transaction(buyer, seller, "x");
        Block block = new Block(List.of(t));

        List<Transaction> tx = block.getTransaction();
        assertEquals(1, tx.size());
        assertEquals(t.signature, tx.get(0).signature);
    }
}

