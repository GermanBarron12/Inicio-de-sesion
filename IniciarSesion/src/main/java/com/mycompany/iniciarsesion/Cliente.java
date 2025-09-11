package com.mycompany.iniciarsesion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
            boolean enMenuPrincipal = false;
            
            while ((respuesta = entrada.readLine()) != null) {
                System.out.println("Servidor: " + respuesta);
                
                // Detectar si estamos en el menú principal
                if (respuesta.contains("=== MENU PRINCIPAL ===")) {
                    enMenuPrincipal = true;
                } else if (respuesta.contains("=== JUEGO: ADIVINA EL NUMERO ===") ||
                          respuesta.contains("Nuevo juego: Adivina el numero")) {
                    enMenuPrincipal = false;
                } else if (respuesta.contains("Regresando al menu principal")) {
                    enMenuPrincipal = true;
                }
                
                // Si el servidor espera una respuesta del cliente
                if (respuesta.endsWith("?") ||
                    respuesta.toLowerCase().contains("usuario") ||
                    respuesta.toLowerCase().contains("contraseña") ||
                    respuesta.toLowerCase().contains("opcion") ||
                    respuesta.toLowerCase().contains("si/no") ||
                    respuesta.contains("Elige opcion:") ||
                    respuesta.contains("Nuevo juego: Adivina el numero") ||
                    respuesta.contains("Intentos restantes:") ||
                    respuesta.contains("no es un numero valido")) {
                    
                    System.out.print("Tu respuesta: ");
                    String dato = teclado.readLine();
                    salida.println(dato);
                    
                    // Solo cerrar si estamos en el menú principal Y el usuario elige "3"
                    if ("3".equals(dato.trim()) && enMenuPrincipal && respuesta.contains("Elige opcion:")) {
                        // Leer la respuesta de "Adios!" antes de cerrar
                        respuesta = entrada.readLine();
                        if (respuesta != null) {
                            System.out.println("Servidor: " + respuesta);
                        }
                        System.out.println("Cliente: conexión cerrada.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }
}