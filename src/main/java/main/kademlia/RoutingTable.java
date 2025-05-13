package main.kademlia;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;
import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.Comparator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

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

        // Prevent checking self
        if (nodeId.equals(this.selfNodeId[2])) {
            return false;
        }

        BigInteger distance = distance(this.selfNodeId[2], nodeId); 
        int range = distance.bitLength() - 1; // 2^i -> 2^(i+1)
        
        // Guard against invalid range
        if (range < 0 || range >= this.buckets.size()) {
            return false;
        }

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
        saveRoutingTable();
    }

    public void removeNode(String nodeId){
        BigInteger distance = distance(this.selfNodeId[2], nodeId); 
        int range = distance.bitLength() - 1;

        Bucket b = this.buckets.get(range);
        List<String[]> nodes = b.getNodes();
        nodes.removeIf(node -> node[2].equals(nodeId));
        saveRoutingTable();
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
                if(!(n[2].equals(targetId)))
                    pq.offer(n);
            }
        }
        
        List<String[]> result = new ArrayList<>();
        while (!pq.isEmpty() && result.size() < k) {
            result.add(pq.poll());
        }

        return result;
    }
    public void saveRoutingTable() {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdir();

            FileOutputStream fileOut = new FileOutputStream("data/routing_table.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            System.err.println("Error saving routing table: " + i.getMessage());
        }
    }

    public static RoutingTable loadRoutingTable(String[] selfNodeId) {
        try {
            FileInputStream fileIn = new FileInputStream("data/routing_table.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            RoutingTable table = (RoutingTable) in.readObject();
            in.close();
            fileIn.close();
            return table;
        } catch (Exception e) {
            System.out.println("No previous routing table found. Starting new.");
            return new RoutingTable(selfNodeId);
        }
    }


}

