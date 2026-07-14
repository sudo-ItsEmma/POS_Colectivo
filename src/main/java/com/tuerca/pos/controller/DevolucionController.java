package com.tuerca.pos.controller;

import com.tuerca.pos.dao.DevolucionDAO;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.GestionDevoluciones;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import com.tuerca.pos.view.components.AutorizacionAdminDialog;
import com.tuerca.pos.view.components.BusquedaConDebounce;

import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Controlador de Gestión de Devoluciones (FN.5). La devolución opera por
 * línea de venta ({@code SaleDetail}), y solo el Administrador puede
 * autorizarla: si la sesión activa no es de Admin, se pide usuario/
 * contraseña de un Administrador antes de procesarla (patrón "autorización
 * de gerente"), reutilizando {@link EmpleadoDAO#autenticar}.
 */
public class DevolucionController {

    private final GestionDevoluciones vista;
    private final MainView mainView;
    private final DevolucionDAO devolucionDao;
    private final EmpleadoDAO empleadoDao;

    public DevolucionController(GestionDevoluciones vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.devolucionDao = new DevolucionDAO();
        this.empleadoDao = new EmpleadoDAO();

        vista.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                cargarTabla("");
            }
        });

        BusquedaConDebounce.aplicar(vista.getTxtBuscar(), 300,
                () -> cargarTabla(vista.getTxtBuscar().getText().trim()));

        initTablaAcciones();
        cargarTabla("");
    }

    private void initTablaAcciones() {
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                int idSale = (int) vista.getTablaVentas().getValueAt(row, 0);
                abrirDetalleVenta(idSale);
            }

            @Override
            public void onEliminar(int row) {
                // No se usa en Devoluciones
            }
        };

        AccionesRender render = new AccionesRender();
        render.getBtnEliminar().setVisible(false);
        vista.getTablaVentas().getColumnModel().getColumn(5).setCellRenderer(render);
        vista.getTablaVentas().getColumnModel().getColumn(5).setCellEditor(new AccionesEditar(evento) {
            @Override
            public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                java.awt.Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (c instanceof AccionesRender panel) {
                    panel.getBtnEliminar().setVisible(false);
                }
                return c;
            }
        });
        vista.getTablaVentas().setRowHeight(40);
    }

    private void cargarTabla(String filtro) {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVentas().getModel();
        modelo.setRowCount(0);

        for (Object[] fila : devolucionDao.buscarVentas(filtro)) {
            modelo.addRow(new Object[]{
                fila[0],
                "$" + String.format("%.2f", (double) fila[1]),
                fila[2],
                fila[3],
                fila[4],
                ""
            });
        }
        initTablaAcciones();
    }

    private void abrirDetalleVenta(int idSale) {
        JDialog dialogo = new JDialog(mainView, "Detalle de Venta #" + idSale + " - Devoluciones", true);
        dialogo.setSize(800, 400);
        dialogo.setLocationRelativeTo(mainView);

        JTable tablaDetalle = new JTable();
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[][]{},
                new String[]{"idSaleDetail", "Código", "Descripción", "Cantidad", "Precio U.", "Subtotal", "Estado", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
        tablaDetalle.setModel(modelo);
        // Ocultamos la columna técnica idSaleDetail (columna 0), igual que se hace con el ID en otras tablas
        tablaDetalle.getColumnModel().getColumn(0).setMinWidth(0);
        tablaDetalle.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaDetalle.getColumnModel().getColumn(0).setPreferredWidth(0);
        tablaDetalle.setRowHeight(40);

        cargarDetalle(idSale, modelo, tablaDetalle);

        dialogo.add(new JScrollPane(tablaDetalle));
        dialogo.setVisible(true);

        // Al cerrar el diálogo, refrescamos la tabla principal (la venta pudo pasar a 'Devuelta')
        cargarTabla(vista.getTxtBuscar().getText().trim());
    }

    private void cargarDetalle(int idSale, DefaultTableModel modelo, JTable tablaDetalle) {
        modelo.setRowCount(0);
        List<Object[]> detalles = devolucionDao.obtenerDetallesConEstado(idSale);

        for (Object[] d : detalles) {
            boolean yaDevuelto = (boolean) d[6];
            modelo.addRow(new Object[]{
                d[0], d[1], d[2], d[3],
                "$" + String.format("%.2f", (double) d[4]),
                "$" + String.format("%.2f", (double) d[5]),
                yaDevuelto ? "Devuelto" : "Activo",
                ""
            });
        }

        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                int idSaleDetail = (int) modelo.getValueAt(row, 0);
                String codigo = (String) modelo.getValueAt(row, 1);
                double subtotal = (double) detalles.get(row)[5];
                procesarDevolucion(idSaleDetail, codigo, subtotal, () -> cargarDetalle(idSale, modelo, tablaDetalle));
            }

            @Override
            public void onEliminar(int row) {
                // No se usa
            }
        };

        tablaDetalle.getColumnModel().getColumn(7).setCellRenderer(new AccionesRender() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                getBtnEliminar().setVisible(false);
                boolean yaDevuelto = (boolean) detalles.get(row)[6];
                getBtnEditar().setVisible(!yaDevuelto);
                return this;
            }
        });
        tablaDetalle.getColumnModel().getColumn(7).setCellEditor(new AccionesEditar(evento) {
            @Override
            public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                java.awt.Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (c instanceof AccionesRender panel) {
                    panel.getBtnEliminar().setVisible(false);
                    boolean yaDevuelto = (boolean) detalles.get(row)[6];
                    panel.getBtnEditar().setVisible(!yaDevuelto);
                }
                return c;
            }
        });
    }

    private void procesarDevolucion(int idSaleDetail, String codigoProducto, double montoReembolso, Runnable alTerminar) {
        int confirmar = JOptionPane.showConfirmDialog(
            vista,
            "¿Devolver el producto " + codigoProducto + "?\n\n" +
            "Monto a reembolsar: $" + String.format("%.2f", montoReembolso) + "\n" +
            "El stock se devolverá al inventario.\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Devolución",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirmar != JOptionPane.YES_OPTION) return;

        String motivo = JOptionPane.showInputDialog(vista, "Motivo de la devolución:", "Motivo", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null || motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "La devolución requiere un motivo. Operación cancelada.");
            return;
        }

        int idUserAccountAutoriza;
        if (Sesion.getInstancia().isAdmin()) {
            idUserAccountAutoriza = Sesion.getInstancia().getIdUserAccount();
        } else {
            Integer idAdmin = AutorizacionAdminDialog.solicitar(vista, empleadoDao);
            if (idAdmin == null) return; // Canceló o las credenciales no eran de un Admin
            idUserAccountAutoriza = idAdmin;
        }

        if (devolucionDao.procesarDevolucion(idSaleDetail, idUserAccountAutoriza, motivo.trim(), montoReembolso)) {
            JOptionPane.showMessageDialog(vista, "Devolución registrada. El stock fue devuelto al inventario.");
            alTerminar.run();
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo procesar la devolución.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
