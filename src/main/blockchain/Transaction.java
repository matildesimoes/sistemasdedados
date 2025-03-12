package main.blockchain;

import main.auctions.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Transaction{
    private User sender;
    private User receiver;
    private String information;
    private String signature;

    public Transaction(User sender, User receiver, String information){
        this.sender = sender;
        this.receiver = receiver;
        this.information = information;
        this.signature = signTransaction(sender.getPrivateKey());
    }

    private String signTransaction(PrivateKey privateKey){
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(this.toString().getBytes());
            byte[] signedBytes = signer.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error signing Transaction", e);
        }

    }

    public String toString(){
        return this.sender.getEmail() + this.receiver.getEmail() + this.information;
    }
}
