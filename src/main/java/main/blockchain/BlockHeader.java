package main.blockchain;

import java.time.Instant;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;




public class BlockHeader implements Serializable {
    private String prevHash;
    private String merkleRoot;
    private Timestamp timestamp;
    private int nounce;
    private String hash;
    private String signature;

    public BlockHeader() {
    }

    public BlockHeader(String prevHash, int nounce, String merkleRoot) {
        this.prevHash = prevHash;
        this.timestamp = Timestamp.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        this.nounce = nounce;
        this.signature = null;
        this.merkleRoot = merkleRoot;

    }

    public String getPrevHash() {
        return this.prevHash;
    }

    public String getMerkleRoot() {
        return this.merkleRoot;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public int getNounce() {
        return this.nounce;
    }

    public void setNounce(int nounce){
        this.nounce = nounce;
    }

    public String getHash(){
        return this.hash;
    }

    public void setHash(String hash){
        this.hash = hash;
    }

    public void setPrevHash(String prevHash){
        this.prevHash = prevHash;
    }

    public void setSignature(String signature){
        this.signature = signature;
    }

    public String signBlockHeader(PrivateKey privateKey) {
        try {
            // Serialize the block or block header (depending on what you want to sign)
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(prevHash); 
            oos.writeObject(timestamp); 
            oos.writeObject(nounce); 
            oos.writeObject(merkleRoot); 

            oos.flush();
            byte[] data = bos.toByteArray();

            // Create signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error signing block", e);
        }
    }

    public boolean verifyBlockHeader(String signatureBase64, PublicKey publicKey) {
        try {
            // Serialize the block header
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(prevHash); 
            oos.writeObject(timestamp); 
            oos.writeObject(nounce); 
            oos.writeObject(merkleRoot); 

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

