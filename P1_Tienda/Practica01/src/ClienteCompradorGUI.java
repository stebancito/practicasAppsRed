import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteCompradorGUI {
    private Socket socketCliente;
    private DataInputStream in;
    private DataOutputStream out;
    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayout;
    private boolean conectado = false;
    
    // Áreas de texto separadas para cada panel
    private JTextArea textAreaBuscar;
    private JTextArea textAreaListar;
    private JTextArea textAreaAgregar;
    private JTextArea textAreaCarrito;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteCompradorGUI().iniciar());
    }

    public void iniciar() {
        crearGUI();
        conectarServidor();
    }

    private void conectarServidor() {
        try {
            socketCliente = new Socket("127.0.0.1", 8080);
            in = new DataInputStream(socketCliente.getInputStream());
            out = new DataOutputStream(socketCliente.getOutputStream());
            conectado = true;
            System.out.println("Conectado al servidor en 127.0.0.1:8080");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, 
                "Error al conectar con el servidor: " + e.getMessage() + 
                "\nAsegúrate de que el servidor esté ejecutándose.", 
                "Error de Conexión", 
                JOptionPane.ERROR_MESSAGE);
            conectado = false;
        }
    }

    private void crearGUI() {
        frame = new JFrame("Tienda Online - Cliente Comprador");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        panelPrincipal = new JPanel(cardLayout);

        panelPrincipal.add(crearPanelMenuPrincipal(), "menu");
        panelPrincipal.add(crearPanelBuscar(), "buscar");
        panelPrincipal.add(crearPanelListar(), "listar");
        panelPrincipal.add(crearPanelAgregarCarrito(), "agregar");
        panelPrincipal.add(crearPanelCarrito(), "carrito");

        frame.add(panelPrincipal);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarConexion();
            }
        });
    }

    private JPanel crearPanelMenuPrincipal() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel("🛒 TIENDA ONLINE", JLabel.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        panel.add(titulo, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(5, 1, 15, 15));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        String[] opciones = {
            "🔍 Buscar Productos",
            "📋 Listar Productos por Tipo", 
            "🛒 Agregar al Carrito",
            "✏️ Editar Carrito",
            "💰 Finalizar Compra"
        };

        for (String opcion : opciones) {
            JButton boton = new JButton(opcion);
            boton.setFont(new Font("Arial", Font.PLAIN, 18));
            boton.setPreferredSize(new Dimension(300, 60));
            boton.addActionListener(e -> manejarOpcionMenu(boton.getText()));
            panelBotones.add(boton);
        }

        panel.add(panelBotones, BorderLayout.CENTER);

        JLabel estadoConexion = new JLabel(conectado ? "✅ Conectado al servidor" : "❌ No conectado", JLabel.CENTER);
        estadoConexion.setFont(new Font("Arial", Font.ITALIC, 12));
        estadoConexion.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        panel.add(estadoConexion, BorderLayout.SOUTH);

        return panel;
    }

    private void manejarOpcionMenu(String textoBoton) {
        if (!conectado) {
            JOptionPane.showMessageDialog(frame, 
                "No hay conexión con el servidor.\nReinicia la aplicación cuando el servidor esté disponible.", 
                "Error de Conexión", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        switch (textoBoton) {
            case "🔍 Buscar Productos":
                cardLayout.show(panelPrincipal, "buscar");
                break;
            case "📋 Listar Productos por Tipo":
                cardLayout.show(panelPrincipal, "listar");
                break;
            case "🛒 Agregar al Carrito":
                cardLayout.show(panelPrincipal, "agregar");
                break;
            case "✏️ Editar Carrito":
                cardLayout.show(panelPrincipal, "carrito");
                verCarrito();
                break;
            case "💰 Finalizar Compra":
                finalizarCompra();
                break;
        }
    }

    private JPanel crearPanelBuscar() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        
        JLabel label = new JLabel("Buscar producto por nombre o marca:");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextField campoBusqueda = new JTextField(20);
        campoBusqueda.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton botonBuscar = new JButton("🔍 Buscar");
        botonBuscar.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton botonVolver = new JButton("← Volver al Menú");
        botonVolver.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelControles.add(label);
        panelControles.add(campoBusqueda);
        panelControles.add(botonBuscar);

        panelSuperior.add(panelControles, BorderLayout.CENTER);
        panelSuperior.add(botonVolver, BorderLayout.WEST);

        // Área de texto específica para buscar
        textAreaBuscar = new JTextArea();
        textAreaBuscar.setEditable(false);
        textAreaBuscar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaBuscar.setLineWrap(true);
        textAreaBuscar.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textAreaBuscar);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Resultados de la Búsqueda"));

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        botonBuscar.addActionListener(e -> {
            String busqueda = campoBusqueda.getText().trim();
            if (!busqueda.isEmpty()) {
                buscarProducto(busqueda);
            } else {
                JOptionPane.showMessageDialog(frame, 
                    "Por favor ingresa un término de búsqueda.", 
                    "Búsqueda Vacía", 
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        campoBusqueda.addActionListener(e -> botonBuscar.doClick());

        botonVolver.addActionListener(e -> {
            campoBusqueda.setText("");
            textAreaBuscar.setText("");
            cardLayout.show(panelPrincipal, "menu");
        });

        return panel;
    }

    private JPanel crearPanelListar() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        
        JLabel label = new JLabel("Seleccione el tipo de producto:");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        
        String[] tipos = {"Laptop", "Smartphone", "Monitor", "Periférico", "Impresora", "Televisor", "Consola"};
        JComboBox<String> comboTipos = new JComboBox<>(tipos);
        comboTipos.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton botonListar = new JButton("📋 Listar");
        botonListar.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton botonVolver = new JButton("← Volver al Menú");
        botonVolver.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelControles.add(label);
        panelControles.add(comboTipos);
        panelControles.add(botonListar);

        panelSuperior.add(panelControles, BorderLayout.CENTER);
        panelSuperior.add(botonVolver, BorderLayout.WEST);

        // Área de texto específica para listar
        textAreaListar = new JTextArea();
        textAreaListar.setEditable(false);
        textAreaListar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaListar.setLineWrap(true);
        textAreaListar.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textAreaListar);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Productos Encontrados"));

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        botonListar.addActionListener(e -> {
            String tipo = (String) comboTipos.getSelectedItem();
            listarProductos(tipo);
        });

        botonVolver.addActionListener(e -> {
            textAreaListar.setText("");
            cardLayout.show(panelPrincipal, "menu");
        });

        return panel;
    }

    private JPanel crearPanelAgregarCarrito() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        
        JLabel label = new JLabel("Ingrese el ID del producto a agregar:");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextField campoId = new JTextField(10);
        campoId.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton botonAgregar = new JButton("🛒 Agregar al Carrito");
        botonAgregar.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton botonVolver = new JButton("← Volver al Menú");
        botonVolver.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelControles.add(label);
        panelControles.add(campoId);
        panelControles.add(botonAgregar);

        panelSuperior.add(panelControles, BorderLayout.CENTER);
        panelSuperior.add(botonVolver, BorderLayout.WEST);

        // Área de texto específica para agregar
        textAreaAgregar = new JTextArea();
        textAreaAgregar.setEditable(false);
        textAreaAgregar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaAgregar.setLineWrap(true);
        textAreaAgregar.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textAreaAgregar);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Respuesta del Servidor"));

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        botonAgregar.addActionListener(e -> {
            String id = campoId.getText().trim();
            if (!id.isEmpty()) {
                if (id.matches("\\d+")) {
                    agregarAlCarrito(id);
                    campoId.setText("");
                } else {
                    JOptionPane.showMessageDialog(frame, 
                        "El ID debe ser un número.", 
                        "ID Inválido", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, 
                    "Por favor ingresa un ID de producto.", 
                    "ID Vacío", 
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        campoId.addActionListener(e -> botonAgregar.doClick());

        botonVolver.addActionListener(e -> {
            campoId.setText("");
            textAreaAgregar.setText("");
            cardLayout.show(panelPrincipal, "menu");
        });

        return panel;
    }

    private JPanel crearPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        JButton botonVerCarrito = new JButton("🔄 Actualizar Carrito");
        botonVerCarrito.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton botonFinalizar = new JButton("💰 Finalizar Compra");
        botonFinalizar.setFont(new Font("Arial", Font.BOLD, 14));
        botonFinalizar.setBackground(new Color(50, 150, 50));
        botonFinalizar.setForeground(Color.WHITE);
        
        JButton botonVolver = new JButton("← Volver al Menú");
        botonVolver.setFont(new Font("Arial", Font.PLAIN, 12));

        panelBotones.add(botonVerCarrito);
        panelBotones.add(botonFinalizar);
        panelBotones.add(botonVolver);

        // Área de texto específica para carrito
        textAreaCarrito = new JTextArea();
        textAreaCarrito.setEditable(false);
        textAreaCarrito.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaCarrito.setLineWrap(true);
        textAreaCarrito.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textAreaCarrito);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Tu Carrito de Compras"));

        panel.add(panelBotones, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        botonVerCarrito.addActionListener(e -> verCarrito());
        
        botonFinalizar.addActionListener(e -> finalizarCompra());
        
        botonVolver.addActionListener(e -> {
            textAreaCarrito.setText("");
            cardLayout.show(panelPrincipal, "menu");
        });

        return panel;
    }

    private void buscarProducto(String producto) {
        try {
            textAreaBuscar.setText("Buscando productos...");
            String comando = "BUSCAR " + producto;
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                textAreaBuscar.setText("Resultados para: '" + producto + "'\n\n" + respuesta);
            } else {
                textAreaBuscar.setText("No se recibió respuesta del servidor.");
            }
            
        } catch (SocketException e) {
            manejarErrorConexion("Error de conexión durante la búsqueda: " + e.getMessage());
        } catch (Exception e) {
            textAreaBuscar.setText("Error al buscar producto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void listarProductos(String tipo) {
        try {
            textAreaListar.setText("Cargando productos...");
            String comando = "LISTAR " + tipo;
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                textAreaListar.setText("Productos de tipo: '" + tipo + "'\n\n" + respuesta);
            } else {
                textAreaListar.setText("No se recibió respuesta del servidor.");
            }
            
        } catch (SocketException e) {
            manejarErrorConexion("Error de conexión al listar productos: " + e.getMessage());
        } catch (Exception e) {
            textAreaListar.setText("Error al listar productos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void agregarAlCarrito(String id) {
        try {
            textAreaAgregar.setText("Agregando producto al carrito...");
            String comando = "AGREGAR " + id;
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[4096];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                textAreaAgregar.setText("Respuesta del servidor:\n\n" + respuesta);
            } else {
                textAreaAgregar.setText("No se recibió respuesta del servidor.");
            }
            
        } catch (SocketException e) {
            manejarErrorConexion("Error de conexión al agregar al carrito: " + e.getMessage());
        } catch (Exception e) {
            textAreaAgregar.setText("Error al agregar al carrito: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verCarrito() {
        try {
            textAreaCarrito.setText("Cargando carrito...");
            String comando = "EDITAR";
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                textAreaCarrito.setText("🛒 Tu Carrito de Compras:\n\n" + respuesta);
            } else {
                textAreaCarrito.setText("No se recibió respuesta del servidor.");
            }
            
        } catch (SocketException e) {
            manejarErrorConexion("Error de conexión al cargar el carrito: " + e.getMessage());
        } catch (Exception e) {
            textAreaCarrito.setText("Error al cargar el carrito: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finalizarCompra() {
        int confirmacion = JOptionPane.showConfirmDialog(frame,
            "¿Estás seguro de que quieres finalizar la compra?\nEsta acción no se puede deshacer.",
            "Confirmar Compra",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            textAreaCarrito.setText("Procesando compra...");
            String comando = "COMPRAR";
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buf = new byte[16384];
            int n = in.read(buf);
            
            if (n == -1) {
                textAreaCarrito.setText("El servidor cerró la conexión antes de enviar el ticket.");
                return;
            }

            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                
                mostrarTicketCompra(respuesta);
                
                textAreaCarrito.setText("✅ Compra finalizada. ¡Gracias por tu compra!\n\n" + respuesta);
                
                int opcion = JOptionPane.showConfirmDialog(frame,
                    "¿Quieres salir de la aplicación?",
                    "Compra Finalizada",
                    JOptionPane.YES_NO_OPTION);
                    
                if (opcion == JOptionPane.YES_OPTION) {
                    cerrarConexion();
                    System.exit(0);
                }
            } else {
                textAreaCarrito.setText("No se recibió ticket del servidor.");
            }
            
        } catch (SocketException e) {
            manejarErrorConexion("Error de conexión al finalizar compra: " + e.getMessage());
        } catch (Exception e) {
            textAreaCarrito.setText("Error al finalizar compra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarTicketCompra(String ticket) {
        JTextArea ticketArea = new JTextArea(ticket);
        ticketArea.setEditable(false);
        ticketArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        ticketArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        
        JOptionPane.showMessageDialog(frame, scrollPane, "🎫 TICKET DE COMPRA - ¡GRACIAS POR SU COMPRA!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void manejarErrorConexion(String mensaje) {
        conectado = false;
        JOptionPane.showMessageDialog(frame, 
            mensaje + "\n\nLa aplicación se cerrará.", 
            "Error de Conexión", 
            JOptionPane.ERROR_MESSAGE);
        cerrarConexion();
        System.exit(1);
    }
    
    private void cerrarConexion() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socketCliente != null && !socketCliente.isClosed()) {
                socketCliente.close();
            }
            System.out.println("Conexión cerrada correctamente");
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}