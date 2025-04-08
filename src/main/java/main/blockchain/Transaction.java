package main.blockchain;

import main.auctions.*;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.sql.Timestamp;
import java.time.Instant;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;


public class Transaction implements Serializable{
    private User buyer;
    private User seller;
    private String information;
    public String signature;
    private Timestamp timestamp;

    public Transaction(User buyer, User seller, String information){
        this.buyer = buyer;
        this.seller = seller;
        this.information = information;
        this.signature = signTransaction(buyer.getPrivateKey());
        this.timestamp = Timestamp.from(Instant.now());
    }

    public Timestamp getTimestamp(){
        return this.timestamp;
    }

    private String signTransaction(PrivateKey privateKey){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            
            oos.writeObject(buyer);
            oos.writeObject(seller);
            oos.writeObject(information);
            oos.writeObject(timestamp);
            
            oos.flush();
            byte[] dataToSign = bos.toByteArray();

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(dataToSign);

            byte[] signedBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error signing Transaction!", e);
        }

    }

}
