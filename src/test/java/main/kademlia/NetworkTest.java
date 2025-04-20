import org.junit.jupiter.api.Test;

import main.kademlia.*;
import main.blockchain.*;
import main.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkTest {

    @Test
    public void testPingCommunication() throws Exception {
        // Setup test environment
        int testPort = 9000;
        Node serverNode = new Node("127.0.0.1", testPort);
        String[] serverContact = new String[] {
            "127.0.0.1",
            String.valueOf(testPort),
            serverNode.getNodeId()
        };
        RoutingTable serverRoutingTable = new RoutingTable(serverContact);
        Server server = new Server("127.0.0.1", testPort, serverRoutingTable, serverNode);

        // Start server in a new thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> server.start());

        // Give the server time to start up
        Thread.sleep(500);

        // Setup client
        Node clientNode = new Node("127.0.0.1", 9001);
        Client client = new Client(clientNode);


        Communication ping = new Communication(
            Communication.MessageType.PING,
            "Hello Server",
            new String[] { clientNode.getNodeIp(), String.valueOf(clientNode.getNodePort()), clientNode.getNodeId() },
            serverContact
        );

        Communication response = client.sendMessage(serverContact, ping);

        assertNotNull(response);
        assertEquals(Communication.MessageType.ACK, response.getType());
    }

    @Test
    public void testStoreAndFindValue() throws Exception {
        int port = 9002;
        Node serverNode = new Node("127.0.0.1", port);
        serverNode.getBlockchain().createNewBlockchain();
        String[] serverContact = {
            "127.0.0.1", String.valueOf(port), serverNode.getNodeId()
        };
        RoutingTable serverRoutingTable = new RoutingTable(serverContact);
        Server server = new Server("127.0.0.1", port, serverRoutingTable, serverNode);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(server::start);
        Thread.sleep(500);

        // Create client
        Node clientNode = new Node("127.0.0.1", 9003);
        Client client = new Client(clientNode);


        // Create dummy transaction and block
        Transaction tx = new Transaction(
            Transaction.Type.REWARD,
            clientNode,
            1,
            "test reward"
        );

        Block block = new Block(List.of(tx));
        String hash = Utils.hashSHA256(block.getBlockHeader());

        block.getBlockHeader().setHash(hash);

        // Send STORE
        Communication store = new Communication(
            Communication.MessageType.STORE,
            block.toString(),
            new String[] { clientNode.getNodeIp(), String.valueOf(clientNode.getNodePort()), clientNode.getNodeId() },
            serverContact
        );

        Communication storeResponse = client.sendMessage(serverContact, store);
        assertNotNull(storeResponse);
        assertEquals(Communication.MessageType.ACK, storeResponse.getType());

        // Send FIND_VALUE
        Communication find = new Communication(
            Communication.MessageType.FIND_VALUE,
            hash,
            new String[] { clientNode.getNodeIp(), String.valueOf(clientNode.getNodePort()), clientNode.getNodeId() },
            serverContact
        );

        Communication findResponse = client.sendMessage(serverContact, find);
        assertNotNull(findResponse);
        assertEquals(Communication.MessageType.FIND_VALUE, findResponse.getType());

        Block found = Block.fromString(findResponse.getInformation());
        assertNotNull(found);
        assertEquals(block.getBlockHeader().getHash(), found.getBlockHeader().getHash());
    }

    @Test
    public void testFindBlockchain() throws Exception {
        int port = 9004;
        Node serverNode = new Node("127.0.0.1", port);
        serverNode.getBlockchain().createNewBlockchain();
        String[] serverContact = {
            "127.0.0.1", String.valueOf(port), serverNode.getNodeId()
        };
        RoutingTable serverRoutingTable = new RoutingTable(serverContact);
        Server server = new Server("127.0.0.1", port, serverRoutingTable, serverNode);

        // Create and add block to server's blockchain
        Transaction tx = new Transaction(Transaction.Type.REWARD, serverNode, 0, "reward");
        serverNode.getBlockchain().createBlock(List.of(tx));
        Block block = serverNode.getBlockchain().getChains().get(0).getLatest();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(server::start);
        Thread.sleep(500);

        Node clientNode = new Node("127.0.0.1", 9005);
        Client client = new Client(clientNode);


        Communication request = new Communication(
            Communication.MessageType.FIND_BLOCKCHAIN,
            "give me your chain",
            new String[] { clientNode.getNodeIp(), String.valueOf(clientNode.getNodePort()), clientNode.getNodeId() },
            serverContact
        );

        Communication response = client.sendMessage(serverContact, request);
        assertNotNull(response);
        assertEquals(Communication.MessageType.ACK, response.getType());

        List<Chain> chains = Blockchain.blockchainFromString(response.getInformation());
        assertNotNull(chains);
        assertFalse(chains.isEmpty());
        assertFalse(chains.get(0).getBlocks().isEmpty());

        Block receivedBlock = chains.get(0).getLatest();
        assertEquals(block.getBlockHeader().getHash(), receivedBlock.getBlockHeader().getHash());
    }


}

