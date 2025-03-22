package main.blockchain;

import java.time.Instant;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;


public class BlockHeader implements Serializable {
    private String prevHash;
    private String merkleRoot;
    private Timestamp timestamp;
    private int nounce;
    private String hash;

    public BlockHeader(String prevHash, int nounce, String merkleRoot) {
        this.prevHash = prevHash;
        this.timestamp = Timestamp.from(Instant.now());
        this.nounce = nounce;

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

}

