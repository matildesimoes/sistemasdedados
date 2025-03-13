package main.auctions;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;


public class User implements Serializable{
    protected int id;
    protected KeyPair keyPair;

    public User(){
        this.keyPair = generateKeyPair();
    }

    private KeyPair generateKeyPair(){
        try{
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        }catch (Exception e) {
            throw new RuntimeException("Error generating Key Pair", e);
        }
    }

    public int getId(){
        return this.id;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
