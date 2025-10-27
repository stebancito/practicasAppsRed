import java.net.*;
import java.io.*;

public class cliente {

    public static void main(String[] args) {
        String servidorIP = "127.0.0.1"; // Cambia por la IP del servidor si es otra
        int puertoServidor = 9876;
        int tamPaquete = 1024;

        try {
            DatagramSocket clienteSocket = new DatagramSocket();

            InetAddress direccionServidor = InetAddress.getByName(servidorIP);

            // Enviar primer mensaje al servidor para "registrarse"
            byte[] mensaje = "Hola servidor".getBytes();
            DatagramPacket paqueteInicial = new DatagramPacket(mensaje, mensaje.length, direccionServidor, puertoServidor);
            clienteSocket.send(paqueteInicial);

            // Preparar buffer para recibir datos
            byte[] buffer = new byte[tamPaquete];
            FileOutputStream fos = new FileOutputStream("recibido.mp3");

            System.out.println("Esperando recibir datos...");

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                clienteSocket.receive(paqueteRecibido);

                // Escribimos solo los bytes válidos recibidos
                fos.write(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());

                // Aquí podrías agregar condición de fin de archivo
                // Por ejemplo, si el paquete recibido es menor al tamaño del paquete
                if (paqueteRecibido.getLength() < tamPaquete) {
                    break;
                }
            }

            System.out.println("Archivo recibido correctamente.");
            fos.close();
            clienteSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
