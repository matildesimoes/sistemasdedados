package main;

import java.util.Scanner;
import main.blockchain.*;
import main.auctions.*;
import java.util.List;
import java.util.ArrayList;

import main.kademlia.*;

public class main{

    /*
    public static void createTestBlockchain(Blockchain blockchain, List<Transaction> trans) {
        User user1 = new User();
        User user2 = new User();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user1, user2, "ricardo");
        Transaction trans2 = new Transaction(user4, user3, "maria");
        Transaction trans3 = new Transaction(user3, user1, "matilde");

        trans.add(trans1);
        trans.add(trans2);
        trans.add(trans3);

        Chain mainChain = blockchain.getChains().get(0);

        blockchain.addBlock(trans, mainChain);

        blockchain.saveBlockchain();
    }
    */

    public static void createNode(String bootstrapIp){
        String savePath = "data/Node";

        InfoNode node = new InfoNode(Node.Type.BOOTSTRAP,"127.0.0.1", 5001, List.of("192.168.1.10:5000"));
        try {
            node.save(savePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args){

       /*
       //Blockchain blockchain = Blockchain.createNewBlockchain(); 
        List<Transaction> trans = new ArrayList<>();
       //createTestBlockchain(blockchain,trans);

       Blockchain blockchain = Blockchain.loadBlockchain();
        User user3 = new User();
        User user4 = new User();

        Transaction trans1 = new Transaction(user3, user4, "ricardo");
        Chain mainChain = blockchain.getChains().get(0);
        trans.add(trans1);
        blockchain.addBlock(trans,mainChain);

        blockchain.saveBlockchain();
        */
    
        int port = Integer.parseInt(args[0]);
        String bootstrapIp = args.length == 2 ? args[1] : null;


        Scanner in = new Scanner(System.in);

        if(bootstrapIp != null)
            createNode(bootstrapIp);
            

        while (true) {
            System.out.println("\n=======================================");
            System.out.println("      PUBLIC LEDGER FOR AUCTIONS");
            System.out.println("=======================================");
            System.out.println("1. View blockchain");
            System.out.println("2. Create new bid (transaction)");
            System.out.println("3. Mine new block (PoW)");
            System.out.println("4. Gossip latest transaction");
            System.out.println("5. View connected peers");
            System.out.println("6. Exit");
            System.out.print("\nChoose an option: ");

            String option = in.nextLine().trim();

            switch (option) {
                case "1":
                    break;
                case "2":
                    System.out.print("Enter bid value: ");
                    String value = in.nextLine();
                    break;
                case "3":
                    break;
                case "4":
                    break;
                case "5":
                    break;
                case "6":
                    System.out.println("Shutting down...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }

    }
}
