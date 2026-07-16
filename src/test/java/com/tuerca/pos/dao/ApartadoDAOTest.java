package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Convierte en tests permanentes los escenarios que ya se habían verificado
 * a mano con {@code ApartadoHarness.java} durante el Paso 7: reserva de
 * stock al crear, rollback completo si no alcanza, sin doble descuento al
 * liquidar, y devolución de stock al cancelar.
 */
class ApartadoDAOTest extends AbstractDaoIntegrationTest {

    private final ApartadoDAO dao = new ApartadoDAO();
    private int idEntrepreneur;
    private int idProduct;

    @BeforeEach
    void crearEmprendedorYProductoDePrueba() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT APARTADO DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JT99', 'PRODUCTO DE PRUEBA JUNIT', 'TEST', 20.00, 5, 1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idEntrepreneur);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idProduct = rs.getInt(1);
            }
        }
    }

    @AfterEach
    void limpiarDatosDePrueba() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE sd FROM SaleDetail sd JOIN Sale s ON sd.idSale = s.idSale WHERE s.idBooking IN (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST%')");
            st.executeUpdate("DELETE FROM Sale WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST%') AS b)");
            st.executeUpdate("DELETE FROM BookingPayment WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST%') AS b)");
            st.executeUpdate("DELETE FROM BookingDetail WHERE idBooking IN (SELECT idBooking FROM (SELECT idBooking FROM Booking WHERE customerName LIKE 'JUNIT TEST%') AS b)");
            st.executeUpdate("DELETE FROM Booking WHERE customerName LIKE 'JUNIT TEST%'");
            st.executeUpdate("DELETE FROM Product WHERE idProduct = " + idProduct);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idEntrepreneur);
        }
    }

    private int stockActual() throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT currentStock FROM Product WHERE idProduct = ?")) {
            ps.setInt(1, idProduct);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String estadoBooking(int idBooking) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT bookingStatus FROM Booking WHERE idBooking = ?")) {
            ps.setInt(1, idBooking);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private int crearApartado(String nombreCliente, int cantidad) throws SQLException {
        Apartado apt = new Apartado();
        apt.setIdUserAccount(1);
        apt.setCustomerName(nombreCliente);
        apt.setCustomerPhone("5555555555");
        apt.setTotalAmount(20.00 * cantidad);
        apt.setAdvanceAmount(10.00);
        apt.setPendingBalance(20.00 * cantidad - 10.00);
        apt.setBookingStatus("Activo");

        ApartadoDetail det = new ApartadoDetail();
        det.setIdProduct(idProduct);
        det.setQuantity(cantidad);
        det.setUnitPrice(20.00);
        det.setSubtotalDetail(20.00 * cantidad);
        List<ApartadoDetail> detalles = new ArrayList<>();
        detalles.add(det);

        dao.registrarApartadoCompleto(apt, detalles);

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
    void registrarApartadoCompleto_reservaStockAtomicamente() throws SQLException {
        int stockInicial = stockActual();

        crearApartado("JUNIT TEST RESERVA", 2);

        assertEquals(stockInicial - 2, stockActual(), "el stock debe descontarse al crear el apartado");
    }

    @Test
    void registrarApartadoCompleto_sinStockSuficiente_revierteTodaLaTransaccion() throws SQLException {
        int stockInicial = stockActual();

        Apartado apt = new Apartado();
        apt.setIdUserAccount(1);
        apt.setCustomerName("JUNIT TEST SIN STOCK");
        apt.setCustomerPhone("5555555555");
        apt.setTotalAmount(2000.00);
        apt.setAdvanceAmount(10.00);
        apt.setPendingBalance(1990.00);
        apt.setBookingStatus("Activo");

        ApartadoDetail det = new ApartadoDetail();
        det.setIdProduct(idProduct);
        det.setQuantity(999); // muy por encima del stock disponible
        det.setUnitPrice(20.00);
        det.setSubtotalDetail(2000.00);
        List<ApartadoDetail> detalles = new ArrayList<>();
        detalles.add(det);

        assertThrows(SQLException.class, () -> dao.registrarApartadoCompleto(apt, detalles));
        assertEquals(stockInicial, stockActual(), "el stock no debe cambiar si la transacción se revierte");

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM Booking WHERE customerName = 'JUNIT TEST SIN STOCK'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1), "no debe quedar ningún Booking huérfano del intento fallido");
            }
        }
    }

    @Test
    void liquidarApartadoCompleto_noVuelveADescontarElStock() throws SQLException {
        int idBooking = crearApartado("JUNIT TEST LIQUIDAR", 1);
        int stockTrasCrear = stockActual();

        List<Object[]> detalles = dao.obtenerResumenDetallesPorFolio(idBooking);
        boolean ok = dao.liquidarApartadoCompleto(idBooking, 1, "Efectivo", detalles);

        assertTrue(ok);
        assertEquals(stockTrasCrear, stockActual(), "liquidar no debe volver a tocar el stock");
        assertEquals("Liquidado", estadoBooking(idBooking));
    }

    @Test
    void cancelarApartado_devuelveElStockYMarcaCancelado() throws SQLException {
        int idBooking = crearApartado("JUNIT TEST CANCELAR", 1);
        int stockTrasCrear = stockActual();

        boolean ok = dao.cancelarApartado(idBooking);

        assertTrue(ok);
        assertEquals(stockTrasCrear + 1, stockActual(), "cancelar debe devolver el stock reservado");
        assertEquals("Cancelado", estadoBooking(idBooking));
    }

    @Test
    void cancelarApartado_yaLiquidado_noHaceNadaYRetornaFalse() throws SQLException {
        int idBooking = crearApartado("JUNIT TEST DOBLE CANCELAR", 1);
        List<Object[]> detalles = dao.obtenerResumenDetallesPorFolio(idBooking);
        dao.liquidarApartadoCompleto(idBooking, 1, "Efectivo", detalles);
        int stockTrasLiquidar = stockActual();

        boolean ok = dao.cancelarApartado(idBooking);

        assertFalse(ok, "no se debe poder cancelar un apartado que ya está Liquidado");
        assertEquals(stockTrasLiquidar, stockActual(), "el stock no debe cambiar si la cancelación no procede");
    }
}
