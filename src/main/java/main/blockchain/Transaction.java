package main.blockchain;

import main.kademlia.Node;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    //Ignore to avoid problems with serialization of PrivateKey 
    private String creatorId; 

    private String information;
    private String signature;
    private Timestamp timestamp;
    private int auctionNumber;
    private Type type;

    public enum Type {
        CREATE_AUCTION, CLOSE_AUCTION, BID, START_AUCTION, REWARD
    }

    //Default constructor required by Jackson
    public Transaction() {
    }

    public Transaction(Type type, Node creator, int auctionNumber, String information){
        this.type = type;
        this.creatorId = creator.getNodeId();
        this.auctionNumber = auctionNumber;
        this.information = information;
        this.signature = signTransaction(creator.getPrivateKey());
        this.timestamp = Timestamp.from(Instant.now());
    }

    public String signTransaction(PrivateKey privateKey){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            
            oos.writeObject(type);
            oos.writeObject(creatorId);
            oos.writeObject(auctionNumber);
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

    public boolean verifyTransactionSignature(String signatureBase64, PublicKey publicKey) {
        try {
            // Rebuild the same byte array from the transaction fields
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(type);
            oos.writeObject(creatorId);
            oos.writeObject(auctionNumber);
            oos.writeObject(information);
            oos.writeObject(timestamp);

            oos.flush();
            byte[] dataToVerify = bos.toByteArray();

            // Initialize signature verification
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(dataToVerify);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            return sig.verify(signatureBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    
    public String getCreatorId() {
        return creatorId;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getAuctionNumber() {
        return auctionNumber;
    }

    public void setAuctionNumber(int auctionNumber) {
        this.auctionNumber = auctionNumber;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }


}
