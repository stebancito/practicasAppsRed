import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ClienteCompradorGUI extends JFrame {
    private Socket socketCliente;
    private DataInputStream in;
    private DataOutputStream out;
    
    // Componentes principales
    private JTextArea textArea;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JButton clearButton;
    
    // Paneles
    private JPanel menuPanel;
    private JPanel buscarPanel;
    private JPanel listarPanel;
    private JPanel carritoPanel;
    private JPanel agregarPanel;
    
    // Tabla para mostrar productos
    private JTable productosTable;
    private DefaultTableModel tableModel;
    
    // Colores para una interfaz moderna
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private final Color TEXT_COLOR = new Color(51, 51, 51);
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private final Color WARNING_COLOR = new Color(231, 76, 60);
    
    public ClienteCompradorGUI() {
        initializeGUI();
        connectToServer();
    }
    
    private void initializeGUI() {
        setTitle("Tienda Online - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        
        // Configurar look and feel
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
        crearPanelAgregar();
        
        // Panel inferior con área de texto y controles
        JPanel bottomPanel = crearPanelInferior();
        
        // Layout principal
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Mostrar menú principal por defecto
        cardLayout.show(mainPanel, "MENU");
    }
    
    private JPanel crearPanelInferior() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        
        // Panel de controles
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        // Botón para limpiar la consola
        clearButton = new JButton("Limpiar Consola");
        clearButton.setFont(new Font("DejaVu Sans", Font.PLAIN, 12));
        clearButton.setBackground(new Color(200, 200, 200));
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> limpiarConsola());
        
        controlPanel.add(clearButton);
        
        // Área de texto con scroll
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("DejaVu Sans Mono", Font.PLAIN, 11));
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setForeground(new Color(30, 30, 30));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 150));
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150)),
            "Consola de Mensajes",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("DejaVu Sans", Font.BOLD, 12),
            PRIMARY_COLOR
        ));
        
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
        
        return bottomPanel;
    }
    
    private void crearPanelAgregar() {
        agregarPanel = new JPanel(new BorderLayout());
        agregarPanel.setBackground(BACKGROUND_COLOR);
        agregarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver al Menu");
        backBtn.addActionListener(e -> {
            cardLayout.show(mainPanel, "MENU");
            mostrarMensaje("Volviendo al menu principal...");
        });
        
        JButton refreshBtn = crearBotonModerno("Actualizar Lista");
        refreshBtn.setBackground(new Color(46, 204, 113));
        refreshBtn.addActionListener(e -> cargarProductos());
        
        JLabel titulo = new JLabel("Agregar Productos al Carrito");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(refreshBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
        // Crear tabla de productos
        String[] columnNames = {"ID", "Nombre", "Marca", "Tipo", "Precio", "Stock", "Acción"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Solo la columna de acción es editable
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4 || columnIndex == 5) {
                    return Integer.class; // Precio y Stock son números
                }
                return String.class;
            }
        };
        
        productosTable = new JTable(tableModel);
        productosTable.setFont(new Font("DejaVu Sans", Font.PLAIN, 12));
        productosTable.setRowHeight(25);
        productosTable.getTableHeader().setFont(new Font("DejaVu Sans", Font.BOLD, 12));
        productosTable.getTableHeader().setBackground(PRIMARY_COLOR);
        productosTable.getTableHeader().setForeground(Color.WHITE);
        
        // Configurar renderizador personalizado para la columna de stock
        productosTable.getColumnModel().getColumn(5).setCellRenderer(new StockCellRenderer());
        
        // Configurar la columna de acción
        productosTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        productosTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane tableScrollPane = new JScrollPane(productosTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150)),
            "Productos Disponibles - Haz clic en 'Agregar' para añadir al carrito",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("DejaVu Sans", Font.BOLD, 12),
            PRIMARY_COLOR
        ));
        
        agregarPanel.add(topPanel, BorderLayout.NORTH);
        agregarPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        mainPanel.add(agregarPanel, "AGREGAR");
    }
    
    // Renderizador personalizado para la columna de stock
    class StockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof Integer) {
                int stock = (Integer) value;
                if (stock <= 0) {
                    c.setBackground(new Color(255, 230, 230)); // Rojo claro para stock 0
                    c.setForeground(WARNING_COLOR);
                    setText("AGOTADO");
                } else if (stock <= 2) {
                    c.setBackground(new Color(255, 250, 230)); // Amarillo claro para stock bajo
                    c.setForeground(new Color(230, 126, 34));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }
    
    private void cargarProductos() {
        limpiarConsola();
        mostrarMensaje("Cargando lista de productos disponibles...");
        
        // Limpiar tabla antes de cargar nuevos datos
        tableModel.setRowCount(0);
        
        // Cargar productos de diferentes categorías
        String[] categorias = {
            "Laptop", "Smartphone", "Monitor", "Periferico", 
            "Impresora", "Televisor", "Consola", "Tablet",
            "Red", "Almacenamiento", "Componente", "Wearable"
        };
        
        for (String categoria : categorias) {
            try {
                out.write(("LISTAR " + categoria).getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                byte[] buf = new byte[8192]; // Buffer más grande para respuestas JSON grandes
                int n = in.read(buf);
                if (n > 0) {
                    String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                    procesarRespuestaProductos(respuesta, categoria);
                }
                Thread.sleep(30); // Pequeña pausa entre requests
            } catch (Exception e) {
                mostrarMensaje("Nota: No se pudieron cargar productos de la categoría: " + categoria);
            }
        }
        
        if (tableModel.getRowCount() == 0) {
            // Si no se cargaron productos, mostrar ejemplos
            agregarProductosEjemplo();
        } else {
            mostrarMensaje("Lista de productos cargada correctamente. " + tableModel.getRowCount() + " productos disponibles.");
            actualizarBotonesAgregar();
        }
    }
    
    private void procesarRespuestaProductos(String respuesta, String categoria) {
        try {
            // Buscar el array de productos en la respuesta JSON
            int startArray = respuesta.indexOf("[");
            int endArray = respuesta.lastIndexOf("]");
            
            if (startArray != -1 && endArray != -1 && endArray > startArray) {
                String arrayStr = respuesta.substring(startArray + 1, endArray);
                // Dividir por objetos JSON individuales
                String[] objetos = dividirObjetosJSON(arrayStr);
                
                int productosProcesados = 0;
                for (String objeto : objetos) {
                    if (objeto.trim().length() > 10) { // Objeto válido mínimo
                        if (procesarObjetoProducto(objeto.trim())) {
                            productosProcesados++;
                        }
                    }
                }
                mostrarMensaje("Categoría '" + categoria + "': " + productosProcesados + " productos cargados");
            } else {
                // Fallback: buscar objetos individuales
                procesarObjetosIndividuales(respuesta);
            }
        } catch (Exception e) {
            mostrarMensaje("Error procesando categoría '" + categoria + "': " + e.getMessage());
        }
    }
    
    private String[] dividirObjetosJSON(String arrayStr) {
        // Dividir el array JSON en objetos individuales
        ArrayList<String> objetos = new ArrayList<>();
        int depth = 0;
        StringBuilder currentObj = new StringBuilder();
        
        for (char c : arrayStr.toCharArray()) {
            if (c == '{') {
                depth++;
            }
            if (c == '}') {
                depth--;
            }
            
            currentObj.append(c);
            
            if (depth == 0 && currentObj.length() > 0) {
                String objStr = currentObj.toString().trim();
                if (objStr.length() > 2) { // Objeto válido mínimo: {}
                    objetos.add(objStr);
                }
                currentObj.setLength(0);
            }
        }
        
        return objetos.toArray(new String[0]);
    }
    
    private void procesarObjetosIndividuales(String respuesta) {
        // Método alternativo: buscar objetos JSON individuales
        String[] lineas = respuesta.split("\n");
        StringBuilder objetoActual = new StringBuilder();
        boolean enObjeto = false;
        
        for (String linea : lineas) {
            if (linea.contains("{") && !enObjeto) {
                enObjeto = true;
                objetoActual.setLength(0);
            }
            
            if (enObjeto) {
                objetoActual.append(linea.trim());
                
                if (linea.contains("}")) {
                    String objetoStr = objetoActual.toString();
                    procesarObjetoProducto(objetoStr);
                    enObjeto = false;
                }
            }
        }
    }
    
    private boolean procesarObjetoProducto(String jsonStr) {
        try {
            // Limpiar el string JSON
            jsonStr = jsonStr.trim();
            if (!jsonStr.startsWith("{")) {
                jsonStr = "{" + jsonStr;
            }
            if (!jsonStr.endsWith("}")) {
                jsonStr = jsonStr + "}";
            }
            
            // Extraer valores del objeto JSON
            String id = extraerValorJSON(jsonStr, "id");
            String nombre = extraerValorJSON(jsonStr, "nombre");
            String marca = extraerValorJSON(jsonStr, "marca");
            String tipo = extraerValorJSON(jsonStr, "tipo");
            String precio = extraerValorJSON(jsonStr, "precio");
            String stock = extraerValorJSON(jsonStr, "stock");
            
            if (id != null && nombre != null) {
                // Verificar si el producto ya existe en la tabla
                boolean existe = false;
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 0).toString().equals(id)) {
                        existe = true;
                        // Actualizar stock existente
                        if (stock != null) {
                            try {
                                tableModel.setValueAt(Integer.parseInt(stock), i, 5);
                            } catch (NumberFormatException e) {
                                tableModel.setValueAt(0, i, 5);
                            }
                        }
                        break;
                    }
                }
                
                if (!existe) {
                    // Agregar nuevo producto
                    int stockValue = 0;
                    if (stock != null) {
                        try {
                            stockValue = Integer.parseInt(stock);
                        } catch (NumberFormatException e) {
                            stockValue = 0;
                        }
                    }
                    
                    String precioStr = "$0";
                    if (precio != null) {
                        try {
                            double precioNum = Double.parseDouble(precio);
                            precioStr = String.format("$%.2f", precioNum);
                        } catch (NumberFormatException e) {
                            precioStr = "$" + precio;
                        }
                    }
                    
                    tableModel.addRow(new Object[]{
                        id, 
                        nombre != null ? nombre : "N/A", 
                        marca != null ? marca : "N/A",
                        tipo != null ? tipo : "N/A",
                        precioStr,
                        stockValue,
                        "Agregar"
                    });
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignorar errores de parsing individuales
        }
        return false;
    }
    
    private String extraerValorJSON(String json, String clave) {
        try {
            String busqueda = "\"" + clave + "\":";
            int start = json.indexOf(busqueda);
            if (start == -1) return null;
            
            start += busqueda.length();
            
            // Buscar el inicio del valor
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }
            
            int end = start;
            if (start < json.length()) {
                char firstChar = json.charAt(start);
                
                if (firstChar == '"') {
                    // Valor string
                    end = json.indexOf("\"", start + 1);
                    while (end != -1 && json.charAt(end - 1) == '\\') {
                        end = json.indexOf("\"", end + 1);
                    }
                    if (end != -1) {
                        return json.substring(start + 1, end);
                    }
                } else {
                    // Valor numérico o booleano
                    end = start;
                    while (end < json.length() && 
                           (Character.isDigit(json.charAt(end)) || 
                            json.charAt(end) == '.' || 
                            json.charAt(end) == '-' ||
                            Character.isLetter(json.charAt(end)))) {
                        end++;
                    }
                    return json.substring(start, end).trim();
                }
            }
        } catch (Exception e) {
            // Fallback: búsqueda simple
            return extraerValorSimple(json, clave);
        }
        return null;
    }
    
    private String extraerValorSimple(String json, String clave) {
        try {
            String pattern = "\"" + clave + "\"\\s*:\\s*\"([^\"]*)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            // Para números
            pattern = "\"" + clave + "\"\\s*:\\s*([^,}\\s]*)";
            p = Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            // Ignorar errores de regex
        }
        return null;
    }
    
    private void agregarProductosEjemplo() {
        // Productos de ejemplo en caso de que el parsing falle
        Object[][] productosEjemplo = {
            {1, "Laptop Victus 16", "HP", "Laptop", "$1200.00", 3, "Agregar"},
            {2, "ThinkPad T14 Gen 2", "Lenovo", "Laptop", "$1500.00", 3, "Agregar"},
            {3, "iPhone 14", "Apple", "Smartphone", "$999.00", 6, "Agregar"},
            {4, "Galaxy S23", "Samsung", "Smartphone", "$899.00", 6, "Agregar"},
            {5, "Monitor UltraSharp 27", "Dell", "Monitor", "$300.00", 8, "Agregar"},
            {6, "Teclado Mecanico K95", "Corsair", "Periferico", "$150.00", 12, "Agregar"},
            {7, "Mouse G502 HERO", "Logitech", "Periferico", "$70.00", 20, "Agregar"},
            {8, "Impresora EcoTank", "Epson", "Impresora", "$250.00", 4, "Agregar"},
            {9, "Smart TV QLED 55", "Samsung", "Televisor", "$750.00", 6, "Agregar"},
            {10, "PlayStation 5", "Sony", "Consola", "$500.00", 2, "Agregar"},
            {11, "MacBook Air M2", "Apple", "Laptop", "$1300.00", 4, "Agregar"},
            {12, "ASUS ROG Zephyrus G14", "ASUS", "Laptop", "$1600.00", 5, "Agregar"},
            {13, "iPhone 15 Pro", "Apple", "Smartphone", "$1199.00", 4, "Agregar"},
            {14, "Xiaomi 13T Pro", "Xiaomi", "Smartphone", "$699.00", 7, "Agregar"},
            {15, "Smart TV OLED 65", "LG", "Televisor", "$1200.00", 3, "Agregar"}
        };
        
        for (Object[] producto : productosEjemplo) {
            tableModel.addRow(producto);
        }
        
        mostrarMensaje("Productos de ejemplo cargados. " + productosEjemplo.length + " productos disponibles.");
        actualizarBotonesAgregar();
    }
    
    private void agregarProductoDesdeTabla(int idProducto, int fila) {
        limpiarConsola();
        mostrarMensaje("Agregando producto ID: " + idProducto + " al carrito...");
        
        // Obtener stock actual
        int stockActual = (Integer) tableModel.getValueAt(fila, 5);
        
        if (stockActual <= 0) {
            mostrarError("Producto agotado. No se puede agregar al carrito.");
            return;
        }
        
        // Enviar comando al servidor
        enviarComando("AGREGAR " + idProducto);
        
        // Actualizar stock localmente (se actualizará completamente cuando se recargue)
        tableModel.setValueAt(stockActual - 1, fila, 5);
        
        // Actualizar estado de los botones
        actualizarBotonesAgregar();
    }
    
    private void actualizarBotonesAgregar() {
        // Actualizar el estado de los botones basado en el stock
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int stock = (Integer) tableModel.getValueAt(i, 5);
            if (stock <= 0) {
                tableModel.setValueAt("AGOTADO", i, 6);
            } else {
                tableModel.setValueAt("Agregar", i, 6);
            }
        }
    }
    
    // Clase para renderizar botones en la tabla
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("DejaVu Sans", Font.BOLD, 11));
            setForeground(Color.WHITE);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String texto = (value == null) ? "" : value.toString();
            setText(texto);
            
            if ("AGOTADO".equals(texto)) {
                setBackground(WARNING_COLOR);
                setEnabled(false);
            } else {
                setBackground(SUCCESS_COLOR);
                setEnabled(true);
            }
            
            return this;
        }
    }
    
    // Clase para editar botones en la tabla
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("DejaVu Sans", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            
            if ("AGOTADO".equals(label)) {
                button.setBackground(WARNING_COLOR);
                button.setEnabled(false);
            } else {
                button.setBackground(SUCCESS_COLOR);
                button.setEnabled(true);
            }
            
            isPushed = true;
            currentRow = row;
            return button;
        }
        
        public Object getCellEditorValue() {
            if (isPushed && !"AGOTADO".equals(label)) {
                // Obtener el ID del producto de la primera columna
                int idProducto = Integer.parseInt(tableModel.getValueAt(currentRow, 0).toString());
                agregarProductoDesdeTabla(idProducto, currentRow);
            }
            isPushed = false;
            return label;
        }
        
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    private void limpiarConsola() {
        textArea.setText("");
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
        
        JLabel subtitulo = new JLabel("Sistema de Compras", SwingConstants.CENTER);
        subtitulo.setFont(new Font("DejaVu Sans", Font.PLAIN, 16));
        subtitulo.setForeground(TEXT_COLOR);
        gbc.insets = new Insets(0, 0, 40, 0);
        menuPanel.add(subtitulo, gbc);
        
        gbc.insets = new Insets(8, 0, 8, 0);
        
        // Botones del menú
        String[] opciones = {
            "Buscar Producto",
            "Listar por Tipo", 
            "Agregar al Carrito",
            "Ver Carrito",
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
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver al Menu");
        backBtn.addActionListener(e -> {
            cardLayout.show(mainPanel, "MENU");
            mostrarMensaje("Volviendo al menu principal...");
        });
        
        JLabel titulo = new JLabel("Buscar Producto");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
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
        
        JButton searchBtn = crearBotonModerno("Buscar Producto");
        searchBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        searchBtn.addActionListener(e -> {
            String producto = searchField.getText().trim();
            if (!producto.isEmpty()) {
                limpiarConsola();
                mostrarMensaje("Buscando: " + producto);
                enviarComando("BUSCAR " + producto);
                searchField.setText("");
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
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BACKGROUND_COLOR);
        
        JButton backBtn = crearBotonModerno("Volver al Menu");
        backBtn.addActionListener(e -> {
            cardLayout.show(mainPanel, "MENU");
            mostrarMensaje("Volviendo al menu principal...");
        });
        
        JLabel titulo = new JLabel("Listar por Tipo");
        titulo.setFont(new Font("DejaVu Sans", Font.BOLD, 20));
        titulo.setForeground(TEXT_COLOR);
        
        topPanel.add(backBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(titulo);
        
        JPanel typesPanel = new JPanel();
        typesPanel.setLayout(new BoxLayout(typesPanel, BoxLayout.Y_AXIS));
        typesPanel.setBackground(BACKGROUND_COLOR);
        typesPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JLabel instruccion = new JLabel("Selecciona el tipo de producto:");
        instruccion.setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
        instruccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        
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
            btn.addActionListener(e -> {
                limpiarConsola();
                mostrarMensaje("Listando productos de tipo: " + tipo);
                enviarComando("LISTAR " + tipo);
            });
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
        
        JButton backBtn = crearBotonModerno("Volver al Menu");
        backBtn.addActionListener(e -> {
            cardLayout.show(mainPanel, "MENU");
            mostrarMensaje("Volviendo al menu principal...");
        });
        
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
        
        // Editar carrito
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editPanel.setBackground(BACKGROUND_COLOR);
        
        JButton editBtn = crearBotonModerno("Ver/Editar Carrito");
        editBtn.addActionListener(e -> {
            limpiarConsola();
            mostrarMensaje("Solicitando edicion del carrito...");
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
                limpiarConsola();
                mostrarMensaje("Procesando compra...");
                enviarComando("COMPRAR");
            }
        });
        
        buyPanel.add(buyBtn);
        
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
            
            mostrarMensaje("=== CONEXION ESTABLECIDA ===");
            mostrarMensaje("Conectado al servidor en " + HOST + ":" + PUERTO);
            mostrarMensaje("Listo para realizar operaciones.");
            mostrarMensaje("=============================");
            
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
            mostrarMensaje("COMANDO ENVIADO: " + comando);
            
            // Recibir respuesta
            byte[] buf = new byte[4096];
            int n = in.read(buf);
            if (n > 0) {
                String respuesta = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                mostrarMensaje("RESPUESTA DEL SERVIDOR:");
                mostrarMensaje(respuesta);
                mostrarMensaje("--- Fin de respuesta ---");
                
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
                    mostrarMensaje("RESULTADO DE EDICION:");
                    mostrarMensaje(respuesta);
                    
                    // Recargar productos para actualizar stocks
                    cargarProductos();
                }
            } catch (IOException e) {
                mostrarError("Error al editar carrito: " + e.getMessage());
            }
        }
    }
    
    private void mostrarTicket(String ticket) {
        limpiarConsola();
        mostrarMensaje("=== TICKET DE COMPRA ===");
        mostrarMensaje(ticket);
        mostrarMensaje("=== COMPRA FINALIZADA ===");
        
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
        
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Quieres salir de la aplicación?",
            "Compra Finalizada",
            JOptionPane.YES_NO_OPTION
        );
        
        if (opcion == JOptionPane.YES_OPTION) {
            salir();
        } else {
            // Recargar productos después de la compra
            cargarProductos();
        }
    }
    
    private void mostrarMensaje(String mensaje) {
        textArea.append(mensaje + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
    
    private void mostrarError(String error) {
        textArea.append("ERROR: " + error + "\n");
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
                    limpiarConsola();
                    mostrarMensaje("Modo: Buscar productos por nombre o marca");
                    break;
                case "Listar por Tipo":
                    cardLayout.show(mainPanel, "LISTAR");
                    limpiarConsola();
                    mostrarMensaje("Modo: Listar productos por categoría");
                    break;
                case "Agregar al Carrito":
                    cardLayout.show(mainPanel, "AGREGAR");
                    limpiarConsola();
                    mostrarMensaje("Modo: Agregar productos al carrito");
                    cargarProductos();
                    break;
                case "Ver Carrito":
                    cardLayout.show(mainPanel, "CARRITO");
                    limpiarConsola();
                    mostrarMensaje("Modo: Gestionar carrito de compras");
                    break;
                case "Finalizar Compra":
                    cardLayout.show(mainPanel, "CARRITO");
                    limpiarConsola();
                    mostrarMensaje("Modo: Finalizar compra");
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