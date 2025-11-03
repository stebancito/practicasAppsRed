import java.net.*;
import java.io.*;
import java.util.Random;

public class retUDPser {

    public static void main(String[] args) {
        int puerto = 9876;
        int tamPaquete = 512;
        int timeout = 5000; // ms
        double probPerdida = 0.2; // 20% pérdida simulada
        Random random = new Random();

        try {
            DatagramSocket servidorSocket = new DatagramSocket(puerto);
            servidorSocket.setReuseAddress(true);
            System.out.println("Servidor UDP iniciado en el puerto " + puerto);

            while (true) {

                byte[] bufferRecibir = new byte[1024];
                System.out.println("\nEsperando nuevo cliente...");
                DatagramPacket paqueteInicial = new DatagramPacket(bufferRecibir, bufferRecibir.length);
                servidorSocket.receive(paqueteInicial);

                String mensaje = new String(paqueteInicial.getData(), 0, paqueteInicial.getLength()).trim();
                if (!mensaje.equals("START")) {
                    System.out.println("⚠️ Ignorado datagrama no válido: " + mensaje);
                    continue;
                }

                InetAddress direccionCliente = paqueteInicial.getAddress();
                int puertoCliente = paqueteInicial.getPort();
                System.out.println("✅ Cliente conectado: " + direccionCliente + ":" + puertoCliente);

                servidorSocket.setSoTimeout(timeout);

                File archivo = new File("texto.txt");
                if (!archivo.exists()) {
                    System.out.println("⚠️ Archivo texto.txt no encontrado.");
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(archivo)) {
                    int numSecuencia = 0;
                    byte[] bufferDatos = new byte[tamPaquete];
                    int bytesLeidos;

                    while ((bytesLeidos = fis.read(bufferDatos)) != -1) {

                        // Empaquetar número de secuencia y bandera "no último"
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeInt(numSecuencia);
                        dos.writeBoolean(false);
                        dos.write(bufferDatos, 0, bytesLeidos);
                        byte[] datosFinales = baos.toByteArray();

                        DatagramPacket paquete = new DatagramPacket(
                                datosFinales, datosFinales.length, direccionCliente, puertoCliente);

                        boolean ackRecibido = false;
                        int reintentos = 0;

                        while (!ackRecibido && reintentos < 10) {
                            // Simular pérdida
                            if (random.nextDouble() > probPerdida) {
                                servidorSocket.send(paquete);
                                System.out.println("📤 Enviado paquete #" + numSecuencia + " (" + bytesLeidos + " bytes)");
                            } else {
                                System.out.println("⚠️ Simulación: paquete #" + numSecuencia + " perdido");
                            }

                            try {
                                byte[] bufferACK = new byte[100];
                                DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length);
                                servidorSocket.receive(paqueteACK);

                                String ack = new String(paqueteACK.getData(), 0, paqueteACK.getLength()).trim();
                                System.out.println("📩 Recibido " + ack);

                                if (ack.equals("ACK" + numSecuencia)) {
                                    ackRecibido = true;
                                } else {
                                    System.out.println("⚠️ ACK inesperado: " + ack);
                                }

                            } catch (SocketTimeoutException e) {
                                reintentos++;
                                System.out.println("⏰ Timeout: reenviando paquete #" + numSecuencia +
                                        " (intento " + reintentos + ")");
                            }
                        }

                        if (!ackRecibido) {
                            System.out.println("❌ No se recibió ACK tras múltiples intentos. Abortando.");
                            break;
                        }

                        numSecuencia++;
                    }

                    // Enviar último paquete con marcador de fin
                    ByteArrayOutputStream baosFin = new ByteArrayOutputStream();
                    DataOutputStream dosFin = new DataOutputStream(baosFin);
                    dosFin.writeInt(-1); // marcador de fin
                    dosFin.writeBoolean(true);
                    byte[] finData = baosFin.toByteArray();

                    DatagramPacket finPacket = new DatagramPacket(
                            finData, finData.length, direccionCliente, puertoCliente);
                    servidorSocket.send(finPacket);
                    System.out.println("📤 Enviado paquete final (EOF).");

                    // Esperar ACKFIN (1-2 segundos)
                    try {
                        servidorSocket.setSoTimeout(2000);
                        byte[] bufferFin = new byte[100];
                        DatagramPacket paqueteFin = new DatagramPacket(bufferFin, bufferFin.length);
                        servidorSocket.receive(paqueteFin);

                        String ackFin = new String(paqueteFin.getData(), 0, paqueteFin.getLength()).trim();
                        if (ackFin.equals("ACKFIN")) {
                            System.out.println("✅ Confirmación final recibida (ACKFIN). Fin de transmisión.");
                        } else {
                            System.out.println("⚠️ Ignorado datagrama no válido tras EOF: " + ackFin);
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("⌛ No se recibió ACKFIN, continuando cierre...");
                    }

                } catch (IOException e) {
                    System.out.println("❌ Error al enviar archivo: " + e.getMessage());
                }

                servidorSocket.setSoTimeout(0);
                System.out.println("🔁 Sesión finalizada. Esperando nuevo cliente...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
