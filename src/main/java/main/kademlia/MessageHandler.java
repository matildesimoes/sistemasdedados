package main.kademlia;

import main.Utils;
import main.blockchain.*;
import main.blockchain.Blockchain.MatchResult;


import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.util.List;
import java.sql.Timestamp;


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
            case CHALLENGE:
                String str = sender[2] + msg.getInformation();
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
                    closestNodes.append(s[0]).append(",").append(s[1]).append(",").append(s[2]).append(",").append(s[3]).append("-");
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
                Blockchain.MatchResult result = selfNode.getBlockchain().storeBlock(block);
                if (result.equals(MatchResult.NOT_FOUND)) {
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
                }else if(result.equals(MatchResult.MATCH_FOUND)){
                    for (Block orphan : orphanBlocks) {
                        Blockchain.MatchResult stored = selfNode.getBlockchain().storeBlock(orphan);
                        if(stored.equals(MatchResult.MATCH_FOUND)){
                            selfNode.getBlockchain().recalculateHeights();
                        }
                    }
                    updateActiveAuctions(block);
                    for (Transaction t : block.getTransaction()) {
                        if (t.getType() == Transaction.Type.CLOSE_AUCTION) {
                            int auctionId = t.getAuctionNumber();
                            int closeHeight = this.selfNode.getBlockchain().getBlockHeight(block.getBlockHeader().getHash());
                            int targetHeight = closeHeight + Utils.BLOCK_CHAIN_LIMIT;

                            
                            // Spawn a thread to wait for confirmation
                            new Thread(() -> {
                                System.out.println("Waiting to confirm winner for Auction ID " + auctionId + " (height " + targetHeight + ")...");
                                while (true) {
                                    int currentHeight = this.selfNode.getBlockchain().getLatestHeight();
                                    if (currentHeight >= targetHeight) {
                                        String winner = MessageHandler.getAuctionWinner(this.selfNode, auctionId);
                                        if (winner != null) {
                                            System.out.println("Auction (id" + auctionId + ") winner is: " + winner);
                                        } else {
                                            System.out.println("Auction (id" + auctionId + ") had no valid bids.");
                                        }
                                        break;
                                    }

                                    try {
                                        Thread.sleep(1000); // Check every second
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                            }).start();
                        }
                    }
                    newMsg = new Communication(Communication.MessageType.ACK, "STORE completed!", selfNodeContact, sender);
                    output.writeObject(newMsg);
                }else if(result.equals(MatchResult.TOO_OLD)) {
                    System.out.println("[!] Block not stored — too old, skipping FIND_VALUE.");
                }
                break;
            case FIND_BLOCKCHAIN:
                selfBlockchain = selfNode.getBlockchain();
                String blockchainString = selfBlockchain.blockchainToString(selfBlockchain.getChains());
                newMsg = new Communication(Communication.MessageType.FIND_BLOCKCHAIN, blockchainString, selfNodeContact, sender);
                output.writeObject(newMsg);
                break;
            case RECENT_BLOCKS_REQUEST:
                List<Block> latestBlocks = selfNode.getBlockchain().getMostRecentBlocks(Utils.RECENT_BLOCK_PEERS);
                String blocksJson = Block.serializeBlockList(latestBlocks);

                Communication blocksMsg = new Communication(
                    Communication.MessageType.RECENT_BLOCKS_REQUEST,
                    blocksJson,
                    selfNodeContact,
                    sender
                );
                output.writeObject(blocksMsg);
                break;
            default:
                System.out.println("Unknown message Type.");
        }
    }

    public static String getAuctionWinner(Node selfNode, int auctionId){
        String winnerId = null;
        int highestBid = -1;
        Timestamp closeTimestamp = null;

        List<Chain> chains = selfNode.getBlockchain().getChains();

        for (Chain chain : chains) {
            for (Block block : chain.getBlocks()) {
                for (Transaction trans : block.getTransaction()) {
                    if (trans.getType() == Transaction.Type.CLOSE_AUCTION && trans.getAuctionNumber() == auctionId) {
                        closeTimestamp = trans.getTimestamp();
                        break;
                    }
                }
                if (closeTimestamp != null) break;
            }
            if (closeTimestamp != null) break;
        }

        if (closeTimestamp == null) {
            System.out.println("Could not determine closing timestamp for auction id: " + auctionId);
            return null;
        }

        for (Chain chain : chains) {
            for (Block block : chain.getBlocks()) {
                if (block.getBlockHeader().getTimestamp().after(closeTimestamp)) continue;

                for (Transaction trans : block.getTransaction()) {
                    if (trans.getType() == Transaction.Type.BID && trans.getAuctionNumber() == auctionId) {
                        try {
                            String info = trans.getInformation(); // Ex: "Bid ammount: 300"
                            int bidValue = Integer.parseInt(info.replaceAll("[^0-9]", ""));
                            if (bidValue > highestBid) {
                                highestBid = bidValue;
                                winnerId = trans.getCreator().getNodeId();
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        return winnerId;
        
    }

    private void updateActiveAuctions(Block block) {
        for (Transaction trans : block.getTransaction()) {
            if (trans.getType().equals(Transaction.Type.START_AUCTION)) {
                peerNode.addActiveAuctions(trans);
                System.out.println("Auction started with id: " + trans.getAuctionNumber());
            }
            if (trans.getType().equals(Transaction.Type.CLOSE_AUCTION)) {
                peerNode.removeActiveAuctions(trans);
                System.out.println("Auction closed with id: " + trans.getAuctionNumber());
            }
        }
    }
}
