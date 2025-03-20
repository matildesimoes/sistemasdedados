package main.blockchain;

import java.util.ArrayList;
import java.util.List;
import main.Utils;

public class MerkleTree{
    public static String getMerkleRoot(List<Block> blockchain, String currentTransaction){
        if(blockchain == null || blockchain.isEmpty() && currentTransaction == null) return "";

        List<String> tempHashes = new ArrayList<>();
        for(Block block : blockchain){
            String trans = block.getTransaction();
            tempHashes.add(Utils.hashSHA256(trans));
        }

        if (currentTransaction != null && !currentTransaction.isEmpty()) {
            tempHashes.add(Utils.hashSHA256(currentTransaction));
        }

        System.out.println("------------------") ;
        System.out.println("chain size: " + tempHashes.size());
        while (tempHashes.size() > 1) {
            List<String> combinedHashes = new ArrayList<>();

            for (int i = 0; i < tempHashes.size(); i += 2) {
                if (i + 1 < tempHashes.size()) {
                    String combined = tempHashes.get(i) + tempHashes.get(i + 1);
                    combinedHashes.add(Utils.hashSHA256(combined));
                } else {
                    combinedHashes.add(tempHashes.get(i));
                }
            }
            tempHashes = combinedHashes;
            System.out.println(tempHashes.size());
        }
        return tempHashes.get(0);
    }
}
