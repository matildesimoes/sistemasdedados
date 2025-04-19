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
        blockchain = Blockchain.createNewBlockchain();
        creator = new Node("127.0.0.1", 8080);
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
        String json = Blockchain.blockchainToString(blockchain.getChains());
        List<Chain> loadedChains = Blockchain.blockchainFromString(json);

        assertNotNull(loadedChains);
        assertEquals(blockchain.getChains().size(), loadedChains.size());
    }
}

