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
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import main.Utils;

public class Blockchain implements Serializable{
    private static final String FILE_PATH = "data/chain_";
    private List<Chain> chains;
    private Map<String, Integer> blockchainHeight;
    private Map<Integer, Integer> blocksPerHeight;

    public Blockchain(){
        this.chains = new ArrayList<>();
        this.blockchainHeight = new HashMap<>();
        this.blocksPerHeight = new HashMap<>();
    }

    public enum MatchResult {
        MATCH_FOUND,
        TOO_OLD,
        NOT_FOUND
    }

    public int getBlockHeight(String hash) {
        return blockchainHeight.getOrDefault(hash, -1);
    }

    public int getLatestHeight() {
        return blockchainHeight.values().stream().max(Integer::compareTo).orElse(0);
    }

    public Block getLatestBlock() {
        Block latest = null;
        Timestamp latestTime = new Timestamp(0);

        for (Chain chain : this.chains) {
            Block block = chain.getLatest();
            Timestamp ts = block.getBlockHeader().getTimestamp();
            if (ts.after(latestTime)) {
                latest = block;
                latestTime = ts;
            }
        }

        return latest;
    }

    public List<Block> getMostRecentBlocks(int count) {
        List<Block> allBlocks = new ArrayList<>();

        for (Chain chain : this.chains) {
            allBlocks.addAll(chain.getBlocks());
        }

        //Order pby timestamp 
        allBlocks.sort((b1, b2) -> b2.getBlockHeader().getTimestamp().compareTo(b1.getBlockHeader().getTimestamp()));

        return allBlocks.subList(0, Math.min(count, allBlocks.size()));
    }



