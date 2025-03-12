package main.auctions;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;


public class User{
    protected String email;
    protected KeyPair keyPair;

    public User(String email){
        this.email = email;
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

    public String getEmail(){
        return this.email;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
