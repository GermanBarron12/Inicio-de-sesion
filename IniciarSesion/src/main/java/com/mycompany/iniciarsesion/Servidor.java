package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final File MENSAJES_DIR = new File("mensajes");
    private static final File BLOQUEOS_DIR = new File("bloqueos");

    public static void main(String[] args) throws IOException {

        if (!MENSAJES_DIR.exists()) {
            MENSAJES_DIR.mkdirs();
        }

        if (!BLOQUEOS_DIR.exists()) {
            BLOQUEOS_DIR.mkdirs();
        }

        // Hilo para comandos del servidor
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
                System.out.println("Cliente conectado desde: " + socket.getRemoteSocketAddress());
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
            String usuarioActual = null;
            try (
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
            ) {
                
                // Bucle principal para permitir multiples sesiones en la misma conexion
                boolean conectado = true;
                while (conectado) {
                    salida.println("Quieres iniciar sesion (1) o registrarte (2)? (Escribe 'exit' para desconectar)");
                    String opcion = entrada.readLine();
                    
                    // Verificar si el cliente se desconecto
                    if (opcion == null) {
                        break;
                    }
                    
                    // Permitir desconexion completa
                    if ("exit".equalsIgnoreCase(opcion.trim())) {
                        salida.println("Hasta luego! Desconectando...");
                        conectado = false;
                        break;
                    }

                    usuarioActual = null;

                    if ("2".equals(opcion)) {
                        // Registro de usuario
                        salida.println("Introduce un nombre de usuario:");
                        usuarioActual = entrada.readLine();
                        
                        if (usuarioActual == null) break;

                        if (existeUsuario(usuarioActual)) {
                            salida.println("El usuario ya existe. Intenta con otro nombre.");
                            continue; // Volver al menu principal
                        }

                        salida.println("Introduce una contrasena:");
                        String contrasena = entrada.readLine();
                        
                        if (contrasena == null) break;

                        guardarUsuario(usuarioActual, contrasena);
                        salida.println("Registro exitoso. Ahora puedes iniciar sesion.");

                    } else if ("1".equals(opcion)) {
                        // Inicio de sesion
                        salida.println("Introduce tu usuario:");
                        usuarioActual = entrada.readLine();
                        
                        if (usuarioActual == null) break;

                        salida.println("Introduce tu contrasena:");
                        String contrasena = entrada.readLine();
                        
                        if (contrasena == null) break;

                        if (validarUsuario(usuarioActual, contrasena)) {
                            salida.println("Inicio de sesion exitoso. Bienvenido " + usuarioActual + "!");
                            System.out.println("Usuario " + usuarioActual + " ha iniciado sesion");

                            // Notificacion de mensajes pendientes
                            List<String> mensajesPendientes = leerInbox(usuarioActual);
                            if (!mensajesPendientes.isEmpty()) {
                                salida.println("Tienes " + mensajesPendientes.size() + " mensaje(s) en tu bandeja.");
                            } else {
                                salida.println("No tienes mensajes nuevos.");
                            }

                            // Mostrar menu y manejar la sesion del usuario
                            mostrarMenu(usuarioActual, entrada, salida);
                            
                            // Despues de cerrar sesion, volver al menu principal
                            System.out.println("Usuario " + usuarioActual + " ha cerrado sesion");
                            salida.println("Sesion cerrada. Puedes iniciar otra sesion o registrar un nuevo usuario.");
                            usuarioActual = null; // Limpiar usuario actual
                            
                        } else {
                            salida.println("Usuario o contrasena incorrectos.");
                        }
                    } else {
                        salida.println("Opcion no valida.");
                    }
                }

            } catch (IOException e) {
                System.out.println("Error con cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    if (usuarioActual != null) {
                        System.out.println("Cliente desconectado (usuario: " + usuarioActual + ")");
                    } else {
                        System.out.println("Cliente desconectado");
                    }
                } catch (IOException e) {
                    System.out.println("Error cerrando socket: " + e.getMessage());
                }
            }
        }

        private void mostrarMenu(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            while (true) {
                salida.println("\n=== MENU PRINCIPAL ===");
                salida.println("1) Ver bandeja de entrada");
                salida.println("2) Jugar - Adivina el numero");
                salida.println("3) Salir (cerrar sesion)");
                salida.println("4) Enviar mensaje a otro usuario");
                salida.println("5) Ver todos los usuarios registrados");
                salida.println("6) Borrar un mensaje de la bandeja");
                salida.println("7) Borrar un mensaje enviado");
                salida.println("8) Bloquear un usuario");
                salida.println("9) Desbloquear un usuario");
                salida.println("10) Ver usuarios bloqueados");
                salida.println("Elige opcion:");

                String opcion = entrada.readLine();
                
                // Si entrada es null, el cliente se desconecto
                if (opcion == null) {
                    throw new IOException("Cliente desconectado"); // Lanzar excepcion para manejar desconexion
                }

                switch (opcion) {
                    case "1":
                        List<String> mensajes = leerInbox(usuario);
                        if (mensajes.isEmpty()) {
                            salida.println("No tienes mensajes.");
                        } else {
                            salida.println("Tus mensajes:");
                            for (int i = 0; i < mensajes.size(); i++) {
                                salida.println(i + ") " + mensajes.get(i));
                            }
                        }
                        break;

                    case "2":
                        iniciarJuego(usuario, entrada, salida);
                        break;

                    case "3":
                        salida.println("Hasta pronto " + usuario + "! Tu sesion ha sido cerrada.");
                        return; // Salir del metodo para cerrar sesion

                    case "4":
                        salida.println("A que usuario deseas enviar el mensaje?");
                        String destinatario = entrada.readLine();
                        
                        if (destinatario == null) throw new IOException("Cliente desconectado");

                        if (!existeUsuario(destinatario)) {
                            salida.println("El usuario " + destinatario + " no existe.");
                        } else if (destinatario.equals(usuario)) {
                            salida.println("No puedes enviarte mensajes a ti mismo.");
                        } else if (estaBloqueado(destinatario, usuario)) {
                            // destinatario tiene bloqueado al remitente
                            salida.println("No puedes enviar mensajes. El usuario " + destinatario + " te ha bloqueado.");
                        } else if (estaBloqueado(usuario, destinatario)) {
                            // remitente tiene bloqueado al destinatario
                            salida.println("No puedes enviar mensajes a " + destinatario + " porque lo tienes bloqueado.");
                        } else {
                            salida.println("Escribe el mensaje:");
                            String mensaje = entrada.readLine();
                            
                            if (mensaje == null) throw new IOException("Cliente desconectado");
                            
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

                    case "6":
                        List<String> inbox = leerInbox(usuario);
                        if (inbox.isEmpty()) {
                            salida.println("No tienes mensajes para borrar.");
                        } else {
                            salida.println("Elige el numero del mensaje a borrar:");
                            for (int i = 0; i < inbox.size(); i++) {
                                salida.println(i + ") " + inbox.get(i));
                            }
                            String idxStr = entrada.readLine();
                            
                            if (idxStr == null) throw new IOException("Cliente desconectado");
                            
                            try {
                                int idx = Integer.parseInt(idxStr);
                                if (borrarMensaje(usuario, idx)) {
                                    salida.println("Mensaje borrado con exito.");
                                } else {
                                    salida.println("Indice invalido, no se borro nada.");
                                }
                            } catch (NumberFormatException e) {
                                salida.println("Entrada invalida. No se borro nada.");
                            }
                        }
                        break;

                    case "7":
                        List<String> enviados = obtenerMensajesEnviados(usuario);
                        if (enviados.isEmpty()) {
                            salida.println("No tienes mensajes enviados.");
                        } else {
                            salida.println("Elige el numero del mensaje enviado a borrar:");
                            for (int i = 0; i < enviados.size(); i++) {
                                salida.println(i + ") " + enviados.get(i));
                            }
                            String idxStr = entrada.readLine();
                            
                            if (idxStr == null) throw new IOException("Cliente desconectado");
                            
                            try {
                                int idx = Integer.parseInt(idxStr);
                                if (borrarMensajeEnviado(usuario, idx)) {
                                    salida.println("Mensaje enviado borrado con exito.");
                                } else {
                                    salida.println("Indice invalido, no se borro nada.");
                                }
                            } catch (NumberFormatException e) {
                                salida.println("Entrada invalida. No se borro nada.");
                            }
                        }
                        break;

                    // ---------- Opciones de bloqueo ----------
                    case "8":
                        salida.println("Que usuario deseas bloquear?");
                        String usuarioBloquear = entrada.readLine();
                        if (usuarioBloquear == null) throw new IOException("Cliente desconectado");

                        if (!existeUsuario(usuarioBloquear)) {
                            salida.println("El usuario " + usuarioBloquear + " no existe.");
                        } else if (usuarioBloquear.equals(usuario)) {
                            salida.println("No puedes bloquearte a ti mismo.");
                        } else {
                            bloquearUsuario(usuario, usuarioBloquear);
                            salida.println("Has bloqueado a " + usuarioBloquear);
                        }
                        break;

                    case "9":
                        salida.println("Que usuario deseas desbloquear?");
                        String usuarioDesbloquear = entrada.readLine();
                        if (usuarioDesbloquear == null) throw new IOException("Cliente desconectado");

                        if (desbloquearUsuario(usuario, usuarioDesbloquear)) {
                            salida.println("Has desbloqueado a " + usuarioDesbloquear);
                        } else {
                            salida.println("Ese usuario no estaba bloqueado.");
                        }
                        break;

                    case "10":
                        List<String> bloqueados = obtenerUsuariosBloqueados(usuario);
                        if (bloqueados.isEmpty()) {
                            salida.println("No tienes usuarios bloqueados.");
                        } else {
                            salida.println("Usuarios bloqueados:");
                            for (String b : bloqueados) {
                                salida.println("- " + b);
                            }
                        }
                        break;

                    default:
                        salida.println("Opcion no valida.");
                }
            }
        }

        // ========================= JUEGO =========================
        private void iniciarJuego(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            Random random = new Random();
            int numeroSecreto = random.nextInt(10) + 1;
            int intentos = 0;

            salida.println("\n=== JUEGO: ADIVINA EL NUMERO ===");
            salida.println("Adivina el numero del 1 al 10. Tienes 3 intentos.");

            while (intentos < 3) {
                String entradaUsuario = entrada.readLine();
                if (entradaUsuario == null) {
                    break;
                }

                try {
                    int intento = Integer.parseInt(entradaUsuario);
                    if (intento == numeroSecreto) {
                        salida.println("Correcto! El numero era " + numeroSecreto);
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
                    salida.println("Eso no es un numero valido. Intenta otra vez.");
                }
            }
            salida.println("Regresando al menu principal...");
        }

        // ========================= USUARIOS =========================
        private void guardarUsuario(String usuario, String contrasena) {
            // Limpiar espacios en blanco y caracteres especiales
            String usuarioLimpio = usuario.trim();
            String contrasenaLimpia = contrasena.trim();
            
            try (FileWriter fw = new FileWriter(ARCHIVO_USUARIOS, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                pw.println(usuarioLimpio + "," + contrasenaLimpia);
                System.out.println("DEBUG: Usuario guardado - '" + usuarioLimpio + "','" + contrasenaLimpia + "'");
            } catch (IOException e) {
                System.out.println("Error guardando usuario: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private boolean validarUsuario(String usuario, String contrasena) {
            try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] datos = linea.split(",");
                    if (datos.length >= 2 && datos[0].trim().equals(usuario.trim()) && datos[1].trim().equals(contrasena.trim())) {
                        return true;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error validando usuario: " + e.getMessage());
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
        } catch (IOException ignored) {}
        return msgs;
    }

    private static boolean borrarMensaje(String usuario, int indice) {
        File f = archivoInbox(usuario);
        if (!f.exists()) {
            return false;
        }

        try {
            List<String> mensajes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String l;
                while ((l = br.readLine()) != null) {
                    mensajes.add(l);
                }
            }

            if (indice < 0 || indice >= mensajes.size()) {
                return false;
            }

            mensajes.remove(indice);

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                for (String msg : mensajes) {
                    pw.println(msg);
                }
            }
            return true;

        } catch (IOException e) {
            System.out.println("Error al borrar mensaje de " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    // Obtener todos los mensajes enviados por un usuario
    private static List<String> obtenerMensajesEnviados(String remitente) {
        List<String> enviados = new ArrayList<>();
        File[] archivos = MENSAJES_DIR.listFiles();
        if (archivos == null) return enviados;

        for (File archivo : archivos) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.contains("[De " + remitente + "]")) {
                        enviados.add(archivo.getName().replace(".txt", "") + " <- " + linea);
                    }
                }
            } catch (IOException ignored) {}
        }
        return enviados;
    }

    // Borrar un mensaje enviado por un usuario en el inbox del destinatario
    private static boolean borrarMensajeEnviado(String remitente, int indice) {
        List<String> enviados = obtenerMensajesEnviados(remitente);
        if (indice < 0 || indice >= enviados.size()) return false;

        String seleccionado = enviados.get(indice);
        String destinatario = seleccionado.split(" <- ")[0];
        String contenido = seleccionado.split(" <- ")[1];

        File inboxFile = archivoInbox(destinatario);
        if (!inboxFile.exists()) return false;

        try {
            List<String> mensajes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(inboxFile))) {
                String l;
                while ((l = br.readLine()) != null) {
                    mensajes.add(l);
                }
            }

            boolean removed = mensajes.removeIf(m -> m.equals(contenido));

            if (removed) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(inboxFile))) {
                    for (String msg : mensajes) {
                        pw.println(msg);
                    }
                }
            }

            return removed;
        } catch (IOException e) {
            System.out.println("Error al borrar mensaje enviado de " + remitente + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean existeUsuario(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length > 0 && partes[0].trim().equals(usuario.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error verificando usuario: " + e.getMessage());
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
        } catch (IOException ignored) {}
        return usuarios;
    }

    // ========================= FUNCION PARA ELIMINAR USUARIO =========================
    /**
     * Elimina un usuario del sistema completamente:
     * - Lo remueve del archivo usuarios.txt
     * - Borra su archivo de mensajes (inbox)
     * - Elimina todos los mensajes que envio a otros usuarios
     * - Elimina su archivo de bloqueos y lo remueve de los bloqueos de otros
     */
    private static boolean eliminarUsuario(String usuario) {
        if (!existeUsuario(usuario)) {
            return false;
        }

        try {
            // 1. Eliminar usuario del archivo usuarios.txt
            List<String> usuariosRestantes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] partes = linea.split(",");
                    if (partes.length > 0 && !partes[0].trim().equals(usuario)) {
                        usuariosRestantes.add(linea);
                    }
                }
            }

            // Reescribir el archivo sin el usuario eliminado
            try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_USUARIOS))) {
                for (String linea : usuariosRestantes) {
                    pw.println(linea);
                }
            }

            // 2. Eliminar el archivo de mensajes del usuario (su inbox)
            File archivoInboxUsuario = archivoInbox(usuario);
            if (archivoInboxUsuario.exists()) {
                archivoInboxUsuario.delete();
            }

            // 3. Eliminar todos los mensajes que este usuario envio a otros usuarios
            eliminarMensajesEnviadosPorUsuario(usuario);

            // 4. Eliminar bloqueos relacionados: eliminar su archivo de bloqueos y quitar su nombre de otros archivos
            eliminarBloqueosDeUsuario(usuario);

            return true;

        } catch (IOException e) {
            System.out.println("Error al eliminar usuario " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina todos los mensajes enviados por un usuario especifico de todos los inboxes
     */
    private static void eliminarMensajesEnviadosPorUsuario(String remitente) {
        File[] archivos = MENSAJES_DIR.listFiles();
        if (archivos == null) return;

        for (File archivo : archivos) {
            if (archivo.isFile() && archivo.getName().endsWith(".txt")) {
                try {
                    List<String> mensajesFiltrados = new ArrayList<>();
                    boolean seEliminoAlgo = false;

                    // Leer todos los mensajes del archivo
                    try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                        String linea;
                        while ((linea = br.readLine()) != null) {
                            // Si el mensaje NO fue enviado por el usuario eliminado, lo conservamos
                            if (!linea.contains("[De " + remitente + "]")) {
                                mensajesFiltrados.add(linea);
                            } else {
                                seEliminoAlgo = true;
                            }
                        }
                    }

                    // Si se elimino algun mensaje, reescribir el archivo
                    if (seEliminoAlgo) {
                        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                            for (String mensaje : mensajesFiltrados) {
                                pw.println(mensaje);
                            }
                        }
                        System.out.println("Se eliminaron mensajes de " + remitente + " del inbox de " + 
                                         archivo.getName().replace(".txt", ""));
                    }

                } catch (IOException e) {
                    System.out.println("Error eliminando mensajes de " + remitente + " en " + 
                                     archivo.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    // ========================= FUNCIONES DE BLOQUEO =========================

    private static File archivoBloqueos(String usuario) {
        return new File(BLOQUEOS_DIR, usuario + ".txt");
    }

    /**
     * Anade 'bloqueado' al archivo de bloqueos de 'usuario' (si no esta ya)
     */
    private static void bloquearUsuario(String usuario, String bloqueado) {
        try {
            File f = archivoBloqueos(usuario);
            Set<String> set = new LinkedHashSet<>();
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        if (!linea.trim().isEmpty()) set.add(linea.trim());
                    }
                }
            }
            if (!set.contains(bloqueado)) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                    bw.write(bloqueado);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error bloqueando usuario: " + e.getMessage());
        }
    }

    /**
     * Elimina 'bloqueado' del archivo de bloqueos de 'usuario' si existe.
     * Devuelve true si se elimino.
     */
    private static boolean desbloquearUsuario(String usuario, String bloqueado) {
        File f = archivoBloqueos(usuario);
        if (!f.exists()) return false;

        try {
            List<String> lista = new ArrayList<>();
            boolean eliminado = false;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String t = linea.trim();
                    if (!t.isEmpty() && !t.equals(bloqueado)) {
                        lista.add(t);
                    } else if (t.equals(bloqueado)) {
                        eliminado = true;
                    }
                }
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                for (String u : lista) {
                    pw.println(u);
                }
            }

            return eliminado;
        } catch (IOException e) {
            System.out.println("Error desbloqueando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna la lista de usuarios que 'usuario' ha bloqueado.
     */
    private static List<String> obtenerUsuariosBloqueados(String usuario) {
        List<String> bloqueados = new ArrayList<>();
        File f = archivoBloqueos(usuario);
        if (!f.exists()) return bloqueados;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String t = linea.trim();
                if (!t.isEmpty()) bloqueados.add(t);
            }
        } catch (IOException ignored) {}
        return bloqueados;
    }

    /**
     * Verifica si 'usuario' tiene bloqueado a 'posibleBloqueado'
     */
    private static boolean estaBloqueado(String usuario, String posibleBloqueado) {
        List<String> bloqueados = obtenerUsuariosBloqueados(usuario);
        return bloqueados.contains(posibleBloqueado);
    }

    /**
     * Elimina el archivo de bloqueos del usuario y lo remueve de los archivos de otros usuarios.
     */
    private static void eliminarBloqueosDeUsuario(String usuario) {
        // eliminar su propio archivo de bloqueos
        File f = archivoBloqueos(usuario);
        if (f.exists()) {
            f.delete();
        }

        // eliminar su nombre de los archivos de bloqueos de otros usuarios
        File[] archivos = BLOQUEOS_DIR.listFiles();
        if (archivos == null) return;

        for (File archivo : archivos) {
            if (!archivo.isFile()) continue;
            try {
                List<String> lista = new ArrayList<>();
                boolean modificado = false;
                try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        String t = linea.trim();
                        if (!t.isEmpty() && !t.equals(usuario)) {
                            lista.add(t);
                        } else if (t.equals(usuario)) {
                            modificado = true;
                        }
                    }
                }
                if (modificado) {
                    try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                        for (String u : lista) pw.println(u);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error actualizando bloqueos en " + archivo.getName() + ": " + e.getMessage());
            }
        }
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
                System.out.println("/eliminar <usuario> - Eliminar usuario y todos sus mensajes");
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
                } else if (comando.toLowerCase().startsWith("/eliminar ")) {
                    String usuario = comando.substring(10).trim();
                    if (usuario.isEmpty()) {
                        System.out.println("Uso: /eliminar <usuario>");
                    } else {
                        if (eliminarUsuario(usuario)) {
                            System.out.println("Usuario '" + usuario + "' eliminado exitosamente.");
                            System.out.println("   - Removido del archivo de usuarios");
                            System.out.println("   - Eliminado su archivo de mensajes");
                            System.out.println("   - Eliminados todos los mensajes que envio a otros usuarios");
                            System.out.println("   - Eliminados bloqueos relacionados");
                        } else {
                            System.out.println("No se pudo eliminar el usuario '" + usuario + "'. Verifica que exista.");
                        }
                    }
                } else {
                    System.out.println("Comando no reconocido. Escribe /help para ver los comandos disponibles.");
                }
                break;
        }
    }
}