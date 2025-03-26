package main.kademlia;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

public class RoutingTable {
    private final Map<String, Node> buckets = new ConcurrentHashMap<>();

    public void addNode(Node node) {
        buckets.putIfAbsent(node.getNodeId(), node);
    }

    public Node findClosest(String targetId) {
        return buckets.values().stream()
            .min(Comparator.comparing(n -> distance(n.getNodeId(), targetId)))
            .orElse(null);
    }

    public static int distance(String src, String dst) {
        if (src.length() != dst.length()) {
            throw new IllegalArgumentException("Both arrays must be the same length.");
        }
        byte[] a = hexToBytes(src);
        byte[] b = hexToBytes(dst);

        int result = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            result += (a[i] ^ b[i]);
        }
        return result;
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

