package main.kademlia;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;

import main.*;
import main.blockchain.*;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;

import com.fasterxml.jackson.databind.ObjectMapper;



public class Client{
    private final Node selfNode;
    private final String[] selfNodeContact;
    
   public Client(Node selfNode){
        this.selfNode = selfNode;
        this.selfNodeContact = new String[]{this.selfNode.getNodeId(), String.valueOf(this.selfNode.getNodePort()), this.selfNode.getNodeId()};

   }

   public Communication sendMessage(String[] receiver, Communication message) {
        try (
            Socket socket = new Socket(receiver[0], Integer.valueOf(receiver[1]));
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream())
        ) {
            output.writeObject(message);
            output.flush();

            switch(message.getType()){
                case STORE:
                    Block block = Block.fromString(message.getInformation());
                    System.out.println("Store sent (to " + receiver[2]+"): " + block.getBlockHeader().getHash());
                    break;
                default:
                    System.out.println("Message sent (to " + receiver[2]+"): " + message.getInformation());
            }

            Communication response = (Communication) input.readObject();

            switch(response.getType()){
                case ACK: 
                    if(response.getInformation().equals("CHALLENGE completed.")){
                        this.selfNode.setTimeAlive(Timestamp.from(Instant.now()));
                    }else if(response.getInformation().equals("PING Received.")){
                        System.out.println("Node: " + receiver[2] + " is alive!");
                    }
                    break; 
                case NACK: 
                    if(response.getInformation().equals("Wrong challenge response.")){
                        input.close();
                        output.close();
                        socket.close();
                        System.exit(0);
                    }
                    System.out.println("Received response (from "+ receiver[2] +"): "+ response.getInformation());
                    break;
                case FIND_NODE: 
                    String[] groups = response.getInformation().split("-");
                    
                    RoutingTable routingTable = this.selfNode.getRoutingTable();

                    for(String group : groups){
                        String[] nodeContact = group.split(",");
                        if(!routingTable.nodeExist(nodeContact))
                            routingTable.addNodeToBucket(nodeContact);
                    }
                    System.out.println("Received response (from "+ receiver[2] +"): "+ response.getInformation());
                    break;
                case FIND_VALUE: 
                    String info = message.getInformation();

                    if (info.startsWith("findPrevBlock|")) {
                        String prevBlockHash = info.substring("findPrevBlock|".length()).trim();

                        Blockchain selfBlockchain = this.selfNode.getBlockchain();
                        boolean found = false;

                        for (Chain chain : selfBlockchain.getChains()) {
                            for (Block b : chain.getBlocks()) {
                                if (b.getBlockHeader().getHash().equals(prevBlockHash)) {
                                    Communication newMsg = new Communication(
                                        Communication.MessageType.STORE,
                                        b.toString(), 
                                        this.selfNodeContact,
                                        receiver
                                    );
                                    String signatureCommunication = newMsg.signCommunication(this.selfNode.getPrivateKey());
                                    newMsg.setSignature(signatureCommunication);
                                    output.writeObject(newMsg);
                                    output.flush();
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }

                        if (!found) {
                            Communication newMsg = new Communication(
                                Communication.MessageType.NACK,
                                "Value not found.",
                                this.selfNodeContact,
                                receiver
                            );
                            output.writeObject(newMsg);
                            output.flush();
                        }
                        break;
                    }
                        
                    Block block = Block.fromString(message.getInformation());
                    System.out.println("Received Value (from " + receiver[2]+"): " + block.getBlockHeader().getHash());
                    break;
                case FIND_BLOCKCHAIN:
                    System.out.println("Received Blockchain (from " + receiver[2] + ")");
                    break;
                default:
                    System.out.println("Received response (from "+ receiver[2] +"): "+ response.getInformation());
            }
            return response;

        } catch (Exception e) {
            System.err.println("Failed to communicate with node " + receiver[2] + ". Error:  " + e.toString() );
            if (message.getType() == Communication.MessageType.PING) {
                System.out.println("Node " + receiver[2] + " did not respond to PING. Removing from routing table.");
                this.selfNode.getRoutingTable().removeNode(receiver[2]);
            }
            return null;
        }
    } 
    public void joinNetwork(String bootstrapAddress){

        String[] parts = bootstrapAddress.split(":");
        String bootstrapIp = parts[0];
        int bootstrapPort = Integer.parseInt(parts[1]);

        Node bootstrapNode = new Node(bootstrapIp,bootstrapPort);

        String[] bootstrapNodeContact = {bootstrapIp, String.valueOf(bootstrapPort), bootstrapNode.getNodeId()};


        Communication ping = new Communication(
            Communication.MessageType.CHALLENGE_INIT,
            "join?",
            this.selfNodeContact,
            bootstrapNodeContact
        );


        Communication response = this.sendMessage(bootstrapNodeContact, ping);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

        int nounce =  Utils.createRandomNumber(999999);
        String string = this.selfNode.getNodeId() + response.getInformation() + nounce;
        String hash = Utils.hashSHA256(string);
        String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY);
        while(!hash.startsWith(prefix)){
            nounce =  Utils.createRandomNumber(999999);
            string = this.selfNode.getNodeId() + response.getInformation() + nounce;
            hash = Utils.hashSHA256(string);
        }

        Communication challenge = new Communication(
            Communication.MessageType.CHALLENGE,
            String.valueOf(nounce),
            this.selfNodeContact,
            bootstrapNodeContact
        );

        response = this.sendMessage(bootstrapNodeContact, challenge);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

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

        RoutingTable selfRoutingTable = this.selfNode.getRoutingTable();
        List<String[]> closest = selfRoutingTable.findClosest(this.selfNodeContact[2], 1);

        Communication findBlockchain = new Communication(
            Communication.MessageType.FIND_BLOCKCHAIN,
            "I want your blockchain :)",
            this.selfNodeContact,
            closest.get(0)
        );

        response = this.sendMessage(bootstrapNodeContact, findBlockchain);

        if (response == null) {
            System.out.println("No response from bootstrap node.");
            return;
        }

        Blockchain selfBlockchain = this.selfNode.getBlockchain();
        List<Chain> chains = selfBlockchain.blockchainFromString(response.getInformation());
        selfBlockchain.setChains(chains);
        
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

}
