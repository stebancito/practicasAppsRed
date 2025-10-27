import java.net.*;
import java.io.*;

public class servidor {

    public static void main(String[] args) {
        int puerto = 9876; // Puerto del servidor
        int tamPaquete = 1024; // Tamaño de cada paquete en bytes

        try {
            DatagramSocket servidorSocket = new DatagramSocket(puerto);
            servidorSocket.setReuseAddress(true);
            System.out.println("Servidor UDP iniciado en el puerto e IP " + puerto + servidorSocket.getInetAddress());

            // Cargar el archivo MP3
            File archivo = new File("C:\\Users\\esteb\\Documents\\6to_sem\\AplicacionesRed\\practicasAppRedes\\practicasAppsRed\\P2_ControlFlujo\\ControlFlujo\\RH.mp3");
            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[tamPaquete];
            int bytesLeidos;

            InetAddress direccionCliente = null;
            int puertoCliente = 0;

            System.out.println("Esperando cliente...");

            // Espera el primer paquete del cliente para obtener su dirección y puerto
            byte[] recibirDatos = new byte[1024];
            DatagramPacket paqueteRecibido = new DatagramPacket(recibirDatos, recibirDatos.length);
            servidorSocket.receive(paqueteRecibido);

            direccionCliente = paqueteRecibido.getAddress();
            puertoCliente = paqueteRecibido.getPort();

            System.out.println("Cliente conectado: " + direccionCliente + ":" + puertoCliente);

            // Enviar archivo en paquetes
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                DatagramPacket paquete = new DatagramPacket(buffer, bytesLeidos, direccionCliente, puertoCliente);
                servidorSocket.send(paquete);
                // Puedes agregar aquí un Thread.sleep(10) para simular retardo de red
            }

            System.out.println("Archivo enviado completamente.");
            fis.close();
            servidorSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
