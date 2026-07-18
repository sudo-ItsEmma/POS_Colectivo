package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import com.tuerca.pos.model.CashCount;
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
 * Tests de {@link ArqueoDAO}. Las consultas filtran por {@code desde} (un
 * {@link LocalDateTime}), así que cada test fija {@code desde} justo antes
 * de insertar sus propias filas de prueba — eso excluye de forma natural
 * cualquier venta/abono real anterior de las sumas calculadas. Aun así, la
 * limpieza se hace por lista exacta de IDs generados por el propio test
 * (no por monto): la BD real ya tiene ventas viejas de pruebas manuales de
 * fases anteriores con montos redondos coincidentes (ej. $80.00 Mixto),
 * y limpiar por monto podría borrar esas filas ajenas por error.
 */
class ArqueoDAOTest extends AbstractDaoIntegrationTest {

    private final ArqueoDAO dao = new ArqueoDAO();
    private final ApartadoDAO apartadoDao = new ApartadoDAO();
    private int idEntrepreneur;
    private int idProduct;
    private int idUserAccount;
    private final List<Integer> idsVentasCreadas = new ArrayList<>();

    @BeforeEach
    void crearFixtures() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT ARQUEO DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JT88', 'PRODUCTO DE PRUEBA JUNIT ARQUEO', 'TEST', 20.00, 10, 1)",
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
            if (!idsVentasCreadas.isEmpty()) {
                String idsCsv = idsVentasCreadas.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                st.executeUpdate("DELETE FROM SaleDetail WHERE idSale IN (" + idsCsv + ")");
                st.executeUpdate("DELETE FROM Sale WHERE idSale IN (" + idsCsv + ")");
            }
            st.executeUpdate("DELETE FROM BookingPayment WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST ARQUEO%') AS b)");
            st.executeUpdate("DELETE FROM BookingDetail WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST ARQUEO%') AS b)");
            st.executeUpdate("DELETE FROM Booking WHERE customerName LIKE 'JUNIT TEST ARQUEO%'");
            st.executeUpdate("DELETE FROM Product WHERE idProduct = " + idProduct);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idEntrepreneur);
        }
    }

    private void insertarVenta(BigDecimal total, int idPaymentMethod, String paymentDetails) throws SQLException {
        String sql = "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount, paymentDetails) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUserAccount);
            ps.setInt(2, idPaymentMethod);
            ps.setBigDecimal(3, total);
            ps.setString(4, paymentDetails);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idsVentasCreadas.add(rs.getInt(1));
            }
        }
    }

    @Test
    void calcularVentasEfectivo_sumaEfectivoMasParteEfectivoDeMixto() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        insertarVenta(new BigDecimal("100.00"), 1, null); // Efectivo puro
        insertarVenta(new BigDecimal("80.00"), 3, "E:30.00|T:50.00"); // Mixto

        BigDecimal total = dao.calcularVentasEfectivo(desde);

        assertEquals(0, new BigDecimal("130.00").compareTo(total),
                "debe sumar el efectivo puro (100) + la parte en efectivo del Mixto (30)");
    }

    @Test
    void calcularVentasTransferencia_sumaSoloLaParteTransferidaDelMixto() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        insertarVenta(new BigDecimal("100.00"), 1, null); // Efectivo puro, no cuenta aquí
        insertarVenta(new BigDecimal("80.00"), 3, "E:30.00|T:50.00"); // Mixto

        BigDecimal total = dao.calcularVentasTransferencia(desde);
        int cantidad = dao.contarVentasConTransferencia(desde);

        assertEquals(0, new BigDecimal("50.00").compareTo(total));
        assertEquals(1, cantidad, "solo la venta Mixta tuvo componente de transferencia");
    }

    @Test
    void calcularAbonosEfectivo_sumaElAnticipoDeUnApartadoEnEfectivo() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);

        Apartado apt = new Apartado();
        apt.setIdUserAccount(idUserAccount);
        apt.setCustomerName("JUNIT TEST ARQUEO ABONO");
        apt.setCustomerPhone("5555555555");
        apt.setTotalAmount(new BigDecimal("60.00"));
        apt.setAdvanceAmount(new BigDecimal("60.00"));
        apt.setPendingBalance(BigDecimal.ZERO);
        apt.setBookingStatus("Activo");

        ApartadoDetail det = new ApartadoDetail();
        det.setIdProduct(idProduct);
        det.setQuantity(1);
        det.setUnitPrice(new BigDecimal("60.00"));
        det.setSubtotalDetail(new BigDecimal("60.00"));
        List<ApartadoDetail> detalles = new ArrayList<>();
        detalles.add(det);

        apartadoDao.registrarApartadoCompleto(apt, detalles, "Efectivo");

        BigDecimal abonos = dao.calcularAbonosEfectivo(desde);

        assertEquals(0, new BigDecimal("60.00").compareTo(abonos));
    }

    @Test
    void obtenerVentasDesde_desgloseCorrectoParaVentaMixta() throws SQLException {
        LocalDateTime desde = LocalDateTime.now().minusSeconds(2);
        insertarVenta(new BigDecimal("80.00"), 3, "E:30.00|T:50.00");

        List<Object[]> ventas = dao.obtenerVentasDesde(desde);

        assertEquals(1, ventas.size());
        Object[] fila = ventas.get(0);
        assertEquals("Mixto", fila[1]);
        assertEquals(0, new BigDecimal("80.00").compareTo((BigDecimal) fila[2]));
        assertEquals(0, new BigDecimal("30.00").compareTo((BigDecimal) fila[3]));
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) fila[4]));
    }

    @Test
    void registrarArqueo_insertaElConteoCorrectamente() throws SQLException {
        int idSesionReal = obtenerIdSesionAbiertaReal();
        String marcador = "JUNIT TEST ARQUEO REGISTRO";

        CashCount registro = new CashCount();
        registro.setIdCashSession(idSesionReal);
        registro.setIdUserAccount(idUserAccount);
        registro.setTheoricalAmount(new BigDecimal("1000.00"));
        registro.setCountedAmount(new BigDecimal("990.00"));
        registro.setCashDifference(new BigDecimal("-10.00"));
        registro.setJustificationComment(marcador);

        boolean ok = dao.registrarArqueo(registro);
        assertTrue(ok);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT theoricalAmount, countedAmount, cashDifference FROM CashCount WHERE justificationComment = ?")) {
            ps.setString(1, marcador);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "el arqueo insertado debe poder leerse de vuelta");
                assertEquals(0, new BigDecimal("1000.00").compareTo(rs.getBigDecimal("theoricalAmount")));
                assertEquals(0, new BigDecimal("990.00").compareTo(rs.getBigDecimal("countedAmount")));
                assertEquals(0, new BigDecimal("-10.00").compareTo(rs.getBigDecimal("cashDifference")));
            }
        } finally {
            // Limpieza por marcador exacto — nunca se toca la CashSession real, solo esta
            // fila de auditoría de prueba que le pertenece.
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM CashCount WHERE justificationComment = ?")) {
                ps.setString(1, marcador);
                ps.executeUpdate();
            }
        }
    }

    private int obtenerIdSesionAbiertaReal() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT idCashSession FROM CashSession WHERE sessionStatus = 'Abierta' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
