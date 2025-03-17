package main.blockchain;

import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.File;

import main.auctions.*;
import main.Utils;

public class Blockchain{
    private static final String FILE_PATH = "data/chain_";
    private List<Chain> chains;

    private Blockchain(){
        this.chains = new ArrayList<>();
    }

    public void addBlock(String transaction, Chain chain){
        int nounce = 0;
        String previousHash = chain.blocks.get(chain.size()-1).getHash();
        Block newBlock = new Block(transaction,nounce,previousHash);
        newBlock.merkleRoot = MerkleTree.getMerkleRoot(chain.blocks, newBlock.getTransaction()); 
        newBlock.hash = Utils.hashSHA256(newBlock);
        boolean miner = PoW.miner(newBlock);
        if(newBlock.getPrevHash() == chain.getLatest().getPrevHash()) // ver lógica!!
            createNewChain(chain, newBlock);
        else
            chain.addCompletedBlock(newBlock);
    }

    public static Blockchain createNewBlockchain(){
        Blockchain blockchain = new Blockchain();

        Chain genesisChain = new Chain();
        Block genesisBlock = new Block(Utils.createRandomString((16)));  
        genesisBlock.merkleRoot = MerkleTree.getMerkleRoot(genesisChain.blocks, genesisBlock.getTransaction());
        genesisBlock.hash = Utils.hashSHA256(genesisBlock);
        genesisChain.addCompletedBlock(genesisBlock);
        
        blockchain.chains.add(genesisChain);
        return blockchain;
    }

    public void createNewChain(Chain chain, Block newBlock){
        Chain newChain = new Chain();
        for(Block block : chain.blocks){
            if(block == chain.getLatest())
                break;
            newChain.addCompletedBlock(block);
        }
        newChain.addCompletedBlock(newBlock);
        this.chains.add(newChain);
        
    }

    public void saveBlockchain() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        int index = 1;
        for(Chain chain : this.chains){
            String fileName = FILE_PATH + index + ".json";
            try (FileWriter writer = new FileWriter(fileName)) {
                gson.toJson(chain, writer);
            } catch (IOException e) {
                System.err.println("Error saving blockchain: " + e.getMessage());
            }
            index++;
        }
    }

    public static Blockchain loadBlockchain() {
        Gson gson = new Gson();
        Blockchain blockchain = new Blockchain();
        blockchain.chains.clear();

        int index = 1;
        while(true){
            String fileName = FILE_PATH + index + ".json";
            File file = new File(fileName);
            if (!file.exists()) break;
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<Chain>() {}.getType();
                Chain loadedChain = gson.fromJson(reader, listType);
                blockchain.chains.add(loadedChain);
            } catch (IOException e) {
                System.err.println("Error loading Blockchain: " + e.getMessage());
            }
            index++;
        }
        return blockchain;
    }

    
    
    public List<Chain> getChains() {
        return this.chains;
    }
    



}
