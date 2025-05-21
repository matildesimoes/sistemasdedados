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
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.io.File;


import main.kademlia.*;
import main.blockchain.Blockchain.MatchResult;


public class main{
    public static Map<Integer, String> myAuctions = new HashMap<>();
    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void fullPoolLogic(Node node,PeerNode peer,Transaction trans, ScheduledFuture<?> future, Runnable task){
        node.addToTransactionPool(trans);
        if(node.isTransactionPoolFull()){
            future.cancel(true);
            Block newBlock = node.getBlockchain().createBlock(node.getTransactionPool());
            node.clearTransactionPool();
            peer.store(newBlock);

            // Check for CLOSE_AUCTION transactions and print the winner
            for (Transaction t : newBlock.getTransaction()) {
                if (t.getType() == Transaction.Type.CLOSE_AUCTION) {
                    int auctionId = t.getAuctionNumber();
                    String blockHash = newBlock.getBlockHeader().getHash();
                    int closeHeight = node.getBlockchain().getBlockHeight(blockHash);
                    int targetHeight = closeHeight + Utils.BLOCK_CHAIN_LIMIT;

                    
                    // Spawn a thread to wait for confirmation
                    new Thread(() -> {
                        System.out.println("Waiting to confirm winner for Auction ID " + auctionId + " (height " + targetHeight + ")...");
                        while (true) {
                            int currentHeight = node.getBlockchain().getLatestHeight();
                            if (currentHeight >= targetHeight) {
                                String winner = MessageHandler.getAuctionWinner(node, auctionId);
                                if (winner != null) {
                                    System.out.println("Auction (id" + auctionId + ") winner is: " + winner);
                                } else {
                                    System.out.println("Auction (id" + auctionId + ") had no valid bids.");
                                }
                                break;
                            }

                            try {
                                Thread.sleep(1000); // Check every second
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }).start();
                }
            }
            future = scheduler.scheduleAtFixedRate(task, Utils.TRANS_POOL_LIMIT_TIME, Utils.TRANS_POOL_LIMIT_TIME, TimeUnit.SECONDS);
        }
    }

    public static void printForkTree(Block block, String prefix, boolean hasSibling, Map<String, List<Block>> childrenMap) {
        BlockHeader header = block.getBlockHeader();

        System.out.println(prefix + "+--------------------------------------------------+");
        System.out.println(prefix + "| Block:      " + header.getHash());
        System.out.println(prefix + "| PrevHash:   " + header.getPrevHash() );
        System.out.println(prefix + "| Timestamp:  " + header.getTimestamp());
        System.out.println(prefix + "| MerkleRoot: " + header.getMerkleRoot());


        List<Transaction> transactions = block.getTransaction();
        if (transactions != null && !transactions.isEmpty()) {
            System.out.println(prefix + "| Transactions:");
            for (Transaction tx : transactions) {
                System.out.println(prefix + "|    - Type: " + tx.getType() + "; Item: " + tx.getInformation());
            }
        } else {
            System.out.println(prefix + "|  (No transactions)");
        }

        System.out.println(prefix + "+--------------------------------------------------+");


        List<Block> children = childrenMap.getOrDefault(header.getHash(), new ArrayList<>());

        for (int i = 0; i < children.size(); i++) {
            Block child = children.get(i);
            boolean isLastChild = (i == children.size() - 1);

            String newPrefix = prefix;

            if (children.size() > 1) {
                if (i == 0) {
                    System.out.println(prefix + "|\\");
                } 
                newPrefix += "| ";
            } else if (children.size() == 1) {
                newPrefix += "  ";
            }

            printForkTree(child, newPrefix, !isLastChild, childrenMap);

            if (isLastChild && children.size() > 1) {
                System.out.println(prefix + "|/");
            }
        }
    }

    public static void main(String[] args){

        if (args.length < 1) {
            System.out.println("Usage: ./run.sh <myIp:myPort> <bootstrapIP:bootstrapPort>");
            return;
        }
    
        String[] parts = args[0].split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        String bootstrapAddress = args.length == 2 ? args[1] : null;

        Node node = new Node(ip, port);
        String[] nodeContact = {ip, String.valueOf(port), node.getNodeId(), node.getTimeAlive()};
        RoutingTable routingTable = RoutingTable.loadRoutingTable(nodeContact);
        node.setRoutingTable(routingTable);

        boolean routingWasLoaded = new File("data/routing_table.ser").exists();

        File firstChain = new File("data/chain_1.json");
        if (firstChain.exists()) {
            node.getBlockchain().loadBlockchain();
            System.out.println("Loaded existing blockchain from disk.");
        } else {
            node.getBlockchain().createNewBlockchain();
            System.out.println("No local blockchain found. Created new one.");
        }

        boolean blockchainWasLoaded = firstChain.exists();

        PeerNode peer = new PeerNode(node);
        peer.startListener();

        System.out.println("Node started at " + ip + ":" + port + ".\nNodeId: " + node.getNodeId());


        if(bootstrapAddress != null)
            peer.joinNetwork(bootstrapAddress, routingWasLoaded, blockchainWasLoaded);
            
        ScheduledFuture<?> future;

        Runnable task = () -> {
            if (node.transactionPoolSize() == 0) return;

            Block newBlock = node.getBlockchain().createBlock(node.getTransactionPool());
            node.clearTransactionPool();
            peer.store(newBlock);
        };

        future = scheduler.scheduleAtFixedRate(task, Utils.TRANS_POOL_LIMIT_TIME, Utils.TRANS_POOL_LIMIT_TIME, TimeUnit.SECONDS);

        Scanner in = new Scanner(System.in);

        /* Test 
        Block tempBlock = node.getBlockchain().addBlock(null, node.getBlockchain().getChains().get(0));

        Block tempBlock2 = node.getBlockchain().addBlock(null, node.getBlockchain().getChains().get(0));

        tempBlock2.getBlockHeader().setPrevHash(tempBlock.getBlockHeader().getHash());

        Block tempBlock3 = node.getBlockchain().addBlock(null, node.getBlockchain().getChains().get(0));

        tempBlock3.getBlockHeader().setPrevHash(tempBlock.getBlockHeader().getPrevHash());
        */

        while (true) {
            System.out.println("\n=======================================");
            System.out.println("      PUBLIC LEDGER FOR AUCTIONS");
            System.out.println("=======================================");
            System.out.println("1. View blockchain");
            System.out.println("2. View Routing Table");
            System.out.println("3. Create new transaction");
            System.out.println("4. View Total Reward");
            System.out.println("5. Hacker Menu");
            System.out.println("0. Exit");
            System.out.println("\nChoose an option: ");

            String option = in.nextLine().trim();

            switch (option) {
                case "1":
                    System.out.println("=== Blockchain ===");

                    // Prepare block lookups
                    Map<String, Block> blockMap = new HashMap<>();
                    Map<String, List<Block>> childrenMap = new HashMap<>();

                    List<Chain> chains = node.getBlockchain().getChains();

                    // First, index all blocks
                    for (Chain chain : chains) {
                        for (Block block : chain.getBlocks()) {
                            String hash = block.getBlockHeader().getHash();
                            String prevHash = block.getBlockHeader().getPrevHash();
                            blockMap.put(hash, block);
                            childrenMap.computeIfAbsent(prevHash, k -> new ArrayList<>()).add(block);
                        }
                    }

                    // Find roots (genesis blocks or starting points of forks)
                    List<Block> roots = new ArrayList<>();
                    for (Block block : blockMap.values()) {
                        if (!blockMap.containsKey(block.getBlockHeader().getPrevHash())) {
                            roots.add(block);
                        }
                    }

                    // Now print all roots
                    for (Block root : roots) {
                        printForkTree(root, "", false, childrenMap);
                    }

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
                                String timestamp = n.length > 3 ? n[3] : "unknown-timestamp";

                                System.out.println("  - IP: " + ip + ", Port: " + Integer.valueOf(p)+ ", ID: " + id + ", Timestamp: " + timestamp);
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
                        System.out.println("\nChoose an option: ");
                        
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
                                myAuctions.put(random, item);
                                System.out.println("Auction created with id: " + random);
                                fullPoolLogic(node,peer,create,future,task);
                                break;
                            case "2":
                                for (Map.Entry<Integer, String> entry : myAuctions.entrySet()) {
                                    System.out.println("- " + entry.getValue() + " (id=" + entry.getKey() + ")");
                                }

                                System.out.print("Auction id: ");
                                int id = Integer.parseInt(in.nextLine().trim());
                                item = myAuctions.get(id);
                                if (item == null) {
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
                                fullPoolLogic(node,peer,start, future,task);
                                break;
                            case "3":
                                Set<String> activeAuctions = peer.getActiveAuctions();
                                int index = 1;
                                for (String auction : activeAuctions) {
                                    System.out.println(index + ". " + auction);
                                    index++;
                                }
                                  
                                System.out.print("Auction id: ");
                                id = Integer.parseInt(in.nextLine().trim());

                                item = null;
                                for (String auctionStr : activeAuctions) {
                                    if (auctionStr.matches(".*\\(id=\\s*" + id + "\\)")) {
                                        item = auctionStr.split(" \\(id=")[0].trim();; 
                                        break;
                                    }
                                }

                                if (item == null) {
                                    System.out.println("Invalid Auction id or auction not active.");
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
                                fullPoolLogic(node,peer,bid, future,task);

                                break;
                            case "4":
                                for (Map.Entry<Integer, String> entry : myAuctions.entrySet()) {
                                    System.out.println("- " + entry.getValue() + " (id=" + entry.getKey() + ")");
                                }

                                System.out.print("Auction id: ");
                                id = Integer.parseInt(in.nextLine().trim());
                                item = myAuctions.get(id);
                                if (item == null) {
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
                                myAuctions.remove(id);
                                fullPoolLogic(node,peer,close,future,task);

                                break;
                            default:
                                break;
                        }
                        if(option.equals("0")) break;
                    }
                    break;
                case "4":
                    double reward = 0.0;
                    for (Chain chain : node.getBlockchain().getChains()) {
                        for (Block block : chain.getBlocks()) {
                            BlockHeader header = block.getBlockHeader();
                            String signature = header.getSignature();

                            if (header.verifyBlockHeader(signature, node.getPublicKey())) {
                                reward += Utils.BLOCK_REWARD;
                            }
                        }
                    }
                    System.out.println("Total reward for this node: " + reward);
                    break;
                    
                case "5":
                    while (true) {
                        System.out.println("\n=======================================");
                        System.out.println("           HACKER MENU ");
                        System.out.println("=======================================");
                        System.out.println("1. Manually create custom block");
                        System.out.println("0. Go back");
                        System.out.println("\nChoose an option: ");

                        String hackerOption = in.nextLine().trim();

                        switch (hackerOption) {
                            case "1":
                                System.out.print("Enter target node IP: ");
                                String targetNodeIp = in.nextLine().trim();

                                System.out.print("Enter target node Port: ");
                                String targetNodePort = in.nextLine().trim();

                                System.out.print("Enter target node ID: ");
                                String targetNodeId = in.nextLine().trim();

                                Block customBlock = createManualBlock(in, node);
                                if (customBlock != null) {
                                    String[] receiver = new String[] { targetNodeIp, targetNodePort, targetNodeId }; 

                                    Communication customMsg = new Communication(
                                        Communication.MessageType.STORE,
                                        customBlock.toString(),
                                        nodeContact,
                                        receiver
                                    );

                                    System.out.print("Enter custom base64 signature for Communication (or leave blank or type 'mine' to auto-sign): ");
                                    String commSig = in.nextLine().trim();

                                    if (commSig.equalsIgnoreCase("mine")) {
                                        String autoSig = customMsg.signCommunication(node.getPrivateKey());
                                        customMsg.setSignature(autoSig);
                                        System.out.println("Communication auto-signed.");
                                    } else if (!commSig.isEmpty()) {
                                        customMsg.setSignature(commSig);
                                        System.out.println("Custom signature applied.");
                                    } else {
                                        System.out.println("No Communication signature set (null).");
                                    }

                                    Communication response = peer.sendMessage(receiver, customMsg);
                                    if (response == null) {
                                        System.out.println("No response from node.");
                                    }
                                }

                                break;
                            default:
                                break;
                        }
                        if(hackerOption.equals("0")) break;
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
    public static Block createManualBlock(Scanner in, Node node) {
        try {
            System.out.print("Enter prevHash: ");
            String prevHash = in.nextLine().trim();

            System.out.print("Enter nounce (int): ");
            int nounce = Integer.parseInt(in.nextLine().trim());

            System.out.print("Enter timestamp (millis) or 'now': ");
            String tsInput = in.nextLine().trim();
            long millis = tsInput.equalsIgnoreCase("now")
                ? System.currentTimeMillis()
                : Long.parseLong(tsInput);
            Timestamp timestamp = new Timestamp(millis);

            List<Transaction> transactions = new ArrayList<>();
            while (true) {
                System.out.print("Add transaction? (y/n): ");
                if (!in.nextLine().trim().equalsIgnoreCase("y")) break;

                System.out.print("  - Type (CREATE_AUCTION / START_AUCTION / BID / CLOSE_AUCTION): ");
                Transaction.Type type = Transaction.Type.valueOf(in.nextLine().trim());

                System.out.print("  - Auction ID (int): ");
                int auctionId = Integer.parseInt(in.nextLine().trim());

                System.out.print("  - Info: ");
                String info = in.nextLine().trim();

                Transaction tx = new Transaction(type, node, auctionId, info); 
                transactions.add(tx);
            }

            Block block = new Block(transactions, nounce, prevHash);
            block.getBlockHeader().setTimestamp(timestamp);


            System.out.print("Enter custom Merkle Root (or leave blank to keep auto): ");
            String customMerkleRoot = in.nextLine().trim();
            if (!customMerkleRoot.isEmpty()) {
                block.getBlockHeader().setMerkleRoot(customMerkleRoot);
            }
            String hash = Utils.hashSHA256(block.getBlockHeader());
            block.getBlockHeader().setHash(hash);

            Blockchain.MatchResult storeBlock = node.getBlockchain().storeBlock(block);

            System.out.print("Enter custom base64 signature for BlockHeader (leave blank OR type 'mine' to auto-sign): ");
            String sig = in.nextLine().trim();

            if (sig.equalsIgnoreCase("mine")) {
                String autoSignature = block.getBlockHeader().signBlockHeader(node.getPrivateKey());
                System.out.println(autoSignature);
                block.getBlockHeader().setSignature(autoSignature);
                System.out.println("BlockHeader auto-signed.");
            } else if (!sig.isEmpty()) {
                block.getBlockHeader().setSignature(sig);
            } else {
                System.out.println("No signature set (null).");
            }


            return block;
        } catch (Exception e) {
            System.out.println("Error creating custom block: " + e.getMessage());
            return null;
        }
    }

}


