package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final File MENSAJES_DIR = new File("mensajes");

    public static void main(String[] args) throws IOException {

        if (!MENSAJES_DIR.exists()) {
            MENSAJES_DIR.mkdirs();
        }

        // Hilo para la consola admin
        Thread adminThread = new Thread(Servidor::consolaAdmin);
        adminThread.setDaemon(true);
        adminThread.start();

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

    // ========================= CONSOLA ADMIN =========================
    private static void consolaAdmin() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.startsWith("/enviar ")) {
                    // Formato: /enviar <usuario> <mensaje>
                    String resto = linea.substring(8).trim();
                    int espacio = resto.indexOf(' ');
                    if (espacio <= 0) {
                        System.out.println("Uso: /enviar <usuario> <mensaje>");
                        continue;
                    }
                    String usuario = resto.substring(0, espacio).trim();
                    String mensaje = resto.substring(espacio + 1).trim();

                    if (!existeUsuario(usuario)) {
                        System.out.println("Usuario no existe: " + usuario);
                        continue;
                    }

                    enviarMensajeASingle(usuario, "[ADMIN] " + mensaje);
                    System.out.println("Mensaje enviado a " + usuario);
                } else if (linea.equalsIgnoreCase("/usuarios")) {
                    List<String> usuarios = listarUsuarios();
                    System.out.println("Usuarios registrados: " + usuarios);
                } else if (linea.equalsIgnoreCase("/help")) {
                    System.out.println("""
                        Comandos disponibles:
                        /usuarios -> lista usuarios registrados
                        /enviar <usuario> <mensaje> -> envia mensaje a usuario
                        /help -> muestra comandos
                    """);
                } else {
                    System.out.println("Comando no reconocido. Usa /help");
                }
            }
        } catch (IOException e) {
            System.out.println("Consola admin cerrada: " + e.getMessage());
        }
    }

    private static boolean existeUsuario(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes[0].trim().equals(usuario)) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    private static List<String> listarUsuarios() {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 1) usuarios.add(partes[0].trim());
            }
        } catch (IOException ignored) {}
        return usuarios;
    }

    // ========================= CLIENTE =========================
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
                        mostrarMenu(usuario, entrada, salida);
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

        private void mostrarMenu(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean continuar = true;
            while (continuar) {
                salida.println("\n=== MENU PRINCIPAL ===");
                salida.println("1) Ver bandeja de entrada");
                salida.println("2) Jugar - Adivina el número");
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
                        iniciarJuego(usuario, entrada, salida);
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

        // ========================= JUEGO ADIVINAR NÚMERO =========================
        private void iniciarJuego(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean seguirJugando = true;
            
            while (seguirJugando) {
                Random random = new Random();
                int numeroSecreto = random.nextInt(10) + 1;
                int intentos = 0;
                boolean acertado = false;
                
                salida.println("\n=== JUEGO: ADIVINA EL NÚMERO ===");
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
                        salida.println("¡Correcto! Adivinaste el número.");
                        acertado = true;
                        // Guardar estadística de victoria
                        enviarMensajeASingle(usuario, "¡Ganaste el juego de adivinanza! Número: " + numeroSecreto + " en " + (intentos + 1) + " intento(s).");
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
                            // Guardar estadística de derrota
                            enviarMensajeASingle(usuario, "Perdiste el juego de adivinanza. El número era: " + numeroSecreto);
                        }
                    }
                }
                
                // Preguntar si quiere jugar de nuevo
                if (!seguirJugando) {
                    break;
                }
                
                String respuesta;
                while (true) {
                    salida.println("¿Quieres jugar otra vez? (si/no)");
                    respuesta = entrada.readLine();
                    if (respuesta == null) {
                        seguirJugando = false;
                        break;
                    }
                    respuesta = respuesta.trim().toLowerCase();
                    if (respuesta.equals("si")) {
                        break; // Vuelve a empezar el juego
                    } else if (respuesta.equals("no")) {
                        seguirJugando = false;
                        salida.println("Gracias por jugar. Regresando al menu principal...");
                        break;
                    } else {
                        salida.println("Respuesta invalida. Escribe solo 'si' o 'no'.");
                    }
                }
            }
        }

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
        } catch (IOException ignored) {}
        return msgs;
    }

    private static void vaciarInbox(String usuario) {
        File f = archivoInbox(usuario);
        if (f.exists()) {
            try (PrintWriter pw = new PrintWriter(f)) {
                // truncar archivo
            } catch (IOException ignored) {}
        }
    }
}