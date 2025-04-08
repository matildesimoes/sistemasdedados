package main.kademlia;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class Bucket implements Serializable{
    private List<String[]> nodes;
    private int size; // size is K
    private int range;

    public Bucket(int size){
        this.nodes = new ArrayList<>();
        this.size = size;
    }

    public List<String[]> getNodes(){
        return this.nodes;
    }

    public boolean isFull(){
        return nodes.size() >= size;
    }

    public int getRange(){
        return this.range;
    }

    public void update(String[] newNode){
        if(isFull()){
            nodes.remove(nodes.size()-1);
        }
        nodes.add(newNode);
    }
}
