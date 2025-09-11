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
                } else if (respuesta.contains("=== JUEGO: ADIVINA EL NÚMERO ===")) {
                    enMenuPrincipal = false;
                } else if (respuesta.contains("Regresando al menu principal")) {
                    enMenuPrincipal = true;
                }
                
                // Detectar preguntas del servidor
                if (respuesta.endsWith("?") ||
                    respuesta.toLowerCase().contains("introduce") ||
                    respuesta.toLowerCase().contains("contraseña") ||
                    respuesta.contains("Elige opcion:") ||
                    respuesta.contains("Intentos restantes:") ||
                    respuesta.contains("no es un numero valido") ||
                    respuesta.toLowerCase().contains("a qué usuario deseas enviar el mensaje") ||
                    respuesta.toLowerCase().contains("escribe el mensaje") ||
                    respuesta.contains("¿Quieres iniciar sesión (1) o registrarte (2)?") ||
                    respuesta.contains("Adivina el numero del 1 al 10") ||
                    (respuesta.toLowerCase().contains("incorrecto") && respuesta.contains("Intentos restantes")) ||
                    respuesta.contains("Intenta otra vez")) {
                    
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