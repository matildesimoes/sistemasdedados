package main;

import java.util.Random;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;

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

}
