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

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


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
            System.out.println("Message sent!");

            Communication response = (Communication) input.readObject();
            System.out.println("Received response: " + response.getInformation());

            switch(response.getType()){
                case ACK: 
                    if(response.getInformation().equals("CHALLENGE completed.")){
                        this.selfNode.setTimeAlive(Timestamp.from(Instant.now()));
                    }
                    break; 
                case NACK: 
                    if(response.getInformation().equals("Wrong challenge response.")){
                        input.close();
                        output.close();
                        socket.close();
                        System.exit(0);
                    }
                case FIND_NODE: 
                    String[] groups = response.getInformation().split("-");
                    
                    RoutingTable routingTable = this.selfNode.getRoutingTable();

                    for(String group : groups){
                        String[] nodeContact = group.split(",");
                        routingTable.addNodeToBucket(nodeContact);
                    }
                    break;
            }
            return response;

        } catch (Exception e) {
            System.err.println("Failed to communicate with node " + receiver[2]);
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
            Communication.MessageType.PING,
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
                    if (!visited.contains(contactId)) {
                        toVisit.add(contact);
                    }
                }
            }
            steps++;
        }
        
    }

    public void checkIfNodeAlive(){
        List<Bucket> buckets = this.selfNode.getRoutingTable().getBuckets();
        for(Bucket bucket : buckets){
            List<String[]> nodes = bucket.getNodes();
            for(String[] nodeContact : nodes){
                if(!nodeContact.equals(this.selfNodeContact)){
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
