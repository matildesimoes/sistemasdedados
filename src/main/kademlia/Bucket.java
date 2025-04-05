package main.kademlia;

import java.util.List;
import java.util.ArrayList;

public class Bucket{
    private List<Node> nodes;
    private int size;
    private int range;

    public Bucket(int size){
        this.nodes = new ArrayList<>();
        this.size = size;
    }

    public List<Node> getNodes(){
        return this.nodes;
    }

    public boolean isFull(){
        return this.nodes.size() == this.size;
    }

    public int getRange(){
        return this.range;
    }

    public void update(Node newNode){
        if(isFull()){
            nodes.remove(nodes.size()-1);
            nodes.add(newNode);
            return;
        }
        nodes.add(newNode);
    }
}
