package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== CONECTADO AL SERVIDOR ===");

            // Hilo para escuchar mensajes del servidor
            Thread hiloEscucha = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println(mensaje);
                        
                        // Si el servidor pide una opcion, mostrar prompt
                        if (mensaje.contains("Elige opcion:") || 
                            mensaje.contains("Elige una opcion:") || 
                            mensaje.contains("Introduce") || 
                            mensaje.contains("Escribe") ||
                            mensaje.contains("A que usuario") ||
                            mensaje.contains("Que usuario") ||
                            mensaje.contains("Adivina el numero") ||
                            mensaje.endsWith("?")) {
                            System.out.print("> ");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Conexion cerrada por el servidor.");
                }
            });
            hiloEscucha.setDaemon(true); // Hacer que el hilo se cierre cuando termine main
            hiloEscucha.start();

            // Permitir un poco de tiempo para que el servidor envie el primer mensaje
            Thread.sleep(100);

            // Bucle principal para enviar datos al servidor
            String input;
            while ((input = scanner.nextLine()) != null) {
                salida.println(input);
                
                // Si el usuario escribio 'exit', terminar
                if ("exit".equalsIgnoreCase(input.trim())) {
                    break;
                }
                
                // Si el usuario eligio salir (opcion 3) desde el menu, esperar confirmacion y luego continuar
                if ("3".equals(input.trim())) {
                    Thread.sleep(500); // Dar tiempo para recibir mensaje del servidor
                    System.out.print("> "); // Mostrar prompt para la siguiente interaccion
                }
            }

        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Conexion interrumpida.");
        }
    }
}