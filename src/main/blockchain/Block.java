package main.blockchain;

import java.util.Random;

public class Block{
    private String transaction;
    private int nounce;
    private String hash;
    
    public Block(String transaction){
        this.transaction = transaction;
    }

    public Block(String transaction, int nounce, String hash){
        this.transaction = transaction;
        this.nounce = nounce;
        this.hash = hash;
    }

    public String getTransaction(){
        return this.transaction;
    }

    public String getHash(){
        return this.hash;
    }

    public String toString(){
        return this.transaction + Integer.toString(this.nounce) + this.hash;
    }
}


