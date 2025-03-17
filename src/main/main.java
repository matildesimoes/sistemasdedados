package main;

import java.util.Scanner;
import main.blockchain.*;
import main.auctions.*;


public class main{

    public static void createTestBlockchain(Blockchain blockchain) {
        User user1 = new User();
        User user2 = new User();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user1, user2, "ricardo");
        Transaction trans2 = new Transaction(user4, user3, "maria");
        Transaction trans3 = new Transaction(user3, user1, "matilde");

        Chain mainChain = blockchain.getChains().get(0);

        blockchain.addBlock(trans1.signature, mainChain);
        blockchain.addBlock(trans2.signature, mainChain);
        blockchain.addBlock(trans3.signature, mainChain);

        blockchain.saveBlockchain();
    }

    public static void main(String[] args){

       //Blockchain blockchain = Blockchain.createNewBlockchain(); 
       //createTestBlockchain(blockchain);

       Blockchain blockchain = Blockchain.loadBlockchain();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user3, user4, "ricardo");
        Chain mainChain = blockchain.getChains().get(0);
        blockchain.addBlock(trans1.signature,mainChain);

        blockchain.saveBlockchain();

    }
}
