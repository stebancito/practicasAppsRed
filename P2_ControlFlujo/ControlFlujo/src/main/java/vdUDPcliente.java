import java.io.*;
import java.net.*;
import java.util.Random;

public class vdUDPcliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 9876;
    private static final double PROB_PERDIDA_ACK = 0.2;

    public static void main(String[] args) {
        Random random = new Random();

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress servidor = InetAddress.getByName(HOST);

            // enviar START
            byte[] start = "INICIO".getBytes();
            socket.send(new DatagramPacket(start, start.length, servidor, PUERTO));
            System.out.println("INICIO enviado");

            FileOutputStream fos = new FileOutputStream("recibido.txt");
            int esperado = 0;
            boolean fin = false;

            byte[] recvBuf = new byte[1024 + 16];
            boolean finRecibido = false;
            long ultimoPaqueteTiempo = 0;
            final int TIMEOUT_FIN_MS = 3000; // espera 3 segundos extra por reenvíos del último paquete

            while (true) {
                DatagramPacket pkt = new DatagramPacket(recvBuf, recvBuf.length);
                socket.setSoTimeout(finRecibido ? 1000 : 0); // si ya llegó el último, limita espera
                try {
                    socket.receive(pkt);
                } catch (SocketTimeoutException e) {
                    if (finRecibido && (System.currentTimeMillis() - ultimoPaqueteTiempo > TIMEOUT_FIN_MS)) {
                        System.out.println("\u001B[32m Fin confirmado: no se recibieron más paquetes.\u001B[0m");
                        break;
                    } else {
                        continue; // seguir esperando más paquetes
                    }
                }

                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pkt.getData(), 0, pkt.getLength()));
                int seq = dis.readInt();
                boolean isLast = dis.readBoolean();

                int headerSize = 4 + 1;
                int payloadLen = pkt.getLength() - headerSize;
                byte[] payload = new byte[payloadLen];
                dis.readFully(payload);

                if (seq == esperado) {
                    // paquete correcto y en orden
                    fos.write(payload);
                    System.out.println("\u001B[32m Paquete #" + seq + " recibido correctamente (" + payloadLen + " bytes)\u001B[0m");
                    esperado++;

                    if (isLast) {
                        finRecibido = true;
                        ultimoPaqueteTiempo = System.currentTimeMillis();
                        System.out.println("\u001B[32m Último paquete recibido (#" + seq + ")\u001B[0m");
                    }

                    // enviar ACK del paquete recibido correctamente
                    enviarAck(socket, servidor, seq, random);

                } else if (seq < esperado) {
                    //  Paquete ya recibido anteriormente: reenviar último ACK
                    System.out.println("\u001B[34m Paquete duplicado #" + seq + " — reenviando ACK" + (esperado - 1) + "\u001B[0m");
                    enviarAck(socket, servidor, esperado - 1, random);

                } else {
                    // paquete fuera de orden futuro
                    System.out.println("\u001B[33m Paquete fuera de orden (esperado " + esperado + ", llegó " + seq + ") — ignorado \u001B[0m");

                    // si ya vimos el último paquete, reenviar último ACK por seguridad
                    if (finRecibido) {
                        System.out.println("\u001B[34m Reenviado ACK" + (esperado - 1) + " (confirmando fin) \u001B[0m");
                        enviarAck(socket, servidor, esperado - 1, random);
                    }
                }

            }

            fos.close();
            System.out.println("Archivo recibido y guardado como recibido.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void enviarAck(DatagramSocket socket, InetAddress servidor, int ackNum, Random random) throws IOException {
        String ackMsg = "ACK" + ackNum;
        if (random.nextDouble() < PROB_PERDIDA_ACK) {
            System.out.println("\u001B[31m Simulación: " + ackMsg + " perdido \u001B[0m");
            return;
        }
        byte[] ackBytes = ackMsg.getBytes();
        DatagramPacket ackPkt = new DatagramPacket(ackBytes, ackBytes.length, servidor, PUERTO);
        socket.send(ackPkt);
        System.out.println("\u001B[34m Enviado " + ackMsg + "\u001B[0m");
    }

}
