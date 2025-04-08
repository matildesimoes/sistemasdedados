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

    public static void createNode(String bootstrapAddress, Node node){
        Client client = new Client(node);

        String[] parts = bootstrapAddress.split(":");
        String bootstrapIp = parts[0];
        int bootstrapPort = Integer.parseInt(parts[1]);

        Node bootstrapNode = new Node(bootstrapIp,bootstrapPort);

        Communication ping = new Communication(
            Communication.MessageType.PING,
            "join?",
            node,
            bootstrapNode
        );

        Communication response = client.sendMessage(bootstrapNode, ping);


        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

        int nounce =  Utils.createRandomNumber(999999);
        String string = node.getNodeId() + response.getInformation() + nounce;
        String hash = Utils.hashSHA256(string);
        String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY);
        while(!hash.startsWith(prefix)){
            nounce =  Utils.createRandomNumber(999999);
            string = node.getNodeId() + response.getInformation() + nounce;
            hash = Utils.hashSHA256(string);
        }

        Communication challenge = new Communication(
            Communication.MessageType.CHALLENGE,
            String.valueOf(nounce),
            node,
            bootstrapNode
        );

        response = client.sendMessage(bootstrapNode, challenge);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

        Bucket nodeBucket = new Bucket(1); 
        String[] nodeContact = new String[] {node.getNodeIp(), String.valueOf(node.getNodePort()),node.getNodeId()};
        nodeBucket.update(nodeContact);

        Bucket bootstrapBucket = new Bucket(Utils.BUCKET_SIZE); 
        String[] bootstrapContact = new String[] {bootstrapNode.getNodeIp(), String.valueOf(bootstrapNode.getNodePort()), bootstrapNode.getNodeId()};
        bootstrapBucket.update(bootstrapContact);

        node.setRoutingTable(nodeBucket);
        node.setRoutingTable(bootstrapBucket);
        
        Communication find = new Communication(
            Communication.MessageType.FIND_NODE,
            node.getNodeIp() + ","+ String.valueOf(node.getNodePort()) + "," + node.getNodeId() ,
            node,
            bootstrapNode
        );

        response = client.sendMessage(bootstrapNode, find);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
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

        if (args.length < 1) {
            System.out.println("Usage: ./run.sh <myIp:myPort> <bootstrapIP:bootstrapPort>");
            return;
        }
    
        String[] parts = args[0].split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        String bootstrapAddress = args.length == 2 ? args[1] : null;

        Node node = new Node(ip, port);
        RoutingTable routingTable = new RoutingTable(node.getNodeId());
        Server server = new Server(ip, port, routingTable, node);

        new Thread(server::start).start();

        System.out.println("Node started at " + ip + ":" + port);

        if(bootstrapAddress != null)
        createNode(bootstrapAddress, node);

        Scanner in = new Scanner(System.in);

            

        while (true) {
            System.out.println("\n=======================================");
            System.out.println("      PUBLIC LEDGER FOR AUCTIONS");
            System.out.println("=======================================");
            System.out.println("1. View blockchain");
            System.out.println("2. Create new bid (transaction)");
            System.out.println("3. Mine new block (PoW)");
            System.out.println("4. Gossip latest transaction");
            System.out.println("5. View connected peers");
            System.out.println("0. Exit");
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
                case "0":
                    System.out.println("Shutting down...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }

    }
}
