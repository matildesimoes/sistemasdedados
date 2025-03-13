package main.blockchain;

import java.util.Random;
import java.time.Instant;
import java.io.Serializable;
import java.sql.Timestamp;

public class Block implements Serializable{
    private String transaction;
    public int nounce;
    public String hash;
    private String prevHash;
    private Timestamp timestamp;
    
    public Block(String transaction){
        this.transaction = transaction;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public Block(String transaction, int nounce, String prevHash){
        this.transaction = transaction;
        this.nounce = nounce;
        this.prevHash = prevHash;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public String getTransaction(){
        return this.transaction;
    }

    public int getNounce(){
        return this.nounce;
    }

    public String getHash(){
        return this.hash;
    }

    public String getprevHash(){
        return this.prevHash;
    }

}


