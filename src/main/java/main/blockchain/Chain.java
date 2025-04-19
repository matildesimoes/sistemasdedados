package main.blockchain;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Chain implements Serializable{
    private List<Block> blocks = new ArrayList<>();

    public void addCompletedBlock(Block b) { 
        blocks.add(b); 
    }

    public List<Block> getBlocks(){
        return this.blocks;
    }

    public Block getBlock(int pos){
        return this.blocks.get(pos);
    }

    public Block getLatest() { 
        return blocks.get(blocks.size() - 1); 
    }

    public int size() { 
        return blocks.size(); 
    }

    public void removeBlock(int index) {
        if (index >= 0 && index < blocks.size()) {
            blocks.remove(index);
        }
    }

}
