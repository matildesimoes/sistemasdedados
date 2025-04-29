package main.blockchain;

import java.util.ArrayList;
import java.util.List;
import main.Utils;

public class MerkleTree{
    public static String getMerkleRoot(List<Transaction> transactions){
        if(transactions == null || transactions.isEmpty()) return "null";

        List<String> tempHashes = new ArrayList<>();
        for(Transaction transaction : transactions){
            String trans = transaction.getSignature();
            tempHashes.add(Utils.hashSHA256(trans));
        }

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
        }
        return tempHashes.get(0);
    }
}
