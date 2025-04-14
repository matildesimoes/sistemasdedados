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
import java.sql.Timestamp;


import main.Utils;

public class Blockchain{
    private static final String FILE_PATH = "data/chain_";
    private List<Chain> chains;

    private Blockchain(){
        this.chains = new ArrayList<>();
    }

    public void addBlock(List<Transaction> transactions, Chain chain){
        int nounce = 0;
        BlockHeader prevBlockHeader = chain.getBlock(chain.size()-1).getBlockHeader();
        Block newBlock = new Block(transactions, nounce, prevBlockHeader.getHash());

        BlockHeader newBlockHeader = newBlock.getBlockHeader();
        boolean miner = PoW.miner(newBlockHeader);

        BlockHeader latestBlockHeader = chain.getLatest().getBlockHeader();
        if(newBlockHeader.getPrevHash() == latestBlockHeader.getPrevHash()) 
            createNewChain(newBlock);
        else
            chain.addCompletedBlock(newBlock);
    }

    public void createBlock(List<Transaction> transactions){
        if(this.chains.size()==1){
            addBlock(transactions, this.chains.get(0));
        }else{
            Chain chain = null;
            Timestamp firstTimestamp;
            Timestamp secondTimestamp;
            for(int i=0; i < this.chains.size()-1; i++){
                firstTimestamp = this.chains.get(i).getLatest().getBlockHeader().getTimestamp();
                secondTimestamp = this.chains.get(i+1).getLatest().getBlockHeader().getTimestamp();
                if(firstTimestamp.before(secondTimestamp) || firstTimestamp.equals(secondTimestamp)){
                    chain = this.chains.get(i+1);
                }else{
                    chain = this.chains.get(i);
                }
            }
            addBlock(transactions, chain);
        }

    }

    public void storeBlock(Block block){
        if(this.chains.size()==1){
            BlockHeader latestBlockHeader = this.chains.get(0).getLatest().getBlockHeader();
            if(block.getBlockHeader().getPrevHash() == latestBlockHeader.getPrevHash()) 
                createNewChain(block);
            else
                this.chains.get(0).addCompletedBlock(block);
        }else{
            Chain chain = null;
            Timestamp firstTimestamp;
            Timestamp secondTimestamp;
            for(int i=0; i < this.chains.size()-1; i++){
                firstTimestamp = this.chains.get(i).getLatest().getBlockHeader().getTimestamp();
                secondTimestamp = this.chains.get(i+1).getLatest().getBlockHeader().getTimestamp();
                if(firstTimestamp.before(secondTimestamp) || firstTimestamp.equals(secondTimestamp)){
                    chain = this.chains.get(i+1);
                }else{
                    chain = this.chains.get(i);
                }
            }
            BlockHeader latestBlockHeader = chain.getLatest().getBlockHeader();
            if(block.getBlockHeader().getPrevHash() == latestBlockHeader.getPrevHash()) 
                createNewChain(block);
            else
                chain.addCompletedBlock(block);
        }
    }

    public static Blockchain createNewBlockchain(){
        Blockchain blockchain = new Blockchain();

        Chain genesisChain = new Chain();
        List<Transaction> transactions = new ArrayList<>();

        Block genesisBlock = new Block(transactions);  

        BlockHeader genesisBlockHeader = genesisBlock.getBlockHeader();
        genesisBlockHeader.setHash(Utils.hashSHA256(genesisBlockHeader));

        genesisChain.addCompletedBlock(genesisBlock);
        blockchain.chains.add(genesisChain);
        return blockchain;
    }

    public void createNewChain(Block block){
        Chain newChain = new Chain();
        newChain.addCompletedBlock(block);
        this.chains.add(newChain);
        
    }

    public void deleteChain(){


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
