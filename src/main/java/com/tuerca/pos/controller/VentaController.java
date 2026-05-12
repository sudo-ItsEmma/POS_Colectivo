/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Producto;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.Ventas; // Ajusta al nombre real de tu clase de vista
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mannycalderon
 */
public class VentaController {

    private final Ventas vista;
    private final MainView mainView;
    private final ProductoDAO productoDao;
    private JPopupMenu menuSugerencias;
    private JList<Producto> listaSugerencias;

    // Constantes para evitar números mágicos en las columnas de la tabla
    private final int COL_CANTIDAD = 0;
    private final int COL_CODIGO = 1;
    private final int COL_PRECIO = 3;
    private final int COL_DSCTO_PER = 4; // Nueva columna
    private final int COL_SUBTOTAL = 5;  // Se recorrió
    private final int COL_ACCION = 6;    // Se recorrió

    public VentaController(Ventas vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.productoDao = new ProductoDAO();
        
        prepararModeloTabla();
        configurarAccionesTabla();
        configurarMenuFlotante();
        initListeners();
    }

    private void initListeners() {
        // --- Lógica de la Tabla ---
        vista.getTablaVenta().getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int col = e.getColumn();
                if (col == COL_CANTIDAD || col == COL_DSCTO_PER) {
                    actualizarFilaDinamicamente(e.getFirstRow());
                }
            }
        });

        vista.getTablaVenta().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = vista.getTablaVenta().rowAtPoint(e.getPoint());
                int col = vista.getTablaVenta().columnAtPoint(e.getPoint());
                if (col == COL_ACCION && fila != -1) {
                    ((DefaultTableModel) vista.getTablaVenta().getModel()).removeRow(fila);
                    recalcularTodo();
                }
            }
        });
        
        // quitar producto del carrito
        vista.getTablaVenta().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = vista.getTablaVenta().rowAtPoint(e.getPoint());
                int col = vista.getTablaVenta().columnAtPoint(e.getPoint());

                // Verificamos que se haya hecho clic en la columna de eliminar (índice 5)
                if (col == COL_ACCION && fila != -1) {
                    // Obtenemos el nombre del producto para una alerta más personalizada
                    String producto = vista.getTablaVenta().getValueAt(fila, 2).toString();

                    int confirmar = JOptionPane.showConfirmDialog(
                        vista, 
                        "¿Estás seguro de quitar '" + producto + "' del carrito?", 
                        "Confirmar eliminación", 
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );

                    if (confirmar == JOptionPane.YES_OPTION) {
                        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
                        modelo.removeRow(fila);
                        recalcularTodo(); // Esto actualiza el label del total
                    }
                }
            }
        });
        
        vista.getTablaVenta().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = vista.getTablaVenta().columnAtPoint(e.getPoint());
                if (col == COL_ACCION) {
                    vista.getTablaVenta().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vista.getTablaVenta().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });

        // --- Lógica de Búsqueda ---
        vista.getTxtBusqueda().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarSugerencias(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarSugerencias(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarSugerencias(); }
        });

        listaSugerencias.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    seleccionarProductoDesdeLista();
                }
            }
        });

        // --- Botones de Acción ---
        vista.getBtnCancelar().addActionListener(e -> cancelarVenta());
        vista.getBtnCobrar().addActionListener(e -> procesarCobro());
    }

    // --- Métodos de Configuración ---
    
    private void configurarMenuFlotante() {
        menuSugerencias = new JPopupMenu();
        listaSugerencias = new JList<>();
        listaSugerencias.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String texto = String.format("[%s] %s - %s", 
                    value.getFullProductCode(), value.getProductDescription(), value.getBrandName());
            JLabel label = new JLabel(texto);
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(new Color(0, 120, 215));
                label.setForeground(Color.WHITE);
            }
            return label;
        });

        JScrollPane scroll = new JScrollPane(listaSugerencias);
        scroll.setBorder(null);
         scroll.setPreferredSize(new java.awt.Dimension(vista.getTxtBusqueda().getWidth(), 200));
        menuSugerencias.add(scroll);
    }

    public void prepararModeloTabla() {
        // Definimos el modelo. El '0' al final indica que inicia con cero filas.
        DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Cant.", "Código", "Descripción", "Precio U.", "Descuento" ,"Subtotal", "Acción"}, 
            0 
        ) {
            // Mantenemos tus reglas de edición
            @Override 
            public boolean isCellEditable(int r, int c) { 
                return c == COL_CANTIDAD || c == 4 || c == COL_ACCION;
            }

            // Mantenemos los tipos de dato para cálculos precisos
            @Override 
            public Class<?> getColumnClass(int c) {
                if (c == COL_CANTIDAD) return Integer.class;
                if (c == COL_PRECIO || c == COL_SUBTOTAL) return Double.class;
                return Object.class;
            }
        };

        // Aplicamos el modelo a la tabla
        vista.getTablaVenta().setModel(modelo);

        // Ajustamos anchos de columnas
        vista.getTablaVenta().getColumnModel().getColumn(COL_CANTIDAD).setPreferredWidth(40);
        vista.getTablaVenta().getColumnModel().getColumn(COL_CODIGO).setPreferredWidth(80);
        vista.getTablaVenta().getColumnModel().getColumn(2).setPreferredWidth(300); // Descripción más ancha
        vista.getTablaVenta().getColumnModel().getColumn(COL_PRECIO).setPreferredWidth(90);
        vista.getTablaVenta().getColumnModel().getColumn(COL_DSCTO_PER).setPreferredWidth(90);
        vista.getTablaVenta().getColumnModel().getColumn(COL_SUBTOTAL).setPreferredWidth(90);
        vista.getTablaVenta().getColumnModel().getColumn(COL_ACCION).setPreferredWidth(100);

        // IMPORTANTE: Para que los botones se vean bien, aumentamos el alto de las filas
        vista.getTablaVenta().setRowHeight(40);

        // Quitamos el foco de las celdas para que no se vea el recuadro punteado al hacer clic
        vista.getTablaVenta().setFocusable(false);
    }
    
    private void configurarAccionesTabla() {
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                // No se usa en ventas, pero la interfaz lo pide
            }

            @Override
            public void onEliminar(int row) {
                // Obtenemos descripción de la columna 2
                String producto = vista.getTablaVenta().getValueAt(row, 2).toString();
                int confirmar = JOptionPane.showConfirmDialog(
                    vista, "¿Quitar '" + producto + "' del carrito?", 
                    "Confirmar", JOptionPane.YES_NO_OPTION
                );

                if (confirmar == JOptionPane.YES_OPTION) {
                    if (vista.getTablaVenta().isEditing()) {
                        vista.getTablaVenta().getCellEditor().stopCellEditing();
                    }
                    ((DefaultTableModel) vista.getTablaVenta().getModel()).removeRow(row);
                    recalcularTodo();
                }
            }
        };

        // 1. Configuramos el RENDERER y ocultamos el botón de editar
        vista.getTablaVenta().getColumnModel().getColumn(COL_ACCION).setCellRenderer(new AccionesRender() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // "this" ya es el JPanel (AccionesRender)
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.getBtnEditar().setVisible(false); // Ocultamos el botón de editar
                return this;
            }
        });

        // 2. Configuramos el EDITOR y ocultamos el botón de editar
        vista.getTablaVenta().getColumnModel().getColumn(COL_ACCION).setCellEditor(new AccionesEditar(evento) {
            @Override
            public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                // AccionesEditar usa internamente un panel tipo AccionesRender
                // Necesitamos acceder a ese panel para ocultar el botón
                java.awt.Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (c instanceof AccionesRender panel) {
                    panel.getBtnEditar().setVisible(false);
                }
                return c;
            }
        });

        vista.getTablaVenta().setRowHeight(40);
    }
    
    // --- Lógica de Negocio ---

    private void filtrarSugerencias() {
        String texto = vista.getTxtBusqueda().getText().trim();
        if (texto.length() < 2) {
            menuSugerencias.setVisible(false);
            return;
        }

        List<Producto> lista = productoDao.buscarPorCriterio(texto);
        if (!lista.isEmpty()) {
            DefaultListModel<Producto> model = new DefaultListModel<>();
            lista.forEach(model::addElement);
            listaSugerencias.setModel(model);
            menuSugerencias.show(vista.getTxtBusqueda(), 0, vista.getTxtBusqueda().getHeight());
            vista.getTxtBusqueda().requestFocus();
        } else {
            menuSugerencias.setVisible(false);
        }
    }

    private void seleccionarProductoDesdeLista() {
        Producto p = listaSugerencias.getSelectedValue();
        if (p != null) {
            agregarProductoAlCarrito(p);
            menuSugerencias.setVisible(false);
            vista.getTxtBusqueda().setText("");
        }
    }

    private void agregarProductoAlCarrito(Producto p) {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();

        double precioU = p.getCurrentPrice();

        // Por defecto, al agregar un producto nuevo, el descuento es 0
        double porcentajeInicial = 0.0; 

        // El subtotal inicial será simplemente el precio unitario (cantidad 1 * precioU)
        double subtotalInicial = precioU;

        modelo.addRow(new Object[]{
            1,                         // COL_CANTIDAD (0)
            p.getFullProductCode(),    // Col Código (1)
            p.getProductDescription(), // Col Descripción (2)
            precioU,                   // COL_PRECIO (3)
            porcentajeInicial,         // COL_DSCTO_PER (4) -> Nueva columna editable
            subtotalInicial,           // COL_SUBTOTAL (5)
            ""                         // COL_ACCION (6) -> Botón eliminar
        });

        // Refrescamos el total de la etiqueta inferior
        recalcularTodo();

        // Opcional: Desplazar el scroll al final si hay muchos productos
        vista.getTablaVenta().scrollRectToVisible(
            vista.getTablaVenta().getCellRect(modelo.getRowCount() - 1, 0, true)
        );
    }

    private void actualizarFilaDinamicamente(int fila) {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();

        // 1. Validación de seguridad para evitar el ArrayIndexOutOfBoundsException
        if (fila < 0 || fila >= modelo.getRowCount()) return;

        try {
            // 2. Extraer valores de las celdas usando las constantes de columna
            // Nota: Convertimos a String y luego parseamos para ser más robustos con los tipos de Swing
            int cantidad = Integer.parseInt(modelo.getValueAt(fila, COL_CANTIDAD).toString());
            double precioU = Double.parseDouble(modelo.getValueAt(fila, COL_PRECIO).toString());

            // Obtenemos el descuento específico de esta fila (Columna 4)
            Object valDscto = modelo.getValueAt(fila, COL_DSCTO_PER);
            double porcentaje = (valDscto == null) ? 0.0 : Double.parseDouble(valDscto.toString());

            // 3. Lógica matemática
            double subtotalBase = cantidad * precioU;
            double montoDescuento = subtotalBase * (porcentaje / 100);
            double nuevoSubtotal = subtotalBase - montoDescuento;

            // 4. Actualización visual segura
            SwingUtilities.invokeLater(() -> {
                // Verificamos de nuevo que la fila no haya sido eliminada mientras calculábamos
                if (fila < modelo.getRowCount()) {
                    modelo.setValueAt(nuevoSubtotal, fila, COL_SUBTOTAL);
                    recalcularTodo(); // Actualiza el total general de la venta
                }
            });
            
            vista.getTxtBusqueda().requestFocus();

        } catch (NumberFormatException e) {
            System.err.println("Error de formato en la fila " + fila + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado al actualizar fila: " + e.getMessage());
        }
    }
    
    

    private void recalcularTodo() {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
        double total = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            total += (double) modelo.getValueAt(i, COL_SUBTOTAL);
        }
        vista.getLblTotal().setText(String.format("$%.2f", total));
    }

    private void cancelarVenta() {
        if (JOptionPane.showConfirmDialog(vista, "¿Cancelar venta?", "Confirmar", 0) == 0) {
            limpiarModulo();
        }
    }

    private void limpiarModulo() {
        vista.getTxtBusqueda().setText("");
        ((DefaultTableModel) vista.getTablaVenta().getModel()).setRowCount(0);
        vista.getLblTotal().setText("$0.00");
        vista.getCbMetodoPago().setSelectedIndex(0);
    }

    private void procesarCobro() {
        if (vista.getTablaVenta().getRowCount() == 0) {
            JOptionPane.showMessageDialog(vista, "El carrito está vacío");
            return;
        }
        // Próximo paso: Abrir ventana de cobro y guardar en DB
        JOptionPane.showMessageDialog(vista, "Iniciando proceso de cobro...");
    }
    
}
