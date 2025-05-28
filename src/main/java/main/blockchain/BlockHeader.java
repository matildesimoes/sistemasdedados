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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.temporal.ChronoUnit;


import com.fasterxml.jackson.annotation.JsonFormat;




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
        this.timestamp = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
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

    public void setMerkleRoot(String merkleRoot){
        this.merkleRoot = merkleRoot;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
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

    public String getSignature(){
        return this.signature;
    }


    public String signBlockHeader(PrivateKey privateKey) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeUTF(prevHash != null ? prevHash : "");
            dos.writeLong(timestamp.getTime());
            dos.writeInt(nounce);
            dos.writeUTF(merkleRoot != null ? merkleRoot : "");

            dos.flush();
            byte[] data = bos.toByteArray();

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
            if (signatureBase64 == null) return false;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeUTF(prevHash != null ? prevHash : "");
            dos.writeLong(timestamp.getTime());
            dos.writeInt(nounce);
            dos.writeUTF(merkleRoot != null ? merkleRoot : "");

            dos.flush();
            byte[] data = bos.toByteArray();

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            return sig.verify(signatureBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}

