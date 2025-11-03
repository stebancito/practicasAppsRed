import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class controlFlujoSer {
    private static final int PUERTO = 9876;
    private static final int TIMEOUT = 3000;
    private static final double PROB_PERDIDA = 0.2;

    public static void main(String[] args) {
        Random random = new Random();
        Scanner sc = new Scanner(System.in);

        System.out.print("Ingrese el tamaño de la ventana: ");
        int VENTANA = sc.nextInt();

        System.out.print("Ingrese el tamaño de cada paquete en bytes: ");
        int TAMANO_PAQUETE = sc.nextInt();

        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            socket.setReuseAddress(true);
            System.out.println("Servidor escuchando en el puerto " + PUERTO);

            while (true) {
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
                System.out.println("\u001B[32mCliente conectado:\u001B[0m " + clienteDir + ":" + clientePuerto);

                socket.setSoTimeout(TIMEOUT);

                // ========================================
                // 🗂️ Selector de archivo MP3 (JFileChooser)
                // ========================================
                File archivo = null;
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Seleccionar archivo MP3 a enviar");
                chooser.setFileFilter(new FileNameExtensionFilter("Archivos MP3", "mp3"));
                int resultado = chooser.showOpenDialog(null);

                if (resultado == JFileChooser.APPROVE_OPTION) {
                    archivo = chooser.getSelectedFile();
                    System.out.println("\u001B[32mArchivo seleccionado:\u001B[0m " + archivo.getAbsolutePath());
                } else {
                    System.out.println("\u001B[31mNo se seleccionó ningún archivo. Se enviarán datos de prueba.\u001B[0m");
                }

                byte[] allData;
                if (archivo != null && archivo.exists()) {
                    try (FileInputStream fis = new FileInputStream(archivo)) {
                        allData = fis.readAllBytes();
                    }
                } else {
                    allData = new byte[3072];
                    for (int i = 0; i < allData.length; i++) allData[i] = (byte) (65 + (i % 26));
                    System.out.println("Archivo no encontrado — usando datos de prueba (" + allData.length + " bytes)");
                }

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
                int reintentos = 0;
                final int MAX_REINTENTOS = 5;

                Map<Integer, byte[]> buffer = new HashMap<>();
                for (int i = 0; i < totalPaquetes; i++) buffer.put(i, payloads.get(i));

                boolean sesionActiva = true;
                while (sesionActiva) {
                    System.out.println("\u001B[36m---------- ENVIANDO VENTANA ----------\u001B[0m");
                    while (nextSeq < base + VENTANA && nextSeq < totalPaquetes) {
                        byte[] payload = buffer.get(nextSeq);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeInt(nextSeq);
                        dos.writeBoolean(nextSeq == totalPaquetes - 1);
                        dos.write(payload);
                        dos.flush();
                        byte[] paqueteBytes = baos.toByteArray();

                        if (random.nextDouble() < PROB_PERDIDA) {
                            System.out.println("\u001B[31m=> Paquete #" + nextSeq + " perdido (simulado)\u001B[0m");
                        } else {
                            DatagramPacket dp = new DatagramPacket(paqueteBytes, paqueteBytes.length, clienteDir, clientePuerto);
                            socket.send(dp);
                            System.out.println("\u001B[34m=> Enviado #" + nextSeq + " (" + paqueteBytes.length + " bytes)\u001B[0m");
                        }

                        if (!timerOn) {
                            timerOn = true;
                            timerStart = System.currentTimeMillis();
                        }
                        nextSeq++;
                    }

                    try {
                        System.out.println("\u001B[36m---------- RECIBIENDO ACUSES ----------\u001B[0m");
                        byte[] bufAck = new byte[128];
                        DatagramPacket ackPkt = new DatagramPacket(bufAck, bufAck.length);
                        socket.receive(ackPkt);
                        reintentos = 0;

                        String ackMsg = new String(ackPkt.getData(), 0, ackPkt.getLength()).trim();
                        if (ackMsg.startsWith("ACK")) {
                            int ackNum = Integer.parseInt(ackMsg.substring(3));
                            System.out.println("\u001B[32m<= Recibido " + ackMsg + "\u001B[0m");

                            if (ackNum >= base) {
                                base = ackNum + 1;
                                if (base == nextSeq) {
                                    timerOn = false;
                                } else {
                                    timerOn = true;
                                    timerStart = System.currentTimeMillis();
                                }

                                if (ackNum == totalPaquetes - 1) {
                                    System.out.println("\u001B[32m<= Último ACK recibido. Fin de transmisión.\u001B[0m");
                                    sesionActiva = false;
                                }
                            } else {
                                System.out.println("\u001B[33mACK duplicado: " + ackMsg + "\u001B[0m");
                            }
                        }

                    } catch (SocketTimeoutException ste) {
                        reintentos++;
                        if (reintentos >= MAX_REINTENTOS) {
                            System.out.println("\u001B[31mCliente no responde tras " + MAX_REINTENTOS + " intentos. Finalizando sesión.\u001B[0m");
                            sesionActiva = false;
                            break;
                        }

                        System.out.println("\u001B[33m---------- RETRANSMITIENDO VENTANA ----------\u001B[0m");
                        if (timerOn && (System.currentTimeMillis() - timerStart) >= TIMEOUT) {
                            System.out.println("\u001B[33mTimeout — reenviando ventana desde #" + base + " hasta #" + (nextSeq - 1) + "\u001B[0m");
                            for (int i = base; i < nextSeq; i++) {
                                byte[] payload = buffer.get(i);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                DataOutputStream dos = new DataOutputStream(baos);
                                dos.writeInt(i);
                                dos.writeBoolean(i == totalPaquetes - 1);
                                dos.write(payload);
                                dos.flush();
                                byte[] reenvio = baos.toByteArray();

                                if (random.nextDouble() < PROB_PERDIDA) {
                                    System.out.println("\u001B[31m => Reenvío perdido (simulado) #" + i + "\u001B[0m");
                                    continue;
                                }
                                DatagramPacket dpRe = new DatagramPacket(reenvio, reenvio.length, clienteDir, clientePuerto);
                                socket.send(dpRe);
                                System.out.println("\u001B[33m => Reenviado #" + i + "\u001B[0m");
                            }
                            timerStart = System.currentTimeMillis();
                        }
                    }
                }

                socket.setSoTimeout(0);
                System.out.println("Sesión terminada. Esperando nuevo cliente...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
