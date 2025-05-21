package main.kademlia;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.security.spec.PKCS8EncodedKeySpec;
import com.fasterxml.jackson.databind.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

import main.blockchain.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import main.Utils;

public class Node implements Serializable{
    private String nodeId;
    private final String ip;
    private final int port;
    
    // To not have problems serializing
    @JsonIgnore
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private RoutingTable routingTable;
    private String timeAlive;
    private Blockchain blockchain;
    private List<Transaction> transactionPool; 

    public enum Type {
        BOOTSTRAP, USER, MINER
    }

    public Node(String ip, int port){
        this.ip = ip;
        this.port = port;
        try {
            String pubKeyPem = loadPublicKeyByPort(port);
            this.publicKey = parsePublicKey(pubKeyPem);
            this.nodeId = Utils.publicKeySignature(this.publicKey);

            String privateKeyPath = "data/private_node.pem";
            this.privateKey = loadPrivateKeyFromPem(privateKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Error getting the public key: " + e.getMessage());
        }
        this.timeAlive = String.valueOf(System.currentTimeMillis());

        this.routingTable = null;

        this.transactionPool = new ArrayList<>();

        this.blockchain = new Blockchain();
    }
    
    public String getNodeId() {
        return this.nodeId;
    }
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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

    public void setRoutingTable(RoutingTable routingTable){
        this.routingTable = routingTable;
    }

    
    @JsonIgnore
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    @JsonIgnore
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    public String getTimeAlive(){
        return this.timeAlive;
    }
    
    public void setTimeAlive(String timestamp){
        this.timeAlive = timestamp;
    }

    public Blockchain getBlockchain(){
        return this.blockchain;
    }

    public List<Transaction> getTransactionPool(){
        return this.transactionPool;
    }

    public void addToTransactionPool(Transaction transaction){
        this.transactionPool.add(transaction);
    }

    public int transactionPoolSize(){
        return this.transactionPool.size();
    }

    public boolean isTransactionPoolFull(){
        return this.transactionPool.size() == Utils.TRANS_POOL_LIMIT_LENGTH;
    }

    public void clearTransactionPool(){
        this.transactionPool = new ArrayList<>();
    }

    public static String loadPublicKeyByPort(int port) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("data/infonode.json"));
        JsonNode nodes = root.get("nodes");

        for (JsonNode node : nodes) {
            if (node.get("port").asInt() == port) {
                return node.get("public_key").asText();
            }
        }
        throw new IllegalArgumentException("Port not found on JSON: " + port);
    }

    
    public static PrivateKey loadPrivateKeyFromPem(String filename) throws Exception {
        String keyPem = Files.readString(Paths.get(filename))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(keyPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static PublicKey parsePublicKey(String pem) throws Exception {
        String cleanPem = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