    public boolean hasBlock(String hash) {
        for (Chain chain : this.chains) {
            for (Block block : chain.getBlocks()) {
                if (block.getBlockHeader().getHash().equals(hash)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Block addBlock(List<Transaction> transactions, Chain chain){
        int nounce = 0;
        BlockHeader prevBlockHeader = chain.getBlock(chain.size()-1).getBlockHeader();
        Block newBlock = new Block(transactions, nounce, prevBlockHeader.getHash());

        BlockHeader newBlockHeader = newBlock.getBlockHeader();
        boolean miner = PoW.miner(newBlockHeader);

        BlockHeader latestBlockHeader = chain.getLatest().getBlockHeader();
        if(newBlockHeader.getPrevHash() == latestBlockHeader.getPrevHash()){
            createNewChain(newBlock);
        }else{
            chain.addCompletedBlock(newBlock);
            trimFork(chain, newBlock);
        }
        return newBlock;
    }


    public void recalculateHeights() {
        blockchainHeight.clear();
        blocksPerHeight.clear();

        Map<String, Block> hashToBlock = new HashMap<>();
        for (Chain chain : chains) {
            for (Block block : chain.getBlocks()) {
                hashToBlock.put(block.getBlockHeader().getHash(), block);
            }
        }

        for (Block block : hashToBlock.values()) {
            int height = computeHeight(block, hashToBlock);

            String hash = block.getBlockHeader().getHash();
        }
    }

    private int computeHeight(Block block, Map<String, Block> hashToBlock) {
        String hash = block.getBlockHeader().getHash();
        if (blockchainHeight.containsKey(hash)) return blockchainHeight.get(hash);

        String prev = block.getBlockHeader().getPrevHash();
        int height = 0;
        if (prev != null && hashToBlock.containsKey(prev)) {
            height = computeHeight(hashToBlock.get(prev), hashToBlock) + 1;
        }
        blockchainHeight.put(hash, height);
        blocksPerHeight.put(height, blocksPerHeight.getOrDefault(height, 0) + 1);
        return height;
    }


    public Block createBlock(List<Transaction> transactions){
        Block newBlock = null;
        if(this.chains.size()==1){
            newBlock = addBlock(transactions, this.chains.get(0));
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
            newBlock = addBlock(transactions, chain);
        }
        
        return newBlock;

    }

    public MatchResult storeBlock(Block block){
        if(this.chains.size()==1){
            BlockHeader latestBlockHeader = this.chains.get(0).getLatest().getBlockHeader();
            if(block.getBlockHeader().getPrevHash().equals(latestBlockHeader.getHash())){
                this.chains.get(0).addCompletedBlock(block);
                trimFork(this.chains.get(0), block);
                this.saveBlockchain();
                return MatchResult.MATCH_FOUND;
            }
            MatchResult result = findRecentMatchingBlock(block, this.chains.get(0), 3);
            if (result == MatchResult.MATCH_FOUND) {
                createNewChain(block);
                this.saveBlockchain();
                return MatchResult.MATCH_FOUND;
            }
            return result;
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
            if(block.getBlockHeader().getPrevHash().equals(latestBlockHeader.getHash())){
                chain.addCompletedBlock(block);
                trimFork(chain, block);
                this.saveBlockchain();
                return MatchResult.MATCH_FOUND;
            }
            MatchResult result = findRecentMatchingBlock(block, chain,3);
            if (result == MatchResult.MATCH_FOUND) {
                createNewChain(block);
                this.saveBlockchain();
                return MatchResult.MATCH_FOUND;
            }
            return result;
        }
    }

    private MatchResult findRecentMatchingBlock(Block block, Chain chain, int depth) {
        String prevHash = block.getBlockHeader().getPrevHash();
        List<Block> blocks = chain.getBlocks();
        int size = blocks.size();

        boolean existsSomewhere = false;
        for (int i = 0; i < size; i++) {
            Block b = blocks.get(i);
            if (b.getBlockHeader().getHash().equals(prevHash)) {
                existsSomewhere = true;
                if (i >= size - depth) {
                    return MatchResult.MATCH_FOUND;
                }
            }
        }

        if (existsSomewhere) return MatchResult.TOO_OLD;
        return MatchResult.NOT_FOUND;
    }

    public Blockchain createNewBlockchain(){

        Chain genesisChain = new Chain();
        List<Transaction> transactions = new ArrayList<>();

        Block genesisBlock = new Block(transactions);  

        BlockHeader genesisBlockHeader = genesisBlock.getBlockHeader();
        genesisBlockHeader.setHash(Utils.hashSHA256(genesisBlockHeader));

        genesisChain.addCompletedBlock(genesisBlock);
        this.chains.add(genesisChain);
        recalculateHeights();
        this.blockchainHeight.put(genesisBlockHeader.getHash(), 0);
        this.blocksPerHeight.put(0, 1);

        this.saveBlockchain();
        return this;
    }

    public void createNewChain(Block block){
        Chain newChain = new Chain();
        newChain.addCompletedBlock(block);
        this.chains.add(newChain);  
    }

    public void trimFork(Chain currentChain, Block newBlock) {
        recalculateHeights();

        String newHash = newBlock.getBlockHeader().getHash();
        int newHeight = blockchainHeight.getOrDefault(newHash, 0);

        for (Chain chain : this.chains) {
            if (chain == currentChain) continue;

            for (int i = 0; i < chain.size(); i++) {
                Block block = chain.getBlock(i);
                String hash = block.getBlockHeader().getHash();
                int height = blockchainHeight.getOrDefault(hash, 0);
                int countAtHeight = blocksPerHeight.getOrDefault(height, 0);
                int diff = Math.abs(height - newHeight);

                System.out.println(diff);
                System.out.println(countAtHeight);
                // Found a forked height with more than 1 block
                if (countAtHeight > 1 && diff >= Utils.BLOCK_CHAIN_LIMIT) {
                    System.out.println("Trimming fork at height: " + height + ".");


                    // Remove all blocks from this point forward
                    for (int j = chain.size() - 1; j >= i; j--) {
                        Block toRemove = chain.getBlock(j);
                        String h = toRemove.getBlockHeader().getHash();
                        int hHeight = blockchainHeight.getOrDefault(h, 0);

                        // Remove from chain
                        chain.removeBlock(j);
                        blockchainHeight.remove(h);

                        // Update block count at height
                        int remaining = blocksPerHeight.getOrDefault(hHeight, 1);
                        if (remaining > 1) {
                            blocksPerHeight.put(hHeight, remaining - 1);
                        } else {
                            blocksPerHeight.remove(hHeight);
                        }

                    }

                    break; 
                }
            }
        }
    }



    public void saveBlockchain() {
        File folder = new File("data");
        File[] files = folder.listFiles((dir, name) -> name.matches("chain_\\d+\\.json"));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }

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

    public Blockchain loadBlockchain() {
        Gson gson = new Gson();
        this.chains.clear();

        int index = 1;
        while(true){
            String fileName = FILE_PATH + index + ".json";
            File file = new File(fileName);
            if (!file.exists()) break;
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<Chain>() {}.getType();
                Chain loadedChain = gson.fromJson(reader, listType);
                this.chains.add(loadedChain);
            } catch (IOException e) {
                System.err.println("Error loading Blockchain: " + e.getMessage());
            }
            index++;
        }
        return this;
    }

    public static String blockchainToString(List<Chain> blockchain) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(blockchain);
        } catch (Exception e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return null;
        }
    }

    public static List<Chain> blockchainFromString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<Chain>>() {});
        } catch (Exception e) {
            System.out.println("Error reading JSON: " + e.getMessage());
            return null;
        }
    }
    
    
    public List<Chain> getChains() {
        return this.chains;
    }

    public void setChains(List<Chain> chains){
        this.chains =chains;
    }



}
