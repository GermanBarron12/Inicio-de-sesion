
package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class clientedos {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;
    private static PrintWriter salida;
    private static BufferedReader entrada;
    private static volatile boolean gestionandoArchivos = false;
    private static final Object lock = new Object();

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             Scanner scanner = new Scanner(System.in)) {

            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("=== CONECTADO AL SERVIDOR ===");

            // Hilo para escuchar mensajes del servidor
            Thread hiloEscucha = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        synchronized (lock) {
                            // Detectar comandos especiales del servidor
                            if (mensaje.equals("LISTAR_ARCHIVOS")) {
                                manejarListarArchivos();
                            } else if (mensaje.equals("ENVIAR_ARCHIVO")) {
                                manejarEnviarArchivo();
                            } else if (mensaje.equals("GESTIONAR_ARCHIVOS_LOCALES")) {
                                gestionandoArchivos = true;
                                System.out.println("\n" + mensaje);
                            } else if (mensaje.equals("SUBMENU_ARCHIVOS")) {
                                // Señal de inicio de submenú
                                continue;
                            } else if (mensaje.equals("FIN_SUBMENU")) {
                                // Señal de fin de submenú
                                continue;
                            } else {
                                // Mensaje normal
                                System.out.println(mensaje);

                                // Mostrar prompt cuando el servidor espera entrada
                                if (mensaje.contains("Elige opcion:") ||
                                    mensaje.contains("Elige una opcion:") ||
                                    mensaje.contains("Introduce") ||
                                    mensaje.contains("Escribe") ||
                                    mensaje.contains("A que usuario") ||
                                    mensaje.contains("Que usuario") ||
                                    mensaje.contains("De que usuario") ||
                                    mensaje.contains("Adivina el numero") ||
                                    mensaje.endsWith("?")) {
                                    System.out.print("> ");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Conexion cerrada por el servidor.");
                }
            });

            hiloEscucha.setDaemon(true);
            hiloEscucha.start();

            // Esperar un momento para que el servidor envíe el primer mensaje
            Thread.sleep(100);

            // Bucle principal para enviar datos al servidor
            String input;
            while ((input = scanner.nextLine()) != null) {
                synchronized (lock) {
                    // Si estamos gestionando archivos locales, manejar el menú local
                    if (gestionandoArchivos) {
                        if (!manejarGestionArchivosLocales(input, scanner)) {
                            gestionandoArchivos = false;
                            System.out.print("> ");
                        }
                        continue;
                    }

                    salida.println(input);

                    // Si el usuario escribió 'exit', terminar
                    if ("exit".equalsIgnoreCase(input.trim())) {
                        break;
                    }

                    // Si el usuario eligió salir (opción 3), esperar y mostrar prompt
                    if ("3".equals(input.trim())) {
                        Thread.sleep(500);
                        System.out.print("> ");
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Conexion interrumpida.");
        }
    }

    // ========================= MANEJO DE COMANDOS ESPECIALES =========================

    /**
     * Listar archivos .txt del directorio actual y enviarlos al servidor
     */
    private static void manejarListarArchivos() {
        try {
            // Leer quién solicitó
            String solicitante = entrada.readLine();
            System.out.println("\n[SISTEMA] " + solicitante + " solicita ver tus archivos .txt");

            // Obtener archivos .txt del directorio actual
            File dirActual = new File(".");
            File[] archivos = dirActual.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

            if (archivos == null || archivos.length == 0) {
                salida.println("No hay archivos .txt en este directorio");
            } else {
                for (File archivo : archivos) {
                    salida.println("- " + archivo.getName() + " (" + archivo.length() + " bytes)");
                }
            }

            // Señal de fin de lista
            salida.println("FIN_LISTA");

        } catch (IOException e) {
            System.out.println("Error listando archivos: " + e.getMessage());
        }
    }

    /**
     * Enviar contenido de un archivo .txt al servidor
     */
    private static void manejarEnviarArchivo() {
        try {
            // Leer quién solicitó y qué archivo
            String solicitante = entrada.readLine();
            String nombreArchivo = entrada.readLine();

            System.out.println("\n[SISTEMA] " + solicitante + " solicita descargar: " + nombreArchivo);

            File archivo = new File(nombreArchivo);

            if (!archivo.exists() || !archivo.isFile()) {
                salida.println("ERROR: El archivo no existe");
                salida.println("FIN_ARCHIVO");
                return;
            }

            if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
                salida.println("ERROR: Solo se permiten archivos .txt");
                salida.println("FIN_ARCHIVO");
                return;
            }

            // Leer y enviar contenido línea por línea
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    salida.println(linea);
                }
            }

            // Señal de fin de archivo
            salida.println("FIN_ARCHIVO");
            System.out.println("[SISTEMA] Archivo enviado exitosamente");

        } catch (IOException e) {
            System.out.println("Error enviando archivo: " + e.getMessage());
            salida.println("ERROR: " + e.getMessage());
            salida.println("FIN_ARCHIVO");
        }
    }

    /**
     * Gestionar archivos locales (crear, editar, eliminar)
     * @return true si continúa en el menú, false si sale
     */
    private static boolean manejarGestionArchivosLocales(String opcion, Scanner scanner) {
        // Primera vez que entra, mostrar el menú
        if (opcion.equals("GESTIONAR_ARCHIVOS_LOCALES")) {
            mostrarMenuArchivos();
            return true;
        }

        switch (opcion.trim()) {
            case "1":
                listarArchivosLocales();
                mostrarMenuArchivos();
                return true;

            case "2":
                crearArchivoLocal(scanner);
                mostrarMenuArchivos();
                return true;

            case "3":
                editarArchivoLocal(scanner);
                mostrarMenuArchivos();
                return true;

            case "4":
                eliminarArchivoLocal(scanner);
                mostrarMenuArchivos();
                return true;

            case "5":
                verContenidoArchivo(scanner);
                mostrarMenuArchivos();
                return true;

            case "6":
                System.out.println("Volviendo al menu principal...");
                return false;

            default:
                System.out.println("Opcion no valida.");
                mostrarMenuArchivos();
                return true;
        }
    }

    private static void mostrarMenuArchivos() {
        System.out.println("\n=== GESTION DE ARCHIVOS LOCALES ===");
        System.out.println("1) Listar mis archivos .txt");
        System.out.println("2) Crear nuevo archivo .txt");
        System.out.println("3) Editar archivo .txt");
        System.out.println("4) Eliminar archivo .txt");
        System.out.println("5) Ver contenido de un archivo");
        System.out.println("6) Volver al menu principal");
        System.out.print("Elige opcion: ");
    }

    private static void listarArchivosLocales() {
        File dirActual = new File(".");
        File[] archivos = dirActual.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("No hay archivos .txt en este directorio");
        } else {
            System.out.println("\n=== TUS ARCHIVOS .TXT ===");
            for (File archivo : archivos) {
                System.out.println("- " + archivo.getName() + " (" + archivo.length() + " bytes)");
            }
        }
    }

    private static void crearArchivoLocal(Scanner scanner) {
        System.out.print("Nombre del nuevo archivo (sin extension): ");
        String nombre = scanner.nextLine().trim();

        if (nombre.isEmpty()) {
            System.out.println("Nombre invalido.");
            return;
        }

        String nombreCompleto = nombre + ".txt";
        File archivo = new File(nombreCompleto);

        if (archivo.exists()) {
            System.out.println("El archivo ya existe. Usa la opcion de editar.");
            return;
        }

        System.out.println("Escribe el contenido del archivo (escribe 'FIN' en una linea sola para terminar):");

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            String linea;
            while (!(linea = scanner.nextLine()).equals("FIN")) {
                pw.println(linea);
            }
            System.out.println("Archivo '" + nombreCompleto + "' creado exitosamente.");
        } catch (IOException e) {
            System.out.println("Error creando archivo: " + e.getMessage());
        }
    }

    private static void editarArchivoLocal(Scanner scanner) {
        System.out.print("Nombre del archivo a editar (con .txt): ");
        String nombreArchivo = scanner.nextLine().trim();

        File archivo = new File(nombreArchivo);

        if (!archivo.exists()) {
            System.out.println("El archivo no existe.");
            return;
        }

        System.out.println("\n=== CONTENIDO ACTUAL ===");
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo: " + e.getMessage());
            return;
        }

        System.out.println("\n1) Sobrescribir contenido");
        System.out.println("2) Agregar al final");
        System.out.print("Elige opcion: ");

        String opcion = scanner.nextLine().trim();

        if (opcion.equals("1")) {
            // Sobrescribir
            System.out.println("Escribe el nuevo contenido (escribe 'FIN' en una linea sola para terminar):");
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                String linea;
                while (!(linea = scanner.nextLine()).equals("FIN")) {
                    pw.println(linea);
                }
                System.out.println("Archivo actualizado exitosamente.");
            } catch (IOException e) {
                System.out.println("Error editando archivo: " + e.getMessage());
            }
        } else if (opcion.equals("2")) {
            // Agregar al final
            System.out.println("Escribe el contenido a agregar (escribe 'FIN' en una linea sola para terminar):");
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
                String linea;
                while (!(linea = scanner.nextLine()).equals("FIN")) {
                    pw.println(linea);
                }
                System.out.println("Contenido agregado exitosamente.");
            } catch (IOException e) {
                System.out.println("Error agregando contenido: " + e.getMessage());
            }
        } else {
            System.out.println("Opcion no valida.");
        }
    }

    private static void eliminarArchivoLocal(Scanner scanner) {
        System.out.print("Nombre del archivo a eliminar (con .txt): ");
        String nombreArchivo = scanner.nextLine().trim();

        File archivo = new File(nombreArchivo);

        if (!archivo.exists()) {
            System.out.println("El archivo no existe.");
            return;
        }

        System.out.print("Estas seguro de eliminar '" + nombreArchivo + "'? (si/no): ");
        String confirmacion = scanner.nextLine().trim().toLowerCase();

        if (confirmacion.equals("si") || confirmacion.equals("s")) {
            if (archivo.delete()) {
                System.out.println("Archivo eliminado exitosamente.");
            } else {
                System.out.println("No se pudo eliminar el archivo.");
            }
        } else {
            System.out.println("Operacion cancelada.");
        }
    }

    private static void verContenidoArchivo(Scanner scanner) {
        System.out.print("Nombre del archivo a ver (con .txt): ");
        String nombreArchivo = scanner.nextLine().trim();

        File archivo = new File(nombreArchivo);

        if (!archivo.exists()) {
            System.out.println("El archivo no existe.");
            return;
        }

        System.out.println("\n=== CONTENIDO DE " + nombreArchivo + " ===");
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numLinea = 1;
            while ((linea = br.readLine()) != null) {
                System.out.println(numLinea++ + ": " + linea);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo: " + e.getMessage());
        }
        System.out.println("=== FIN DEL ARCHIVO ===");
    }
}
