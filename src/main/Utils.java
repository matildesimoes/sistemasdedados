package main;

import java.util.Random;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


public class Utils{

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

public static String hashSHA256(String input) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Convert input string to byte array and compute hash
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert byte array into hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not found!", e);
    }
}

}
