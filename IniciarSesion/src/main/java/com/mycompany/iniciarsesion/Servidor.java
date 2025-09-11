package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final File MENSAJES_DIR = new File("mensajes");

    public static void main(String[] args) throws IOException {
        if (!MENSAJES_DIR.exists()) {
            MENSAJES_DIR.mkdir();
        }

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado");
                new Thread(new ManejadorCliente(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ======================== MANEJADOR DE CLIENTES ========================
    private static class ManejadorCliente implements Runnable {
        private Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
            ) {
                salida.println("¿Quieres iniciar sesión (1) o registrarte (2)?");
                String opcion = entrada.readLine();

                String usuario = null;

                if ("2".equals(opcion)) {
                    salida.println("Introduce un nombre de usuario:");
                    usuario = entrada.readLine();

                    salida.println("Introduce una contraseña:");
                    String contrasena = entrada.readLine();

                    guardarUsuario(usuario, contrasena);
                    salida.println("Registro exitoso. Ahora puedes iniciar sesión.");

                } else if ("1".equals(opcion)) {
                    salida.println("Introduce tu usuario:");
                    usuario = entrada.readLine();

                    salida.println("Introduce tu contraseña:");
                    String contrasena = entrada.readLine();

                    if (validarUsuario(usuario, contrasena)) {
                        salida.println("Inicio de sesión exitoso. Bienvenido " + usuario + "!");

                        // Si es admin entra al menú de admin
                        if ("admin".equalsIgnoreCase(usuario)) {
                            mostrarMenuAdmin(usuario, entrada, salida);
                        } else {
                            mostrarMenu(usuario, entrada, salida);
                        }

                    } else {
                        salida.println("Usuario o contraseña incorrectos.");
                    }
                } else {
                    salida.println("Opción no válida.");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ======================== MENÚ PRINCIPAL DEL USUARIO ========================
        private void mostrarMenu(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean continuar = true;
            while (continuar) {
                salida.println("\n=== MENU PRINCIPAL ===");
                salida.println("1) Ver bandeja de entrada");
                salida.println("2) Jugar Adivina el número");
                salida.println("3) Salir");
                salida.println("Elige opcion:");

                String opcion = entrada.readLine();

                switch (opcion) {
                    case "1":
                        List<String> mensajes = leerInbox(usuario);
                        if (mensajes.isEmpty()) {
                            salida.println("No tienes mensajes nuevos.");
                        } else {
                            salida.println("Tus mensajes:");
                            for (String msg : mensajes) {
                                salida.println(msg);
                            }
                            vaciarInbox(usuario);
                        }
                        break;

                    case "2":
                        jugarAdivinaNumero(entrada, salida);
                        break;

                    case "3":
                        salida.println("Adios!");
                        continuar = false;
                        break;

                    default:
                        salida.println("Opcion no valida.");
                }
            }
            socket.close();
        }

        // ======================== MENÚ ADMIN ========================
        private void mostrarMenuAdmin(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean continuar = true;
            while (continuar) {
                salida.println("\n=== MENU ADMIN ===");
                salida.println("1) Enviar mensaje a un usuario");
                salida.println("2) Salir");
                salida.println("Elige opcion:");

                String opcion = entrada.readLine();

                switch (opcion) {
                    case "1":
                        salida.println("Introduce el usuario destino:");
                        String destino = entrada.readLine();
                        salida.println("Escribe el mensaje:");
                        String mensaje = entrada.readLine();
                        enviarMensajeASingle(destino, "ADMIN -> " + mensaje);
                        salida.println("Mensaje enviado a " + destino);
                        break;

                    case "2":
                        salida.println("Saliendo del modo admin...");
                        continuar = false;
                        break;

                    default:
                        salida.println("Opcion no valida.");
                }
            }
            socket.close();
        }

        // ======================== JUEGO ADIVINA EL NÚMERO ========================
        private void jugarAdivinaNumero(BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean seguirJugando = true;

            while (seguirJugando) {
                Random random = new Random();
                int numeroSecreto = random.nextInt(10) + 1;
                int intentos = 0;
                boolean acertado = false;

                salida.println("Nuevo juego: Adivina el numero del 1 al 10. Tienes 3 intentos.");

                while (intentos < 3) {
                    String entradaUsuario = entrada.readLine();
                    if (entradaUsuario == null) {
                        seguirJugando = false;
                        break;
                    }

                    entradaUsuario = entradaUsuario.trim();
                    int intentoUsuario;
                    try {
                        intentoUsuario = Integer.parseInt(entradaUsuario);
                    } catch (NumberFormatException e) {
                        salida.println("Eso no es un numero valido. Intenta de nuevo (no cuenta como intento).");
                        continue;
                    }

                    if (intentoUsuario == numeroSecreto) {
                        salida.println("Correcto! Adivinaste el número.");
                        acertado = true;
                        break;
                    } else {
                        intentos++;
                        String pista = (intentoUsuario < numeroSecreto)
                                ? "Incorrecto. El numero secreto es mayor."
                                : "Incorrecto. El numero secreto es menor.";

                        if (intentos < 3) {
                            salida.println(pista + " Intentos restantes: " + (3 - intentos));
                        } else {
                            salida.println("No lograste adivinar. El numero era: " + numeroSecreto);
                        }
                    }
                }

                if (!seguirJugando) break;

                salida.println("¿Quieres jugar otra vez? (si/no)");
                String respuesta = entrada.readLine();

                if (respuesta == null || respuesta.trim().equalsIgnoreCase("no")) {
                    seguirJugando = false;
                    salida.println("Gracias por jugar. Regresando al menú principal...");
                }
            }
        }

        // ======================== FUNCIONES DE USUARIO ========================
        private void guardarUsuario(String usuario, String contrasena) {
            try (FileWriter fw = new FileWriter(ARCHIVO_USUARIOS, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                pw.println(usuario + "," + contrasena);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean validarUsuario(String usuario, String contrasena) {
            try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] datos = linea.split(",");
                    if (datos[0].equals(usuario) && datos[1].equals(contrasena)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // ======================== FUNCIONES DE MENSAJES ========================
    private static synchronized void enviarMensajeASingle(String usuario, String texto) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoInbox(usuario), true))) {
            bw.write(new Date() + " | " + texto);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error guardando mensaje para " + usuario + ": " + e.getMessage());
        }
    }

    private static File archivoInbox(String usuario) {
        return new File(MENSAJES_DIR, usuario + ".txt");
    }

    private static List<String> leerInbox(String usuario) {
        List<String> msgs = new ArrayList<>();
        File f = archivoInbox(usuario);
        if (!f.exists()) {
            return msgs;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = br.readLine()) != null) {
                msgs.add(l);
            }
        } catch (IOException ignored) {
        }
        return msgs;
    }

    private static void vaciarInbox(String usuario) {
        File f = archivoInbox(usuario);
        if (f.exists()) {
            try (PrintWriter pw = new PrintWriter(f)) {
                // truncar archivo
            } catch (IOException ignored) {
            }
        }
    }
}
