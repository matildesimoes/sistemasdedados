package main.kademlia;

import main.Utils;
import main.blockchain.*;
import main.blockchain.Blockchain.MatchResult;


import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class PeerNode {
    private final Node selfNode;
    private final String[] selfNodeContact;
    private final Set<String> activeAuctions = new HashSet<>();
    private final List<Block> orphanBlocks = new ArrayList<>();
    private final ConcurrentMap<String, Integer> pendingChallenges = new ConcurrentHashMap<>();
    private final MessageHandler messageHandler;

    public PeerNode(Node selfNode) {
        this.selfNode = selfNode;
        this.selfNodeContact = new String[]{selfNode.getNodeIp(), String.valueOf(selfNode.getNodePort()), selfNode.getNodeId()};
        this.messageHandler = new MessageHandler(this, orphanBlocks);
    }

    public Set<String> getActiveAuctions(){
        return this.activeAuctions;
    }

    public void addActiveAuctions(Transaction trans){
        this.activeAuctions.add(trans.getInformation() + "(id= " + trans.getAuctionNumber() + ")");
    }

    public void removeActiveAuctions(Transaction trans){
        this.activeAuctions.remove(trans.getInformation() + "(id= " + trans.getAuctionNumber() + ")");
    }

    public Node getSelfNode() {
        return this.selfNode;
    }

    public Map<String, Integer> getPendingChallenges() {
        return this.pendingChallenges;
    }


    public void startListener() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(selfNode.getNodePort())) {
                System.out.println("[+] Listening on port " + selfNode.getNodePort());
                while (true) {
                    Socket socket = serverSocket.accept();
                    handleIncomingRequest(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleIncomingRequest(Socket socket) {
        try (
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        ) {
            Communication msg = (Communication) input.readObject();
            messageHandler.handle(msg, output);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Communication sendMessage(String[] receiver, Communication message) {
        try (
            Socket socket = new Socket(receiver[0], Integer.valueOf(receiver[1]));
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream())
        ) {
            output.writeObject(message);
            output.flush();

            switch (message.getType()) {
                case STORE:
                    Block block = Block.fromString(message.getInformation());
                    System.out.println("Store sent (to " + receiver[2] + "): " + block.getBlockHeader().getHash());
                    break;
                default:
                    System.out.println("Message sent (to " + receiver[2] + "): " + message.getInformation());
            }

            Communication response = (Communication) input.readObject();

            switch (response.getType()) {
                case ACK:
                    if (response.getInformation().equals("CHALLENGE completed.")) {
                        selfNode.setTimeAlive(Timestamp.from(Instant.now()));
                    } else if (response.getInformation().equals("PING Received.")) {
                        System.out.println("Node: " + receiver[2] + " is alive!");
                    }
                    break;
                case NACK:
                    if (response.getInformation().equals("Wrong challenge response.")) {
                        input.close();
                        output.close();
                        socket.close();
                        System.exit(0);
                    }
                    System.out.println("Received response (from " + receiver[2] + "): " + response.getInformation());
                    break;
                case FIND_NODE:
                    String[] groups = response.getInformation().split("-");
                    RoutingTable routingTable = selfNode.getRoutingTable();
                    for (String group : groups) {
                        String[] nodeContact = group.split(",");
                        if (!routingTable.nodeExist(nodeContact))
                            routingTable.addNodeToBucket(nodeContact);
                    }
                    System.out.println("Received response (from " + receiver[2] + "): " + response.getInformation());
                    break;
                case FIND_VALUE:
                    String info = response.getInformation();

                    if (info.startsWith("findPrevBlock|")) {
                        String prevBlockHash = info.substring("findPrevBlock|".length()).trim();

                        Blockchain selfBlockchain = this.selfNode.getBlockchain();
                        boolean found = false;

                        for (Chain chain : selfBlockchain.getChains()) {
                            for (Block b : chain.getBlocks()) {
                                if (b.getBlockHeader().getHash().equals(prevBlockHash)) {
                                    System.out.println("Found requested block: " + prevBlockHash);

                                    Communication storeMsg = new Communication(
                                        Communication.MessageType.STORE,
                                        b.toString(),
                                        selfNodeContact,
                                        receiver
                                    );
                                    storeMsg.setSignature(storeMsg.signCommunication(selfNode.getPrivateKey()));
                                    this.sendMessage(receiver,storeMsg);

                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }

                        if (!found) {
                            System.out.println("Could not find requested block: " + prevBlockHash);

                            Communication nack = new Communication(
                                Communication.MessageType.NACK,
                                "Block not found: " + prevBlockHash,
                                selfNodeContact,
                                receiver
                            );
                            this.sendOneWayMessage(receiver,nack);
                        }

                        break;
                    }

                    try {
                        Block receivedBlock = Block.fromString(info);
                        System.out.println("Received block from " + receiver[2] + ": " + receivedBlock.getBlockHeader().getHash());

                        Blockchain.MatchResult stored = this.selfNode.getBlockchain().storeBlock(receivedBlock);
                        
                        if (stored.equals(MatchResult.MATCH_FOUND)) 
                            this.selfNode.getBlockchain().recalculateHeights();

                    } catch (Exception e) {
                        System.out.println("Value was not a valid block: " + info);
                    }

                    break;

                case FIND_BLOCKCHAIN:
                    System.out.println("Received Blockchain (from " + receiver[2] + ")");
                    break;
                default:
                    System.out.println("Received response (from " + receiver[2] + "): " + response.getInformation());
            }
            return response;

        } catch (Exception e) {
            System.err.println("Failed to communicate with node " + receiver[2] + ". Error:  " + e.toString());
            return null;
        }
    }

    public void sendOneWayMessage(String[] receiver, Communication message) {
        try (
            Socket socket = new Socket(receiver[0], Integer.parseInt(receiver[1]));
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        ) {
            output.writeObject(message);
            output.flush();
            System.out.println("One-way message sent to " + receiver[2] + ": " + message.getType());
        } catch (Exception e) {
            System.err.println("Failed to send one-way message: " + e.getMessage());
        }
    }

    public void joinNetwork(String bootstrapAddress,boolean skipFindNode, boolean skipFindBlockchain){

        String[] parts = bootstrapAddress.split(":");
        String bootstrapIp = parts[0];
        int bootstrapPort = Integer.parseInt(parts[1]);

        Node bootstrapNode = new Node(bootstrapIp,bootstrapPort);

        String[] bootstrapNodeContact = {bootstrapIp, String.valueOf(bootstrapPort), bootstrapNode.getNodeId()};

        File challengeFile = new File("data/challenge.txt");
        String nounceStr;

        if (challengeFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(challengeFile))) {
                nounceStr = reader.readLine();
                System.out.println("Loaded stored challenge solution: " + nounceStr);
            } catch (IOException e) {
                System.err.println("Error reading stored challenge: " + e.getMessage());
                return;
            }
        } else {

            int nounce =  Utils.createRandomNumber(999999);
            String string = this.selfNode.getNodeId() + nounce;
            String hash = Utils.hashSHA256(string);
            String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY);
            while(!hash.startsWith(prefix)){
                nounce =  Utils.createRandomNumber(999999);
                string = this.selfNode.getNodeId() + nounce;
                hash = Utils.hashSHA256(string);
            }
            
            nounceStr = String.valueOf(nounce);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(challengeFile))) {
                writer.write(nounceStr);
            } catch (IOException e) {
                System.err.println("Error saving challenge: " + e.getMessage());
            }
        }

        Communication challenge = new Communication(
            Communication.MessageType.CHALLENGE,
            nounceStr,
            this.selfNodeContact,
            bootstrapNodeContact
        );

        Communication response = this.sendMessage(bootstrapNodeContact, challenge);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

        if (!skipFindNode) {
            System.out.println("Starting node discovery (FIND_NODE)...");
            Set<String> visited = new HashSet<>();
            Queue<String[]> toVisit = new LinkedList<>();

            toVisit.add(bootstrapNodeContact);
            
            int steps = 0;
            while (!toVisit.isEmpty() && steps < Utils.RECURSIVE_FIND_NODE) {
                String[] current = toVisit.poll();
                String nodeId = current[2];

                if (visited.contains(nodeId)) continue;
                visited.add(nodeId);

                Communication find = new Communication(
                    Communication.MessageType.FIND_NODE,
                    this.selfNode.getNodeIp() + "," + String.valueOf(this.selfNode.getNodePort()) + "," + this.selfNode.getNodeId(),
                    this.selfNodeContact,
                    current
                );

                response = this.sendMessage(current, find);

                if (response != null) {
                    List<String[]> closest = parseClosestNodes(response.getInformation()); // IP, PORT, ID
                    for (String[] contact : closest) {
                        String contactId = contact[2];
                        if (!visited.contains(contactId) && !selfNode.getRoutingTable().nodeExist(contact)) {
                            toVisit.add(contact);
                        }
                    }
                }
                steps++;
            }
        } else {
            System.out.println("Skipped FIND_NODE — routing table loaded from disk.");
        }

        System.out.println(" Verifying blockchain consistency with peers...");

        List<String[]> availablePeers = selfNode.getRoutingTable().getAllKnownNodes(selfNodeContact[2]);
        if (availablePeers.isEmpty()) {
            System.out.println("Not enough peers in routing table to verify blockchain consistency.");
            return;
        }

        int numPeersToAsk = Math.min(Utils.PEER_MAJORITY_THRESHOLD, availablePeers.size());
        List<String[]> peersToAsk = selfNode.getRoutingTable().findClosest(selfNodeContact[2], numPeersToAsk);

        Map<String, List<Block>> peerRecentBlocks = new HashMap<>();
        Map<String, Integer> blockHashVotes = new HashMap<>();

        for (String[] peer : peersToAsk) {
            Communication req = new Communication(
                Communication.MessageType.RECENT_BLOCKS_REQUEST,
                "give me blocks",
                selfNodeContact,
                peer
            );
            Communication res = this.sendMessage(peer, req);

            if (res != null && res.getType() == Communication.MessageType.RECENT_BLOCKS_REQUEST) {
                try {
                    List<Block> blocks = Block.parseBlockList(res.getInformation());
                    if (blocks.size() > 0) {
                        String firstHash = blocks.get(0).getBlockHeader().getHash();
                        blockHashVotes.put(firstHash, blockHashVotes.getOrDefault(firstHash, 0) + 1);
                        peerRecentBlocks.put(peer[2], blocks);
                    }
                } catch (Exception e) {
                    System.out.println("Could not parse blocks from " + peer[2]);
                }
            }
        }

        if (blockHashVotes.isEmpty()) {
            System.out.println("No recent blocks received. Skipping verification.");
            return;
        }

        // Determine majority
        String majorityFirstHash = null;
        int maxVotes = 0;
        for (Map.Entry<String, Integer> entry : blockHashVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                majorityFirstHash = entry.getKey();
                maxVotes = entry.getValue();
            }
        }

        boolean isConsistent = false;

        if (skipFindBlockchain) {
            String localLatestHash = selfNode.getBlockchain().getLatestBlock().getBlockHeader().getHash();

            // pega blocos do peer que tem o bloco mais votado
            for (Map.Entry<String, List<Block>> entry : peerRecentBlocks.entrySet()) {
                List<Block> blocks = entry.getValue();
                String firstHash = blocks.get(0).getBlockHeader().getHash();
                if (!firstHash.equals(majorityFirstHash)) continue;

                // tenta verificar ligação via prevHash até profundidade limitada
                String prevHash = blocks.get(0).getBlockHeader().getPrevHash();
                int depth = 0;
                Block localLatest = selfNode.getBlockchain().getLatestBlock();

                if (localLatest.getBlockHeader().getHash().equals(firstHash)) {
                    System.out.println("Local chain already contains the majority's latest block. Chain is consistent.");
                    return;
                }
                while (depth < Utils.CHAIN_LINK_DEPTH_LIMIT && prevHash != null) {
                    if (selfNode.getBlockchain().hasBlock(prevHash)) {
                        isConsistent = true;
                        break;
                    }
                    for (Block b : blocks) {
                        if (b.getBlockHeader().getHash().equals(prevHash)) {
                            prevHash = b.getBlockHeader().getPrevHash();
                            break;
                        }
                    }
                    depth++;
                }

                if (isConsistent) {
                    System.out.println(" Majority blocks link to local chain. Integrating...");
                    for (Block b : blocks) {
                        this.tryStoreWithOrphans(b);
                    }
                    break;
                }
            }

            if (!isConsistent) {
                System.out.println(" Local blockchain is inconsistent. Replacing with majority chain...");
                requestAndReplaceBlockchain(peersToAsk.get(0));
            }

        } else {
            System.out.println("No local chain, trusting majority.");
            requestAndReplaceBlockchain(peersToAsk.get(0));
        }
    }

    public void tryStoreWithOrphans(Block block) {
        String prevHash = block.getBlockHeader().getPrevHash();
        if (prevHash == null) {
            System.out.println("Block has null prevHash, likely genesis or invalid block. Skipping FIND_VALUE.");
            return;
        }
        MatchResult result = selfNode.getBlockchain().storeBlock(block);

        if (result.equals(MatchResult.NOT_FOUND)) {
            orphanBlocks.add(block);

            if (orphanBlocks.size() >= Utils.ORPHAN_LIMIT) {
                System.out.println("Discarded Orphan Blocks.");
                orphanBlocks.clear();
            }


        } else if (result.equals(MatchResult.MATCH_FOUND)) {
            for (Block orphan : new ArrayList<>(orphanBlocks)) {
                MatchResult stored = selfNode.getBlockchain().storeBlock(orphan);
                if (stored.equals(MatchResult.MATCH_FOUND)) {
                    selfNode.getBlockchain().recalculateHeights();
                }
            }
            orphanBlocks.clear();
        }
    }


    public List<String[]> findValue(String hash){
        List<String[]> nodesWithoutBlock = new ArrayList<>(); 
        RoutingTable selfRoutingTable = this.selfNode.getRoutingTable();
        List<Bucket> bucketsSnapshot = new ArrayList<>(selfRoutingTable.getBuckets());

        for(Bucket b : bucketsSnapshot){
            List<String[]> nodesSnapshot = new ArrayList<>(b.getNodes());
            for(String[] nodeContact : nodesSnapshot){
                if(!nodeContact[2].equals(this.selfNodeContact[2])){
                    Communication blockHash = new Communication(
                        Communication.MessageType.FIND_VALUE,
                        hash,
                        this.selfNodeContact,
                        nodeContact
                    );
                    Communication response = this.sendMessage(nodeContact, blockHash);

                    if (response == null) {
                        System.out.println("No response from node.");
                        continue;
                    }

                    if(response.getType() == Communication.MessageType.NACK){
                        nodesWithoutBlock.add(nodeContact);

                        Communication findNode = new Communication(
                            Communication.MessageType.FIND_NODE,
                            this.selfNodeContact[2],
                            this.selfNodeContact,
                            nodeContact
                        );
                        response = this.sendMessage(nodeContact, findNode);

                        if (response == null) {
                            System.out.println("No response from node.");
                            continue;
                        }else{
                            List<String[]> closest = parseClosestNodes(response.getInformation()); // IP, PORT, ID
                            for (String[] contact : closest) {
                                String contactId = contact[2];
                                if (!selfRoutingTable.nodeExist(contact)) {
                                    this.selfNode.getRoutingTable().addNodeToBucket(contact);
                                }
                            }
                        }

                    }

                }
            }
        }
        return nodesWithoutBlock;
    }

    public void store(Block block){
        List<String[]> nodesWithoutBlock = findValue(block.getBlockHeader().getHash());
        
        String signatureBlockHeader = block.getBlockHeader().signBlockHeader(this.selfNode.getPrivateKey());
        block.getBlockHeader().setSignature(signatureBlockHeader);

        String blockString = block.toString();

        for(String[] nodeContact : nodesWithoutBlock){
            if(!nodeContact[2].equals(this.selfNodeContact[2])){

                Communication store = new Communication(
                    Communication.MessageType.STORE,
                    blockString,
                    this.selfNodeContact,
                    nodeContact
                );

                String signatureCommunication = store.signCommunication(this.selfNode.getPrivateKey());
                store.setSignature(signatureCommunication);
                
                Communication response = this.sendMessage(nodeContact, store);
                if (response == null) {
                    System.out.println("No response from node.");
                    break;
                }
            }
        }


    }

    public void checkIfNodeAlive(){
        List<Bucket> buckets = this.selfNode.getRoutingTable().getBuckets();
        for(Bucket bucket : buckets){
            List<String[]> nodes = bucket.getNodes();
            for(String[] nodeContact : nodes){
                if(!nodeContact[2].equals(this.selfNodeContact[2])){
                    Communication ping = new Communication(
                        Communication.MessageType.PING,
                        "are you alive?",
                        this.selfNodeContact,
                        nodeContact
                    );

                    Communication response = this.sendMessage(nodeContact, ping);

                    if (response == null) {
                        System.out.println("No PING response from node.");
                        this.selfNode.getRoutingTable().removeNode(nodeContact[2]);
                        return;
                    }
                }
                
            }
        }

    }

    public List<String[]> parseClosestNodes(String data) {
        List<String[]> nodes = new ArrayList<>();
        
        if (data == null || data.isEmpty()) return nodes;

        String[] entries = data.split("-");

        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                nodes.add(parts); 
            }
        }

        return nodes;
    }

    private void requestAndReplaceBlockchain(String[] peer) {
        Communication getFull = new Communication(
            Communication.MessageType.FIND_BLOCKCHAIN,
            "full chain request",
            this.selfNodeContact,
            peer
        );

        Communication res = this.sendMessage(peer, getFull);
        if (res != null && res.getType() == Communication.MessageType.FIND_BLOCKCHAIN) {
            List<Chain> chains = Blockchain.blockchainFromString(res.getInformation());
            selfNode.getBlockchain().setChains(chains);
            selfNode.getBlockchain().recalculateHeights();
            selfNode.getBlockchain().saveBlockchain();
            System.out.println("Blockchain successfully replaced from " + peer[2]);
        } else {
            System.out.println("Failed to fetch blockchain from peer.");
        }
    }

}

