package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link CorteDAO}. Igual que {@link ArqueoDAOTest}, cada test fija
 * {@code desde} justo antes de crear sus propios apartados/abonos de prueba
 * para aislarse de datos reales anteriores.
 *
 * {@code cerrarCaja()} no se prueba sobre la CashSession real y abierta del
 * entorno (cerrarla de verdad afectaría la caja real del usuario) — en vez
 * de eso se crea una fila de {@code CashSession} desechable ya con
 * {@code sessionStatus='Cerrada'} desde el INSERT (nunca pasa por
 * 'Abierta', así que no choca con el UNIQUE de {@code openSessionGuard}) y
 * se le aplica el UPDATE real de {@code cerrarCaja()} encima.
 */
class CorteDAOTest extends AbstractDaoIntegrationTest {

    private final CorteDAO dao = new CorteDAO();
    private final ApartadoDAO apartadoDao = new ApartadoDAO();
    private int idEntrepreneur;
    private int idProduct;
    private int idUserAccount;

    @BeforeEach
    void crearFixtures() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT CORTE DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JT77', 'PRODUCTO DE PRUEBA JUNIT CORTE', 'TEST', 20.00, 10, 1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idEntrepreneur);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idProduct = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT idUserAccount FROM UserAccount LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            idUserAccount = rs.getInt(1);
        }
    }

    @AfterEach
    void limpiarFixtures() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM BookingPayment WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST CORTE%') AS b)");
            st.executeUpdate("DELETE FROM BookingDetail WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST CORTE%') AS b)");
            st.executeUpdate("DELETE FROM Booking WHERE customerName LIKE 'JUNIT TEST CORTE%'");
            st.executeUpdate("DELETE FROM Product WHERE idProduct = " + idProduct);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idEntrepreneur);
        }
    }

    private int crearApartado(String nombreCliente, BigDecimal total, BigDecimal anticipo, String metodo) throws SQLException {
        Apartado apt = new Apartado();
        apt.setIdUserAccount(idUserAccount);
        apt.setCustomerName(nombreCliente);
        apt.setCustomerPhone("5555555555");
        apt.setTotalAmount(total);
        apt.setAdvanceAmount(anticipo);
        apt.setPendingBalance(total.subtract(anticipo));
        apt.setBookingStatus("Activo");

        ApartadoDetail det = new ApartadoDetail();
        det.setIdProduct(idProduct);
        det.setQuantity(1);
        det.setUnitPrice(total);
        det.setSubtotalDetail(total);
        List<ApartadoDetail> detalles = new ArrayList<>();
        detalles.add(det);

        apartadoDao.registrarApartadoCompleto(apt, detalles, metodo);

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT idBooking FROM Booking WHERE customerName = ? ORDER BY idBooking DESC LIMIT 1")) {
            ps.setString(1, nombreCliente);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    void calcularApartadosNuevos_sumaSoloElAnticipoInicial() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        crearApartado("JUNIT TEST CORTE NUEVO", new BigDecimal("100.00"), new BigDecimal("40.00"), "Efectivo");

        BigDecimal nuevos = dao.calcularApartadosNuevos(desde);

        assertEquals(0, new BigDecimal("40.00").compareTo(nuevos));
    }

    @Test
    void calcularApartadosNuevosPorMetodo_filtraPorMetodoDePago() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        crearApartado("JUNIT TEST CORTE METODO", new BigDecimal("100.00"), new BigDecimal("40.00"), "Transferencia");

        BigDecimal transferencia = dao.calcularApartadosNuevosPorMetodo(desde, "Transferencia");
        BigDecimal efectivo = dao.calcularApartadosNuevosPorMetodo(desde, "Efectivo");

        assertEquals(0, new BigDecimal("40.00").compareTo(transferencia));
        assertEquals(0, BigDecimal.ZERO.compareTo(efectivo));
    }

    @Test
    void calcularAbonosApartados_excluyeElAnticipoYSumaSoloElAbonoPosterior() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        int idBooking = crearApartado("JUNIT TEST CORTE ABONO", new BigDecimal("100.00"), new BigDecimal("40.00"), "Efectivo");
        apartadoDao.registrarNuevoAbono(idBooking, new BigDecimal("25.00"), "Efectivo");

        BigDecimal nuevos = dao.calcularApartadosNuevos(desde);
        BigDecimal abonos = dao.calcularAbonosApartados(desde, nuevos);

        assertEquals(0, new BigDecimal("40.00").compareTo(nuevos));
        assertEquals(0, new BigDecimal("25.00").compareTo(abonos),
                "los abonos posteriores no deben incluir el anticipo inicial");
    }

    @Test
    void calcularAbonosApartadosPorMetodo_filtraPorMetodoDePago() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        int idBooking = crearApartado("JUNIT TEST CORTE ABONO METODO", new BigDecimal("100.00"), new BigDecimal("40.00"), "Efectivo");
        apartadoDao.registrarNuevoAbono(idBooking, new BigDecimal("25.00"), "Transferencia");

        BigDecimal nuevosTransferencia = dao.calcularApartadosNuevosPorMetodo(desde, "Transferencia");
        BigDecimal abonosTransferencia = dao.calcularAbonosApartadosPorMetodo(desde, "Transferencia", nuevosTransferencia);

        assertEquals(0, BigDecimal.ZERO.compareTo(nuevosTransferencia), "el anticipo fue en Efectivo, no en Transferencia");
        assertEquals(0, new BigDecimal("25.00").compareTo(abonosTransferencia));
    }

    @Test
    void cerrarCaja_actualizaTodosLosCamposYElEstado() throws SQLException {
        int idSesionDesechable = crearCashSessionDesechableYaCerrada();
        try {
            CashSession cierre = new CashSession();
            cierre.setIdCashSession(idSesionDesechable);
            cierre.setFinalCashAmount(new BigDecimal("1500.00"));
            cierre.setTheoricalAmount(new BigDecimal("1490.00"));
            cierre.setCashDifference(new BigDecimal("10.00"));
            cierre.setCashSalesAmount(new BigDecimal("800.00"));
            cierre.setCashBookingPaymentsAmount(new BigDecimal("90.00"));
            cierre.setTransferSalesAmount(new BigDecimal("300.00"));
            cierre.setTransferSalesCount(3);
            cierre.setBookingsNewAmount(new BigDecimal("40.00"));
            cierre.setBookingsPaymentsAmount(new BigDecimal("25.00"));
            cierre.setBookingsNewAmountTransfer(BigDecimal.ZERO);
            cierre.setBookingsPaymentsAmountTransfer(BigDecimal.ZERO);

            boolean ok = dao.cerrarCaja(cierre);
            assertTrue(ok);

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT sessionStatus, finalCashAmount, cashDifference, transferSalesCount, closingDateTime " +
                         "FROM CashSession WHERE idCashSession = ?")) {
                ps.setInt(1, idSesionDesechable);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Cerrada", rs.getString("sessionStatus"));
                    assertEquals(0, new BigDecimal("1500.00").compareTo(rs.getBigDecimal("finalCashAmount")));
                    assertEquals(0, new BigDecimal("10.00").compareTo(rs.getBigDecimal("cashDifference")));
                    assertEquals(3, rs.getInt("transferSalesCount"));
                    assertTrue(rs.getTimestamp("closingDateTime") != null);
                }
            }
        } finally {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM CashSession WHERE idCashSession = ?")) {
                ps.setInt(1, idSesionDesechable);
                ps.executeUpdate();
            }
        }
    }

    // Nace directamente con sessionStatus='Cerrada' (nunca pasa por 'Abierta'), así no
    // choca con el UNIQUE de openSessionGuard ni con la CashSession real del entorno.
    private int crearCashSessionDesechableYaCerrada() throws SQLException {
        String sql = "INSERT INTO CashSession (idUserAccount, initialCashAmount, sessionStatus) VALUES (?, ?, 'Cerrada')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUserAccount);
            ps.setBigDecimal(2, new BigDecimal("600.00"));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
