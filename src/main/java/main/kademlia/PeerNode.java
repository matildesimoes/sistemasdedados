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

        String[] bootstrapNodeContact = {bootstrapIp, String.valueOf(bootstrapPort), bootstrapNode.getNodeId(), ""};

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
                    this.selfNode.getNodeIp() + "," + String.valueOf(this.selfNode.getNodePort()) + "," + this.selfNode.getNodeId() + "," + this.selfNode.getTimeAlive(),
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
        Map<String, Integer> blockFrequency = new HashMap<>();

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
                    if (!blocks.isEmpty()) {
                        peerRecentBlocks.put(peer[2], blocks);
                        for (Block b : blocks) {
                            String hash = b.getBlockHeader().getHash();
                            blockFrequency.put(hash, blockFrequency.getOrDefault(hash, 0) + 1);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not parse blocks from " + peer[2]);
                }
            }
        }

        if (peerRecentBlocks.isEmpty()) {
            System.out.println("No recent blocks received. Skipping verification.");
            return;
        }

        Set<String> compatiblePeers = new HashSet<>();

        for (Map.Entry<String, List<Block>> entry : peerRecentBlocks.entrySet()) {
            String peerId = entry.getKey();
            List<Block> blocks = entry.getValue();

            boolean hasSharedBlock = blocks.stream()
                .map(b -> b.getBlockHeader().getHash())
                .anyMatch(h -> blockFrequency.getOrDefault(h, 0) > 1);

            if (hasSharedBlock) {
                compatiblePeers.add(peerId);
            }
        }

        int totalPeers = peerRecentBlocks.size();
        int majorityThreshold = totalPeers / 2 + 1;
        System.out.println("Peers with at least one shared block: " + compatiblePeers.size() + "/" + totalPeers);

        boolean isConsistent = compatiblePeers.size() >= majorityThreshold;


        if (skipFindBlockchain) {
            if (isConsistent) {
                System.out.println("Chain is consistent with majority. Attempting to integrate matching blocks...");

                for (Map.Entry<String, List<Block>> entry : peerRecentBlocks.entrySet()) {
                    String peerId = entry.getKey();
                    if (!compatiblePeers.contains(peerId)) continue;

                    List<Block> blocks = entry.getValue();
                    Block firstBlock = blocks.get(0);

                    if (selfNode.getBlockchain().hasBlock(firstBlock.getBlockHeader().getHash())) {
                        System.out.println(" Local chain already contains blocks from peer " + peerId + ". No action needed.");
                        return;
                    }

                    boolean linked = false;
                    String prevHash = firstBlock.getBlockHeader().getPrevHash();
                    int depth = 0;

                    while (depth < Utils.CHAIN_LINK_DEPTH_LIMIT && prevHash != null) {
                        if (selfNode.getBlockchain().hasBlock(prevHash)) {
                            linked = true;
                            break;
                        }

                        final String currentPrevHash = prevHash;
                        Optional<Block> prevBlock = blocks.stream()
                            .filter(b -> b.getBlockHeader().getHash().equals(currentPrevHash))
                            .findFirst();

                        if (prevBlock.isPresent()) {
                            prevHash = prevBlock.get().getBlockHeader().getPrevHash();
                        } else {
                            break;
                        }

                        depth++;
                    }

                    if (linked) {
                        System.out.println(" Blocks from peer " + peerId + " link to local chain. Integrating...");
                        for (Block b : blocks) {
                            this.tryStoreWithOrphans(b);
                        }
                        return;
                    }
                }

                System.out.println(" No compatible blocks linked to local chain. Replacing with majority chain...");
                requestAndReplaceBlockchain(peersToAsk.get(0));

            } else {
                System.out.println(" Chain is inconsistent with majority. Replacing with majority chain...");
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
                    String signatureCommunication = blockHash.signCommunication(this.selfNode.getPrivateKey());
                    blockHash.setSignature(signatureCommunication);

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

