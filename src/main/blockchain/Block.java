package main.blockchain;

import java.util.Random;

public class Block{
    private String transaction;
    public int nounce;
    public String hash;
    private String prevHash;
    
    public Block(String transaction){
        this.transaction = transaction;
    }

    public Block(String transaction, int nounce, String prevHash){
        this.transaction = transaction;
        this.nounce = nounce;
        this.prevHash = prevHash;
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

    public String toString(){
        return this.transaction + Integer.toString(this.nounce) + this.prevHash;
    }
}


