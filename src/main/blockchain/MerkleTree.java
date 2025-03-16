package main.blockchain;

import java.util.ArrayList;
import java.util.List;
import main.Utils;

public class MerkleTree{
    public static String getMerkleRoot(List<Block> blockchain){
        if(blockchain == null || blockchain.isEmpty()) return "";

        List<String> tempHashes = new ArrayList<>();
        for(Block block : blockchain){
            String trans = block.getTransaction();
            tempHashes.add(Utils.hashSHA256(trans));
        }

        System.out.println(tempHashes.size());
        while (tempHashes.size() > 1) {
            List<String> combinedHashes = new ArrayList<>();

            for (int i = 0; i < tempHashes.size(); i += 2) {
                if (i + 1 < tempHashes.size()) {
                    String combined = tempHashes.get(i) + tempHashes.get(i + 1);
                    combinedHashes.add(Utils.hashSHA256(combined));
                } else {
                    // odd -> duplicate the last
                    String combined = tempHashes.get(i) + tempHashes.get(i);
                    combinedHashes.add(Utils.hashSHA256(combined));
                }
            }
            tempHashes = combinedHashes;
            System.out.println(tempHashes.size());
        }
        return tempHashes.get(0);
    }
}
