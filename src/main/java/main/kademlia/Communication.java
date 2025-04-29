package main.kademlia;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;


public class Communication implements Serializable {

    public enum MessageType {
        PING, FIND_NODE, ACK, NACK, CHALLENGE, STORE, FIND_VALUE, FIND_BLOCKCHAIN, 
        CHALLENGE_INIT
    }

    private final MessageType type;
    private final String information;
    private final String[] sender;
    private final String[] receiver;
    private  String signature;

    public Communication(MessageType type, String information, String[] sender, String[] receiver) {
        this.type = type;
        this.information = information;
        this.sender = sender;
        this.signature = null;
        this.receiver = receiver;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getInformation() {
        return this.information;
    }

    public String[] getSender() {
        return this.sender;
    }

    public String[] getReceiver() {
        return this.receiver;
    }
    
    public void setSignature(String signature){
        this.signature = signature;
    }

    public String getSignature(){
        return this.signature;
    }

    public String signCommunication(PrivateKey privateKey) {
        try {
            // Serialize the Communication object
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(type);
            oos.writeObject(information);
            oos.writeObject(sender);
            oos.writeObject(receiver);
            oos.flush();
            byte[] data = bos.toByteArray();

            // Sign the serialized data
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error signing communication", e);
        }
    }

    public boolean verifyCommunication(String signatureBase64, PublicKey publicKey) {
        try {
            // Serialize the Communication object again to check integrity
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(type);
            oos.writeObject(information);
            oos.writeObject(sender);
            oos.writeObject(receiver);
            oos.flush();
            byte[] data = bos.toByteArray();

            // Verify the signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            return signature.verify(signatureBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



}

