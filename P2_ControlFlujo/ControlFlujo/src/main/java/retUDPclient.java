import java.net.*;
import java.io.*;
import java.util.Random;

public class retUDPclient {

    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 9876;
        double probPerdidaAck = 0.2; // 20% pérdida de ACK simulada
        Random random = new Random();

        try {
            DatagramSocket clienteSocket = new DatagramSocket();
            InetAddress direccionServidor = InetAddress.getByName(host);

            // Enviar solicitud de conexión
            String saludo = "START";
            byte[] datosSaludo = saludo.getBytes();
            DatagramPacket paqueteSaludo = new DatagramPacket(datosSaludo, datosSaludo.length, direccionServidor, puerto);
            clienteSocket.send(paqueteSaludo);
            System.out.println("📨 Solicitud de conexión enviada.");

            // Archivo donde se guardará el texto recibido
            FileOutputStream fos = new FileOutputStream("recibido.txt");

            byte[] bufferRecibir = new byte[1024 + 10];
            boolean fin = false;
            int esperado = 0;

            while (!fin) {
                DatagramPacket paquete = new DatagramPacket(bufferRecibir, bufferRecibir.length);
                clienteSocket.receive(paquete);

                ByteArrayInputStream bais = new ByteArrayInputStream(paquete.getData(), 0, paquete.getLength());
                DataInputStream dis = new DataInputStream(bais);

                int numSecuencia = dis.readInt();
                boolean esUltimo = dis.readBoolean();

                if (esUltimo && numSecuencia == -1) {
                    System.out.println("📩 Paquete de fin recibido.");
                    fin = true;
                    String ackFin = "ACKFIN";
                    byte[] ackFinData = ackFin.getBytes();
                    DatagramPacket paqueteFin = new DatagramPacket(
                            ackFinData, ackFinData.length, direccionServidor, puerto);
                    clienteSocket.send(paqueteFin);
                    System.out.println("✅ Enviado ACKFIN.");
                    break;
                }

                byte[] datos = paquete.getData();
                int headerSize = 5; // int(4 bytes) + boolean(1 byte)
                int dataLength = paquete.getLength() - headerSize;
                byte[] contenido = new byte[dataLength];
                System.arraycopy(datos, headerSize, contenido, 0, dataLength);

                System.out.println("📥 Recibido paquete #" + numSecuencia);

                // Solo aceptar en orden esperado
                if (numSecuencia == esperado) {
                    fos.write(contenido);
                    esperado++;
                }

                // Simular pérdida de ACK
                if (random.nextDouble() > probPerdidaAck) {
                    String ack = "ACK" + numSecuencia;
                    byte[] datosAck = ack.getBytes();
                    DatagramPacket paqueteAck = new DatagramPacket(
                            datosAck, datosAck.length, direccionServidor, puerto);
                    clienteSocket.send(paqueteAck);
                    System.out.println("📤 Enviado " + ack);
                } else {
                    System.out.println("⚠️ Simulación: ACK" + numSecuencia + " perdido");
                }
            }

            fos.close();
            clienteSocket.close();
            System.out.println("📁 Archivo recibido correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
