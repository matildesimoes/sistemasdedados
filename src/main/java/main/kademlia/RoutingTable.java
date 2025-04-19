package main.kademlia;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;
import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.Comparator;

import main.Utils;

public class RoutingTable implements Serializable{
    private final List<Bucket> buckets;
    private final String[] selfNodeId;

    public RoutingTable(String[] selfNodeId) {
        this.selfNodeId = selfNodeId;
        this.buckets = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            this.buckets.add(new Bucket(Utils.BUCKET_SIZE)); 
        }
        this.buckets.get(0).update(selfNodeId);
    }

    public List<Bucket> getBuckets(){
        return this.buckets;
    }

    public boolean nodeExist(String[] node){
        String nodeId = node[2];

        BigInteger distance = distance(this.selfNodeId[2], nodeId); 
        int range = distance.bitLength() - 1; // 2^i -> 2^(i+1)
        
        Bucket b = this.buckets.get(range);
        List<String[]> nodes = b.getNodes();
        for (String[] n : nodes) {
            if (n[2].equals(nodeId)) 
                return true;
        }
        return false;
    }

    public void addNodeToBucket(String[] nodeContact){

        BigInteger distance = distance(this.selfNodeId[2], nodeContact[2]); 
        int range = distance.bitLength() - 1;

        this.buckets.get(range).update(nodeContact);

    }

    public void removeNode(String nodeId){
        BigInteger distance = distance(this.selfNodeId[2], nodeId); 
        int range = distance.bitLength() - 1;

        Bucket b = this.buckets.get(range);
        List<String[]> nodes = b.getNodes();
        nodes.removeIf(node -> node[2].equals(nodeId));

    }


    public static BigInteger distance(String src, String dst) {
        if (src.length() != dst.length()) {
            throw new IllegalArgumentException("Both arrays must be the same length.");
        }
        byte[] a = hexToBytes(src);
        byte[] b = hexToBytes(dst);

        byte[] result = new byte[a.length];
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        BigInteger bi= new BigInteger(1,result);
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

    public List<String[]> findClosest(String targetId, int k){
        PriorityQueue<String[]> pq = new PriorityQueue<>(Comparator.comparing(n -> distance(n[2], targetId)));
        for (Bucket b : buckets) {
            for (String[] n : b.getNodes()) {
                if(n[2] == targetId) break;
                pq.offer(n);
            }
        }
        
        List<String[]> result = new ArrayList<>();
        while (!pq.isEmpty() && result.size() < k) {
            result.add(pq.poll());
        }

        return result;
    }

}

