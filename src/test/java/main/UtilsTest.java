package main;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

class UtilsTest {

    @Test
    void createRandomString() {
        String rand = Utils.createRandomString(16);
        assertNotNull(rand);
        assertTrue(rand.length() > 0);
    }

    @Test
    void createRandomNumber() {
        int num = Utils.createRandomNumber(10);
        assertTrue(num >= 0 && num < 10);
    }

    
    @Test
    void testHashSHA256() {
        String input = "hello";
        String hash = Utils.hashSHA256(input);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // 256 bits = 64 hex chars
    }

    @Test
    void testGenerateKeyPair() {
        KeyPair keyPair = Utils.generateKeyPair();
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    void testPublicKeySignature() {
        KeyPair keyPair = Utils.generateKeyPair();
        String signature = Utils.publicKeySignature(keyPair.getPublic());
        assertNotNull(signature);
    }


    @Test
    void testVerify() throws Exception {
        KeyPair keyPair = Utils.generateKeyPair();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        String data = "test message";
        signature.update(data.getBytes());
        byte[] signed = signature.sign();

        assertTrue(Utils.verify(data, signed, keyPair.getPublic()));
    }


}
