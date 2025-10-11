import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteComprador {
    public static void main(String[] args) {
            final String HOST = "10.225.89.93";
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
                int opcion = sc.nextInt();
                sc.nextLine();

                if (opcion == 1){
                    //out.writeUTF("BUSCAR");
//                    out.write("BUSCAR".getBytes());
//                    out.flush();
//                    System.out.println("Acabas de acceder al buscador\n");
//                    System.out.println("Escrribe el nommbre de producto: ");
                    //String producto = sc.nextLine();
                    //out.writeUTF(producto);
                    System.out.print("Nombre del producto: ");
                    String producto = sc.nextLine();
                    String comando = "BUSCAR " + producto;
                    out.write(comando.getBytes());
                    out.flush();

                    byte[] buf = new byte[1024];
                    int n = in.read(buf);
                    String respuesta = new String(buf, 0, n);
                    System.out.println("Servidor: " + respuesta);

                } else if (opcion == 2) {
                    out.writeUTF("LISTAR");
                    System.out.println("Escribe el tipo de producto");
                } else if (opcion == 3){
                    out.writeUTF("AGREGAR");
                } else if (opcion == 4) {
                    out.writeUTF("EDITAR");
                } else if (opcion == 5) {
                    out.writeUTF("FINALIZAR");
                }else {
                    System.out.println("Opcion no valida");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
