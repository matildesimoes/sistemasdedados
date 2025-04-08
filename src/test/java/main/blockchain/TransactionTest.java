package main.blockchain;

import main.auctions.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    public void testTransactionCreation() throws Exception {
        User buyer = new User();
        User seller = new User();

        Transaction tx = new Transaction(buyer, seller, "Auction #123");

        assertNotNull(tx.signature);
        assertNotNull(tx.getTimestamp());
        assertTrue(tx.signature.length() > 10);
    }

}

