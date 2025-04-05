package main.kademlia;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.io.Serializable;


import main.Utils;



public class Server implements Serializable{
    private final String ip;
    private final int port;
    private final RoutingTable routingTable; 
    private final Node selfNode;

    public Server(String ip, int port, RoutingTable routingTable, Node selfNode){
        this.ip = ip;
        this.port = port;
        this.routingTable = routingTable;
        this.selfNode = selfNode;
    }
    
    public void start(){
        ExecutorService executor = Executors.newCachedThreadPool();

        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("NodeServer P2P initialized on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(() -> handleClient(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
        ) {
            Communication msg = (Communication) input.readObject();
            Node sender = msg.getSender();
            
            switch(msg.getType()){
                case PING: 
                    if(!this.routingTable.nodeExist(sender)){
                        int challenge = Utils.createRandomNumber(16);
                        
                        Communication newMsg = new Communication(Communication.MessageType.PING, String.valueOf(challenge), this.selfNode, sender);
                        output.writeObject(newMsg);
                        output.flush();
                    }else{
                        System.out.println("PING Received");
                    }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
