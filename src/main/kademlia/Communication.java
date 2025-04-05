package main.kademlia;

import java.io.Serializable;
import main.auctions.*;

public class Communication implements Serializable {

    public enum MessageType {
        TX, BLOCK, PING, FINDNODE, ACK
    }

    private final MessageType type;
    private final String information;
    private final Node sender;

    public Communication(MessageType type, String information, Node sender, Node recevier) {
        this.type = type;
        this.information = information;
        this.sender = sender;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getInformation() {
        return this.information;
    }

    public Node getSender() {
        return this.sender;
    }

}

