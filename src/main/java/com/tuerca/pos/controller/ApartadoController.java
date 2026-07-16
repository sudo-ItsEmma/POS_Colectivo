/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ApartadoDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.GestionApartados;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.Ventas;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.awt.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

        BigDecimal totalCarrito = calcularTotal();
        BigDecimal sugerido = totalCarrito.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        // 2. Recopilación unificada de datos del cliente
        JTextField txtNombre = new JTextField(20);
        JTextField txtTelefono = new JTextField(15);

        // Agregamos una propiedad de FlatLaf si quieres que muestre un texto de ayuda de fondo (Placeholder)
        txtNombre.putClientProperty("JTextField.placeholderText", "Nombre y Apellido");
        txtTelefono.putClientProperty("JTextField.placeholderText", "Ej. 7771234567");

        // Creamos un panel con un diseño básico (puedes usar GridLayout o GridBagLayout si quieres más orden)
        JPanel panelCliente = new JPanel(new java.awt.GridLayout(4, 1, 5, 5));
        panelCliente.add(new JLabel("Nombre del Cliente:"));
        panelCliente.add(txtNombre);
        panelCliente.add(new JLabel("Teléfono de contacto:"));
        panelCliente.add(txtTelefono);

        int resultado = JOptionPane.showConfirmDialog(
            vista, 
            panelCliente, 
            "Datos del Cliente - Aura POS", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE
        );

        // Si el usuario da clic en Cancelar o cierra la ventana, detenemos el proceso
        if (resultado != JOptionPane.OK_OPTION) return;

        String nombre = txtNombre.getText();
        String telefono = txtTelefono.getText();

        // Validación del campo obligatorio
        if (nombre == null || nombre.trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "El nombre del cliente es obligatorio para registrar el apartado.", "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Gestión del abono inicial
        String montoStr = JOptionPane.showInputDialog(vista, 
            "TOTAL A APARTAR: $" + String.format("%.2f", totalCarrito) + 
            "\nMONTO SUGERIDO (10%): $" + String.format("%.2f", sugerido) + 
            "\n\n¿Cuánto dejará de abono inicial?");
        
        if (montoStr == null) return;
        
        try {
            BigDecimal abonoInput = new BigDecimal(montoStr);

            if (abonoInput.compareTo(BigDecimal.ZERO) <= 0 || abonoInput.compareTo(totalCarrito) > 0) {
                JOptionPane.showMessageDialog(vista, "Monto de abono inválido.");
                return;
            }

            // 4. Creación de la Cabecera (Apartado)
            Apartado apt = new Apartado();
            apt.setIdUserAccount(Sesion.getInstancia().getIdUserAccount());
            apt.setCustomerName(nombre.toUpperCase());
            apt.setCustomerPhone(telefono);
            apt.setTotalAmount(totalCarrito);
            apt.setAdvanceAmount(abonoInput);
            apt.setPendingBalance(totalCarrito.subtract(abonoInput));
            apt.setBookingStatus("Activo");

            // 5. Creación del Detalle (List<ApartadoDetail>)
            List<ApartadoDetail> listaDetalles = new ArrayList<>();

            for (int i = 0; i < modelo.getRowCount(); i++) {
                ApartadoDetail det = new ApartadoDetail();
                String codigo = modelo.getValueAt(i, COL_CODIGO).toString();

                det.setIdProduct(productoDao.obtenerIdPorCodigo(codigo));
                det.setQuantity(Integer.parseInt(modelo.getValueAt(i, COL_CANTIDAD).toString()));
                det.setUnitPrice(new BigDecimal(modelo.getValueAt(i, COL_PRECIO).toString()));
                det.setSubtotalDetail(new BigDecimal(modelo.getValueAt(i, COL_SUBTOTAL).toString()));

                listaDetalles.add(det);
            }

            // 6. Ejecución en el DAO (puede lanzar SQLException si algún producto no tiene stock)
            if (apartadoDao.registrarApartadoCompleto(apt, listaDetalles)) {
                JOptionPane.showMessageDialog(vista,
                    "APARTADO REGISTRADO CON ÉXITO\n" +
                    "Cliente: " + nombre.toUpperCase() + "\n" +
                    "Saldo Pendiente: $" + String.format("%.2f", apt.getPendingBalance()) + "\n" +
                    "Fecha Límite: 14 días naturales.");
                limpiarCarrito();
                filtrarGestion();
            } else {
                JOptionPane.showMessageDialog(vista, "Error al registrar el apartado en la base de datos.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Por favor, ingrese un monto numérico válido.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(vista,
                "No se pudo registrar el apartado:\n" + e.getMessage(),
                "Error de Inventario / Sistema", JOptionPane.WARNING_MESSAGE);
        }
    }

    private BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVenta().getModel();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            total = total.add(new BigDecimal(modelo.getValueAt(i, COL_SUBTOTAL).toString()));
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
        String estadoSeleccionado = vistaGestion.getCbEstado().getSelectedItem().toString();

        // Homologamos lo que ve el usuario con lo que entiende MariaDB
        if (estadoSeleccionado.equalsIgnoreCase("Pendientes") || estadoSeleccionado.equalsIgnoreCase("Activos")) {
            estadoSeleccionado = "Activo";
        } else if (estadoSeleccionado.equalsIgnoreCase("Liquidados")) {
            estadoSeleccionado = "Liquidado";
        } else if (estadoSeleccionado.equalsIgnoreCase("Cancelados")) {
            estadoSeleccionado = "Cancelado";
        } else if (estadoSeleccionado.equalsIgnoreCase("Vencidos")) {
            estadoSeleccionado = "Vencido";
        }

        llenarTablaGestion(texto, estadoSeleccionado);
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
        Apartado apt = apartadoDao.obtenerApartadoPorId(folio);
        // Ahora recibimos la lista de objetos con la información combinada
        List<Object[]> detalles = apartadoDao.obtenerResumenDetallesPorFolio(folio);

        if (apt == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("--- RESUMEN DE APARTADO #").append(folio).append(" ---\n");
        sb.append("Cliente: ").append(apt.getCustomerName()).append("\n\n");

        // Encabezados de la tabla visual
        sb.append(String.format("%-5s | %-12s | %-20s\n", "CANT", "CÓDIGO", "PRODUCTO"));
        sb.append("------------------------------------------\n");

        for (Object[] d : detalles) {
            int cant = (int) d[0];
            String codigo = (String) d[1];
            String desc = (String) d[2];

            // Acortamos la descripción si es muy larga para que no rompa el diseño del JOptionPane
            if (desc.length() > 20) desc = desc.substring(0, 17) + "...";

            sb.append(String.format("%-5d | %-12s | %-20s\n", cant, codigo, desc));
        }

        sb.append("------------------------------------------\n");
        sb.append("TOTAL APARTADO:  $").append(String.format("%.2f", apt.getTotalAmount())).append("\n");
        sb.append("ABONADO:         $").append(String.format("%.2f", apt.getAdvanceAmount())).append("\n");
        sb.append("SALDO RESTANTE:  $").append(String.format("%.2f", apt.getPendingBalance())).append("\n\n");
        sb.append("¿Desea registrar un pago (abono o liquidación) sobre este apartado?");

        String[] opciones = {"Registrar Pago", "Cancelar Apartado", "Cerrar"};

        int seleccion = JOptionPane.showOptionDialog(
            vistaGestion,
            sb.toString(),
            "Gestión de Liquidación",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE, // Cambiamos a PLAIN para que la fuente monoespaciada se vea mejor
            null, opciones, opciones[0]
        );

        // Lógica de respuesta
        if (seleccion == 0) {
            // Un solo flujo: según el monto ingresado, se decide abono parcial o liquidación total
            procesarPago(apt, detalles);
        } else if (seleccion == 1) {
            // Cancelar el apartado y devolver el stock reservado
            procesarCancelacion(apt);
        }
    }

    private void procesarCancelacion(Apartado apt) {
        int confirmar = JOptionPane.showConfirmDialog(
            vistaGestion,
            "¿Cancelar el apartado #" + apt.getIdBooking() + " de " + apt.getCustomerName() + "?\n\n" +
            "El stock reservado de sus productos se devolverá al inventario.\n" +
            "El anticipo/abonos ya cobrados no se reembolsan automáticamente por el sistema.\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Cancelación - Folio " + apt.getIdBooking(),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) return;

        if (apartadoDao.cancelarApartado(apt.getIdBooking())) {
            JOptionPane.showMessageDialog(vistaGestion, "Apartado cancelado. El stock fue devuelto al inventario.");
            filtrarGestion();
        } else {
            JOptionPane.showMessageDialog(vistaGestion,
                "No se pudo cancelar el apartado (puede que ya no esté Activo).",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Flujo único de pago: el monto ingresado decide si es abono parcial o liquidación
    // total (si es igual o mayor al saldo pendiente) — ya no hay dos botones separados
    // para "Registrar Abono" y "Liquidar y Finalizar". El método de pago siempre se
    // pregunta, sea abono o liquidación (antes solo se preguntaba al liquidar).
    private void procesarPago(Apartado apt, List<Object[]> detalles) {
        BigDecimal saldoPendiente = apt.getPendingBalance();

        String input = JOptionPane.showInputDialog(vistaGestion,
            "SALDO PENDIENTE: $" + String.format("%.2f", saldoPendiente) +
            "\n\nIngrese el monto a pagar (si cubre el saldo completo, el apartado se liquidará):",
            "Registrar Pago - Folio " + apt.getIdBooking(),
            JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal monto = new BigDecimal(input);

            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(vistaGestion, "El monto debe ser mayor a cero.");
                return;
            }

            String[] metodos = {"Efectivo", "Transferencia"};
            String metodoPago = (String) JOptionPane.showInputDialog(
                vistaGestion,
                "Seleccione el método de pago:",
                "Método de Pago",
                JOptionPane.QUESTION_MESSAGE,
                null, metodos, metodos[0]
            );
            if (metodoPago == null) return;

            if (monto.compareTo(saldoPendiente) >= 0) {
                BigDecimal cambio = monto.subtract(saldoPendiente);
                String mensajeConfirmacion = cambio.compareTo(BigDecimal.ZERO) > 0
                    ? "El monto ingresado ($" + String.format("%.2f", monto) + ") supera el saldo pendiente ($" + String.format("%.2f", saldoPendiente) + ").\n\n" +
                      "CAMBIO PARA EL CLIENTE: $" + String.format("%.2f", cambio) + "\n" +
                      "¿Desea proceder con la LIQUIDACIÓN TOTAL del apartado?"
                    : "¿Confirmas el cobro de $" + String.format("%.2f", saldoPendiente) + " para liquidar el apartado?";

                int confirmar = JOptionPane.showConfirmDialog(
                    vistaGestion,
                    mensajeConfirmacion,
                    "Confirmar Liquidación - Folio " + apt.getIdBooking(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (confirmar != JOptionPane.YES_OPTION) return;

                ejecutarLiquidacion(apt, detalles, metodoPago);
                return;
            }

            // Abono parcial normal (monto < saldoPendiente)
            if (apartadoDao.registrarNuevoAbono(apt.getIdBooking(), monto, metodoPago)) {
                JOptionPane.showMessageDialog(vistaGestion, "¡Abono registrado con éxito!");
                refrescarTablaGestionSegunFiltro();
            } else {
                JOptionPane.showMessageDialog(vistaGestion, "Error al procesar el pago en la base de datos.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaGestion, "Por favor, ingrese un monto numérico válido.");
        }
    }

    private void ejecutarLiquidacion(Apartado apt, List<Object[]> detalles, String metodoPago) {
        try {
            // Ejecución en el DAO (puede lanzar SQLException si falla el stock)
            if (apartadoDao.liquidarApartadoCompleto(apt.getIdBooking(), Sesion.getInstancia().getIdUserAccount(), metodoPago, detalles)) {
                JOptionPane.showMessageDialog(vistaGestion,
                    "¡APARTADO LIQUIDADO Y VENTA GENERADA CON ÉXITO!\n\n" +
                    "Estado actualizado a: Liquidado.");
                refrescarTablaGestionSegunFiltro();
            } else {
                JOptionPane.showMessageDialog(vistaGestion, "No se pudo completar la transacción por un error interno.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            // AQUÍ SE GESTIONA EL ERROR DEL DAO
            // Se muestra exactamente qué falló (por ejemplo, el mensaje de stock insuficiente)
            JOptionPane.showMessageDialog(vistaGestion,
                "No se pudo liquidar el apartado:\n" + e.getMessage(),
                "Error de Inventario / Sistema",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refrescarTablaGestionSegunFiltro() {
        String textoFiltro = vistaGestion.getTxtBuscar().getText();
        String estadoSeleccionado = vistaGestion.getCbEstado().getSelectedItem().toString();

        if (estadoSeleccionado.equalsIgnoreCase("Pendientes")) estadoSeleccionado = "Activo";
        else if (estadoSeleccionado.equalsIgnoreCase("Liquidados")) estadoSeleccionado = "Liquidado";
        else if (estadoSeleccionado.equalsIgnoreCase("Cancelados")) estadoSeleccionado = "Cancelado";
        else if (estadoSeleccionado.equalsIgnoreCase("Vencidos")) estadoSeleccionado = "Vencido";

        llenarTablaGestion(textoFiltro, estadoSeleccionado);
    }
}
