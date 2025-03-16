package main.blockchain;

import java.util.List;
import java.util.ArrayList;

public class Chain{
    private List<Block> blocks = new ArrayList<>();

    public void addCompletedBlock(Block b) { 
        blocks.add(b); 
    }

    public Block getLatest() { 
        return blocks.get(blocks.size() - 1); 
    }

    public int size() { 
        return blocks.size(); 
    }

}
