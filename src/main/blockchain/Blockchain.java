package main.blockchain;

import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.Reader;

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
        Block newBlock = new Block(transaction,nounce,previousHash);
        newBlock.hash = Utils.hashSHA256(newBlock);
        boolean miner = PoW.miner(newBlock);
        this.blockchain.add(newBlock);
    }

    public static void saveBlockchain(List<Block> blockchain, String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(blockchain);
            System.out.println("Blockchain saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Block> loadBlockchain(String path) {
        try (Reader reader = new FileReader(path)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<List<Block>>() {}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
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

        saveBlockchain(blockchain.blockchain, "dataset.ser");
        
    }


}
