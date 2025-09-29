Sistema Servidor-Cliente Multifuncional
Proyecto de servidor-cliente en Java con sistema completo de gestión de usuarios, mensajería, juego interactivo y transferencia de archivos.
🚀 Características Principales
👤 Sistema de Usuarios

Registro e inicio de sesión seguro
Gestión de múltiples sesiones simultáneas
Sistema de bloqueo/desbloqueo de usuarios
Lista de usuarios registrados y conectados

💬 Sistema de Mensajería

Envío de mensajes entre usuarios
Bandeja de entrada personal
Historial de mensajes enviados
Borrado de mensajes (recibidos y enviados)
Respeto a bloqueos entre usuarios
Mensajes del servidor a usuarios

🎮 Juego Interactivo

Adivina el número del 1 al 10
3 intentos por partida
Pistas inteligentes (mayor/menor)
Múltiples partidas consecutivas

📁 Gestión y Transferencia de Archivos

Listar archivos .txt de otros usuarios conectados
Descargar archivos de otros clientes
Gestión local de archivos:

Crear nuevos archivos .txt
Editar archivos existentes (sobrescribir o agregar)
Eliminar archivos
Ver contenido con numeración de líneas



📋 Cómo Ejecutar el Proyecto
1. Iniciar el Servidor
bashjava com.mycompany.iniciarsesion.Servidor
2. Iniciar Cliente(s)
bashjava com.mycompany.iniciarsesion.Cliente
Puedes ejecutar múltiples clientes para probar la transferencia de archivos
3. Opciones del Cliente
Al conectar:

1 - Iniciar sesión
2 - Registrarte
exit - Desconectar

Menú Principal (una vez dentro):

Ver bandeja de entrada
Jugar - Adivina el número
Salir (cerrar sesión)
Enviar mensaje a otro usuario
Ver todos los usuarios registrados
Borrar un mensaje de la bandeja
Borrar un mensaje enviado
Bloquear un usuario
Desbloquear un usuario
Ver usuarios bloqueados
Listar archivos de otro usuario
Descargar archivo
Gestionar mis archivos locales

🎮 Comandos del Servidor (Consola)
Mientras el servidor está corriendo, puedes usar estos comandos:
ComandoDescripción/helpMostrar lista de comandos/usersMostrar todos los usuarios registrados/connectedVer usuarios actualmente conectados/messages <usuario>Ver mensajes de un usuario específico/enviar <usuario> <mensaje>Enviar mensaje a un usuario/eliminar <usuario>Eliminar usuario y toda su información/clearLimpiar la consola/stopDetener el servidor
📂 Estructura de Archivos Generados
proyecto/
├── usuarios.txt          # Base de datos de usuarios (usuario,contraseña)
├── mensajes/            # Carpeta de mensajes
│   ├── usuario1.txt     # Bandeja de entrada de usuario1
│   └── usuario2.txt     # Bandeja de entrada de usuario2
├── bloqueos/            # Carpeta de bloqueos
│   ├── usuario1.txt     # Lista de usuarios bloqueados por usuario1
│   └── usuario2.txt     # Lista de usuarios bloqueados por usuario2
└── *.txt                # Archivos personales de cada cliente
🔐 Sistema de Bloqueos

Un usuario puede bloquear a otro para evitar recibir mensajes
Los bloqueos son bidireccionales para envío de mensajes
Se pueden desbloquear usuarios en cualquier momento
Ver lista de usuarios bloqueados

📤 Transferencia de Archivos
Requisitos:

Ambos usuarios deben estar conectados
Solo se transfieren archivos .txt
Los archivos se buscan en el directorio de ejecución de cada cliente

Proceso:

Usuario A selecciona opción 11 para listar archivos de Usuario B
Usuario B recibe notificación y automáticamente envía su lista
Usuario A ve los archivos disponibles
Usuario A selecciona opción 12 para descargar un archivo específico
El contenido se muestra en pantalla

🛠️ Gestión Local de Archivos (Opción 13)
Submenú completo para administrar archivos locales:

Listar - Ver todos los archivos .txt en tu directorio
Crear - Crear nuevo archivo (escribir línea por línea, finalizar con "FIN")
Editar - Modificar archivos existentes (sobrescribir o agregar al final)
Eliminar - Borrar archivos con confirmación
Ver - Mostrar contenido con numeración de líneas
Volver - Regresar al menú principal

🎲 Sobre el Juego

Número aleatorio entre 1 y 10
3 intentos por partida
Pistas después de cada intento fallido
Contador de intentos restantes
Opción de jugar múltiples veces

💻 Tecnologías Utilizadas

Java SE
Sockets TCP/IP
Programación multihilo
Gestión de archivos I/O
Sincronización de hilos

📝 Notas Técnicas

Puerto del servidor: 5000
Codificación: UTF-8
Arquitectura cliente-servidor con hilos
Gestión automática de usuarios conectados/desconectados
Sistema de sincronización para evitar conflictos de concurrencia
