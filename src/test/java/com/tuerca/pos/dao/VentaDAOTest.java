package com.tuerca.pos.dao;

import com.tuerca.pos.model.DetalleVenta;
import com.tuerca.pos.model.Venta;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.math.BigDecimal;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link VentaDAO#registrarVenta}. Usa un producto de prueba propio
 * (código {@code JT66}) para que el descuento de stock y el detalle
 * insertado se puedan verificar sin tocar productos reales.
 */
class VentaDAOTest extends AbstractDaoIntegrationTest {

    private final VentaDAO dao = new VentaDAO();
    private int idEntrepreneur;
    private int idProduct;
    private int idUserAccount;

    @BeforeEach
    void crearFixtures() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT VENTA DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JT66', 'PRODUCTO DE PRUEBA JUNIT VENTA', 'TEST', 20.00, 10, 1)",
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
        // Se identifican los idSale exactos vía el propio idProduct de prueba (único en
        // este run, nunca reutilizado por datos reales) ANTES de borrar SaleDetail, para
        // poder borrar luego esos mismos Sale por ID exacto — no por heurísticas sobre
        // idUserAccount, que es una cuenta real compartida con otras ventas ajenas.
        List<Integer> idsVentas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT idSale FROM SaleDetail WHERE idProduct = ?")) {
            ps.setInt(1, idProduct);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) idsVentas.add(rs.getInt(1));
            }
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM SaleDetail WHERE idProduct = " + idProduct);
            if (!idsVentas.isEmpty()) {
                String idsCsv = idsVentas.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                st.executeUpdate("DELETE FROM Sale WHERE idSale IN (" + idsCsv + ")");
            }
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

    private DetalleVenta crearDetalle(int cantidad, BigDecimal precio, BigDecimal descuento, BigDecimal subtotal) {
        DetalleVenta det = new DetalleVenta();
        det.setIdProducto(idProduct);
        det.setCantidad(cantidad);
        det.setPrecioUnitario(precio);
        det.setDescuento(descuento);
        det.setSubtotal(subtotal);
        return det;
    }

    @Test
    void registrarVenta_efectivo_descuentaStockYGuardaDetalle() throws SQLException {
        int stockInicial = stockActual();

        Venta venta = new Venta();
        venta.setIdUsuario(idUserAccount);
        venta.setTotal(new BigDecimal("40.00"));
        venta.setMetodoPago("Efectivo");
        List<DetalleVenta> detalles = List.of(crearDetalle(2, new BigDecimal("20.00"), BigDecimal.ZERO, new BigDecimal("40.00")));

        boolean ok = dao.registrarVenta(venta, detalles);

        assertTrue(ok);
        assertEquals(stockInicial - 2, stockActual(), "debe descontar la cantidad vendida del stock");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT s.idPaymentMethod, sd.quantitySold, sd.subtotalDetail FROM Sale s " +
                     "JOIN SaleDetail sd ON sd.idSale = s.idSale " +
                     "WHERE s.idUserAccount = ? ORDER BY s.idSale DESC LIMIT 1")) {
            ps.setInt(1, idUserAccount);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("idPaymentMethod"), "Efectivo debe mapear a idPaymentMethod 1");
                assertEquals(2, rs.getInt("quantitySold"));
                assertEquals(0, new BigDecimal("40.00").compareTo(rs.getBigDecimal("subtotalDetail")));
            }
        }
    }

    @Test
    void registrarVenta_transferencia_mapeaIdDeMetodoCorrecto() throws SQLException {
        Venta venta = new Venta();
        venta.setIdUsuario(idUserAccount);
        venta.setTotal(new BigDecimal("20.00"));
        venta.setMetodoPago("Transferencia");
        List<DetalleVenta> detalles = List.of(crearDetalle(1, new BigDecimal("20.00"), BigDecimal.ZERO, new BigDecimal("20.00")));

        dao.registrarVenta(venta, detalles);

        assertEquals(2, obtenerUltimoIdPaymentMethod());
    }

    @Test
    void registrarVenta_mixto_guardaPaymentDetailsYMapeaIdTres() throws SQLException {
        Venta venta = new Venta();
        venta.setIdUsuario(idUserAccount);
        venta.setTotal(new BigDecimal("50.00"));
        venta.setMetodoPago("Mixto");
        venta.setPaymentDetails("E:20.00|T:30.00");
        List<DetalleVenta> detalles = List.of(crearDetalle(1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00")));

        dao.registrarVenta(venta, detalles);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT idPaymentMethod, paymentDetails FROM Sale WHERE idUserAccount = ? ORDER BY idSale DESC LIMIT 1")) {
            ps.setInt(1, idUserAccount);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(3, rs.getInt("idPaymentMethod"));
                assertEquals("E:20.00|T:30.00", rs.getString("paymentDetails"));
            }
        }
    }

    @Test
    void registrarVenta_conDescuento_guardaElDescuentoAplicado() throws SQLException {
        Venta venta = new Venta();
        venta.setIdUsuario(idUserAccount);
        venta.setTotal(new BigDecimal("15.00"));
        venta.setMetodoPago("Efectivo");
        List<DetalleVenta> detalles = List.of(crearDetalle(1, new BigDecimal("20.00"), new BigDecimal("5.00"), new BigDecimal("15.00")));

        dao.registrarVenta(venta, detalles);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT sd.discountApplied FROM Sale s JOIN SaleDetail sd ON sd.idSale = s.idSale " +
                     "WHERE s.idUserAccount = ? ORDER BY s.idSale DESC LIMIT 1")) {
            ps.setInt(1, idUserAccount);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, new BigDecimal("5.00").compareTo(rs.getBigDecimal("discountApplied")));
            }
        }
    }

    private int obtenerUltimoIdPaymentMethod() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT idPaymentMethod FROM Sale WHERE idUserAccount = ? ORDER BY idSale DESC LIMIT 1")) {
            ps.setInt(1, idUserAccount);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
