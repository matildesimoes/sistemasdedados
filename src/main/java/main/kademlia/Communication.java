package main.kademlia;

import java.io.Serializable;

public class Communication implements Serializable {

    public enum MessageType {
        PING, FIND_NODE, ACK, NACK, CHALLENGE
    }

    private final MessageType type;
    private final String information;
    private final String[] sender;
    private final String[] receiver;

    public Communication(MessageType type, String information, String[] sender, String[] receiver) {
        this.type = type;
        this.information = information;
        this.sender = sender;
        this.receiver = receiver;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getInformation() {
        return this.information;
    }

    public String[] getSender() {
        return this.sender;
    }

    public String[] getReceiver() {
        return this.receiver;
    }

}

