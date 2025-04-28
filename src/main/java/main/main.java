package main;

import java.util.Scanner;
import main.blockchain.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;

import main.kademlia.*;

public class main{
    public static List<String> myAuctions = new ArrayList<>();
    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
    public static String checkAuctionId(int id){
        String item = null;
        for (String i : myAuctions) {
            String[] p = i.split("id=");
            if (p.length > 1) {
                String idPart = p[1].split("\\)")[0];
                if (Integer.parseInt(idPart) == id) {
                    item = i.split(" \\(id=")[0]; 
                    return item;
                }
            }
        }
        return null;
    }

    public static void fullPoolLogic(Node node,Client client,Transaction trans, ScheduledFuture<?> future, Runnable task){
        node.addToTransactionPool(trans);
        if(node.isTransactionPoolFull()){
            future.cancel(true);
            Block newBlock = node.getBlockchain().createBlock(node.getTransactionPool());
            node.clearTransactionPool();
            client.store(newBlock);
            future = scheduler.scheduleAtFixedRate(task, Utils.TRANS_POOL_LIMIT_TIME, Utils.TRANS_POOL_LIMIT_TIME, TimeUnit.SECONDS);
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
        String[] nodeContact = {ip, String.valueOf(port), node.getNodeId()};
        RoutingTable routingTable = new RoutingTable(nodeContact);
        node.setRoutingTable(routingTable);
        Server server = new Server(ip, port, routingTable, node);
        Client client = new Client(node);
        node.getBlockchain().createNewBlockchain();

        new Thread(server::start).start();

        System.out.println("Node started at " + ip + ":" + port+ ".\nNodeId: " + node.getNodeId());

        if(bootstrapAddress != null)
            client.joinNetwork(bootstrapAddress);
            

        Scanner in = new Scanner(System.in);

        ScheduledFuture<?> future;

        scheduler.scheduleAtFixedRate(() -> {
            client.checkIfNodeAlive();
        }, Utils.PING_FREQUENCY, Utils.PING_FREQUENCY, TimeUnit.SECONDS);


        Runnable task = () -> {
            if (node.transactionPoolSize() == 0) return;

            Block newBlock = node.getBlockchain().createBlock(node.getTransactionPool());
            node.clearTransactionPool();
            client.store(newBlock);
        };

        future = scheduler.scheduleAtFixedRate(task, Utils.TRANS_POOL_LIMIT_TIME, Utils.TRANS_POOL_LIMIT_TIME, TimeUnit.SECONDS);

        while (true) {
            System.out.println("\n=======================================");
            System.out.println("      PUBLIC LEDGER FOR AUCTIONS");
            System.out.println("=======================================");
            System.out.println("1. View blockchain");
            System.out.println("2. View Routing Table");
            System.out.println("3. Create new transaction");
            System.out.println("0. Exit");
            System.out.print("\nChoose an option: ");

            String option = in.nextLine().trim();

            switch (option) {
                case "1":
                    break;
                case "2":
                    List<Bucket> buckets = routingTable.getBuckets();

                    System.out.println("Routing Table:");
                    for (int i = 0; i < buckets.size(); i++) {
                        Bucket bucket = buckets.get(i);

                        List<String[]> nodes = bucket.getNodes();
                        if (nodes.isEmpty()) {
                        } else {
                            System.out.println("Bucket " + i + ":");
                            for (String[] n : nodes) {
                                ip = n.length > 0 ? n[0] : "unknown-ip";
                                String p = n.length > 1 ? n[1] : "unknown-port";
                                String id = n.length > 2 ? n[2] : "unknown-id";

                                System.out.println("  - IP: " + ip + ", Port: " + Integer.valueOf(p)+ ", ID: " + id);
                            }
                        }
                    }
                    break;
                case "3":
                    while(true){
                        System.out.println("\n=======================================");
                        System.out.println("      PUBLIC LEDGER FOR AUCTIONS");
                        System.out.println("=======================================");
                        System.out.println("1. Create Auction");
                        System.out.println("2. Start Auction");
                        System.out.println("3. Make Bid");
                        System.out.println("4. Close Auction");
                        System.out.println("0. Go back");
                        System.out.print("\nChoose an option: ");
                        
                        option = in.nextLine().trim();

                        switch(option){
                            case "1":
                                System.out.print("Auction Item: ");
                                String item = in.nextLine();
                                int random = Utils.createRandomNumber(999999);
                                Transaction create = new Transaction(
                                    Transaction.Type.CREATE_AUCTION,
                                    node,   
                                    random,
                                    item
                                );
                                myAuctions.add(item + " (id="+ random + ") ");
                                System.out.println("Auction created with id: " + random);
                                fullPoolLogic(node,client,create,future,task);
                                break;
                            case "2":
                                for(int i=0; i< myAuctions.size(); i++){
                                    System.out.println((i+1) + ". " + myAuctions.get(i));
                                }

                                System.out.print("Auction id: ");
                                int id = Integer.valueOf(in.nextLine().trim());
                                item = checkAuctionId(id);

                                if(item == null){
                                    System.out.println("Invalid Auction id");
                                    break;
                                }
                        

                                Transaction start = new Transaction(
                                    Transaction.Type.START_AUCTION,
                                    node,   
                                    id,
                                    item
                                );
                            
                                System.out.println("Auction started with id: " + id);
                                fullPoolLogic(node,client,start, future,task);
                                break;
                            case "3":
                                Set<String> activeAuctions = server.getActiveAuctions();
                                int index = 1;
                                for (String auction : activeAuctions) {
                                    System.out.println(index + ". " + auction);
                                    index++;
                                }
                                  

                                System.out.print("Auction id: ");
                                id = Integer.valueOf(in.nextLine().trim());
                                item = checkAuctionId(id);

                                if(item == null){
                                    System.out.println("Invalid Auction id");
                                    break;
                                }

                                System.out.print("Ammount: ");
                                int ammount = Integer.valueOf(in.nextLine().trim());

                                Transaction bid = new Transaction(
                                    Transaction.Type.BID,
                                    node,   
                                    id,
                                    "Bid ammount: " + ammount
                                );
                            
                                System.out.println("Bid of " + ammount + " to Auction id: " + id);
                                fullPoolLogic(node,client,bid, future,task);

                                break;
                            case "4":
                                for(int i=0; i< myAuctions.size(); i++){
                                    System.out.println((i+1) + ". " + myAuctions.get(i));
                                }

                                System.out.print("Auction id: ");
                                id = Integer.valueOf(in.nextLine().trim());
                                item = checkAuctionId(id);

                                if(item == null){
                                    System.out.println("Invalid Auction id");
                                    break;
                                }

                                Transaction close = new Transaction(
                                    Transaction.Type.CLOSE_AUCTION,
                                    node,   
                                    id,
                                    item
                                );
                            
                                System.out.println("Auction closed with id: " + id);
                                myAuctions.remove(item + " (id="+ id + ") ");
                                fullPoolLogic(node,client,close,future,task);
                                break;
                            default:
                                break;
                        }
                        if(option.equals("0")) break;
                    }
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
