package main.kademlia;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.io.Serializable;
import java.util.List;

import main.Utils;

public class Server implements Serializable{
    private final String ip;
    private final int port;
    private final RoutingTable routingTable; 
    private final Node selfNode;
    private final ConcurrentMap<String, Integer> pendingChallenges;

    public Server(String ip, int port, RoutingTable routingTable, Node selfNode){
        this.ip = ip;
        this.port = port;
        this.routingTable = routingTable;
        this.selfNode = selfNode;
        this.pendingChallenges = new ConcurrentHashMap<>();
    }
    
    public void start(){
        ExecutorService executor = Executors.newCachedThreadPool();

        try(ServerSocket serverSocket = new ServerSocket(port)){

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(() -> handleClient(socket));
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
        ) {
            Communication msg = (Communication) input.readObject();
            Node sender = msg.getSender();
            String response = "";
            Communication newMsg = null;
            
            switch(msg.getType()){
                case PING: 
                    if(!this.routingTable.nodeExist(sender)){
                        int challenge = Utils.createRandomNumber(16);
                        pendingChallenges.put(sender.getNodeId(), challenge);
                        
                        newMsg = new Communication(Communication.MessageType.ACK, String.valueOf(challenge), this.selfNode, sender);
                        output.writeObject(newMsg);
                        output.flush();
                    }else{
                        response = "PING Received.";
                        System.out.println(response);            
                        newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNode, sender);
                        output.writeObject(newMsg);
                        break; 
                    }
                    break;
                case CHALLENGE: 
                    int challenge = pendingChallenges.get(sender.getNodeId());
                    String string = sender.getNodeId() + challenge + msg.getInformation();
                    String validateHash = Utils.hashSHA256(string);
                    String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY); 

                    if(!validateHash.startsWith(prefix)){
                        response = "Wrong challenge response.";
                        System.out.println(response);            
                        newMsg = new Communication(Communication.MessageType.NACK, response, this.selfNode, sender);
                        output.writeObject(newMsg);
                        break;
                    }
                    
                    response = "CHALLENGE completed.";
                    System.out.println(response);            
                    newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNode, sender);
                    output.writeObject(newMsg);
                    break; 
                case FIND_NODE:
                    String[] nodeContact = msg.getInformation().split(",");

                    String[] test = new String[] {"127.0.0.1","5000","2T/+UTBm5DpTbbLqVVJ1d4u3Q04="};
                    String[] test2 = new String[] {"127.0.0.1","5001","2T/+UTBm5DpTqwLqVcJ0F2u3Q04="};
                    Bucket bucket = new Bucket(Utils.BUCKET_SIZE);
                    bucket.update(test);
                    bucket.update(test2);
                    this.routingTable.addBucket(bucket);

                    if(!this.routingTable.nodeExist(sender)){
                        this.routingTable.addNodeToBucket(nodeContact);
                    }

                    List<String[]> closest = this.routingTable.findClosest(sender.getNodeId(), Utils.BUCKET_SIZE);       

                    String closestNodes = "";
                    for(String[] s : closest){
                        closestNodes += s[0] +","+ s[1] + ","+ s[2] + "-";

                    }

                    newMsg = new Communication(Communication.MessageType.FIND_NODE, closestNodes, this.selfNode, sender);
                    output.writeObject(newMsg);
                    break;
                default:
                    System.out.println("Unknown message Type.");            
                    break;

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
