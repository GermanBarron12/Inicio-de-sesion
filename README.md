Sistema Servidor-Cliente con Juego
Proyecto de servidor-cliente en Java con sistema de login y juego de adivinar numeros.
¿Qué hace el proyecto?

Registro e inicio de sesión de usuarios
Sistema de mensajes entre admin y usuarios
Juego de adivinar números del 1 al 10
Consola de administrador para enviar mensajes

Cómo ejecutar el proyecto:
1. Ejecutar el servidor
java Servidor
2. Ejecutar el cliente
java Cliente
3. En el cliente puedes:

Registrarte (opción 2) o iniciar sesión (opción 1)
Ver tus mensajes (opción 1)
Jugar adivinanza (opción 2)
Salir (opción 3)

Lista de comandos disponibles en el servidor a traves de la consola
Mientras el servidor está corriendo, puedes escribir:
/help - Mostrar esta ayuda
/users - Mostrar todos los usuarios registrados
/messages <usuario> - Ver mensajes de un usuario
/enviar <usuario> <mensaje> - Enviar mensaje a un usuario
/clear - Limpiar consola
/stop - Detener servidor

Archivos que se crean:

usuarios.txt - Lista de usuarios y contraseñas
mensajes/ - Carpeta con los mensajes de cada usuario

Sobre el juego:

Adivinar número del 1 al 10
Tienes 3 intentos
Te da pistas si es mayor o menor
Puedes jugar varias veces seguidas
