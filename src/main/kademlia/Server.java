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
            
            switch(msg.getType()){
                case PING: 
                    if(!this.routingTable.nodeExist(sender)){
                        int challenge = Utils.createRandomNumber(16);
                        pendingChallenges.put(sender.getNodeId(), challenge);
                        
                        Communication newMsg = new Communication(Communication.MessageType.ACK, String.valueOf(challenge), this.selfNode, sender);
                        output.writeObject(newMsg);
                        output.flush();
                    }else{
                        System.out.println("PING Received");
                    }
                    break;
                case CHALLENGE: 
                    int challenge = pendingChallenges.get(sender.getNodeId());
                    String string = sender.getNodeId() + challenge + msg.getInformation();
                    String validateHash = Utils.hashSHA256(string);

                    

                    String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY); 
                    if(!validateHash.startsWith(prefix)){
                        String response = "Wrong challenge response.";
                        System.out.println(response);            
                        Communication newMsg = new Communication(Communication.MessageType.NACK, response, this.selfNode, sender);
                        output.writeObject(newMsg);
                        break;
                    }
                    
                    String response = "Correct challenge response.";
                    System.out.println(response);            
                    Communication newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNode, sender);
                    output.writeObject(newMsg);
                    break; 
                case FIND_NODE:
                    String[] nodeContact = msg.getInformation().split(",");

                    List<String[]> closest = routingTable.findClosest(sender.getNodeId(), Utils.BUCKET_SIZE);       
                    this.selfNode.setRoutingTable();
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
