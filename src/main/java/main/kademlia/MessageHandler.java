package main.kademlia;

import main.Utils;
import main.blockchain.*;

import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.util.List;

public class MessageHandler {
    private final Node selfNode;
    private final PeerNode peerNode;
    private final String[] selfNodeContact;
    private final List<Block> orphanBlocks;

    public MessageHandler(PeerNode peerNode, List<Block> orphanBlocks) {
        this.peerNode = peerNode;
        this.selfNode = peerNode.getSelfNode();
        this.selfNodeContact = new String[]{selfNode.getNodeIp(), String.valueOf(selfNode.getNodePort()), selfNode.getNodeId()};
        this.orphanBlocks = orphanBlocks;
    }


    public void handle(Communication msg, ObjectOutputStream output) throws Exception {
        String[] sender = msg.getSender();
        Communication newMsg;

        switch (msg.getType()) {
            case NACK:
                System.out.println("Received message (from " + sender[2] + "): " + msg.getInformation());
                break;
            case PING:
                newMsg = new Communication(Communication.MessageType.ACK, "PING Received.", selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case CHALLENGE_INIT:
                int challenge = Utils.createRandomNumber(16);
                peerNode.getPendingChallenges().put(sender[2], challenge);
                newMsg = new Communication(Communication.MessageType.ACK, String.valueOf(challenge), selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case CHALLENGE:
                challenge = peerNode.getPendingChallenges().getOrDefault(sender[2], -1);
                String str = sender[2] + challenge + msg.getInformation();
                String validateHash = Utils.hashSHA256(str);
                String prefix = "0".repeat(Utils.CHALLENGE_DIFFICULTY);
                if (!validateHash.startsWith(prefix)) {
                    newMsg = new Communication(Communication.MessageType.NACK, "Wrong challenge response.", selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                }
                newMsg = new Communication(Communication.MessageType.ACK, "CHALLENGE completed.", selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case FIND_NODE:
                String[] nodeContact = msg.getInformation().split(",");
                if (!selfNode.getRoutingTable().nodeExist(sender)) {
                    selfNode.getRoutingTable().addNodeToBucket(nodeContact);
                }
                List<String[]> closest = selfNode.getRoutingTable().findClosest(sender[2], Utils.BUCKET_SIZE);
                StringBuilder closestNodes = new StringBuilder();
                for (String[] s : closest) {
                    closestNodes.append(s[0]).append(",").append(s[1]).append(",").append(s[2]).append("-");
                }
                newMsg = new Communication(Communication.MessageType.FIND_NODE, closestNodes.toString(), selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case FIND_VALUE:
                String key = msg.getInformation();
                Blockchain selfBlockchain = selfNode.getBlockchain();
                boolean found = false;

                for (Chain chain : selfBlockchain.getChains()) {
                    for (Block block : chain.getBlocks()) {
                        if (block.getBlockHeader().getHash().equals(key)) {
                            newMsg = new Communication(Communication.MessageType.FIND_VALUE, block.toString(), selfNodeContact, sender);
                            output.writeObject(newMsg);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (!found) {
                    newMsg = new Communication(Communication.MessageType.NACK, "Value not found.", selfNodeContact, sender);
                    output.writeObject(newMsg);
                }
                break;
            case STORE:
                String pubKeyPem = selfNode.loadPublicKeyByPort(Integer.parseInt(sender[1]));
                PublicKey pubKey = selfNode.parsePublicKey(pubKeyPem);
                if (!msg.verifyCommunication(msg.getSignature(), pubKey)) {
                    newMsg = new Communication(Communication.MessageType.NACK, "Invalid Communication Signature.", selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                }
                Block block = Block.fromString(msg.getInformation());
                BlockHeader blockHeader = block.getBlockHeader();
                System.out.println(msg.getInformation());
                if (!blockHeader.verifyBlockHeader(blockHeader.getSignature(), pubKey)) {
                    newMsg = new Communication(Communication.MessageType.NACK, "Invalid Block Header Signature.", selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                }
                String newMerkleRoot = MerkleTree.getMerkleRoot(block.getTransaction());
                if (!newMerkleRoot.equals(blockHeader.getMerkleRoot())) {
                    newMsg = new Communication(Communication.MessageType.NACK, "Invalid MerkleRoot.", selfNodeContact, sender);
                    output.writeObject(newMsg);
                    break;
                }
                System.out.println("Received Store (from " + sender[2] + "): " + blockHeader.getHash());
                boolean blockStored = selfNode.getBlockchain().storeBlock(block);
                if (!blockStored) {
                    orphanBlocks.add(block);
                    if (orphanBlocks.size() == Utils.ORPHAN_LIMIT) {
                        System.out.println("Discarded Orphan Blocks.");
                        orphanBlocks.clear();
                        break;
                    }
                    Communication findPrevBlock = new Communication(
                        Communication.MessageType.FIND_VALUE,
                        "findPrevBlock|" + blockHeader.getPrevHash(),
                        selfNodeContact,
                        sender
                    );
                    output.writeObject(findPrevBlock);
                    break;
                }
                for (Block orphan : orphanBlocks) {
                    selfNode.getBlockchain().storeBlock(orphan);
                }
                updateActiveAuctions(block);
                newMsg = new Communication(Communication.MessageType.ACK, "STORE completed!", selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case FIND_BLOCKCHAIN:
                selfBlockchain = selfNode.getBlockchain();
                String blockchainString = selfBlockchain.blockchainToString(selfBlockchain.getChains());
                newMsg = new Communication(Communication.MessageType.FIND_BLOCKCHAIN, blockchainString, selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            default:
                System.out.println("Unknown message Type.");
        }
    }

    private void updateActiveAuctions(Block block) {
        for (Transaction trans : block.getTransaction()) {
            if (trans.getType().equals(Transaction.Type.START_AUCTION)) {
                System.out.println("Auction started with id: " + trans.getAuctionNumber());
            }
            if (trans.getType().equals(Transaction.Type.CLOSE_AUCTION)) {
                System.out.println("Auction closed with id: " + trans.getAuctionNumber());
            }
        }
    }
}
