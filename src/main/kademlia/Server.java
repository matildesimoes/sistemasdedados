package main.kademlia;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.*;

import main.Utils;



public class Server{
    private final int port;
    private final RoutingTable routingTable; 

    public Server(int port, RoutingTable routingTable){
        this.port = port;
        this.routingTable = routingTable;
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


            // Response
           // output.writeObject(new Communication(Communication.MessageType.ACK,null,));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
