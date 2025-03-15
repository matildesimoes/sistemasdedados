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

import main.auctions.*;
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
        System.out.println(previousHash);
        Block newBlock = new Block(transaction,nounce,previousHash);
        newBlock.hash = Utils.hashSHA256(newBlock);
        boolean miner = PoW.miner(newBlock);
        this.blockchain.add(newBlock);
    }

    public void saveBlockchain(String path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(this.blockchain, writer);
            System.out.println("Blockchain saved in: " + path);
        } catch (IOException e) {
            System.err.println("Error saving blockchain: " + e.getMessage());
        }
    }

    public void loadBlockchain(String path) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(path)) {
            Type listType = new TypeToken<List<Block>>() {}.getType();
            List<Block> loadedBlockchain = gson.fromJson(reader, listType);
            this.blockchain = loadedBlockchain;
            System.out.println("Blockchain loaded successfully!");
        } catch (IOException e) {
            System.err.println("Error loading Blockchain: " + e.getMessage());
        }
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

        blockchain.addBlock(trans1.signature);
        blockchain.addBlock(trans2.signature);
        blockchain.addBlock(trans3.signature);

        blockchain.saveBlockchain("data/blockchain.json");
        
    }


}
