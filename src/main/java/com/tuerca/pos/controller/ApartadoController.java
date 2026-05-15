/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ApartadoDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import com.tuerca.pos.view.GestionApartados;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.Ventas;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mannycalderon
 */
public class ApartadoController {
    private MainView mainView;
    private ApartadoDAO apartadoDao;
    private ProductoDAO productoDao;
    private Ventas vista;
     private GestionApartados vistaGestion;
    private int idUsuarioActivo;
    
    private final int COL_CANTIDAD = 0;
    private final int COL_CODIGO = 1;
    private final int COL_DESCRIPCION = 2;
    private final int COL_PRECIO = 3;
    private final int COL_SUBTOTAL = 5;

    public ApartadoController(Ventas vista, GestionApartados vistaGestion, MainView mainView) {
        this.vista = vista;
        this.vistaGestion = vistaGestion;
        this.mainView = mainView;
        
        // Inicializamos los DAOs
        this.apartadoDao = new ApartadoDAO();
        this.productoDao = new ProductoDAO();
        
        // Obtenemos el ID del usuario desde la sesión en MainView
        this.idUsuarioActivo = 2;
        
        configurarEventosVenta();
        configurarEventosGestion();
        
        // Carga inicial de la tabla de gestión
        llenarTablaGestion("", "Activo");
    }

    private void configurarEventosVenta() {
        vista.getBtnApartarProductos().addActionListener(e -> procesarApartado());
    }

    private void procesarApartado() {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
        
        // 1. Validación de carrito vacío
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(vista, "No hay productos para apartar.");
            return;
        }

        double totalCarrito = calcularTotal();
        double sugerido = totalCarrito * 0.10;

        // 2. Recopilación de datos del cliente
        String nombre = JOptionPane.showInputDialog(vista, "Nombre del Cliente (Obligatorio):");
        if (nombre == null || nombre.trim().isEmpty()) return;
        
        String telefono = JOptionPane.showInputDialog(vista, "Teléfono de contacto:");

        // 3. Gestión del abono inicial
        String montoStr = JOptionPane.showInputDialog(vista, 
            "TOTAL A APARTAR: $" + String.format("%.2f", totalCarrito) + 
            "\nMONTO SUGERIDO (10%): $" + String.format("%.2f", sugerido) + 
            "\n\n¿Cuánto dejará de abono inicial?");
        
        if (montoStr == null) return;
        
        try {
            double abonoInput = Double.parseDouble(montoStr);

            if (abonoInput <= 0 || abonoInput > totalCarrito) {
                JOptionPane.showMessageDialog(vista, "Monto de abono inválido.");
                return;
            }

            // 4. Creación de la Cabecera (Apartado)
            Apartado apt = new Apartado();
            apt.setIdUserAccount(idUsuarioActivo);
            apt.setCustomerName(nombre.toUpperCase());
            apt.setCustomerPhone(telefono);
            apt.setTotalAmount(totalCarrito);
            apt.setAdvanceAmount(abonoInput);
            apt.setPendingBalance(totalCarrito - abonoInput);

            // 5. Creación del Detalle (List<ApartadoDetail>)
            List<ApartadoDetail> listaDetalles = new ArrayList<>();
            
            for (int i = 0; i < modelo.getRowCount(); i++) {
                ApartadoDetail det = new ApartadoDetail();
                String codigo = modelo.getValueAt(i, COL_CODIGO).toString();
                
                det.setIdProduct(productoDao.obtenerIdPorCodigo(codigo));
                det.setQuantity(Integer.parseInt(modelo.getValueAt(i, COL_CANTIDAD).toString()));
                det.setUnitPrice(Double.parseDouble(modelo.getValueAt(i, COL_PRECIO).toString()));
                det.setSubtotalDetail(Double.parseDouble(modelo.getValueAt(i, COL_SUBTOTAL).toString()));
                
                listaDetalles.add(det);
            }

            // 6. Ejecución en el DAO
            if (apartadoDao.registrarApartadoCompleto(apt, listaDetalles)) {
                JOptionPane.showMessageDialog(vista, 
                    "APARTADO REGISTRADO CON ÉXITO\n" +
                    "Cliente: " + nombre.toUpperCase() + "\n" +
                    "Saldo Pendiente: $" + String.format("%.2f", apt.getPendingBalance()) + "\n" +
                    "Fecha Límite: 14 días naturales.");
                
                limpiarCarrito();
            } else {
                JOptionPane.showMessageDialog(vista, "Error al registrar el apartado en la base de datos.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Por favor, ingrese un monto numérico válido.");
        }
    }

