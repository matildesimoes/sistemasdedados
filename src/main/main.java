package main;

import java.util.Scanner;
import main.blockchain.*;
import main.auctions.*;


public class main{

    public static void main(String[] args){

       Blockchain blockchain = new Blockchain(); 

       blockchain.createBlockchain();

       blockchain.loadBlockchain();

    }
}
