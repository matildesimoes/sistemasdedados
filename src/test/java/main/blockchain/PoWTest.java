package test.main.blockchain;

import main.blockchain.BlockHeader;
import main.Utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class PoWTest {

    @Test
    public void testProofOfWorkMining() {
        String prevHash = "abc123";
        String merkleRoot = "merkleXYZ";
        int difficulty = 3; 

        BlockHeader header = new BlockHeader(prevHash, 0, merkleRoot);

        int nounce = 0;
        String hash;
        String prefix = "0".repeat(difficulty);

        do {
            header.setNounce(nounce);
            String data = header.getPrevHash() + header.getMerkleRoot() + header.getTimestamp() + nounce;
            hash = Utils.hashSHA256(data);
            nounce++;
        } while (!hash.startsWith(prefix));

        header.setHash(hash);

        System.out.println("PoW with nounce: " + header.getNounce());
        System.out.println("Hash found: " + header.getHash());

        assertTrue(header.getHash().startsWith(prefix));
    }
}

