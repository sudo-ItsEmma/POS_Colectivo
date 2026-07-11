package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ArqueoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.dao.CorteDAO;
import com.tuerca.pos.model.CashCount;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.CorteDeCaja;
import com.tuerca.pos.view.MainView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.JOptionPane;

/**
 * Controla el Corte de Caja (FN.8): consolida el día (efectivo, transferencias
 * y apartados), pide el conteo final y cierra formalmente la {@code CashSession}.
 * A diferencia del Arqueo, esto es un evento único por sesión y fuerza el logout.
 */
public class CorteCajaController {

    private final CorteDeCaja vista;
    private final MainView mainView;
    private final ArqueoDAO arqueoDao;
    private final CorteDAO corteDao;
    private final CashSessionDAO cashSessionDao;

    private CashSession sesionActual;

    public CorteCajaController(CorteDeCaja vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.arqueoDao = new ArqueoDAO();
        this.corteDao = new CorteDAO();
        this.cashSessionDao = new CashSessionDAO();

        vista.getBtnBack().addActionListener(e ->
                mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee"));
        vista.getBtnFinalizarJornada().addActionListener(e -> procesarCierre());
    }

    // Se llama desde MainView.showView() cada vez que se entra a esta pantalla,
    // para que el resumen refleje el estado actual de la sesión abierta.
    public void actualizarDatos() {
        sesionActual = cashSessionDao.obtenerSesionAbierta();

        if (sesionActual == null) {
            JOptionPane.showMessageDialog(vista,
                    "No hay una caja abierta para cerrar.",
                    "Corte no disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.time.LocalDateTime desde = sesionActual.getOpeningDateTime();

        // Resumen de efectivo
        BigDecimal ventasEfectivo = arqueoDao.calcularVentasEfectivo(desde);
        BigDecimal abonosEfectivo = arqueoDao.calcularAbonosEfectivo(desde);
        BigDecimal fondoInicial = sesionActual.getInitialCashAmount();
        BigDecimal totalEfectivo = fondoInicial.add(ventasEfectivo).add(abonosEfectivo);

        vista.setVentasEfectivo(formatoMoneda(ventasEfectivo));
        vista.setAbonosEfectivo(formatoMoneda(abonosEfectivo));
        vista.setFondoInicial(formatoMoneda(fondoInicial));
        vista.setTotalEfectivo(formatoMoneda(totalEfectivo));

        // Resumen de transferencias
        BigDecimal ventasTransferencia = arqueoDao.calcularVentasTransferencia(desde);
        int cantidadTransferencias = arqueoDao.contarVentasConTransferencia(desde);

        vista.setVentasTransferencia(formatoMoneda(ventasTransferencia));
        vista.setCantidadTransferencias(cantidadTransferencias);

        // Resumen de apartados
        BigDecimal apartadosNuevos = corteDao.calcularApartadosNuevos(desde);
        BigDecimal apartadosAbonos = corteDao.calcularAbonosApartados(desde, apartadosNuevos);
        BigDecimal apartadosTotal = apartadosNuevos.add(apartadosAbonos);

        vista.setApartadosNuevos(formatoMoneda(apartadosNuevos));
        vista.setApartadosAbonos(formatoMoneda(apartadosAbonos));
        vista.setApartadosTotal(formatoMoneda(apartadosTotal));

        // Total del día: cuánto dinero debe haber físicamente en la caja ahora mismo
        // (fondo + ventas en efectivo + apartados en efectivo) — el mismo total que
        // ya se muestra en el cuadrante de Efectivo, repetido aquí para que quede
        // clarísimo sin tener que mezclarlo con las transferencias.
        vista.setDebeHaberEnCaja(formatoMoneda(totalEfectivo));
        vista.setEfectivoContado("—");
        vista.setMontoARetirar("—");
    }

    private void procesarCierre() {
        if (sesionActual == null) {
            JOptionPane.showMessageDialog(vista,
                    "No hay una caja abierta para cerrar.",
                    "Corte no disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(vista,
                "¿Confirmas que quieres finalizar la jornada?\n\nEsta acción cierra la caja y no se puede deshacer.",
                "Confirmar cierre de jornada", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmar != JOptionPane.YES_OPTION) return;

        String input = JOptionPane.showInputDialog(vista,
                "Introduce la cantidad contada en efectivo para cerrar la caja:",
                "Corte de Caja", JOptionPane.QUESTION_MESSAGE);

        if (input == null) return; // Canceló

        BigDecimal efectivoContado;
        try {
            efectivoContado = new BigDecimal(input.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Ingresa un monto numérico válido.",
                    "Monto inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.time.LocalDateTime desde = sesionActual.getOpeningDateTime();

        // Recalculamos todo fresco (por si hubo ventas nuevas mientras se abrían los
        // diálogos) — es el mismo desglose que ya se mostró en pantalla, pero se
        // vuelve a calcular aquí para guardarlo junto con el cierre.
        BigDecimal saldoTeorico = arqueoDao.calcularSaldoTeorico(sesionActual);
        BigDecimal diferencia = efectivoContado.subtract(saldoTeorico);

        BigDecimal ventasEfectivo = arqueoDao.calcularVentasEfectivo(desde);
        BigDecimal abonosEfectivo = arqueoDao.calcularAbonosEfectivo(desde);
        BigDecimal ventasTransferencia = arqueoDao.calcularVentasTransferencia(desde);
        int cantidadTransferencias = arqueoDao.contarVentasConTransferencia(desde);
        BigDecimal apartadosNuevos = corteDao.calcularApartadosNuevos(desde);
        BigDecimal apartadosAbonos = corteDao.calcularAbonosApartados(desde, apartadosNuevos);

        String comentario = null;
        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            comentario = JOptionPane.showInputDialog(vista,
                    "Hay una diferencia de " + formatoMoneda(diferencia) + ".\n\n"
                    + "Escribe un comentario que justifique la diferencia:",
                    "Justificación requerida", JOptionPane.WARNING_MESSAGE);

            if (comentario == null || comentario.trim().isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "El corte no se guardó: se requiere un comentario cuando hay diferencia.",
                        "Corte cancelado", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Dejamos registro del conteo final como un arqueo más (auditoría) y
        // además cerramos formalmente la sesión.
        CashCount registro = new CashCount();
        registro.setIdCashSession(sesionActual.getIdCashSession());
        registro.setIdUserAccount(Sesion.getInstancia().getIdUserAccount());
        registro.setTheoricalAmount(saldoTeorico);
        registro.setCountedAmount(efectivoContado);
        registro.setCashDifference(diferencia);
        registro.setJustificationComment(comentario);
        arqueoDao.registrarArqueo(registro);

        // Armamos el cierre con el desglose completo, para que quede guardado en
        // CashSession y sirva para auditorías y reportes diarios/semanales/mensuales
        // sin tener que recalcular desde Sale/BookingPayment cada vez.
        CashSession cierre = new CashSession();
        cierre.setIdCashSession(sesionActual.getIdCashSession());
        cierre.setFinalCashAmount(efectivoContado);
        cierre.setTheoricalAmount(saldoTeorico);
        cierre.setCashDifference(diferencia);
        cierre.setCashSalesAmount(ventasEfectivo);
        cierre.setCashBookingPaymentsAmount(abonosEfectivo);
        cierre.setTransferSalesAmount(ventasTransferencia);
        cierre.setTransferSalesCount(cantidadTransferencias);
        cierre.setBookingsNewAmount(apartadosNuevos);
        cierre.setBookingsPaymentsAmount(apartadosAbonos);

        boolean cerrada = corteDao.cerrarCaja(cierre);

        if (!cerrada) {
            JOptionPane.showMessageDialog(vista, "No se pudo cerrar la caja.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal montoARetirar = efectivoContado.subtract(sesionActual.getInitialCashAmount());
        vista.setEfectivoContado(formatoMoneda(efectivoContado));
        vista.setMontoARetirar(formatoMoneda(montoARetirar));

        JOptionPane.showMessageDialog(vista,
                "JORNADA FINALIZADA\n\n"
                + "Efectivo contado: " + formatoMoneda(efectivoContado) + "\n"
                + "Fondo para mañana: " + formatoMoneda(sesionActual.getInitialCashAmount()) + "\n"
                + "Monto a retirar: " + formatoMoneda(montoARetirar),
                "Corte de Caja", JOptionPane.INFORMATION_MESSAGE);

        mainView.cerrarSesion(); // fuerza el logout, como pide FN.8
    }

    private String formatoMoneda(BigDecimal monto) {
        return String.format("$%.2f", monto);
    }
}
