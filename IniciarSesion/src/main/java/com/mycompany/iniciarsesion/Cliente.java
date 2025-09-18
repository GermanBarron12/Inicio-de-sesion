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
            System.out.println("=== CONECTADO AL SERVIDOR ===");
            System.out.println("Para desconectarte completamente, escribe 'exit' cuando se te pregunte sobre iniciar sesión o registrarte.");
            System.out.println();
            
            String respuesta;
            boolean enMenuPrincipal = false;
            boolean enJuego = false;
            
            while ((respuesta = entrada.readLine()) != null) {
                System.out.println("Servidor: " + respuesta);
                
                // Detectar diferentes estados
                if (respuesta.contains("=== MENU PRINCIPAL ===")) {
                    enMenuPrincipal = true;
                    enJuego = false;
                } else if (respuesta.contains("=== JUEGO: ADIVINA EL NÚMERO ===")) {
                    enMenuPrincipal = false;
                    enJuego = true;
                } else if (respuesta.contains("Regresando al menu principal")) {
                    enMenuPrincipal = true;
                    enJuego = false;
                } else if (respuesta.contains("¡Hasta luego! Desconectando...")) {
                    System.out.println("Cliente: Desconectado del servidor.");
                    break;
                } else if (respuesta.contains("Tu sesión ha sido cerrada") || 
                          respuesta.contains("Sesión cerrada") ||
                          respuesta.contains("¡Hasta pronto")) {
                    enMenuPrincipal = false;
                    enJuego = false;
                    System.out.println("Cliente: Sesión cerrada. Esperando menú de conexión...");
                }
                
                // Detectar cuándo el servidor espera una respuesta
                boolean necesitaRespuesta = 
                    respuesta.endsWith("?") ||
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
                    respuesta.contains("Intenta otra vez") ||
                    respuesta.contains("Elige el número del mensaje a borrar:") ||
                    respuesta.contains("Elige el número del mensaje enviado a borrar:") ||
                    respuesta.contains("(Escribe 'exit' para desconectar)");
                
                if (necesitaRespuesta) {
                    System.out.print("Tu respuesta: ");
                    String dato = teclado.readLine();
                    
                    // Verificar si el usuario quiere salir completamente
                    if ("exit".equalsIgnoreCase(dato.trim())) {
                        salida.println(dato);
                        // Esperar confirmación del servidor antes de cerrar
                        continue;
                    }
                    
                    salida.println(dato);
                    salida.flush();
                    
                    // Si estamos en el menú principal y el usuario elige "3" (Salir)
                    if ("3".equals(dato.trim()) && enMenuPrincipal && respuesta.contains("Elige opcion:")) {
                        // No cerramos la conexión, solo esperamos la respuesta del servidor
                        // El servidor nos dirá que la sesión se cerró y volveremos al menú principal
                        enMenuPrincipal = false;
                        System.out.println("Cliente: Cerrando sesión...");
                    }
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        
        System.out.println("Cliente: Conexión terminada.");
    }
}