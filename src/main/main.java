package main;

import java.util.Scanner;
import main.blockchain.*;
import main.auctions.*;


public class main{

    public static void main(String[] args){

       Blockchain blockchain = new Blockchain(); 
      
       blockchain.loadBlockchain();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user3, user4, "ricardo");
        Chain mainChain = blockchain.getChains().get(0);
        blockchain.addBlock(trans1.signature,mainChain);

    }
}
