package main.kademlia;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.io.Serializable;

import main.blockchain.*;

import main.Utils;

public class Node implements Serializable{
    private final String nodeId;
    private final String ip;
    private final int port;
    private final KeyPair keyPair;
    private final Server server;
    private final RoutingTable routingTable;
    private Timestamp timeAlive;
    private Blockchain blockchain;


    public enum Type {
        BOOTSTRAP, USER, MINER
    }

    public Node(String ip, int port){
        this.ip = ip;
        this.port = port;

        this.keyPair = Utils.generateKeyPair();
        PublicKey publicKey = getPublicKey();
        this.nodeId = Utils.publicKeySignature(publicKey);

        String[] nodeContact = {this.ip, String.valueOf(this.port), this.nodeId};

        this.routingTable = new RoutingTable(nodeContact);
        this.server = new Server(ip,port, this.routingTable, this);

        this.timeAlive = null;
    }
    
    public String getNodeId() {
        return this.nodeId;
    }
    public String getNodeIp() {
        return this.ip;
    }
    public int getNodePort() {
        return this.port;
    }

    public RoutingTable getRoutingTable(){
        return this.routingTable;
    }


    public PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    public Timestamp getTimeAlive(){
        return this.timeAlive;
    }
    
    public void setTimeAlive(Timestamp timestamp){
        this.timeAlive = timestamp;
    }


}
