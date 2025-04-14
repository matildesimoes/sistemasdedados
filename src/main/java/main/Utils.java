package main;

import java.util.Random;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.*;


public class Utils{
    public static int CHALLENGE_DIFFICULTY = 2;
    public static int BUCKET_SIZE = 3;
    public static int PING_FREQUENCY = 120; // seconds
    public static int RECURSIVE_FIND_NODE = 1;
    public static int TRANS_POOL_LIMIT_LENGTH = 2;
    public static int TRANS_POOL_LIMIT_TIME = 120; // seconds
    public static int BLOCK_CHAIN_LIMIT = 2; // number of blocks required to remove the fork

    public static String createRandomString(int length){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static int createRandomNumber(int length){
        Random rand = new Random();
        return rand.nextInt(length);
    }

    public static String hashSHA256(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            byte[] bytes = bos.toByteArray();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(bytes);

            // Convert byte array into hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating Hash!", e);
        }
    }

    public static KeyPair generateKeyPair(){
        try{
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        }catch (Exception e) {
            throw new RuntimeException("Error generating Key Pair", e);
        }
    }

    public static String publicKeySignature(PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Node ID from public key", e);
        }
    }

    public static boolean verify(String data, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes());
            return sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
