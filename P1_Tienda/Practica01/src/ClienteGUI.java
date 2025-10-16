import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteGUI extends JFrame {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private JTextArea areaRespuesta;
    private JTextField campoEntrada;
    private JComboBox<String> comboOpciones;

    public ClienteGUI() {
        setTitle("Tienda en Línea - Cliente");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ----- PANEL SUPERIOR -----
        JPanel panelSuperior = new JPanel(new FlowLayout());
        comboOpciones = new JComboBox<>(new String[]{
                "Buscar producto", "Listar por tipo", "Agregar al carrito", "Editar carrito", "Finalizar compra"
        });
        campoEntrada = new JTextField(20);
        JButton botonEnviar = new JButton("Enviar");
        panelSuperior.add(comboOpciones);
        panelSuperior.add(campoEntrada);
        panelSuperior.add(botonEnviar);
        add(panelSuperior, BorderLayout.NORTH);

        // ----- ÁREA DE RESPUESTA -----
        areaRespuesta = new JTextArea();
        areaRespuesta.setEditable(false);
        add(new JScrollPane(areaRespuesta), BorderLayout.CENTER);

        conectarServidor();

        botonEnviar.addActionListener(e -> enviarComando());
    }

    private void conectarServidor() {
        try {
            socket = new Socket("127.0.0.1", 8080);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            areaRespuesta.append("Conectado al servidor.\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con el servidor", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void enviarComando() {
        try {
            String opcion = (String) comboOpciones.getSelectedItem();
            String entrada = campoEntrada.getText().trim();
            String comando = "";

            switch (opcion) {
                case "Buscar producto" -> comando = "BUSCAR " + entrada;
                case "Listar por tipo" -> comando = "LISTAR " + entrada;
                case "Agregar al carrito" -> comando = "AGREGAR " + entrada;
                case "Editar carrito" -> comando = "EDITAR";
                case "Finalizar compra" -> comando = "SALIR";
            }

            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[4096];
            int n = in.read(buf);
            String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();

            areaRespuesta.append("\n🟩 Servidor: " + respuesta + "\n");

            if (comando.equals("EDITAR")) {
                String id = JOptionPane.showInputDialog(this, "Introduce el ID del producto a eliminar (o 0 para salir):");
                if (id != null && !id.isEmpty()) {
                    out.write(id.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    n = in.read(buf);
                    respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    areaRespuesta.append("\n🟦 Respuesta edición: " + respuesta + "\n");
                }
            }

            if (comando.equals("SALIR")) {
                socket.close();
                areaRespuesta.append("Conexión cerrada.\n");
            }

        } catch (IOException e) {
            areaRespuesta.append("\n❌ Error al comunicar con el servidor: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI gui = new ClienteGUI();
            gui.setVisible(true);
        });
    }
}
