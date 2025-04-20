package main.kademlia;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.io.Serializable;
import java.util.List;

import main.Utils;
import main.blockchain.*;

public class Server implements Serializable{
    private final String ip;
    private final int port;
    private final RoutingTable routingTable; 
    private final Node selfNode;
    private final String[] selfNodeContact;
    private final ConcurrentMap<String, Integer> pendingChallenges;

    public Server(String ip, int port, RoutingTable routingTable, Node selfNode){
        this.ip = ip;
        this.port = port;
        this.routingTable = routingTable;
        this.selfNode = selfNode;
        this.selfNodeContact = new String[]{this.ip, String.valueOf(this.port), this.selfNode.getNodeId()};
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
            String[] sender = msg.getSender();
            String response = "";
            Communication newMsg = null;
            
            switch(msg.getType()){
                case PING: 
                    if(!this.routingTable.nodeExist(sender)){
                        int challenge = Utils.createRandomNumber(16);
                        pendingChallenges.put(sender[2], challenge);
                        
                        newMsg = new Communication(Communication.MessageType.ACK, String.valueOf(challenge), this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        output.flush();
                    }else{
                        response = "PING Received.";
                        System.out.println(response);            
                        newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break; 
                    }
                    break;
                case CHALLENGE: 
                    int challenge = pendingChallenges.get(sender[2]);
                    String string = sender[2] + challenge + msg.getInformation();
                    String validateHash = Utils.hashSHA256(string);
                    String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY); 

                    if(!validateHash.startsWith(prefix)){
                        response = "Wrong challenge response.";
                        System.out.println(response);            
                        newMsg = new Communication(Communication.MessageType.NACK, response, this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break;
                    }
                    
                    response = "CHALLENGE completed.";
                    System.out.println(response);            
                    newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break; 
                case FIND_NODE:
                    String[] nodeContact = msg.getInformation().split(",");

                    //String[] test = new String[] {"127.0.0.1","5000","2T/+UTBm5DpTbbLqVVJ1d4u3Q04="};
                    //this.routingTable.addNodeToBucket(test);

                    if(!this.routingTable.nodeExist(sender)){
                        this.routingTable.addNodeToBucket(nodeContact);
                    }

                    List<String[]> closest = this.routingTable.findClosest(sender[2], Utils.BUCKET_SIZE);       

                    String closestNodes = "";
                    for(String[] s : closest){
                        closestNodes += s[0] +","+ s[1] + ","+ s[2] + "-";

                    }
                    newMsg = new Communication(Communication.MessageType.FIND_NODE, closestNodes, this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                case FIND_VALUE:
                    String key = msg.getInformation();

                    Blockchain selfBlockchain = this.selfNode.getBlockchain();

                    boolean found = false;
                    for(Chain chain: selfBlockchain.getChains()){
                        for(Block block : chain.getBlocks()){
                            if(block.getBlockHeader().getHash().equals(key)){
                               newMsg = new Communication(Communication.MessageType.FIND_VALUE, block.toString(), this.selfNodeContact, sender);
                               output.writeObject(newMsg);
                               found = true;
                               break;
                            }
                        }
                        if (found) break;
                    }
                
                    if (!found) {
                        newMsg = new Communication(
                            Communication.MessageType.NACK,
                            "Value not found.",
                            this.selfNodeContact,
                            sender
                        );
                        output.writeObject(newMsg);
                    }
                    break;
                case STORE:
                    Block block = Block.fromString(msg.getInformation());

                    String newMerkleRoot = MerkleTree.getMerkleRoot(block.getTransaction()); 

                    if(!newMerkleRoot.equals(block.getBlockHeader().getMerkleRoot())){
                    
                        newMsg = new Communication(Communication.MessageType.NACK, "Invalid MerkleRoot.", this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break;
                    }
                    
                    System.out.println("STORE Received!");
                    this.selfNode.getBlockchain().storeBlock(block);
                    newMsg = new Communication(Communication.MessageType.ACK, "STORE completed!", this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                case FIND_BLOCKCHAIN:
                    selfBlockchain = this.selfNode.getBlockchain();
                    
                    String blockchainString = selfBlockchain.blockchainToString(selfBlockchain.getChains());
                    
                    newMsg = new Communication(Communication.MessageType.ACK, blockchainString, this.selfNodeContact, sender);
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
