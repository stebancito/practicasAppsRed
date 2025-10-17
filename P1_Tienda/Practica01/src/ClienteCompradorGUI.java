import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClienteCompradorGUI extends JFrame {
    private Socket socketCliente;
    private DataInputStream in;
    private DataOutputStream out;
    
    // Componentes principales
    private JTextArea textArea;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    // Paneles
    private JPanel menuPanel;
    private JPanel buscarPanel;
    private JPanel listarPanel;
    private JPanel carritoPanel;
    
    // Colores para una interfaz moderna
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private final Color TEXT_COLOR = new Color(51, 51, 51);
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);
    
    public ClienteCompradorGUI() {
        initializeGUI();
        connectToServer();
    }
    
    private void initializeGUI() {
        setTitle("Tienda Online - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        // Configurar look and feel para Ubuntu
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Panel principal con CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BACKGROUND_COLOR);
        
        crearMenuPrincipal();
        crearPanelBuscar();
        crearPanelListar();
        crearPanelCarrito();
        
        // Área de texto para mostrar respuestas
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("DejaVu Sans", Font.PLAIN, 12));
        textArea.setBackground(new Color(253, 253, 253));
        textArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 200));
        
        // Layout principal
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
        
        // Mostrar menú principal por defecto
        cardLayout.show(mainPanel, "MENU");
    }
    
    private void crearMenuPrincipal() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridBagLayout());
        menuPanel.setBackground(BACKGROUND_COLOR);
        menuPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        
        // Título
        JLabel titulo = new JLabel("TIENDA ONLINE", SwingConstants.CENTER);
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 28));
        titulo.setForeground(PRIMARY_COLOR);
        gbc.insets = new Insets(0, 0, 30, 0);
        menuPanel.add(titulo, gbc);
        
        gbc.insets = new Insets(8, 0, 8, 0);
        
        // Botones del menú
        String[] opciones = {
            "Buscar Producto",
            "Listar por Tipo", 
            "Agregar al Carrito",
            "Editar Carrito",
            "Finalizar Compra",
            "Salir"
        };
        
        for (String opcion : opciones) {
            JButton btn = crearBotonModerno(opcion);
            btn.addActionListener(new MenuListener());
            menuPanel.add(btn, gbc);
        }
        
        mainPanel.add(menuPanel, "MENU");
    }
    
    private void crearPanelBuscar() {
        buscarPanel = new JPanel(new BorderLayout());
        buscarPanel.setBackground(BACKGROUND_COLOR);
        buscarPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Panel superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver");
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "MENU"));
        
        JLabel titulo = new JLabel("Buscar Producto");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
        // Panel de búsqueda
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBackground(BACKGROUND_COLOR);
        searchPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JLabel instruccion = new JLabel("Ingresa el nombre o marca del producto:");
        instruccion.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        instruccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField searchField = new JTextField(20);
        searchField.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        searchField.setMaximumSize(new Dimension(400, 35));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton searchBtn = crearBotonModerno("Buscar");
        searchBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        searchBtn.addActionListener(e -> {
            String producto = searchField.getText().trim();
            if (!producto.isEmpty()) {
                enviarComando("BUSCAR " + producto);
            } else {
                mostrarError("Por favor ingresa un término de búsqueda");
            }
        });
        
        searchPanel.add(instruccion);
        searchPanel.add(Box.createVerticalStrut(10));
        searchPanel.add(searchField);
        searchPanel.add(Box.createVerticalStrut(15));
        searchPanel.add(searchBtn);
        
        buscarPanel.add(topPanel, BorderLayout.NORTH);
        buscarPanel.add(searchPanel, BorderLayout.CENTER);
        
        mainPanel.add(buscarPanel, "BUSCAR");
    }
    
    private void crearPanelListar() {
        listarPanel = new JPanel(new BorderLayout());
        listarPanel.setBackground(BACKGROUND_COLOR);
        listarPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Panel superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver");
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "MENU"));
        
        JLabel titulo = new JLabel("Listar por Tipo");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
        // Panel de tipos
        JPanel typesPanel = new JPanel();
        typesPanel.setLayout(new BoxLayout(typesPanel, BoxLayout.Y_AXIS));
        typesPanel.setBackground(BACKGROUND_COLOR);
        typesPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JLabel instruccion = new JLabel("Selecciona el tipo de producto:");
        instruccion.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        instruccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Tipos de productos comunes
        String[] tipos = {
            "Laptop", "Smartphone", "Monitor", "Periferico", 
            "Impresora", "Televisor", "Consola", "Tablet",
            "Red", "Almacenamiento", "Componente", "Wearable"
        };
        
        JPanel buttonsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        buttonsPanel.setBackground(BACKGROUND_COLOR);
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsPanel.setMaximumSize(new Dimension(400, 300));
        
        for (String tipo : tipos) {
            JButton btn = crearBotonModerno(tipo);
            btn.addActionListener(e -> enviarComando("LISTAR " + tipo));
            buttonsPanel.add(btn);
        }
        
        typesPanel.add(instruccion);
        typesPanel.add(Box.createVerticalStrut(15));
        typesPanel.add(buttonsPanel);
        
        listarPanel.add(topPanel, BorderLayout.NORTH);
        listarPanel.add(typesPanel, BorderLayout.CENTER);
        
        mainPanel.add(listarPanel, "LISTAR");
    }
    
    private void crearPanelCarrito() {
        carritoPanel = new JPanel(new BorderLayout());
        carritoPanel.setBackground(BACKGROUND_COLOR);
        carritoPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Panel superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver");
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "MENU"));
        
        JLabel titulo = new JLabel("Gestionar Carrito");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
        // Panel de opciones del carrito
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(BACKGROUND_COLOR);
        optionsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        // Agregar producto
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBackground(BACKGROUND_COLOR);
        
        JLabel addLabel = new JLabel("Agregar producto (ID):");
        addLabel.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        
        JTextField idField = new JTextField(10);
        idField.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        
        JButton addBtn = crearBotonModerno("Agregar");
        addBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            if (!id.isEmpty() && id.matches("\\d+")) {
                enviarComando("AGREGAR " + id);
                idField.setText("");
            } else {
                mostrarError("Por favor ingresa un ID válido (número)");
            }
        });
        
        addPanel.add(addLabel);
        addPanel.add(idField);
        addPanel.add(addBtn);
        
        // Editar carrito
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editPanel.setBackground(BACKGROUND_COLOR);
        
        JButton editBtn = crearBotonModerno("Editar Carrito");
        editBtn.addActionListener(e -> {
            enviarComando("EDITAR");
        });
        
        editPanel.add(editBtn);
        
        // Finalizar compra
        JPanel buyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buyPanel.setBackground(BACKGROUND_COLOR);
        
        JButton buyBtn = crearBotonModerno("Finalizar Compra");
        buyBtn.setBackground(SUCCESS_COLOR);
        buyBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de que quieres finalizar la compra?",
                "Confirmar Compra",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                enviarComando("COMPRAR");
            }
        });
        
        buyPanel.add(buyBtn);
        
        optionsPanel.add(addPanel);
        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(editPanel);
        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(buyPanel);
        
        carritoPanel.add(topPanel, BorderLayout.NORTH);
        carritoPanel.add(optionsPanel, BorderLayout.CENTER);
        
        mainPanel.add(carritoPanel, "CARRITO");
    }
    
    private JButton crearBotonModerno(String texto) {
        JButton button = new JButton(texto);
        button.setFont(new Font("DejaVu Sans", Font.BOLD, 14));
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 45));
        button.setMaximumSize(new Dimension(200, 45));
        
        // Efecto hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(SECONDARY_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    private void connectToServer() {
        final String HOST = "127.0.0.1";
        final int PUERTO = 8080;
        
        try {
            socketCliente = new Socket(HOST, PUERTO);
            in = new DataInputStream(socketCliente.getInputStream());
            out = new DataOutputStream(socketCliente.getOutputStream());
            
            mostrarMensaje("Conectado al servidor en " + HOST + ":" + PUERTO);
            
        } catch (IOException e) {
            mostrarError("No se pudo conectar al servidor: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "No se pudo conectar al servidor. Asegúrate de que el servidor esté ejecutándose.",
                "Error de Conexión",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void enviarComando(String comando) {
        try {
            out.write(comando.getBytes(StandardCharsets.UTF_8));
            out.flush();
            mostrarMensaje("Enviado: " + comando);
            
            // Recibir respuesta
            byte[] buf = new byte[4096];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                mostrarMensaje("Servidor: " + respuesta);
                
                // Manejar casos especiales
                if (comando.equals("EDITAR") && !respuesta.contains("Debes tener un carrito")) {
                    manejarEdicionCarrito(respuesta);
                } else if (comando.equals("COMPRAR")) {
                    mostrarTicket(respuesta);
                }
            }
        } catch (IOException e) {
            mostrarError("Error de comunicación: " + e.getMessage());
        }
    }
    
    private void manejarEdicionCarrito(String carritoActual) {
        String idProducto = JOptionPane.showInputDialog(
            this,
            "Carrito actual:\n" + carritoActual + "\n\nIngresa el ID del producto a eliminar:",
            "Editar Carrito",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (idProducto != null && !idProducto.trim().isEmpty()) {
            try {
                out.write(idProducto.trim().getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                byte[] buf = new byte[4096];
                int n = in.read(buf);
                if (n > 0) {
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    mostrarMensaje("Servidor: " + respuesta);
                }
            } catch (IOException e) {
                mostrarError("Error al editar carrito: " + e.getMessage());
            }
        }
    }
    
    private void mostrarTicket(String ticket) {
        JTextArea ticketArea = new JTextArea(ticket);
        ticketArea.setEditable(false);
        ticketArea.setFont(new Font("DejaVu Sans Mono", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Ticket de Compra",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        // Preguntar si quiere salir
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Quieres salir de la aplicación?",
            "Compra Finalizada",
            JOptionPane.YES_NO_OPTION
        );
        
        if (opcion == JOptionPane.YES_OPTION) {
            salir();
        }
    }
    
    private void mostrarMensaje(String mensaje) {
        textArea.append(mensaje + "\n\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
    
    private void mostrarError(String error) {
        textArea.append("ERROR: " + error + "\n\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
        JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void salir() {
        try {
            if (out != null) {
                out.write("SALIR".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            if (socketCliente != null && !socketCliente.isClosed()) {
                socketCliente.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    // Listener para los botones del menú
    private class MenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String comando = ((JButton) e.getSource()).getText();
            
            switch (comando) {
                case "Buscar Producto":
                    cardLayout.show(mainPanel, "BUSCAR");
                    break;
                case "Listar por Tipo":
                    cardLayout.show(mainPanel, "LISTAR");
                    break;
                case "Agregar al Carrito":
                case "Editar Carrito":
                case "Finalizar Compra":
                    cardLayout.show(mainPanel, "CARRITO");
                    break;
                case "Salir":
                    int confirm = JOptionPane.showConfirmDialog(
                        ClienteCompradorGUI.this,
                        "¿Estás seguro de que quieres salir?",
                        "Confirmar Salida",
                        JOptionPane.YES_NO_OPTION
                    );
                    if (confirm == JOptionPane.YES_OPTION) {
                        salir();
                    }
                    break;
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClienteCompradorGUI().setVisible(true);
        });
    }
}