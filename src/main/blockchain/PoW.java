package main.blockchain;

import main.Utils;

public class PoW{
    private static final int DIFFICULTY = 2;

    public static boolean miner(BlockHeader blockHeader){
        blockHeader.setNounce(Utils.createRandomNumber(999999));
        String hash = Utils.hashSHA256(blockHeader);
        String prefix = "0".repeat(DIFFICULTY);
        while(!hash.startsWith(prefix)){
            blockHeader.setNounce(Utils.createRandomNumber(999999));
            hash = Utils.hashSHA256(blockHeader);
        }
        blockHeader.setHash(hash);
        return true;
    }
}
