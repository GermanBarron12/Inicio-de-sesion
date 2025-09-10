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

            while ((respuesta = entrada.readLine()) != null) {
                System.out.println("Servidor: " + respuesta);

                // Si el servidor espera una respuesta del cliente:
                if (respuesta.endsWith("?") ||
                    respuesta.toLowerCase().contains("usuario") ||
                    respuesta.toLowerCase().contains("contraseña") ||
                    respuesta.toLowerCase().contains("opcion") ||
