package com.tuerca.pos.controller;

import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.dao.VentaDAO;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Producto;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.Ventas;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code agregarProductoAlCarrito()} solo es alcanzable desde el listener del
 * mouse sobre {@code listaSugerencias} (un {@link javax.swing.JList} privado,
 * sin getter, dentro de un {@link javax.swing.JPopupMenu} también privado) —
 * se invoca por reflexión, mismo criterio ya usado en otros controladores
 * para lógica sin puerta de entrada pública.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaControllerTest {

    @Mock
    private Ventas vista;
    @Mock
    private MainView mainView;

    private final JTable tablaVenta = new JTable();
    private final JTextField txtBusqueda = new JTextField();
    private final JLabel lblTotal = new JLabel();
    private final JComboBox<String> cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Mixto"});
    private final JButton btnCobrar = new JButton();
    private final JButton btnCancelar = new JButton();
    private final JButton btnBack = new JButton();

    private MockedConstruction<ProductoDAO> construccionProductoDao;
    private MockedConstruction<CashSessionDAO> construccionCashSessionDao;
    private MockedConstruction<VentaDAO> construccionVentaDao;
    private ProductoDAO productoDao;
    private CashSessionDAO cashSessionDao;
    private VentaController controller;

    @BeforeEach
    void construirController() {
        when(vista.getTablaVenta()).thenReturn(tablaVenta);
        when(vista.getTxtBusqueda()).thenReturn(txtBusqueda);
        when(vista.getLblTotal()).thenReturn(lblTotal);
        when(vista.getCbMetodoPago()).thenReturn(cbMetodoPago);
        when(vista.getBtnCobrar()).thenReturn(btnCobrar);
        when(vista.getBtnCancelar()).thenReturn(btnCancelar);
        when(vista.getBtnBack()).thenReturn(btnBack);

        construccionProductoDao = mockConstruction(ProductoDAO.class);
        construccionCashSessionDao = mockConstruction(CashSessionDAO.class);
        construccionVentaDao = mockConstruction(VentaDAO.class,
                (m, ctx) -> when(m.registrarVenta(any(), any())).thenReturn(true));

        controller = new VentaController(vista, mainView);
        productoDao = construccionProductoDao.constructed().get(0);
        cashSessionDao = construccionCashSessionDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(21);
        empleado.setId(1);
        empleado.setNombre("Test");
        empleado.setPaterno("User");
        empleado.setUsername("testuser");
        empleado.setIdRole(2);
        empleado.setRoleName("Sales");
        Sesion.getInstancia().iniciarSesion(empleado);

        // Por defecto simulamos caja abierta y stock suficiente; cada test que
        // necesite lo contrario sobreescribe el stub correspondiente.
        CashSession sesionAbierta = new CashSession();
        sesionAbierta.setIdCashSession(1);
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesionAbierta);
        when(productoDao.obtenerStockReal(anyString())).thenReturn(999);
        when(productoDao.obtenerIdPorCodigo(anyString())).thenReturn(1);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionProductoDao.close();
        construccionCashSessionDao.close();
        construccionVentaDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private Producto productoDePrueba(String codigo, double precio, int stock) {
        Producto p = new Producto();
        p.setFullProductCode(codigo);
        p.setProductDescription("PRODUCTO JUNIT " + codigo);
        p.setBrandName("Marca");
        p.setCurrentPrice(new BigDecimal(String.valueOf(precio)));
        p.setCurrentStock(stock);
        return p;
    }

    private Object invocarPrivado(String nombre, Class<?>[] tipos, Object... args) throws Exception {
        Method m = VentaController.class.getDeclaredMethod(nombre, tipos);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    private void agregarProductoAlCarrito(Producto p) throws Exception {
        invocarPrivado("agregarProductoAlCarrito", new Class<?>[]{Producto.class}, p);
    }

    // --- agregarProductoAlCarrito(Producto) ---

    @Test
    void agregarProductoAlCarrito_sinStock_muestraError() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 0));

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("Producto sin stock disponible."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
    }

    @Test
    void agregarProductoAlCarrito_conStock_agregaFilaYActualizaTotal() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));

        assertEquals(1, tablaVenta.getModel().getRowCount());
        assertEquals("$10.00", lblTotal.getText());
    }

    // --- cancelarVenta() (btnCancelar) ---

    @Test
    void cancelarVenta_usuarioConfirma_limpiaElModulo() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(eq(vista), anyString(), eq("Confirmar"), eq(0)))
                    .thenReturn(0);

            btnCancelar.doClick();
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
        assertEquals("$0.00", lblTotal.getText());
    }

    @Test
    void cancelarVenta_usuarioCancela_noLimpiaElCarrito() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), eq(0)))
                    .thenReturn(1);

            btnCancelar.doClick();
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    // --- btnBack ---

    @Test
    void btnBack_comoVendedor_navegaAEmployee() {
        btnBack.doClick();
        verify(mainView).showView("employee");
    }

    // --- procesarCobro() (método simple: Efectivo/Transferencia) ---

    @Test
    void procesarCobro_carritoVacio_muestraAviso() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("El carrito está vacío.")));
        }
    }

    @Test
    void procesarCobro_sinCajaAbierta_muestraAviso() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Caja cerrada"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        assertTrue(construccionVentaDao.constructed().isEmpty());
    }

    @Test
    void procesarCobro_stockInsuficiente_muestraErrorYNoRegistra() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        when(productoDao.obtenerStockReal("AA00")).thenReturn(0);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            cbMetodoPago.setSelectedItem("Transferencia");
            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Error de Inventario"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobro_metodoNoSeleccionado_muestraAviso() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.addItem("Seleccionar...");
        cbMetodoPago.setSelectedItem("Seleccionar...");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Por favor, selecciona un método de pago.")));
        }
    }

    @Test
    void procesarCobro_efectivo_usuarioCancelaElInput_noRegistraNada() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Efectivo");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString(), eq("Cobro en Efectivo"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn(null);

            btnCobrar.doClick();
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobro_efectivo_montoInsuficiente_muestraAviso() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Efectivo");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString(), eq("Cobro en Efectivo"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("5.00");

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), anyString()));
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobro_efectivo_montoNoNumerico_muestraAviso() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Efectivo");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString(), eq("Cobro en Efectivo"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("no-numero");

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Ingrese un monto numérico válido.")));
        }
    }

    @Test
    void procesarCobro_efectivo_exitoso_muestraTicketYLimpiaCarrito() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Efectivo");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString(), eq("Cobro en Efectivo"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("20.00"); // paga con 20, cambio de 10

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Ticket de Venta"), eq(JOptionPane.PLAIN_MESSAGE)));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
        assertEquals("$0.00", lblTotal.getText());
        VentaDAO ventaDao = construccionVentaDao.constructed().get(0);
        verify(ventaDao).registrarVenta(any(), any());
    }

    @Test
    void procesarCobro_transferencia_exitoso_registraYLimpiaCarrito() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Transferencia");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Ticket de Venta"), eq(JOptionPane.PLAIN_MESSAGE)));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
        VentaDAO ventaDao = construccionVentaDao.constructed().get(0);
        verify(ventaDao).registrarVenta(any(), any());
    }

    @Test
    void procesarCobro_elDaoFalla_noLimpiaElCarrito() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Transferencia");

        // El VentaDAO se construye dentro de registrarVentaEnBD(), en el momento del clic;
        // stubbeamos vía el callback de mockConstruction para que ya esté configurado
        // desde su creación.
        construccionVentaDao.close();
        construccionVentaDao = mockConstruction(VentaDAO.class,
                (m, ctx) -> when(m.registrarVenta(any(), any())).thenReturn(false));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Ticket de Venta"), eq(JOptionPane.PLAIN_MESSAGE)), never());
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    // --- procesarCobroMixto() (cbMetodoPago == "Mixto") ---

    @Test
    void procesarCobroMixto_sinCajaAbierta_muestraAviso() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCobrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Caja cerrada"), eq(JOptionPane.WARNING_MESSAGE)));
        }
    }

    @Test
    void procesarCobroMixto_usuarioCancelaMontoTransferencia_noRegistraNada() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn(null);

            btnCobrar.doClick();
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobroMixto_montoTransferenciaExcedeTotal_muestraError() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("50.00"); // total es 10.00

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("El monto de transferencia no puede exceder el total de la venta.")));
        }
    }

    @Test
    void procesarCobroMixto_transferenciaCubreTodo_noPideEfectivoYRegistra() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("10.00"); // cubre el total exacto

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Ticket de Venta"), eq(JOptionPane.PLAIN_MESSAGE)));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobroMixto_usuarioCancelaMontoEfectivo_noRegistraNada() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("4.00", (String) null); // transferencia 4.00, luego cancela el de efectivo

            btnCobrar.doClick();
        }
        assertEquals(1, tablaVenta.getModel().getRowCount());
    }

    @Test
    void procesarCobroMixto_efectivoInsuficiente_muestraError() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("4.00", "1.00"); // faltan 6.00 en efectivo, solo da 1.00

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("El monto en efectivo es insuficiente.")));
        }
    }

    @Test
    void procesarCobroMixto_exitoso_registraTransferenciaYEfectivo() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("4.00", "6.00"); // 4 transferencia + 6 efectivo exacto

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Ticket de Venta"), eq(JOptionPane.PLAIN_MESSAGE)));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
        VentaDAO ventaDao = construccionVentaDao.constructed().get(0);
        verify(ventaDao).registrarVenta(any(), any());
    }

    @Test
    void procesarCobroMixto_montoNoNumerico_muestraError() throws Exception {
        agregarProductoAlCarrito(productoDePrueba("AA00", 10.00, 5));
        cbMetodoPago.setSelectedItem("Mixto");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(eq(vista), anyString()))
                    .thenReturn("no-numero");

            btnCobrar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Por favor, ingrese solo montos numéricos válidos.")));
        }
    }
}
