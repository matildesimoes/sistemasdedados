
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
        routingTable.addNodeToBucket(node1);
        routingTable.addNodeToBucket(node2);

        List<String[]> closest = routingTable.findClosest(selfNode[2], 1);
        assertEquals(1, closest.size());
        
        BigInteger dist1 = RoutingTable.distance(selfNode[2], node1[2]);
        BigInteger dist2 = RoutingTable.distance(selfNode[2], node2[2]);

        BigInteger expectedMin = dist1.compareTo(dist2) <= 0 ? dist1 : dist2;
        BigInteger returnedDistance = RoutingTable.distance(selfNode[2], closest.get(0)[2]);

        assertEquals(expectedMin, returnedDistance, "Returned node is not the closest by distance");
    }

    @Test
    public void testGetBuckets() {
        List<Bucket> buckets = routingTable.getBuckets();
        assertEquals(160, buckets.size());
    }
}

