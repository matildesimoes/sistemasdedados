package main.kademlia;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


public class Client{
    
   public Communication sendMessage(Node receiver, Communication message) {
        try (
            Socket socket = new Socket(receiver.getNodeIp(), receiver.getNodePort());
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream())
        ) {
            output.writeObject(message);
            output.flush();
            System.out.println("Message sent!");

            Communication response = (Communication) input.readObject();
            System.out.println("Received response: " + response.getInformation());

            switch(response.getType()){
                case NACK: 
                    if(response.getInformation().equals("Wrong challenge response.")){
                        input.close();
                        output.close();
                        socket.close();
                        System.exit(0);
                    }
            }
            return response;

        } catch (Exception e) {
            System.err.println("Failed to communicate with node " + receiver.getNodeId());
            e.printStackTrace();
            return null;
        }
    } 
}
