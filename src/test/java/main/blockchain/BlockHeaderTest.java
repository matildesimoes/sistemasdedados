package test.main.blockchain;

import main.blockchain.BlockHeader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;

public class BlockHeaderTest {

    @Test
    public void testBlockHeaderInitialization() {
        String prevHash = "abc123";
        int nounce = 42;
        String merkleRoot = "merkle123";

        BlockHeader header = new BlockHeader(prevHash, nounce, merkleRoot);

        assertEquals(prevHash, header.getPrevHash());
        assertEquals(nounce, header.getNounce());
        assertEquals(merkleRoot, header.getMerkleRoot());
        assertNotNull(header.getTimestamp());
        assertNull(header.getHash());
    }

    @Test
    public void testNounceSetter() {
        BlockHeader header = new BlockHeader("prev", 0, "merkle");
        header.setNounce(99);
        assertEquals(99, header.getNounce());
    }

    @Test
    public void testHashSetterGetter() {
        BlockHeader header = new BlockHeader("prev", 0, "merkle");
        String hash = "fakehashvalue";
        header.setHash(hash);
        assertEquals(hash, header.getHash());
    }
}

