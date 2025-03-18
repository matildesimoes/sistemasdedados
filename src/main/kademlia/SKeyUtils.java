package main.kademlia;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Base64;

public class SKeyUtils {

    public static String generateNodeId(PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Node ID from public key", e);
        }
    }
}

