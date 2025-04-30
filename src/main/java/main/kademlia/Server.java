package main.kademlia;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import main.Utils;
import main.blockchain.*;

public class Server implements Serializable{
    private final String ip;
    private final int port;
    private final RoutingTable routingTable; 
    private final Node selfNode;
    private final String[] selfNodeContact;
    private final ConcurrentMap<String, Integer> pendingChallenges;
    private final Set<String> activeAuctions;
    private final List<Block> orphanBlocks = new ArrayList<>();

    public Server(String ip, int port, RoutingTable routingTable, Node selfNode){
        this.ip = ip;
        this.port = port;
        this.routingTable = routingTable;
        this.selfNode = selfNode;
        this.selfNodeContact = new String[]{this.ip, String.valueOf(this.port), this.selfNode.getNodeId()};
        this.pendingChallenges = new ConcurrentHashMap<>();
        this.activeAuctions = new HashSet<>();
    }
    
    public Set<String> getActiveAuctions(){
        return this.activeAuctions;
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
                    response = "PING Received.";
                    newMsg = new Communication(Communication.MessageType.ACK, response, this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    output.flush();
                    break;
                case CHALLENGE_INIT: 
                    int challenge = Utils.createRandomNumber(16);
                    pendingChallenges.put(sender[2], challenge);
                    
                    newMsg = new Communication(Communication.MessageType.ACK, String.valueOf(challenge), this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    output.flush();
                    break;
                case CHALLENGE: 
                    challenge = pendingChallenges.get(sender[2]);
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

                    String pubKeyPem =  this.selfNode.loadPublicKeyByPort(Integer.valueOf(sender[1]));
                    PublicKey pubKey = this.selfNode.parsePublicKey(pubKeyPem);

                    if(!msg.verifyCommunication(msg.getSignature(), pubKey)){
                        newMsg = new Communication(Communication.MessageType.NACK, "Invalid Communication Signature.", this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break;
                    }

                    Block block = Block.fromString(msg.getInformation());
                    BlockHeader blockHeader = block.getBlockHeader();
                    if(!blockHeader.verifyBlockHeader(blockHeader.getSignature(), pubKey)){
                        newMsg = new Communication(Communication.MessageType.NACK, "Invalid Block Header Signature.", this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break;

                    } 

                    String newMerkleRoot = MerkleTree.getMerkleRoot(block.getTransaction()); 

                    if(!newMerkleRoot.equals(block.getBlockHeader().getMerkleRoot())){
                    
                        newMsg = new Communication(Communication.MessageType.NACK, "Invalid MerkleRoot.", this.selfNodeContact, sender);
                        output.writeObject(newMsg);
                        break;
                    }

                    System.out.println("Received Store (from " + sender[2] +"): " + block.getBlockHeader().getHash());
                    boolean blockStored = this.selfNode.getBlockchain().storeBlock(block);
                    if(!blockStored){
                        orphanBlocks.add(block);
                        if(orphanBlocks.size() == Utils.ORPHAN_LIMIT){
                            System.out.println("Discarded Orphan Blocks.");
                            orphanBlocks.clear();
                            break;
                        }
                        Communication findPrevBlock =  new Communication(
                                Communication.MessageType.FIND_VALUE,
                                "findPrevBlock| " + block.getBlockHeader().getPrevHash(),
                                this.selfNodeContact,
                                sender
                        );
                        output.writeObject(findPrevBlock);
                        break;
                    }
                    
                    for(Block orphan : orphanBlocks){
                        blockStored = this.selfNode.getBlockchain().storeBlock(orphan);
                    }
                    updateActiveAuctions(block);
                    newMsg = new Communication(Communication.MessageType.ACK, "STORE completed!", this.selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                case FIND_BLOCKCHAIN:
                    selfBlockchain = this.selfNode.getBlockchain();
                    
                    String blockchainString = selfBlockchain.blockchainToString(selfBlockchain.getChains());
                    
                    newMsg = new Communication(Communication.MessageType.FIND_BLOCKCHAIN, blockchainString, this.selfNodeContact, sender);
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

    public void updateActiveAuctions(Block block){
        for(Transaction trans : block.getTransaction()){
            if(trans.getType().equals(Transaction.Type.START_AUCTION)){
                activeAuctions.add(trans.getInformation() + "(id= " + trans.getAuctionNumber() + ")");
                System.out.println("Auction started with id: " + trans.getAuctionNumber());


            }
            if(trans.getType().equals(Transaction.Type.CLOSE_AUCTION)){
                activeAuctions.remove(trans.getInformation() + "(id= " + trans.getAuctionNumber() + ")");
                System.out.println("Auction closed with id: " + trans.getAuctionNumber());


            }
        }
    }

}
