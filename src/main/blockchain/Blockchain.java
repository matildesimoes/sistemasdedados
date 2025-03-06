package main.blockchain;

import java.util.List;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class Blockchain{
    public List<Block> blockchain;

    public Blockchain(){
        this.blockchain = new ArrayList<>();
        Block genesisBlock = new Block(createRandomString((16)));  
        this.blockchain.add(genesisBlock);
    }

    private String createRandomString(int length){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private int createRandomNumber(int length){
        Random rand = new Random();
        return rand.nextInt(length);
    }

    public String createPreviousBlockHash(){
        Block previousBlock = this.blockchain.get(this.blockchain.size()-1);
        String hash;
        return hash;
    }

    public void addBlock(String transaction){
        int nounce = createRandomNumber(999999);
        String previousHash = createPreviousBlockHash();
        Block newBlock = new Block(transaction,nounce,previousHash);
        this.blockchain.add(newBlock);
    }

}
