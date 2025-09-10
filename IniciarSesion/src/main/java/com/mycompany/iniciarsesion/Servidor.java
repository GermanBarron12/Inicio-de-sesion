
package com.mycompany.iniciarsesion;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Servidor {

    public static void main(String[] args) throws IOException {
        int puerto = 5000;
        try (ServerSocket serverSocket = new ServerSocket(puerto)){
            System.out.println("Servidor iniciado en el puerto "+puerto);
            
            
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado");
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
