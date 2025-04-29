package main.blockchain;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Block implements Serializable{
    private BlockHeader blockHeader;
    private List<Transaction> transactions;
    
    public Block() {
        this.transactions = new ArrayList<>();
        this.blockHeader = new BlockHeader();
    }

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

    public void setTransaction(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public BlockHeader getBlockHeader(){
        return this.blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return null;
        }
    }

    public static Block fromString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Block.class);
        } catch (Exception e) {
            System.out.println("Error reading JSON: " + e.getMessage());
            return null;
        }
    }



}


