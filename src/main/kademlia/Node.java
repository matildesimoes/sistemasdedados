package main.kademlia;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import main.Utils;

public class Node{
    private final String nodeId;
    private final KeyPair keyPair;
    private final Server server;
    private final RoutingTable routingTable;

    public enum Type {
        BOOTSTRAP, USER, MINER
    }

    public Node(int port){
        this.keyPair = Utils.generateKeyPair();
        PublicKey publicKey = getPublicKey();
        this.nodeId = Utils.publicKeySignature(publicKey);

        this.routingTable = new RoutingTable();
        this.server = new Server(port, this.routingTable);
    }
    
    public String getNodeId() {
        return this.nodeId;
    }

    public PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }


}
