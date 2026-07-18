package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ArqueoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.model.CashCount;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.ArqueoDeCaja;
import com.tuerca.pos.view.MainView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Controla el Arqueo de Caja (FN.7): compara el saldo teórico de la
 * sesión abierta contra el efectivo contado, y guarda cada comparación
 * en {@code CashCount} (puede haber varias por sesión, no bloquea ventas).
 */
public class ArqueoCajaController {

    private final ArqueoDeCaja vista;
    private final MainView mainView;
    private final ArqueoDAO arqueoDao;
    private final CashSessionDAO cashSessionDao;

    private CashSession sesionActual;

    public ArqueoCajaController(ArqueoDeCaja vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.arqueoDao = new ArqueoDAO();
        this.cashSessionDao = new CashSessionDAO();

        vista.getBtnBack().addActionListener(e ->
                mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee"));
        vista.getBtnIntroducirCantidad().addActionListener(e -> procesarArqueo());
        vista.getBtnCancelar().addActionListener(e -> {
            limpiarComparacion();
            JOptionPane.showMessageDialog(vista, "Comparación de arqueo cancelada.");
            mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        });
    }

    // Se llama desde MainView.showView() cada vez que se entra a esta pantalla,
    // para que el saldo teórico y la tabla de ventas reflejen el estado actual.
    public void actualizarDatos() {
        sesionActual = cashSessionDao.obtenerSesionAbierta();

        if (sesionActual == null) {
            JOptionPane.showMessageDialog(vista,
                    "No hay una caja abierta. Abre la caja para poder hacer un arqueo.",
                    "Arqueo no disponible", JOptionPane.WARNING_MESSAGE);
            vista.getLblSaldoTeorico().setText("$0.00");
            vista.getLblTransferencias().setText("$0.00");
            llenarTablaVentas(List.of());
            limpiarComparacion();
            return;
        }

        BigDecimal saldoTeorico = arqueoDao.calcularSaldoTeorico(sesionActual);
        vista.getLblSaldoTeorico().setText(formatoMoneda(saldoTeorico));

        BigDecimal ventasTransferencia = arqueoDao.calcularVentasTransferencia(sesionActual.getOpeningDateTime());
        vista.getLblTransferencias().setText(formatoMoneda(ventasTransferencia));

        llenarTablaVentas(arqueoDao.obtenerVentasDesde(sesionActual.getOpeningDateTime()));
        limpiarComparacion();
    }

    private void llenarTablaVentas(List<Object[]> ventas) {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVentas().getModel();
        modelo.setRowCount(0);

        BigDecimal totalGeneral = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Object[] fila : ventas) {
            modelo.addRow(fila);
            totalGeneral = totalGeneral.add((BigDecimal) fila[2]);
            totalEfectivo = totalEfectivo.add((BigDecimal) fila[3]);
            totalTransferencia = totalTransferencia.add((BigDecimal) fila[4]);
        }

        // Fila de totales al final, para que no haya que sumar la columna a mano
        if (!ventas.isEmpty()) {
            modelo.addRow(new Object[]{"", "TOTAL", totalGeneral, totalEfectivo, totalTransferencia});
        }
    }

    private void procesarArqueo() {
        if (sesionActual == null) {
            JOptionPane.showMessageDialog(vista,
                    "No hay una caja abierta. Abre la caja para poder hacer un arqueo.",
                    "Arqueo no disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(vista,
                "Introduce la cantidad contada en efectivo:",
                "Arqueo de Caja", JOptionPane.QUESTION_MESSAGE);

        if (input == null) return; // Canceló

        BigDecimal efectivoContado;
        try {
            efectivoContado = new BigDecimal(input.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Ingresa un monto numérico válido.",
                    "Monto inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Recalculamos el saldo teórico por si hubo ventas nuevas mientras se abría el diálogo
        BigDecimal saldoTeorico = arqueoDao.calcularSaldoTeorico(sesionActual);
        BigDecimal diferencia = efectivoContado.subtract(saldoTeorico);

        vista.getLblSaldoTeorico().setText(formatoMoneda(saldoTeorico));
        vista.getLblTransferencias().setText(
                formatoMoneda(arqueoDao.calcularVentasTransferencia(sesionActual.getOpeningDateTime())));
        vista.getLblEfectivoContado().setText(formatoMoneda(efectivoContado));
        vista.getLblDiferencia().setText(formatoMoneda(diferencia));

        String comentario = null;
        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            comentario = JOptionPane.showInputDialog(vista,
                    "Hay una diferencia de " + formatoMoneda(diferencia) + ".\n\n"
                    + "Escribe un comentario que justifique la diferencia:",
                    "Justificación requerida", JOptionPane.WARNING_MESSAGE);

            if (comentario == null || comentario.trim().isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "El arqueo no se guardó: se requiere un comentario cuando hay diferencia.",
                        "Arqueo cancelado", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        CashCount registro = new CashCount();
        registro.setIdCashSession(sesionActual.getIdCashSession());
        registro.setIdUserAccount(Sesion.getInstancia().getIdUserAccount());
        registro.setTheoricalAmount(saldoTeorico);
        registro.setCountedAmount(efectivoContado);
        registro.setCashDifference(diferencia);
        registro.setJustificationComment(comentario);

        if (arqueoDao.registrarArqueo(registro)) {
            JOptionPane.showMessageDialog(vista, "Arqueo registrado correctamente.");
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo guardar el arqueo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarComparacion() {
        vista.getLblEfectivoContado().setText("$0.00");
        vista.getLblDiferencia().setText("$0.00");
    }

    private String formatoMoneda(BigDecimal monto) {
        return String.format("$%.2f", monto);
    }
}
