package main.kademlia;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import main.Utils;

public class Node{
    private final String nodeId;
    private final String ip;
    private final String port;
    private final KeyPair keyPair;

    public Node(String ip, String port){
        this.ip = ip;
        this.port = port;
        this.keyPair = Utils.generateKeyPair();
        PublicKey publicKey = getPublicKey();
        this.nodeId = SKeyUtils.generateNodeId(publicKey);
    }
    
    public String getNodeId() {
        return this.nodeId;
    }

    public String getIp() {
        return this.ip;
    }
    public String getPort() {
        return this.port;
    }

    public PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    public static byte[] distance(byte[] src, byte[] dst) {
        if (src.length != dst.length) {
            throw new IllegalArgumentException("Both arrays must be the same length.");
        }

        byte[] result = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = (byte) (src[i] ^ dst[i]);
        }

        return result;
    }

}
