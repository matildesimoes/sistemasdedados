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

    public Blockchain(){
        this.chains = new ArrayList<>();
        Chain genesisChain = new Chain();

        Block genesisBlock = new Block(Utils.createRandomString((16)));  
        genesisBlock.hash = Utils.hashSHA256(genesisBlock);

        genesisChain.addCompletedBlock(genesisBlock);
        this.chains.add(genesisChain);
    }

    public void addBlock(String transaction, Chain chain){
        int nounce = 0;
        String previousHash = chain.blocks.get(chain.size()-1).getHash();
        Block newBlock = new Block(transaction,nounce,previousHash);
        newBlock.hash = Utils.hashSHA256(newBlock);
        boolean miner = PoW.miner(newBlock);
        newBlock.merkleRoot = MerkleTree.getMerkleRoot(chain.blocks); 
        if(newBlock.getPrevHash() == chain.getLatest().getPrevHash())
            createNewChain(chain, newBlock);
        else
            chain.addCompletedBlock(newBlock);
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

    public void loadBlockchain() {
        Gson gson = new Gson();
        chains.clear();
        int index = 1;
        while(true){
            String fileName = FILE_PATH + index + ".json";
            File file = new File(fileName);
            if (!file.exists()) break;
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<Chain>() {}.getType();
                Chain loadedChain = gson.fromJson(reader, listType);
                chains.add(loadedChain);
            } catch (IOException e) {
                System.err.println("Error loading Blockchain: " + e.getMessage());
            }
            index++;
        }
    }
    
    
    public List<Chain> getChains() {
        return chains;
    }

    public static void createBlockchain(){
        User user1 = new User();
        User user2 = new User();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user1, user2, "ricardo");
        Transaction trans2 = new Transaction(user4, user3, "maria");
        Transaction trans3 = new Transaction(user3, user1, "matilde");

        Blockchain blockchain = new Blockchain();
        Chain mainChain = blockchain.getChains().get(0);

        blockchain.addBlock(trans1.signature, mainChain);
        blockchain.addBlock(trans2.signature, mainChain);
        blockchain.addBlock(trans3.signature, mainChain);

        blockchain.saveBlockchain();
        
    }


}
