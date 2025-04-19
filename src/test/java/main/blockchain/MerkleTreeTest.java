package test.main.blockchain;

import main.blockchain.MerkleTree;
import main.blockchain.Transaction;
import main.kademlia.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class MerkleTreeTest {

    private Node creator;

    @BeforeEach
    public void setUp() {
        creator = new Node("127.0.0.1", 8060);
    }

    @Test
    public void testEmptyTransactionList() {
        List<Transaction> transactions = Collections.emptyList();
        String merkleRoot = MerkleTree.getMerkleRoot(transactions);
        assertEquals("", merkleRoot);
    }

    @Test
    public void testSingleTransaction() {
        Transaction tx = new Transaction(Transaction.Type.REWARD, creator, 1, "tx1");
        String merkleRoot = MerkleTree.getMerkleRoot(List.of(tx));

        printMerkleTree(List.of(tx));

        assertNotNull(merkleRoot);
        assertFalse(merkleRoot.isEmpty());
    }

    @Test
    public void testMultipleTransactions() {
        Transaction tx1 = new Transaction(Transaction.Type.REWARD, creator, 1, "tx1");
        Transaction tx2 = new Transaction(Transaction.Type.REWARD, creator, 2, "tx2");
        Transaction tx3 = new Transaction(Transaction.Type.REWARD, creator, 3, "tx3");

        List<Transaction> transactions = Arrays.asList(tx1, tx2, tx3);
        String merkleRoot = MerkleTree.getMerkleRoot(transactions);

        printMerkleTree(transactions);

        assertNotNull(merkleRoot);
        assertFalse(merkleRoot.isEmpty());
    }

    @Test
    public void testEvenNumberOfTransactions() {
        Transaction tx1 = new Transaction(Transaction.Type.REWARD, creator, 1, "tx1");
        Transaction tx2 = new Transaction(Transaction.Type.REWARD, creator, 2, "tx2");
        Transaction tx3 = new Transaction(Transaction.Type.REWARD, creator, 3, "tx3");
        Transaction tx4 = new Transaction(Transaction.Type.REWARD, creator, 4, "tx4");

        List<Transaction> transactions = Arrays.asList(tx1, tx2, tx3, tx4);
        String merkleRoot = MerkleTree.getMerkleRoot(transactions);

        printMerkleTree(transactions);

        assertNotNull(merkleRoot);
        assertFalse(merkleRoot.isEmpty());
    }

    private void printMerkleTree(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("Empty transaction list.");
            return;
        }

        List<String> tempHashes = new ArrayList<>();
        for (Transaction transaction : transactions) {
            String trans = transaction.getSignature();
            String hashed = main.Utils.hashSHA256(trans); 
            tempHashes.add(hashed);
        }

        int level = 0;
        System.out.println("Merkle Tree (bottom to top) ("+ transactions.size() +" transactions) :");
        while (tempHashes.size() > 1) {
            System.out.println("Level " + level + ":");
            for (String h : tempHashes) {
                System.out.println("  " + h);
            }

            List<String> combinedHashes = new ArrayList<>();
            for (int i = 0; i < tempHashes.size(); i += 2) {
                if (i + 1 < tempHashes.size()) {
                    String combined = tempHashes.get(i) + tempHashes.get(i + 1);
                    combinedHashes.add(main.Utils.hashSHA256(combined));
                } else {
                    combinedHashes.add(tempHashes.get(i));
                }
            }
            tempHashes = combinedHashes;
            level++;
        }

        System.out.println("Level " + level + " (Root):");
        System.out.println("  " + tempHashes.get(0));
        System.out.println();
    }


}

