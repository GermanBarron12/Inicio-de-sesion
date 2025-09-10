
package com.mycompany.iniciarsesion;

import java.io.IOException;
import java.net.Socket;


public class Cliente {
    public static void main(String[] args) throws IOException{
        String host = "localhost";
        int puerto = 5000;
        
        try(Socket socket = new Socket(host, puerto)){
            System.out.println("Conectado al servidor.");
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
