package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final File MENSAJES_DIR = new File("mensajes");

    public static void main(String[] args) throws IOException {

        if (!MENSAJES_DIR.exists()) {
            MENSAJES_DIR.mkdirs();
        }

        // Lanzar hilo para leer comandos desde la consola del servidor
        new Thread(() -> {
            try (BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {
                String comando;
                System.out.println("Escribe /help para ver los comandos disponibles");
                while ((comando = consola.readLine()) != null) {
                    manejarComandoServidor(comando);
                }
            } catch (IOException e) {
                System.out.println("Error en la consola del servidor: " + e.getMessage());
            }
        }).start();

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

    // ========================= MANEJADOR DE CLIENTES =========================
    private static class ManejadorCliente implements Runnable {

        private Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {
                salida.println("¿Quieres iniciar sesión (1) o registrarte (2)?");
                String opcion = entrada.readLine();

                String usuario = null;

                if ("2".equals(opcion)) {
                    salida.println("Introduce un nombre de usuario:");
                    usuario = entrada.readLine();

                    if (existeUsuario(usuario)) {
                        salida.println("El usuario ya existe. Intenta con otro nombre.");
                        return;
                    }

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

                        // 🔔 Notificación de mensajes pendientes
                        List<String> mensajesPendientes = leerInbox(usuario);
                        if (!mensajesPendientes.isEmpty()) {
                            salida.println("Tienes " + mensajesPendientes.size() + " mensaje(s) sin leer en tu bandeja.");
                        } else {
                            salida.println("No tienes mensajes nuevos.");
                        }

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
                salida.println("4) Enviar mensaje a otro usuario");
                salida.println("5) Ver todos los usuarios registrados");
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

                    case "4":
                        salida.println("¿A qué usuario deseas enviar el mensaje?");
                        String destinatario = entrada.readLine();

                        if (!existeUsuario(destinatario)) {
                            salida.println("El usuario " + destinatario + " no existe.");
                        } else if (destinatario.equals(usuario)) {
                            salida.println("No puedes enviarte mensajes a ti mismo.");
                        } else {
                            salida.println("Escribe el mensaje:");
                            String mensaje = entrada.readLine();
                            enviarMensajeASingle(destinatario,
                                    "[De " + usuario + "] " + mensaje);
                            salida.println("Mensaje enviado a " + destinatario);
                        }
                        break;

                    case "5":
                        List<String> todosLosUsuarios = obtenerTodosLosUsuarios();
                        if (todosLosUsuarios.isEmpty()) {
                            salida.println("No hay usuarios registrados.");
                        } else {
                            salida.println("Usuarios registrados:");
                            for (String u : todosLosUsuarios) {
                                if (!u.equals(usuario)) {
                                    salida.println("- " + u);
                                }
                            }
                        }
                        break;

                    default:
                        salida.println("Opcion no valida.");
                }
            }
            socket.close();
        }

        // ========================= JUEGO =========================
        private void iniciarJuego(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            Random random = new Random();
            int numeroSecreto = random.nextInt(10) + 1;
            int intentos = 0;

            salida.println("\n=== JUEGO: ADIVINA EL NÚMERO ===");
            salida.println("Adivina el numero del 1 al 10. Tienes 3 intentos.");

            while (intentos < 3) {
                String entradaUsuario = entrada.readLine();
                if (entradaUsuario == null) {
                    break;
                }

                try {
                    int intento = Integer.parseInt(entradaUsuario);
                    if (intento == numeroSecreto) {
                        salida.println("¡Correcto! El número era " + numeroSecreto);
                        return;
                    } else {
                        intentos++;
                        if (intentos < 3) {
                            salida.println("Incorrecto. El numero secreto es "
                                    + (intento < numeroSecreto ? "mayor" : "menor")
                                    + ". Intentos restantes: " + (3 - intentos));
                        } else {
                            salida.println("No lograste adivinar. El numero era: " + numeroSecreto);
                        }
                    }
                } catch (NumberFormatException e) {
                    salida.println("Eso no es un número válido. Intenta otra vez.");
                }
            }
            salida.println("Regresando al menu principal...");
        }

        // ========================= USUARIOS =========================
        private void guardarUsuario(String usuario, String contrasena) {
            try (FileWriter fw = new FileWriter(ARCHIVO_USUARIOS, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter pw = new PrintWriter(bw)) {
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
                    if (datos.length >= 2 && datos[0].equals(usuario) && datos[1].equals(contrasena)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // ========================= BANDEJA DE ENTRADA =========================
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

    private static boolean existeUsuario(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length > 0 && partes[0].trim().equals(usuario)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static List<String> obtenerTodosLosUsuarios() {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length > 0) {
                    usuarios.add(partes[0].trim());
                }
            }
        } catch (IOException ignored) {
        }
        return usuarios;
    }

    // ========================= COMANDOS DE CONSOLA DEL SERVIDOR =========================
    private static void manejarComandoServidor(String comando) {
        switch (comando.toLowerCase().trim()) {
            case "/help":
                System.out.println("=== COMANDOS DEL SERVIDOR ===");
                System.out.println("/help - Mostrar esta ayuda");
                System.out.println("/users - Mostrar todos los usuarios registrados");
                System.out.println("/messages <usuario> - Ver mensajes de un usuario");
                System.out.println("/enviar <usuario> <mensaje> - Enviar mensaje a un usuario");
                System.out.println("/clear - Limpiar consola");
                System.out.println("/stop - Detener servidor");
                break;

            case "/users":
                List<String> usuarios = obtenerTodosLosUsuarios();
                if (usuarios.isEmpty()) {
                    System.out.println("No hay usuarios registrados.");
                } else {
                    System.out.println("=== USUARIOS REGISTRADOS ===");
                    for (int i = 0; i < usuarios.size(); i++) {
                        System.out.println((i + 1) + ". " + usuarios.get(i));
                    }
                    System.out.println("Total: " + usuarios.size() + " usuarios");
                }
                break;

            case "/clear":
                for (int i = 0; i < 50; i++) {
                    System.out.println();
                }
                System.out.println("Servidor iniciado en el puerto " + PUERTO);
                System.out.println("Escribe /help para ver los comandos disponibles");
                break;

            case "/stop":
                System.out.println("Deteniendo servidor...");
                System.exit(0);
                break;

            default:
                if (comando.toLowerCase().startsWith("/messages ")) {
                    String usuario = comando.substring(10).trim();
                    if (existeUsuario(usuario)) {
                        List<String> mensajes = leerInbox(usuario);
                        if (mensajes.isEmpty()) {
                            System.out.println("El usuario " + usuario + " no tiene mensajes.");
                        } else {
                            System.out.println("=== MENSAJES DE " + usuario.toUpperCase() + " ===");
                            for (String msg : mensajes) {
                                System.out.println(msg);
                            }
                        }
                    } else {
                        System.out.println("El usuario '" + usuario + "' no existe.");
                    }
                } else if (comando.toLowerCase().startsWith("/enviar ")) {
                    // Formato: /enviar usuario mensaje
                    String[] partes = comando.split(" ", 3);
                    if (partes.length < 3) {
                        System.out.println("Uso: /enviar <usuario> <mensaje>");
                    } else {
                        String usuario = partes[1].trim();
                        String mensaje = partes[2].trim();
                        if (!existeUsuario(usuario)) {
                            System.out.println("El usuario '" + usuario + "' no existe.");
                        } else {
                            enviarMensajeASingle(usuario, "[SERVIDOR] " + mensaje);
                            System.out.println("Mensaje enviado a " + usuario + ": " + mensaje);
                        }
                    }
                } else {
                    System.out.println("Comando no reconocido. Escribe /help para ver los comandos disponibles.");
                }
                break;
        }
    }
}
