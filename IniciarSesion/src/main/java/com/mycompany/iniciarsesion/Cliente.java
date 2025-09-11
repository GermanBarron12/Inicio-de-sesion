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
            boolean enJuego = false;  // estado del cliente
            String respuesta;

            while ((respuesta = entrada.readLine()) != null) {
                System.out.println("Servidor: " + respuesta);

                String low = respuesta.toLowerCase();

                boolean esperaInput =
                    respuesta.endsWith("?") ||
                    respuesta.endsWith(":") ||
                    low.contains("usuario") ||
                    low.contains("contraseña") ||
                    low.contains("elige opcion") ||
                    low.contains("elige opción") ||
                    low.contains("si/no") ||
                    low.contains("adivina") ||
                    low.contains("intentos") ||
                    low.contains("¿qué deseas hacer ahora?") ||
                    low.contains("1) jugar otra vez") ||
                    low.contains("2) regresar al menú principal") ||
                    low.contains("introduce");

                if (esperaInput) {
                    System.out.print("Tu: ");
                    String dato = teclado.readLine();
                    if (dato == null) break;

                    salida.println(dato);

                    // Detectar cambio de estado
                    if (low.contains("adivina")) {
                        enJuego = true;
                    }
                    if (dato.trim().equals("2") && low.contains("regresar")) {
                        enJuego = false; // vuelve al menú
                    }

                    // Si está en menú principal y elige "3", salir
                    if (!enJuego && dato.trim().equals("3")) {
                        System.out.println("Cliente: solicitaste salir. Cerrando conexión local.");
                        break;
                    }
                }

                if (low.contains("adios") || low.contains("hasta luego")) {
                    System.out.println("Servidor pidió cerrar la conexión. Saliendo.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error en cliente: " + e.getMessage());
        }
    }
}
