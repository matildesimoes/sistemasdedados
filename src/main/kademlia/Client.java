package main.kademlia;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;


import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


public class Client{
    private final Node selfNode;
    
   public Client(Node selfNode){
        this.selfNode = selfNode;

   }

   public Communication sendMessage(Node receiver, Communication message) {
        try (
            Socket socket = new Socket(receiver.getNodeIp(), receiver.getNodePort());
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
                        receiver.setTimeAlive(Timestamp.from(Instant.now()));
                    }
                    receiver.setLatestPing(Timestamp.from(Instant.now()));
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
            System.err.println("Failed to communicate with node " + receiver.getNodeId());
            if (message.getType() == Communication.MessageType.PING) {
                System.out.println("Node " + receiver.getNodeId() + " did not respond to PING. Removing from routing table.");
                this.selfNode.getRoutingTable().removeNode(receiver.getNodeId());
            }
            return null;
        }
    } 
}
