package main;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.List;

import main.kademlia.*;

public class InfoNode{
    private final Node.Type nodeType;
    private final String ip;
    private final int port;
    private transient KeyPair keyPair;
    private final List<String> bootstrapAddress;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();


    public InfoNode(Node.Type nodeType, String ip, int port, List<String> bootstrapAddress){
        this.nodeType = nodeType;
        this.ip = ip;
        this.port = port;
        this.keyPair = Utils.generateKeyPair();
        this.bootstrapAddress = bootstrapAddress;
    }



    public Node.Type getNodeType() { return this.nodeType; }
    public String getIp() { return this.ip; }
    public int getPort() { return this.port; }
    public KeyPair getKeyPair() { return this.keyPair; }
    public List<String> getBootstrapAddress() { return this.bootstrapAddress; }


    public void save(String directory) throws Exception {
        Files.createDirectories(Paths.get(directory));

        savePrivateKey(keyPair.getPrivate(), directory + "/private.key");
        savePublicKey(keyPair.getPublic(), directory + "/public.key");

        String json = gson.toJson(this);
        Files.write(Paths.get(directory + "/infonode.json"), json.getBytes());
    }

    public static InfoNode load(String directory) throws Exception {
       String json = Files.readString(Paths.get(directory + "/infonode.json"));
        InfoNode info = gson.fromJson(json, InfoNode.class);

        // Recarregar KeyPair
        KeyPair keyPair = loadKeyPair(directory + "/public.key", directory + "/private.key");
        info.keyPair = keyPair;
        return info;
    }

    
    public static void savePrivateKey(PrivateKey privateKey, String path) throws IOException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        Files.write(Paths.get(path), spec.getEncoded());
    }

    public static void savePublicKey(PublicKey publicKey, String path) throws IOException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
        Files.write(Paths.get(path), spec.getEncoded());
    }

    public static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static KeyPair loadKeyPair(String pubPath, String privPath) throws Exception {
        PublicKey pub = loadPublicKey(pubPath);
        PrivateKey priv = loadPrivateKey(privPath);
        return new KeyPair(pub, priv);
    }


}
