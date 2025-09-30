package com.mycompany.iniciarsesion;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

class MensajeInfo {
    String contenido;
    String remitente;
    
    public MensajeInfo(String contenido, String remitente) {
        this.contenido = contenido;
        this.remitente = remitente;
    }
}

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final File MENSAJES_DIR = new File("mensajes");
    private static final File BLOQUEOS_DIR = new File("bloqueos");
    private static final Map<String, ManejadorCliente> usuariosConectados = new HashMap<>();

    public static void main(String[] args) throws IOException {

        if (!MENSAJES_DIR.exists()) {
            MENSAJES_DIR.mkdirs();
        }

        if (!BLOQUEOS_DIR.exists()) {
            BLOQUEOS_DIR.mkdirs();
        }

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

    private static class ManejadorCliente implements Runnable {

        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private String usuarioActual;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);

                boolean conectado = true;
                while (conectado) {
                    salida.println("Quieres iniciar sesion (1) o registrarte (2)? (Escribe 'exit' para desconectar)");
                    String opcion = entrada.readLine();

                    if (opcion == null) {
                        break;
                    }

                    if ("exit".equalsIgnoreCase(opcion.trim())) {
                        salida.println("Hasta luego! Desconectando...");
                        conectado = false;
                        break;
                    }

                    usuarioActual = null;

                    if ("2".equals(opcion)) {
                        salida.println("Introduce un nombre de usuario:");
                        usuarioActual = entrada.readLine();

                        if (usuarioActual == null) {
                            break;
                        }

                        if (existeUsuario(usuarioActual)) {
                            salida.println("El usuario ya existe. Intenta con otro nombre.");
                            continue;
                        }

                        salida.println("Introduce una contrasena:");
                        String contrasena = entrada.readLine();

                        if (contrasena == null) {
                            break;
                        }

                        guardarUsuario(usuarioActual, contrasena);
                        salida.println("Registro exitoso. Ahora puedes iniciar sesion.");

                    } else if ("1".equals(opcion)) {
                        salida.println("Introduce tu usuario:");
                        usuarioActual = entrada.readLine();

                        if (usuarioActual == null) {
                            break;
                        }

                        salida.println("Introduce tu contrasena:");
                        String contrasena = entrada.readLine();

                        if (contrasena == null) {
                            break;
                        }

                        if (validarUsuario(usuarioActual, contrasena)) {
                            salida.println("Inicio de sesion exitoso. Bienvenido " + usuarioActual + "!");
                            System.out.println("Usuario " + usuarioActual + " ha iniciado sesion");

                            synchronized (usuariosConectados) {
                                usuariosConectados.put(usuarioActual, this);
                            }

                            List<String> mensajesPendientes = leerInbox(usuarioActual);
                            if (!mensajesPendientes.isEmpty()) {
                                salida.println("Tienes " + mensajesPendientes.size() + " mensaje(s) en tu bandeja.");
                            } else {
                                salida.println("No tienes mensajes nuevos.");
                            }

                            mostrarMenu(usuarioActual, entrada, salida);

                            synchronized (usuariosConectados) {
                                usuariosConectados.remove(usuarioActual);
                            }

                            System.out.println("Usuario " + usuarioActual + " ha cerrado sesion");
                            salida.println("Sesion cerrada. Puedes iniciar otra sesion o registrar un nuevo usuario.");
                            usuarioActual = null;

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
                    if (usuarioActual != null) {
                        synchronized (usuariosConectados) {
                            usuariosConectados.remove(usuarioActual);
                        }
                        System.out.println("Cliente desconectado (usuario: " + usuarioActual + ")");
                    } else {
                        System.out.println("Cliente desconectado");
                    }
                    socket.close();
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
                salida.println("11) Listar archivos de otro usuario");
                salida.println("12) Descargar archivo");
                salida.println("13) Gestionar mis archivos locales");
                salida.println("Elige opcion:");

                String opcion = entrada.readLine();

                if (opcion == null) {
                    throw new IOException("Cliente desconectado");
                }

                switch (opcion) {
                    case "1":
                        verBandejaConPaginacion(usuario, entrada, salida);
                        break;

                    case "2":
                        iniciarJuego(usuario, entrada, salida);
                        break;

                    case "3":
                        salida.println("Hasta pronto " + usuario + "! Tu sesion ha sido cerrada.");
                        return;

                    case "4":
                        salida.println("A que usuario deseas enviar el mensaje?");
                        String destinatario = entrada.readLine();

                        if (destinatario == null) {
                            throw new IOException("Cliente desconectado");
                        }

                        if (!existeUsuario(destinatario)) {
                            salida.println("El usuario " + destinatario + " no existe.");
                        } else if (destinatario.equals(usuario)) {
                            salida.println("No puedes enviarte mensajes a ti mismo.");
                        } else if (estaBloqueado(destinatario, usuario)) {
                            salida.println("No puedes enviar mensajes. El usuario " + destinatario + " te ha bloqueado.");
                        } else if (estaBloqueado(usuario, destinatario)) {
                            salida.println("No puedes enviar mensajes a " + destinatario + " porque lo tienes bloqueado.");
                        } else {
                            salida.println("Escribe el mensaje:");
                            String mensaje = entrada.readLine();

                            if (mensaje == null) {
                                throw new IOException("Cliente desconectado");
                            }

                            enviarMensajeASingle(destinatario, "[De " + usuario + "] " + mensaje);
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

                            if (idxStr == null) {
                                throw new IOException("Cliente desconectado");
                            }

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

                            if (idxStr == null) {
                                throw new IOException("Cliente desconectado");
                            }

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

                    case "8":
                        salida.println("Que usuario deseas bloquear?");
                        String usuarioBloquear = entrada.readLine();
                        if (usuarioBloquear == null) {
                            throw new IOException("Cliente desconectado");
                        }

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
                        if (usuarioDesbloquear == null) {
                            throw new IOException("Cliente desconectado");
                        }

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

                    case "11":
                        salida.println("De que usuario quieres listar los archivos .txt?");
                        String usuarioArchivos = entrada.readLine();
                        if (usuarioArchivos == null) {
                            throw new IOException("Cliente desconectado");
                        }
                        solicitarListadoArchivos(usuario, usuarioArchivos, salida);
                        break;

                    case "12":
                        salida.println("De que usuario quieres descargar el archivo?");
                        String usuarioOrigen = entrada.readLine();
                        salida.println("Escribe el nombre del archivo .txt que deseas descargar:");
                        String nombreArchivo = entrada.readLine();
                        if (usuarioOrigen == null || nombreArchivo == null) {
                            throw new IOException("Cliente desconectado");
                        }
                        solicitarArchivo(usuario, usuarioOrigen, nombreArchivo, salida);
                        break;

                    case "13":
                        salida.println("GESTIONAR_ARCHIVOS_LOCALES");
                        manejarGestionArchivos(usuario, entrada, salida);
                        break;

                    default:
                        salida.println("Opcion no valida. Selecciona un numero dentro del intervalo de opciones.");
                }
            }
        }

        private void manejarGestionArchivos(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("SUBMENU_ARCHIVOS");
            salida.println("FIN_SUBMENU");
        }

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

        private void guardarUsuario(String usuario, String contrasena) {
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

        public BufferedReader getEntrada() {
            return entrada;
        }

        public PrintWriter getSalida() {
            return salida;
        }
    }

    private static String extraerRemitente(String mensaje) {
        int inicio = mensaje.indexOf("[De ");
        if (inicio == -1) {
            return "Desconocido";
        }
        inicio += 4;
        int fin = mensaje.indexOf("]", inicio);
        if (fin == -1) {
            return "Desconocido";
        }
        return mensaje.substring(inicio, fin).trim();
    }

    private static Map<String, List<String>> obtenerMensajesPorRemitente(String usuario) {
        Map<String, List<String>> mensajesPorRemitente = new LinkedHashMap<>();
        List<String> mensajes = leerInbox(usuario);
        
        for (String mensaje : mensajes) {
            String remitente = extraerRemitente(mensaje);
            mensajesPorRemitente.computeIfAbsent(remitente, k -> new ArrayList<>()).add(mensaje);
        }
        
        return mensajesPorRemitente;
    }

    private static void verBandejaConPaginacion(String usuario, BufferedReader entrada, PrintWriter salida) throws IOException {
        Map<String, List<String>> mensajesPorRemitente = obtenerMensajesPorRemitente(usuario);
        
        if (mensajesPorRemitente.isEmpty()) {
            salida.println("No tienes mensajes.");
            return;
        }
        
        salida.println("\n=== TIENES MENSAJES DE ===");
        List<String> remitentes = new ArrayList<>(mensajesPorRemitente.keySet());
        for (int i = 0; i < remitentes.size(); i++) {
            String remitente = remitentes.get(i);
            int cantidad = mensajesPorRemitente.get(remitente).size();
            salida.println(i + ") " + remitente + " (" + cantidad + " mensaje" + (cantidad > 1 ? "s" : "") + ")");
        }
        salida.println(remitentes.size() + ") Volver al menu principal");
        salida.println("Selecciona el numero del usuario para ver sus mensajes:");
        
        String seleccion = entrada.readLine();
        if (seleccion == null) {
            throw new IOException("Cliente desconectado");
        }
        
        try {
            int idx = Integer.parseInt(seleccion);
            
            if (idx == remitentes.size()) {
                return;
            }
            
            if (idx < 0 || idx >= remitentes.size()) {
                salida.println("Seleccion invalida.");
                return;
            }
            
            String remitenteSeleccionado = remitentes.get(idx);
            List<String> mensajesDelRemitente = mensajesPorRemitente.get(remitenteSeleccionado);
            
            mostrarMensajesPaginados(usuario, remitenteSeleccionado, mensajesDelRemitente, entrada, salida);
            
        } catch (NumberFormatException e) {
            salida.println("Entrada invalida.");
        }
    }

    private static void mostrarMensajesPaginados(String usuario, String remitente, List<String> mensajes, 
                                                BufferedReader entrada, PrintWriter salida) throws IOException {
        final int MENSAJES_POR_PAGINA = 5;
        int totalPaginas = (int) Math.ceil((double) mensajes.size() / MENSAJES_POR_PAGINA);
        int paginaActual = 0;
        
        while (true) {
            int inicio = paginaActual * MENSAJES_POR_PAGINA;
            int fin = Math.min(inicio + MENSAJES_POR_PAGINA, mensajes.size());
            
            salida.println("\n=== MENSAJES DE " + remitente.toUpperCase() + " ===");
            salida.println("Pagina " + (paginaActual + 1) + " de " + totalPaginas);
            salida.println("Mostrando mensajes " + (inicio + 1) + "-" + fin + " de " + mensajes.size());
            salida.println("----------------------------------------");
            
            for (int i = inicio; i < fin; i++) {
                salida.println("[" + i + "] " + mensajes.get(i));
            }
            
            salida.println("\n--- OPCIONES ---");
            if (paginaActual > 0) {
                salida.println("A) Pagina anterior");
            }
            if (paginaActual < totalPaginas - 1) {
                salida.println("S) Pagina siguiente");
            }
            salida.println("B) Borrar un mensaje de esta conversacion");
            salida.println("V) Volver a la lista de remitentes");
            salida.println("Elige opcion:");
            
            String opcion = entrada.readLine();
            if (opcion == null) {
                throw new IOException("Cliente desconectado");
            }
            
            opcion = opcion.trim().toLowerCase();
            
            switch (opcion) {
                case "a":
                case "anterior":
                    if (paginaActual > 0) {
                        paginaActual--;
                    } else {
                        salida.println("Ya estas en la primera pagina.");
                    }
                    break;
                    
                case "s":
                case "siguiente":
                    if (paginaActual < totalPaginas - 1) {
                        paginaActual++;
                    } else {
                        salida.println("Ya estas en la ultima pagina.");
                    }
                    break;
                    
                case "b":
                case "borrar":
                    salida.println("Escribe el numero del mensaje a borrar [" + inicio + "-" + (fin - 1) + "]:");
                    String numStr = entrada.readLine();
                    if (numStr == null) {
                        throw new IOException("Cliente desconectado");
                    }
                    
                    try {
                        int numMensaje = Integer.parseInt(numStr.trim());
                        if (numMensaje < inicio || numMensaje >= fin) {
                            salida.println("Numero fuera del rango actual.");
                        } else {
                            String mensajeABorrar = mensajes.get(numMensaje);
                            if (borrarMensajeEspecifico(usuario, mensajeABorrar)) {
                                salida.println("Mensaje borrado con exito.");
                                mensajes.remove(numMensaje);
                                
                                totalPaginas = (int) Math.ceil((double) mensajes.size() / MENSAJES_POR_PAGINA);
                                if (paginaActual >= totalPaginas && totalPaginas > 0) {
                                    paginaActual = totalPaginas - 1;
                                }
                                
                                if (mensajes.isEmpty()) {
                                    salida.println("No quedan mas mensajes de este usuario.");
                                    return;
                                }
                            } else {
                                salida.println("Error al borrar el mensaje.");
                            }
                        }
                    } catch (NumberFormatException e) {
                        salida.println("Entrada invalida.");
                    }
                    break;
                    
                case "v":
                case "volver":
                    return;
                    
                default:
                    salida.println("Opcion no valida.");
            }
        }
    }

    private static boolean borrarMensajeEspecifico(String usuario, String mensajeABorrar) {
        File f = archivoInbox(usuario);
        if (!f.exists()) {
            return false;
        }

        try {
            List<String> mensajes = new ArrayList<>();
            boolean eliminado = false;
            
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (!linea.equals(mensajeABorrar)) {
                        mensajes.add(linea);
                    } else {
                        eliminado = true;
                    }
                }
            }

            if (eliminado) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    for (String msg : mensajes) {
                        pw.println(msg);
                    }
                }
            }

            return eliminado;
        } catch (IOException e) {
            System.out.println("Error al borrar mensaje de " + usuario + ": " + e.getMessage());
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
        } catch (IOException ignored) {
        }
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

    private static List<String> obtenerMensajesEnviados(String remitente) {
        List<String> enviados = new ArrayList<>();
        File[] archivos = MENSAJES_DIR.listFiles();
        if (archivos == null) {
            return enviados;
        }

        for (File archivo : archivos) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.contains("[De " + remitente + "]")) {
                        enviados.add(archivo.getName().replace(".txt", "") + " <- " + linea);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return enviados;
    }

    private static boolean borrarMensajeEnviado(String remitente, int indice) {
        List<String> enviados = obtenerMensajesEnviados(remitente);
        if (indice < 0 || indice >= enviados.size()) {
            return false;
        }

        String seleccionado = enviados.get(indice);
        String destinatario = seleccionado.split(" <- ")[0];
        String contenido = seleccionado.split(" <- ")[1];

        File inboxFile = archivoInbox(destinatario);
        if (!inboxFile.exists()) {
            return false;
        }

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
        } catch (IOException ignored) {
        }
        return usuarios;
    }

    private static boolean eliminarUsuario(String usuario) {
        if (!existeUsuario(usuario)) {
            return false;
        }

        try {
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

            try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_USUARIOS))) {
                for (String linea : usuariosRestantes) {
                    pw.println(linea);
                }
            }

            File archivoInboxUsuario = archivoInbox(usuario);
            if (archivoInboxUsuario.exists()) {
                archivoInboxUsuario.delete();
            }

            eliminarMensajesEnviadosPorUsuario(usuario);
            eliminarBloqueosDeUsuario(usuario);

            return true;

        } catch (IOException e) {
            System.out.println("Error al eliminar usuario " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    private static void eliminarMensajesEnviadosPorUsuario(String remitente) {
        File[] archivos = MENSAJES_DIR.listFiles();
        if (archivos == null) {
            return;
        }

        for (File archivo : archivos) {
            if (archivo.isFile() && archivo.getName().endsWith(".txt")) {
                try {
                    List<String> mensajesFiltrados = new ArrayList<>();
                    boolean seEliminoAlgo = false;

                    try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                        String linea;
                        while ((linea = br.readLine()) != null) {
                            if (!linea.contains("[De " + remitente + "]")) {
                                mensajesFiltrados.add(linea);
                            } else {
                                seEliminoAlgo = true;
                            }
                        }
                    }

                    if (seEliminoAlgo) {
                        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                            for (String mensaje : mensajesFiltrados) {
                                pw.println(mensaje);
                            }
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Error eliminando mensajes de " + remitente + " en "
                            + archivo.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static File archivoBloqueos(String usuario) {
        return new File(BLOQUEOS_DIR, usuario + ".txt");
    }

    private static void bloquearUsuario(String usuario, String bloqueado) {
        try {
            File f = archivoBloqueos(usuario);
            Set<String> set = new LinkedHashSet<>();
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        if (!linea.trim().isEmpty()) {
                            set.add(linea.trim());
                        }
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

    private static boolean desbloquearUsuario(String usuario, String bloqueado) {
        File f = archivoBloqueos(usuario);
        if (!f.exists()) {
            return false;
        }

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

    private static List<String> obtenerUsuariosBloqueados(String usuario) {
        List<String> bloqueados = new ArrayList<>();
        File f = archivoBloqueos(usuario);
        if (!f.exists()) {
            return bloqueados;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String t = linea.trim();
                if (!t.isEmpty()) {
                    bloqueados.add(t);
                }
            }
        } catch (IOException ignored) {
        }
        return bloqueados;
    }

    private static boolean estaBloqueado(String usuario, String posibleBloqueado) {
        List<String> bloqueados = obtenerUsuariosBloqueados(usuario);
        return bloqueados.contains(posibleBloqueado);
    }

    private static void eliminarBloqueosDeUsuario(String usuario) {
        File f = archivoBloqueos(usuario);
        if (f.exists()) {
            f.delete();
        }

        File[] archivos = BLOQUEOS_DIR.listFiles();
        if (archivos == null) {
            return;
        }

        for (File archivo : archivos) {
            if (!archivo.isFile()) {
                continue;
            }
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
                        for (String u : lista) {
                            pw.println(u);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error actualizando bloqueos en " + archivo.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void solicitarListadoArchivos(String solicitante, String objetivo, PrintWriter salidaSolicitante) {
        ManejadorCliente manejadorObjetivo;
        synchronized (usuariosConectados) {
            manejadorObjetivo = usuariosConectados.get(objetivo);
        }

        if (manejadorObjetivo == null) {
            salidaSolicitante.println("El usuario " + objetivo + " no esta conectado.");
            return;
        }

        try {
            PrintWriter salidaObjetivo = manejadorObjetivo.getSalida();
            BufferedReader entradaObjetivo = manejadorObjetivo.getEntrada();

            salidaObjetivo.println("LISTAR_ARCHIVOS");
            salidaObjetivo.println(solicitante);

            salidaSolicitante.println("=== Archivos .txt de " + objetivo + " ===");
            String linea;
            while (!(linea = entradaObjetivo.readLine()).equals("FIN_LISTA")) {
                salidaSolicitante.println(linea);
            }
            salidaSolicitante.println("=== Fin del listado ===");
        } catch (IOException e) {
            salidaSolicitante.println("Error solicitando archivos a " + objetivo);
        }
    }

    private static void solicitarArchivo(String solicitante, String objetivo, String nombreArchivo, PrintWriter salidaSolicitante) {
        ManejadorCliente manejadorObjetivo;
        synchronized (usuariosConectados) {
            manejadorObjetivo = usuariosConectados.get(objetivo);
        }

        if (manejadorObjetivo == null) {
            salidaSolicitante.println("El usuario " + objetivo + " no esta conectado.");
            return;
        }

        try {
            PrintWriter salidaObjetivo = manejadorObjetivo.getSalida();
            BufferedReader entradaObjetivo = manejadorObjetivo.getEntrada();

            salidaObjetivo.println("ENVIAR_ARCHIVO");
            salidaObjetivo.println(solicitante);
            salidaObjetivo.println(nombreArchivo);

            String linea;
            salidaSolicitante.println("=== Contenido del archivo " + nombreArchivo + " de " + objetivo + " ===");
            while (!(linea = entradaObjetivo.readLine()).equals("FIN_ARCHIVO")) {
                salidaSolicitante.println(linea);
            }
            salidaSolicitante.println("=== Fin del archivo ===");
        } catch (IOException e) {
            salidaSolicitante.println("Error transfiriendo archivo desde " + objetivo);
        }
    }

    private static void manejarComandoServidor(String comando) {
        switch (comando.toLowerCase().trim()) {
            case "/help":
                System.out.println("=== COMANDOS DEL SERVIDOR ===");
                System.out.println("/help - Mostrar esta ayuda");
                System.out.println("/users - Mostrar todos los usuarios registrados");
                System.out.println("/connected - Mostrar usuarios conectados");
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

            case "/connected":
                synchronized (usuariosConectados) {
                    if (usuariosConectados.isEmpty()) {
                        System.out.println("No hay usuarios conectados.");
                    } else {
                        System.out.println("=== USUARIOS CONECTADOS ===");
                        int i = 1;
                        for (String user : usuariosConectados.keySet()) {
                            System.out.println(i++ + ". " + user);
                        }
                        System.out.println("Total: " + usuariosConectados.size() + " conectados");
                    }
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