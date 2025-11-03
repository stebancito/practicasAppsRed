import java.net.*;
import java.io.*;

public class clientUDPack {

    public static void main(String[] args) {
        String servidorIP = "127.0.0.1";
        int puertoServidor = 9876;
        int tamPaquete = 512;

        try {
            DatagramSocket clienteSocket = new DatagramSocket();
            InetAddress direccionServidor = InetAddress.getByName(servidorIP);

            // Enviar mensaje inicial para conectar con el servidor
            byte[] mensaje = "Hola servidor".getBytes();
            DatagramPacket paqueteInicial = new DatagramPacket(mensaje, mensaje.length, direccionServidor, puertoServidor);
            clienteSocket.send(paqueteInicial);

            System.out.println("Esperando recibir datos...");

            FileOutputStream fos = new FileOutputStream("recibido.txt");

            while (true) {
                byte[] buffer = new byte[tamPaquete + 4]; // +4 bytes para el número de secuencia
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                clienteSocket.receive(paqueteRecibido);

                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(paqueteRecibido.getData()));
                int numSecuencia = dis.readInt();

                // Leer datos del paquete (sin el encabezado)
                byte[] datos = new byte[paqueteRecibido.getLength() - 4];
                dis.readFully(datos);
                fos.write(datos);

                System.out.println("Recibido paquete #" + numSecuencia + " (" + datos.length + " bytes)");

                // Enviar ACK
                String ack = "ACK" + numSecuencia;
                byte[] ackBytes = ack.getBytes();
                DatagramPacket paqueteACK = new DatagramPacket(ackBytes, ackBytes.length, direccionServidor, puertoServidor);
                clienteSocket.send(paqueteACK);

                // Fin de archivo (si paquete es más pequeño que el tamaño del paquete)
                if (datos.length < tamPaquete) {
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
