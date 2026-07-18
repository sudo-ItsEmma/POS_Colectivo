package com.tuerca.pos.controller;

import com.tuerca.pos.dao.DevolucionDAO;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.GestionDevoluciones;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.components.AutorizacionAdminDialog;

import java.lang.reflect.Method;
import java.util.List;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code abrirDetalleVenta()} construye un {@link javax.swing.JDialog} modal
 * con una tabla anidada completamente local al método (sin getter en la
 * vista) — no hay forma de simular un clic real en su botón de acciones
 * desde fuera. Se prueba {@code procesarDevolucion()} (la lógica de negocio
 * real: confirmar, pedir motivo, autorizar, y registrar) invocándolo por
 * reflexión, un patrón aceptado cuando el método no es alcanzable por la API
 * pública y no se justifica refactorizar producción solo para probarlo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevolucionControllerTest {

    @Mock
    private GestionDevoluciones vista;
    @Mock
    private MainView mainView;

    private final JTextField txtBuscar = new JTextField();
    private final JTable tablaVentas = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Folio", "Total", "Productos", "Fecha", "Vendedor", "Acciones"}));

    private MockedConstruction<DevolucionDAO> construccionDevolucionDao;
    private MockedConstruction<EmpleadoDAO> construccionEmpleadoDao;
    private DevolucionDAO devolucionDao;
    private DevolucionController controller;

    @BeforeEach
    void construirController() {
        when(vista.getTxtBuscar()).thenReturn(txtBuscar);
        when(vista.getTablaVentas()).thenReturn(tablaVentas);

        construccionDevolucionDao = mockConstruction(DevolucionDAO.class,
                (mock, context) -> when(mock.buscarVentas(any())).thenReturn(List.of()));
        construccionEmpleadoDao = mockConstruction(EmpleadoDAO.class);

        controller = new DevolucionController(vista, mainView);
        devolucionDao = construccionDevolucionDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionDevolucionDao.close();
        construccionEmpleadoDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private void invocarProcesarDevolucion(int idSaleDetail, String codigo, double monto, Runnable alTerminar) throws Exception {
        Method m = DevolucionController.class.getDeclaredMethod(
                "procesarDevolucion", int.class, String.class, double.class, Runnable.class);
        m.setAccessible(true);
        m.invoke(controller, idSaleDetail, codigo, monto, alTerminar);
    }

    private void iniciarSesionComo(String roleName) {
        Empleado emp = new Empleado();
        emp.setIdUserAccount(9);
        emp.setId(1);
        emp.setNombre("Test");
        emp.setPaterno("User");
        emp.setUsername("testuser");
        emp.setIdRole(roleName.equals("Admin") ? 1 : 2);
        emp.setRoleName(roleName);
        Sesion.getInstancia().iniciarSesion(emp);
    }

    @Test
    void constructor_cargaLaTablaDeVentasAlIniciar() {
        verify(devolucionDao).buscarVentas("");
    }

    @Test
    void procesarDevolucion_usuarioCancelaLaConfirmacion_noHaceNadaMas() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(
                    any(), anyString(), eq("Confirmar Devolución"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarProcesarDevolucion(1, "JT01", 20.0, () -> { });

            jOptionPane.verify(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()), never());
        }
        verify(devolucionDao, never()).procesarDevolucion(anyInt(), anyInt(), any(), anyDouble());
    }

    @Test
    void procesarDevolucion_sinMotivo_muestraAvisoYNoRegistra() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()))
                    .thenReturn("   "); // motivo en blanco

            invocarProcesarDevolucion(1, "JT01", 20.0, () -> { });

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(), eq("La devolución requiere un motivo. Operación cancelada.")));
        }
        verify(devolucionDao, never()).procesarDevolucion(anyInt(), anyInt(), any(), anyDouble());
    }

    @Test
    void procesarDevolucion_comoAdmin_autorizaConSuPropioIdSinPedirCredenciales() throws Exception {
        iniciarSesionComo("Admin");
        when(devolucionDao.procesarDevolucion(1, 9, "Producto defectuoso", 20.0)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<AutorizacionAdminDialog> autorizacion = mockStatic(AutorizacionAdminDialog.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()))
                    .thenReturn("Producto defectuoso");

            boolean[] alTerminarLlamado = {false};
            invocarProcesarDevolucion(1, "JT01", 20.0, () -> alTerminarLlamado[0] = true);

            autorizacion.verifyNoInteractions();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(), eq("Devolución registrada. El stock fue devuelto al inventario.")));
            org.junit.jupiter.api.Assertions.assertTrue(alTerminarLlamado[0]);
        }
    }

    @Test
    void procesarDevolucion_comoEmpleado_pideAutorizacionDeAdminYContinuaSiSeConcede() throws Exception {
        iniciarSesionComo("Sales");
        when(devolucionDao.procesarDevolucion(1, 77, "Cliente arrepentido", 20.0)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<AutorizacionAdminDialog> autorizacion = mockStatic(AutorizacionAdminDialog.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()))
                    .thenReturn("Cliente arrepentido");
            autorizacion.when(() -> AutorizacionAdminDialog.solicitar(any(), any())).thenReturn(77);

            invocarProcesarDevolucion(1, "JT01", 20.0, () -> { });

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(), eq("Devolución registrada. El stock fue devuelto al inventario.")));
        }
    }

    @Test
    void procesarDevolucion_comoEmpleado_siAdminCancela_noRegistraNada() throws Exception {
        iniciarSesionComo("Sales");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<AutorizacionAdminDialog> autorizacion = mockStatic(AutorizacionAdminDialog.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()))
                    .thenReturn("Cualquier motivo");
            autorizacion.when(() -> AutorizacionAdminDialog.solicitar(any(), any())).thenReturn(null);

            invocarProcesarDevolucion(1, "JT01", 20.0, () -> { });
        }
        verify(devolucionDao, never()).procesarDevolucion(anyInt(), anyInt(), any(), anyDouble());
    }

    @Test
    void procesarDevolucion_elDaoFalla_muestraError() throws Exception {
        iniciarSesionComo("Admin");
        when(devolucionDao.procesarDevolucion(anyInt(), anyInt(), any(), anyDouble())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()))
                    .thenReturn("Motivo válido");

            invocarProcesarDevolucion(1, "JT01", 20.0, () -> { });

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(), eq("No se pudo procesar la devolución."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }
}
