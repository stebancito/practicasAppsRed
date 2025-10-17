import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClienteComprador {
    public static void main(String[] args) {
        //final String HOST = "127.0.0.1";
        final String HOST = "127.0.0.1";
        final int PUERTO = 8080;
        DataInputStream in;
        DataOutputStream out;

        try {
            Socket socketCliente = new Socket(HOST,PUERTO);

            in = new DataInputStream(socketCliente.getInputStream());
            out = new DataOutputStream(socketCliente.getOutputStream());

            Scanner sc = new Scanner(System.in);
            while(true){
                System.out.println("1. Buscar");
                System.out.println("2. Listar");
                System.out.println("3. Agregar al carrito");
                System.out.println("4. Editar carrito");
                System.out.println("5. Finalizar compra");
                System.out.println("6. Salir");

                int opcion = sc.nextInt();
                sc.nextLine();

                if (opcion == 1){
                    System.out.print("Nombre del producto: ");
                    String producto = sc.nextLine();

                    String comando = "BUSCAR " + producto;
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf = new byte[1024];
                    int n = in.read(buf);
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    System.out.println("Servidor: " + respuesta);
                } else if (opcion == 2) {
                    System.out.println("Escribe el tipo de producto");
                    String producto = sc.nextLine();
                    String comando = "LISTAR " + producto;
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf = new byte[1024];
                    int n = in.read(buf);
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    System.out.println("Servidor: " + respuesta);
                } else if (opcion == 3){
                    System.out.println("Escribe el id del producto");
                    String id = sc.nextLine();
                    String comando = "AGREGAR " + id;
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf = new byte[1024];
                    int n = in.read(buf);
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    System.out.println("Servidor: " + respuesta);

                } else if (opcion == 4) {
                    System.out.println("Este es tu carrito");

                    String comando = "EDITAR";
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf = new byte[1024];
                    int n = in.read(buf);
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    System.out.println("Servidor: " + respuesta);

                    if (respuesta.contains("Debes tener un carrito")) {
                        System.out.println("No tienes carrito, regresando al menú principal...\n");
                        continue; // Salta esta iteración y vuelve al menú
                    }


                    System.out.println("Escribe el id del producto (o escribe 'salir' para regresar):");
                    String id = sc.nextLine().trim();

                    if (id.equalsIgnoreCase("salir") || id.equals("0")) {
                        out.write("0".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        byte[] buf2 = new byte[1024];
                        int n2 = in.read(buf2);
                        String respuesta2 = new String(buf2, 0, n2, StandardCharsets.UTF_8).trim();
                        
                        continue;
                    }

                    out.write(id.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf2 = new byte[1024];
                    int n2 = in.read(buf2);
                    String respuesta2 = new String(buf2, 0, n2, StandardCharsets.UTF_8).trim();
                    System.out.println("Servidor: " + respuesta2);

                } else if (opcion == 5) {
                    String comando = "COMPRAR";
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    byte[] buf = new byte[4096];
                    int n = in.read(buf);
                    
                    if (n == -1) {
                        System.out.println("El servidor cerró la conexión antes de enviar el ticket.");
                        break;
                    }

                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    System.out.println("\n****************** TICKET DE COMPRA ******************");
                    System.out.println(respuesta);
                    System.out.println("******************************************************");
                    
                    System.out.println("\nCompra finalizada. ¡Gracias por tu compra!");
                    break;
                }else if (opcion == 6) {
                    String comando = "SALIR";
                    out.write(comando.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    System.out.println("Cerrando sesión y devolviendo productos...");
                    socketCliente.close();
                    System.out.println("Conexión cerrada correctamente.");
                    break; 
                }
                else {
                    System.out.println("Opcion no valida");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
