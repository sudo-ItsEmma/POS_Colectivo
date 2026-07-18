package com.tuerca.pos.dao;

import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link DevolucionDAO}. Convierte a permanentes los escenarios ya
 * verificados a mano con {@code DevolucionHarness.java} durante el Paso 8:
 * devolución parcial (la venta se queda 'Activa'), bloqueo de doble
 * devolución de la misma línea, y devolución de la última línea (la venta
 * pasa a 'Devuelta').
 */
class DevolucionDAOTest extends AbstractDaoIntegrationTest {

    private final DevolucionDAO dao = new DevolucionDAO();
    private int idEntrepreneur;
    private int idProduct;
    private int idUserAccount;
    private int idSale;
    private int idSaleDetail1;
    private int idSaleDetail2;

    @BeforeEach
    void crearFixtures() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT DEVOLUCION DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JC01', 'PRODUCTO DE PRUEBA JUNIT DEVOLUCION', 'TEST', 20.00, 10, 1)",
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
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount) VALUES (?, 1, 40.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUserAccount);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idSale = rs.getInt(1);
            }
        }
        idSaleDetail1 = insertarSaleDetail(1, new java.math.BigDecimal("20.00"), new java.math.BigDecimal("20.00"));
        idSaleDetail2 = insertarSaleDetail(1, new java.math.BigDecimal("20.00"), new java.math.BigDecimal("20.00"));
    }

    private int insertarSaleDetail(int cantidad, java.math.BigDecimal precio, java.math.BigDecimal subtotal) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "INSERT INTO SaleDetail (idSale, idProduct, quantitySold, unitPriceAtSale, discountApplied, subtotalDetail) " +
                "VALUES (?, ?, ?, ?, 0.00, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idSale);
            ps.setInt(2, idProduct);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, precio);
            ps.setBigDecimal(5, subtotal);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @AfterEach
    void limpiarFixtures() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM ProductReturn WHERE idSaleDetail IN (" + idSaleDetail1 + ", " + idSaleDetail2 + ")");
            st.executeUpdate("DELETE FROM SaleDetail WHERE idSale = " + idSale);
            st.executeUpdate("DELETE FROM Sale WHERE idSale = " + idSale);
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

    private String estadoVenta() throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT saleStatus FROM Sale WHERE idSale = ?")) {
            ps.setInt(1, idSale);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    @Test
    void buscarVentas_encuentraLaVentaActivaConSusDosProductos() {
        List<Object[]> ventas = dao.buscarVentas(String.valueOf(idSale));

        assertEquals(1, ventas.size());
        assertEquals(2, (int) ventas.get(0)[2], "totalProductos debe contar las 2 líneas de la venta");
    }

    @Test
    void obtenerDetallesConEstado_marcaYaDevueltoSoloEnLaLineaDevuelta() {
        dao.procesarDevolucion(idSaleDetail1, idUserAccount, "Producto defectuoso", 20.00);

        List<Object[]> detalles = dao.obtenerDetallesConEstado(idSale);

        assertEquals(2, detalles.size());
        for (Object[] fila : detalles) {
            int idDetalle = (int) fila[0];
            boolean yaDevuelto = (boolean) fila[6];
            assertEquals(idDetalle == idSaleDetail1, yaDevuelto);
        }
    }

    @Test
    void procesarDevolucion_devuelveStockYLaVentaSigueActivaSiQuedanLineas() throws SQLException {
        int stockInicial = stockActual();

        boolean ok = dao.procesarDevolucion(idSaleDetail1, idUserAccount, "Cliente cambió de opinión", 20.00);

        assertTrue(ok);
        assertEquals(stockInicial + 1, stockActual());
        assertEquals("Activa", estadoVenta(), "aún queda una línea sin devolver");
    }

    @Test
    void procesarDevolucion_lineaYaDevuelta_noPermiteDevolverlaDeNuevo() throws SQLException {
        dao.procesarDevolucion(idSaleDetail1, idUserAccount, "Primera devolución", 20.00);
        int stockTrasPrimera = stockActual();

        boolean segundoIntento = dao.procesarDevolucion(idSaleDetail1, idUserAccount, "Segundo intento", 20.00);

        assertFalse(segundoIntento, "el UNIQUE sobre idSaleDetail debe impedir devolver la misma línea dos veces");
        assertEquals(stockTrasPrimera, stockActual(), "el stock no debe cambiar en el intento rechazado");
    }

    @Test
    void procesarDevolucion_ultimaLineaSinDevolver_marcaLaVentaCompletaComoDevuelta() throws SQLException {
        dao.procesarDevolucion(idSaleDetail1, idUserAccount, "Devolución parcial", 20.00);
        assertEquals("Activa", estadoVenta());

        dao.procesarDevolucion(idSaleDetail2, idUserAccount, "Devolución de la última línea", 20.00);

        assertEquals("Devuelta", estadoVenta(), "al devolver todas las líneas, la venta completa debe pasar a Devuelta");
    }
}
