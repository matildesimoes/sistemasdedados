package test.main.blockchain;

import main.blockchain.*;
import main.kademlia.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockchainTest {

    private Blockchain blockchain;
    private Node creator;

    @BeforeEach
    public void setUp() {
        blockchain = new Blockchain();
        blockchain = blockchain.createNewBlockchain();
        creator = new Node("127.0.0.1", 5000);
    }

    @Test
    public void testGenesisBlockExists() {
        List<Chain> chains = blockchain.getChains();
        assertEquals(1, chains.size());
        assertEquals(1, chains.get(0).size());
    }

    @Test
    public void testCreateBlockAddsToChain() {
        Transaction tx = new Transaction(Transaction.Type.REWARD, creator, 1, "test");
        blockchain.createBlock(List.of(tx));
        List<Chain> chains = blockchain.getChains();
        assertTrue(chains.get(0).size() > 1);
    }

    @Test
    public void testBlockStorageCreatesNewChainIfNeeded() {
        Chain original = blockchain.getChains().get(0);
        Block latestBlock = original.getLatest();

        // Create a forked block with same prevHash (forces createNewChain)
        Block forkBlock = new Block(List.of(), 0, latestBlock.getBlockHeader().getPrevHash());
        forkBlock.getBlockHeader().setHash(main.Utils.hashSHA256(forkBlock.getBlockHeader()));

        blockchain.storeBlock(forkBlock);

        assertTrue(blockchain.getChains().size() > 1);
    }

    @Test
    public void testSerializationAndDeserialization() {
        List<Chain> originalChains = blockchain.getChains();
        String json = Blockchain.blockchainToString(originalChains);
        List<Chain> loadedChains = Blockchain.blockchainFromString(json);

        assertNotNull(loadedChains);
        assertEquals(originalChains.size(), loadedChains.size());

        for (int i = 0; i < originalChains.size(); i++) {
            Chain original = originalChains.get(i);
            Chain loaded = loadedChains.get(i);

            assertEquals(original.size(), loaded.size());

            for (int j = 0; j < original.size(); j++) {
                Block originalBlock = original.getBlock(j);
                Block loadedBlock = loaded.getBlock(j);

                // Compare basic block info
                assertEquals(originalBlock.getBlockHeader().getHash(), loadedBlock.getBlockHeader().getHash());
                assertEquals(originalBlock.getBlockHeader().getPrevHash(), loadedBlock.getBlockHeader().getPrevHash());
                assertEquals(originalBlock.getBlockHeader().getMerkleRoot(), loadedBlock.getBlockHeader().getMerkleRoot());
                assertEquals(
                    originalBlock.getBlockHeader().getTimestamp().getTime(),
                    loadedBlock.getBlockHeader().getTimestamp().getTime()
                );
                assertEquals(originalBlock.getBlockHeader().getNounce(), loadedBlock.getBlockHeader().getNounce());

                // Compare transactions
                List<Transaction> originalTx = originalBlock.getTransaction();
                List<Transaction> loadedTx = loadedBlock.getTransaction();
                assertEquals(originalTx.size(), loadedTx.size());

                for (int k = 0; k < originalTx.size(); k++) {
                    Transaction t1 = originalTx.get(k);
                    Transaction t2 = loadedTx.get(k);

                    assertEquals(t1.getType(), t2.getType());
                    assertEquals(t1.getAuctionNumber(), t2.getAuctionNumber());
                    assertEquals(t1.getInformation(), t2.getInformation());
                    assertEquals(t1.getSignature(), t2.getSignature());
                    assertEquals(t1.getTimestamp().getTime(), t2.getTimestamp().getTime());

                    assertNotNull(t1.getCreator());
                    assertNotNull(t2.getCreator());
                    assertEquals(t1.getCreator().getNodeId(), t2.getCreator().getNodeId());

                }
            }
        }
    }


}

