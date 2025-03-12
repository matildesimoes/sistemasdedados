package main.blockchain;

import main.Utils;

public class PoW{
    private static final int DIFFICULTY = 2;

    public static boolean miner(Block block){
        String hash = block.getHash();
        String prefix = "0".repeat(DIFFICULTY);
        while(!hash.startsWith(prefix)){
            block.nounce = Utils.createRandomNumber(999999);
            hash = Utils.hashSHA256(block.toString());
            System.out.println(hash);
        }
        return true;
    }
}
