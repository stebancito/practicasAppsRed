import java.io.*;
import java.net.*;
import java.util.*;

public class vdUDPser {
    private static final int PUERTO = 9876;
    private static final int TAMANO_PAQUETE = 512;
    private static final int TIMEOUT = 3000; // ms
    private static final double PROB_PERDIDA = 0.2; // pérdida simulada

    public static void main(String[] args) {
        Random random = new Random();
        Scanner sc = new Scanner(System.in);

        System.out.print("Ingrese el tamaño de la ventana: ");
        int VENTANA = sc.nextInt();

        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            socket.setReuseAddress(true);
            System.out.println("Servidor escuchando en el puerto " + PUERTO);

            while (true) { // escucha indefinidamente
                System.out.println("\nEsperando nuevo cliente...");
                DatagramPacket inicioPkt = new DatagramPacket(new byte[1024], 1024);
                socket.receive(inicioPkt);

                String inicio = new String(inicioPkt.getData(), 0, inicioPkt.getLength()).trim();
                if (!"INICIO".equalsIgnoreCase(inicio)) {
                    System.out.println("Paquete no válido, ignorado: " + inicio);
                    continue;
                }

                InetAddress clienteDir = inicioPkt.getAddress();
                int clientePuerto = inicioPkt.getPort();
                System.out.println("\u001B[32m Cliente conectado: \u001B[0m" + clienteDir + ":" + clientePuerto);

                socket.setSoTimeout(TIMEOUT);

                // Cargar archivo o generar datos
                File archivo = new File("texto.txt");
                byte[] allData;
                if (archivo.exists()) {
                    try (FileInputStream fis = new FileInputStream(archivo)) {
                        allData = fis.readAllBytes();
                    }
                } else {
                    allData = new byte[3072];
                    for (int i = 0; i < allData.length; i++) allData[i] = (byte) (65 + (i % 26));
                    System.out.println("texto.txt no encontrado — usando datos de prueba (" + allData.length + " bytes)");
                }

                // Dividiemos el archivo
                List<byte[]> payloads = new ArrayList<>();
                for (int offset = 0; offset < allData.length; offset += TAMANO_PAQUETE) {
                    int end = Math.min(allData.length, offset + TAMANO_PAQUETE);
                    payloads.add(Arrays.copyOfRange(allData, offset, end));
                }
                final int totalPaquetes = payloads.size();
                System.out.println("Total de paquetes a enviar: " + totalPaquetes);


                int base = 0;
                int nextSeq = 0;
                long timerStart = 0;
                boolean timerOn = false;
                Map<Integer, byte[]> buffer = new HashMap<>();
                for (int i = 0; i < totalPaquetes; i++) buffer.put(i, payloads.get(i));

                boolean sesionActiva = true;
                while (sesionActiva) {
                    // Enviar hasta llenar ventana
                    System.out.println("\u001B[36m---------- ENVIANDO VENTANA ----------\u001B[0m");
                    while (nextSeq < base + VENTANA && nextSeq < totalPaquetes) {
                        byte[] payload = buffer.get(nextSeq);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeInt(nextSeq);
                        dos.writeBoolean(nextSeq == totalPaquetes - 1); // marcar último
                        dos.write(payload);
                        dos.flush();
                        byte[] paqueteBytes = baos.toByteArray();

                        if (random.nextDouble() < PROB_PERDIDA) {
                            System.out.println("\u001B[31m => Paquete #" + nextSeq + " perdido (simulado) \u001B[0m");
                        } else {
                            DatagramPacket dp = new DatagramPacket(paqueteBytes, paqueteBytes.length, clienteDir, clientePuerto);
                            socket.send(dp);
                            System.out.println("\u001B[34m => Enviado #" + nextSeq + " bytes: " + paqueteBytes.length + (nextSeq == totalPaquetes - 1 ? " (ultimo)\u001B[0m" : "\u001B[0m"));
                        }

                        if (!timerOn) {
                            timerOn = true;
                            timerStart = System.currentTimeMillis();
                        }
                        nextSeq++;
                    }

                    // Recibir ACKs
                    try {
                        System.out.println("\u001B[36m---------- RECIBIENDO ACUSES ----------\u001B[0m");
                        byte[] bufAck = new byte[128];
                        DatagramPacket ackPkt = new DatagramPacket(bufAck, bufAck.length);
                        socket.receive(ackPkt);

                        String ackMsg = new String(ackPkt.getData(), 0, ackPkt.getLength()).trim();

                        if (ackMsg.startsWith("ACK")) {
                            String numStr = ackMsg.substring(3);
                            try {
                                int ackNum = Integer.parseInt(numStr);
                                System.out.println("\u001B[32m <= Recibido \u001B[0m" + ackMsg);

                                if (ackNum >= base) {
                                    base = ackNum + 1;
                                    if (base == nextSeq) {
                                        timerOn = false;
                                    } else {
                                        timerStart = System.currentTimeMillis();
                                        timerOn = true;
                                    }

                                    // Si ACK del último paquete → fin
                                    if (ackNum == totalPaquetes - 1) {
                                        System.out.println("\u001B[32m <= Ultimo ACK recibido. Fin de comunicacion.\u001B[0m");
                                        sesionActiva = false;
                                    }
                                } else {
                                    System.out.println("\u001B[33m ACK antiguo o duplicado: \u001B[0m" + ackMsg);
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("\u001B[33m ACK con formato inválido: \u001B[0m" + ackMsg);
                            }
                        }

                    } catch (SocketTimeoutException ste) {
                        System.out.println("\u001B[36m---------- RETRANSMITIENDO VENTANA ----------\u001B[0m");
                        if (timerOn && (System.currentTimeMillis() - timerStart) >= TIMEOUT) {
                            System.out.println(" ===> Timeout — reenviando ventana desde #" + base + " hasta #" + (nextSeq - 1));
                            for (int i = base; i < nextSeq; i++) {
                                byte[] payload = buffer.get(i);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                DataOutputStream dos = new DataOutputStream(baos);
                                dos.writeInt(i);
                                dos.writeBoolean(i == totalPaquetes - 1); // es ultimo o nel?
                                dos.write(payload);
                                dos.flush();
                                byte[] reenvio = baos.toByteArray();

                                if (random.nextDouble() < PROB_PERDIDA) {
                                    System.out.println("\u001B[31m Reenvio perdido (simulado) #" + i + "\u001B[0m");
                                    continue;
                                }
                                DatagramPacket dpRe = new DatagramPacket(reenvio, reenvio.length, clienteDir, clientePuerto);
                                socket.send(dpRe);
                                System.out.println("\u001B[34m Reenviado #" + i + " bytes: " + reenvio.length + "\u001B[0m");
                            }
                            timerStart = System.currentTimeMillis();
                        }
                    }
                }

                socket.setSoTimeout(0);
                System.out.println("Comunicacion finalizada. Esperando nuevo cliente...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
