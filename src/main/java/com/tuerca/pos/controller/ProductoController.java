/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Producto;
import com.tuerca.pos.view.GestionProductos;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoProducto;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mannycalderon
 */
public class ProductoController {
    private GestionProductos vistaGestion;
    private NuevoProducto vistaRegistro;
    private MainView mainView;
    private ProductoDAO productoDao;
    
    // EL CONSTRUCTOR: Es el corazón de la conexión
    public ProductoController(GestionProductos vistaGestion, NuevoProducto vistaRegistro, MainView mainView) {
        this.vistaGestion = vistaGestion;
        this.vistaRegistro = vistaRegistro;
        this.mainView = mainView;
        this.productoDao = new ProductoDAO();

        // 1. Cargamos los datos iniciales al arrancar
        cargarCombos();
        configurarTabla();
        cargarTablaProductos();
        
        // 2. Aquí conectaremos los botones y eventos más adelante
        initTablaAcciones();
        initListeners();
    }
    
    private void initTablaAcciones() {
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                // Obtenemos el ID de la columna 0
                int id = (int) vistaGestion.getTablaProductos().getValueAt(row, 0);
                // prepararEdicion(id);
            }

            @Override
            public void onEliminar(int row) {
                int id = (int) vistaGestion.getTablaProductos().getValueAt(row, 0);
                // confirmarEliminacion(id, row);
            }
        };

        // La columna de acciones ahora es la 6
        int columnaAcciones = 6; 
        vistaGestion.getTablaProductos().getColumnModel().getColumn(columnaAcciones).setCellRenderer(new AccionesRender());
        vistaGestion.getTablaProductos().getColumnModel().getColumn(columnaAcciones).setCellEditor(new AccionesEditar(evento));

        // OCULTAR LA COLUMNA 0 (ID)
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setMinWidth(0);
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setMaxWidth(0);
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setPreferredWidth(0);

        // Ajustar el cursor (ahora en la col 6)
        vistaGestion.getTablaProductos().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaProductos().columnAtPoint(e.getPoint());
                if (col == 6) {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
        // Tip: Aumenta un poco el alto de las filas para que los botones luzcan mejor
        vistaGestion.getTablaProductos().setRowHeight(40);
    }

    private void initListeners() {
        // Acción para el botón registrar
        vistaRegistro.getBtnRegistrar().addActionListener(e -> registrarProducto());

        // Acción para el botón cancelar o volver 
        vistaRegistro.getBtnBack().addActionListener(e -> {
            vistaRegistro.limpiarFormulario();
            mainView.showView("gestionProductos");
        });
        
        vistaRegistro.getBtnCancelar().addActionListener(e -> {
            vistaRegistro.limpiarFormulario();
            mainView.showView("gestionProductos");
        });
        
        // AGREGAMOS EL LISTENER DEL MOUSE
        vistaGestion.getTablaProductos().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaProductos().columnAtPoint(e.getPoint());
                if (col == 5) {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }
    
    private void configurarTabla() {
        javax.swing.JTable tabla = vistaGestion.getTablaProductos();

        // 0. ID (Sigue oculto)
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);

        // 1. Código (Más estrecho, no necesita tanto aire)
        tabla.getColumnModel().getColumn(1).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(1).setMinWidth(80);

        // 2. Descripción (ESTA DEBE SER LA MÁS ANCHA)
        // Al no ponerle MaxWidth, tomará todo el espacio sobrante
        tabla.getColumnModel().getColumn(2).setPreferredWidth(450); 

        // 3. Emprendimiento
        tabla.getColumnModel().getColumn(3).setPreferredWidth(150);

        // 4. Precio
        tabla.getColumnModel().getColumn(4).setPreferredWidth(100);

        // 5. Stock (Pequeño y compacto)
        tabla.getColumnModel().getColumn(5).setPreferredWidth(80);

        // 6. Acciones (Suficiente espacio para que no se corten los botones)
        tabla.getColumnModel().getColumn(6).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(6).setMinWidth(120);
    }
    
    public void cargarTablaProductos() {
        DefaultTableModel modelo = (DefaultTableModel) vistaGestion.getTablaProductos().getModel();
        modelo.setRowCount(0);

        List<Producto> lista = productoDao.listarTodos();

        for (Producto p : lista) {
            modelo.addRow(new Object[]{
                p.getIdProduct(),        // 0. ID
                p.getFullProductCode(),   // 1. Código
                p.getProductDescription(),// 2. Descripción
                p.getBrandName(),         // 3. Emprendimiento
                "$" + String.format("%.2f", p.getCurrentPrice()), // 4. Precio
                p.getCurrentStock(),      // 5. Stock
                ""                        // 6. Acciones
            });
        }
        initTablaAcciones();
    }
    
    
    public void cargarCombos() {
        EmprendedorDAO empDao = new EmprendedorDAO();
        List<Emprendedor> lista = empDao.listarNombresYId();

        // Limpiar y llenar combo de la tabla (filtro)
        vistaGestion.getCbFiltroEmprendedor().removeAllItems();
        vistaGestion.getCbFiltroEmprendedor().addItem("--- Todos ---");
        
        // Limpiar y llenar combo del registro (obligatorio)
        vistaRegistro.getCbEmprendedor().removeAllItems();
        vistaRegistro.getCbEmprendedor().addItem("Selecciona un emprendedor...");

        for (Emprendedor emp : lista) {
            vistaGestion.getCbFiltroEmprendedor().addItem(emp);
            vistaRegistro.getCbEmprendedor().addItem(emp);
        }
    }
    
    public int getSelectedEntrepreneurId() {
        Object seleccion = vistaGestion.getCbFiltroEmprendedor().getSelectedItem();

        if (seleccion instanceof Emprendedor) {
            return ((Emprendedor) seleccion).getId();
        }
        return 0; // Si seleccionó "--- Todos ---"
    }
    
    private void registrarProducto() {
        // 1. Validar selección del Emprendedor
        Object seleccion = vistaRegistro.getCbEmprendedor().getSelectedItem();
        if (!(seleccion instanceof Emprendedor)) {
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, selecciona un emprendedor válido.");
            return;
        }
        Emprendedor emp = (Emprendedor) seleccion;

        // 2. Capturar datos de texto
        String codigo = vistaRegistro.getCodigoField().toUpperCase();
        String desc = vistaRegistro.getDescripcionField().toUpperCase();
        String depto = vistaRegistro.getDepartamentoField().toUpperCase();
        String precioStr = vistaRegistro.getPrecioField();
        String stockStr = vistaRegistro.getStockField();

        // 3. Validaciones de campos vacíos
        if (codigo.isEmpty() || desc.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty()) {
            JOptionPane.showMessageDialog(vistaRegistro, "Completa todos los campos obligatorios.");
            return;
        }

        try {
            // 4. Empaquetar datos y conversión numérica
            Producto p = new Producto();
            p.setIdEntrepreneur(emp.getId());
            p.setFullProductCode(codigo);
            p.setProductDescription(desc);
            p.setDepartment(depto);
            p.setCurrentPrice(Double.parseDouble(precioStr));
            p.setCurrentStock(Integer.parseInt(stockStr));
            p.setMinStockAlert(1);

            // 5. Ejecutar registro
            if (productoDao.registrar(p)) {
                JOptionPane.showMessageDialog(vistaRegistro, "¡Producto " + codigo + " registrado!");
                // El controlador le da la orden a la vista
                vistaRegistro.limpiarFormulario();
                cargarTablaProductos(); // Refrescamos la tabla de gestión
                mainView.showView("products"); // Regresamos a la tabla
            } else {
                JOptionPane.showMessageDialog(vistaRegistro, "Error al guardar. Verifica que el código no esté duplicado.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaRegistro, "El precio y el stock deben ser valores numéricos.");
        }
    }
    

}
