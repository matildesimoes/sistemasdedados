package main.blockchain;

import java.util.List;
import java.util.ArrayList;

import main.Utils;

public class Blockchain{
    public List<Block> blockchain;

    public Blockchain(){
        this.blockchain = new ArrayList<>();
        Block genesisBlock = new Block(Utils.createRandomString((16)));  
        this.blockchain.add(genesisBlock);
    }

    public void addBlock(String transaction){
        int nounce = 0;
        String previousHash = this.blockchain.get(this.blockchain.size()-1).getHash();
        Block newBlock = new Block(transaction,nounce,previousHash);
        newBlock.hash = Utils.hashSHA256(newBlock.toString());
        boolean miner = PoW.miner(newBlock);
        this.blockchain.add(newBlock);
    }

}
