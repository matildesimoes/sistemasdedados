package test.main.kademlia;

import main.kademlia.RoutingTable;
import main.kademlia.Bucket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigInteger;


import static org.junit.jupiter.api.Assertions.*;

public class RoutingTableTest {

    private RoutingTable routingTable;
    private String[] selfNode;

    @BeforeEach
    public void setUp() {
        selfNode = new String[]{"127.0.0.1", "8001", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"};
        routingTable = new RoutingTable(selfNode);
    }

    @Test
    public void testAddNodeToBucketAndExistence() {
        String[] node = {"127.0.0.1", "8002", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"};
        routingTable.addNodeToBucket(node);
        assertTrue(routingTable.nodeExist(node));
    }

    @Test
    public void testRemoveNode() {
        String[] node = {"127.0.0.1", "8002", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"};
        routingTable.addNodeToBucket(node);
        routingTable.removeNode(node[2]);
        assertFalse(routingTable.nodeExist(node));
    }

    @Test
    public void testFindClosest() {
        String[] node1 = {"127.0.0.1", "8002", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"};
        String[] node2 = {"127.0.0.1", "8003", "cccccccccccccccccccccccccccccccc"};
        String[] node3 = {"127.0.0.1", "8003", "ffffffffffffffffffffffffffffffff"};

        routingTable.addNodeToBucket(node1);
        routingTable.addNodeToBucket(node2);
        routingTable.addNodeToBucket(node3);


        // Find the 2 closest nodes to selfId
        List<String[]> closest = routingTable.findClosest(selfNode[2], 2);

        assertEquals(2, closest.size());

        // Compute distances of the first two closest nodes
        BigInteger dist0 = routingTable.distance(selfNode[2], closest.get(0)[2]);
        BigInteger dist1 = routingTable.distance(selfNode[2], closest.get(1)[2]);

        // Assert that the list is sorted by XOR distance
        assertTrue(dist0.compareTo(dist1) <= 0);

        // Check if the first node is actually the closest among all three
        BigInteger distNode1 = routingTable.distance(selfNode[2], node1[2]);
        BigInteger distNode2 = routingTable.distance(selfNode[2], node2[2]);
        BigInteger distNode3 = routingTable.distance(selfNode[2], node3[2]);

        BigInteger min = distNode1.min(distNode2).min(distNode3);
        assertEquals(min, dist0);

    }

    @Test
    public void testGetBuckets() {
        List<Bucket> buckets = routingTable.getBuckets();
        assertEquals(160, buckets.size());
    }
}

