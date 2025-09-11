package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(HOST, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            String respuesta;
            while ((respuesta = entrada.readLine()) != null) {
                // Mostrar lo que manda el servidor
                System.out.println("Servidor: " + respuesta);

                // Si el servidor hace una pregunta o espera un input, pedimos al usuario
                if (respuesta.contains("?") 
                    || respuesta.toLowerCase().contains("usuario")
                    || respuesta.toLowerCase().contains("contraseña")
                    || respuesta.toLowerCase().contains("elige opcion")
                    || respuesta.toLowerCase().contains("escribe")
                    || respuesta.toLowerCase().contains("introduce")
                    || respuesta.toLowerCase().contains("si/no")) {

                    System.out.print("Tu: ");
                    String dato = teclado.readLine();
                    salida.println(dato);
                }
            }

        } catch (IOException e) {
            System.out.println("Error en cliente: " + e.getMessage());
        }
    }
}
