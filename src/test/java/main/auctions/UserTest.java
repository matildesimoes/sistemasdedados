package main.auctions;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private static final String COUNTER_FILE = "data/userId_counter.txt";

    @BeforeEach
    public void resetCounter() throws Exception {
        File file = new File(COUNTER_FILE);
        file.getParentFile().mkdirs(); 
        Files.writeString(file.toPath(), "1");
    }

    @Test
    public void testUserCreationAndKeys() {
        User user = new User();

        assertEquals(1, user.getId());
        assertNotNull(user.getPrivateKey());
        assertNotNull(user.getPublicKey());

        assertTrue(user.getPrivateKey() instanceof PrivateKey);
        assertTrue(user.getPublicKey() instanceof PublicKey);
    }

    @Test
    public void testUserIdIncrements() {
        User user1 = new User(); // id = 1
        User user2 = new User(); // id = 2
        assertEquals(1, user1.getId());
        assertEquals(2, user2.getId());
    }
}

