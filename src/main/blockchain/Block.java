package main.blockchain;

import java.io.Serializable;
import java.util.List;

public class Block implements Serializable{
    private List<Transaction> transactions;
    private BlockHeader blockHeader;
    
    public Block(List<Transaction> transactions){
        this.transactions = transactions;
        String merkleRoot = MerkleTree.getMerkleRoot(this.transactions);
        this.blockHeader = new BlockHeader(null, 0, merkleRoot);
    }

    public Block(List<Transaction> transactions, int nounce, String prevHash){
        this.transactions = transactions;
        String merkleRoot = MerkleTree.getMerkleRoot(this.transactions);
        this.blockHeader  = new BlockHeader(prevHash, nounce, merkleRoot);
    }

    public List<Transaction> getTransaction(){
        return this.transactions;
    }

    public BlockHeader getBlockHeader(){
        return this.blockHeader;
    }

}


