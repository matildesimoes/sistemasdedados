package main.kademlia;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

public class RoutingTable {
    private final List<Bucket> buckets;
    private final String selfNodeId;

    public RoutingTable(String selfNodeId) {
        this.selfNodeId = selfNodeId;
        this.buckets = new ArrayList<>();

    }

    public void addBucket(Bucket bucket){
        this.buckets.add(bucket);
    }


    public boolean nodeExist(Node node){
        String nodeId = node.getNodeId();

        BigInteger distance = distance(this.selfNodeId, nodeId); 
        int range = distance.bitLength() - 1;

        for(Bucket b : buckets){
            if(b.getRange() == range){
                List<Node> nodes = b.getNodes();

                for(Node n : nodes){
                    if(n.getNodeId() == nodeId)
                        return true;
                }
                break;
            }
        }

        return false;

    }


    public static BigInteger distance(String src, String dst) {
        if (src.length() != dst.length()) {
            throw new IllegalArgumentException("Both arrays must be the same length.");
        }
        byte[] a = hexToBytes(src);
        byte[] b = hexToBytes(dst);

        byte[] result = new byte[a.length];
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            result[i] += (a[i] ^ b[i]);
        }
        return new BigInteger(1, result);
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return result;
    }

}

