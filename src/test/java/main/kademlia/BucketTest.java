
package test.main.kademlia;

import main.kademlia.Bucket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class BucketTest {

    private Bucket bucket;

    @BeforeEach
    public void setUp() {
        bucket = new Bucket(3); 
    }

    @Test
    public void testAddNode() {
        String[] node1 = {"127.0.0.1", "8001", "nodeID1"};
        bucket.update(node1);

        List<String[]> nodes = bucket.getNodes();
        assertEquals(1, nodes.size());
        assertArrayEquals(node1, nodes.get(0));
    }

    @Test
    public void testIsFull() {
        bucket.update(new String[]{"127.0.0.1", "8001", "nodeID1"});
        bucket.update(new String[]{"127.0.0.1", "8002", "nodeID2"});
        bucket.update(new String[]{"127.0.0.1", "8003", "nodeID3"});

        assertTrue(bucket.isFull());
    }

    @Test
    public void testEvictionWhenFull() {
        String[] node1 = {"127.0.0.1", "8001", "nodeID1"};
        String[] node2 = {"127.0.0.1", "8002", "nodeID2"};
        String[] node3 = {"127.0.0.1", "8003", "nodeID3"};
        String[] node4 = {"127.0.0.1", "8004", "nodeID4"};

        bucket.update(node1);
        bucket.update(node2);
        bucket.update(node3);

        assertTrue(bucket.isFull());

        bucket.update(node4); // should remove the last (node3) and add (node4)

        List<String[]> nodes = bucket.getNodes();
        assertEquals(3, nodes.size());
        assertFalse(nodes.contains(node3)); // node3 was removed
        assertTrue(nodes.contains(node4));
    }
}