    private double calcularTotal() {
        double total = 0;
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            total += Double.parseDouble(modelo.getValueAt(i, COL_SUBTOTAL).toString());
        }
        return total;
    }

    private void limpiarCarrito() {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
        modelo.setRowCount(0);
        vista.getLblTotal().setText("$0.00");
        vista.getTxtBusqueda().requestFocus();
    }
    
    // --- LÓGICA DE GESTIÓN (VISTA GESTIÓN APARTADOS) ---
    private void configurarEventosGestion() {
        // Búsqueda por texto
        vistaGestion.getTxtBuscar().addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filtrarGestion();
            }
        });

        // Cambio de estado en ComboBox
        vistaGestion.getCbEstado().addActionListener(e -> filtrarGestion());

        // Configuración de Acciones en la tabla (Icono Editar)
        AccionTableEvent event = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                int folio = Integer.parseInt(vistaGestion.getTablaApartados().getValueAt(row, 0).toString());
                abrirOpcionesApartado(folio);
            }

            @Override
            public void onEliminar(int row) {
                // No hace nada
            }
        };

        // 1. Configuramos el RENDERER (el que se ve en la fila 2 de tu foto)
        AccionesRender miRender = new AccionesRender();
        miRender.getBtnEliminar().setVisible(false); // Ocultamos el bote de basura
        vistaGestion.getTablaApartados().getColumnModel().getColumn(6).setCellRenderer(miRender);

        // 2. Configuramos el EDITOR (el que se activa en la fila 1 de tu foto)
        // 1. Configuramos el RENDERER (Como ya lo tienes)
        AccionesRender renderApt = new AccionesRender();
        renderApt.getBtnEliminar().setVisible(false);
        vistaGestion.getTablaApartados().getColumnModel().getColumn(6).setCellRenderer(renderApt);

        // 2. Configuramos el EDITOR
        AccionesEditar editorApt = new AccionesEditar(event);

        // Aquí el truco: Vamos a buscar el botón dentro del panel que usa el editor
        // Esto funciona si AccionesEditar usa internamente un panel que contiene los botones
        Component c = editorApt.getTableCellEditorComponent(vistaGestion.getTablaApartados(), null, true, 0, 6);
        if (c instanceof AccionesRender) {
            ((AccionesRender) c).getBtnEliminar().setVisible(false);
        }

        vistaGestion.getTablaApartados().getColumnModel().getColumn(6).setCellEditor(editorApt);
        vistaGestion.getTablaApartados().setRowHeight(40);
    }

    private void filtrarGestion() {
        String texto = vistaGestion.getTxtBuscar().getText();
        String estado = vistaGestion.getCbEstado().getSelectedItem().toString();
        llenarTablaGestion(texto, estado);
    }

    public void llenarTablaGestion(String filtro, String estado) {
        DefaultTableModel modelo = (DefaultTableModel) vistaGestion.getTablaApartados().getModel();
        modelo.setRowCount(0);
        
        List<Apartado> lista = apartadoDao.listarApartados(filtro, estado);
        for (Apartado a : lista) {
            modelo.addRow(new Object[]{
                a.getIdBooking(),
                a.getCustomerName(),
                a.getTotalAmount(),
                a.getAdvanceAmount(),
                a.getPendingBalance(),
                a.getExpirationDate(),
                null // Celda de acciones
            });
        }
    }

    private void abrirOpcionesApartado(int folio) {
        // Aquí programaremos el siguiente paso: Abonar o Liquidar
        JOptionPane.showMessageDialog(vistaGestion, "Gestionando Folio: " + folio);
    }
}
