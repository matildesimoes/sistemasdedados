package main.auctions;

import java.io.Serializable;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import main.Utils;

public class User implements Serializable{
    private static final String COUNTER_FILE = "data/userId_counter.txt";
    protected int id;
    protected transient KeyPair keyPair;

    public User(){
        this.id = getNextId();
        this.keyPair = Utils.generateKeyPair();
    }


    public int getId(){
        return this.id;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    private static synchronized int getNextId() {
        int currentId = 1;

        File file = new File(COUNTER_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null) {
                    currentId = Integer.parseInt(line.trim());
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Failed to read ID counter, defaulting to 1.");
            }
        }

        // Save next value 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(Integer.toString(currentId + 1));
        } catch (IOException e) {
            System.err.println("Failed to write ID counter: " + e.getMessage());
        }

        return currentId;
    }
}
