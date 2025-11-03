import java.net.*;
import java.io.*;

public class serUDPack {

    public static void main(String[] args) {
        int puerto = 9876;
        int tamPaquete = 512;

        try {
            DatagramSocket servidorSocket = new DatagramSocket(puerto);
            System.out.println("Servidor UDP esperando cliente...");

            // Esperar al cliente
            byte[] bufferRecibir = new byte[1024];
            DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecibir, bufferRecibir.length);
            servidorSocket.receive(paqueteRecibido);

            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();
            System.out.println("Cliente conectado: " + direccionCliente + ":" + puertoCliente);

            // Cargar archivo de texto
            File archivo = new File("texto.txt");
            FileInputStream fis = new FileInputStream(archivo);

            int numSecuencia = 0;
            byte[] bufferDatos = new byte[tamPaquete];
            int bytesLeidos;

            // Enviar datos por bloques
            while ((bytesLeidos = fis.read(bufferDatos)) != -1) {

                // Crear paquete con número de secuencia + datos
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(numSecuencia); // encabezado con número de secuencia
                dos.write(bufferDatos, 0, bytesLeidos);
                byte[] datosFinales = baos.toByteArray();

                DatagramPacket paquete = new DatagramPacket(datosFinales, datosFinales.length, direccionCliente, puertoCliente);
                servidorSocket.send(paquete);
                System.out.println("Enviado paquete #" + numSecuencia + " (" + bytesLeidos + " bytes)");

                // Esperar ACK del cliente
                byte[] bufferACK = new byte[100];
                DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length);
                servidorSocket.receive(paqueteACK);
                String ack = new String(paqueteACK.getData(), 0, paqueteACK.getLength());

                System.out.println("Recibido " + ack);

                numSecuencia++;
            }

            System.out.println("Archivo enviado completamente.");
            fis.close();
            servidorSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
