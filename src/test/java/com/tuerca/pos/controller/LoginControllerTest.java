package com.tuerca.pos.controller;

import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.AperturaCajaPanel;
import com.tuerca.pos.view.LoginPanel;
import com.tuerca.pos.view.MainView;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginControllerTest {

    @Mock
    private LoginPanel vista;
    @Mock
    private AperturaCajaPanel aperturaCajaPanel;
    @Mock
    private MainView mainView;

    private final JButton btnIniciarSesion = new JButton();
    private final JTextField userField = new JTextField();
    private final JPasswordField contraField = new JPasswordField();

    private MockedConstruction<EmpleadoDAO> construccionEmpleadoDao;
    private MockedConstruction<CashSessionDAO> construccionCashSessionDao;
    private EmpleadoDAO empleadoDao;
    private CashSessionDAO cashSessionDao;
    private LoginController controller;

    @BeforeEach
    void construirController() {
        when(vista.getBtnIniciarSesion()).thenReturn(btnIniciarSesion);
        when(vista.getUserField()).thenReturn(userField);
        when(vista.getContraField()).thenReturn(contraField);

        construccionEmpleadoDao = mockConstruction(EmpleadoDAO.class);
        construccionCashSessionDao = mockConstruction(CashSessionDAO.class);

        controller = new LoginController(vista, aperturaCajaPanel, mainView);

        empleadoDao = construccionEmpleadoDao.constructed().get(0);
        cashSessionDao = construccionCashSessionDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionEmpleadoDao.close();
        construccionCashSessionDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private Empleado empleadoDePrueba(String roleName, boolean mustChangePassword) {
        Empleado emp = new Empleado();
        emp.setIdUserAccount(3);
        emp.setId(1);
        emp.setNombre("Test");
        emp.setPaterno("User");
        emp.setUsername("testuser");
        emp.setIdRole(roleName.equals("Admin") ? 1 : 2);
        emp.setRoleName(roleName);
        emp.setMustChangePassword(mustChangePassword);
        return emp;
    }

    @Test
    void iniciarSesion_camposVacios_muestraAvisoYReactivaBoton() {
        when(vista.getUsuario()).thenReturn("");
        when(vista.getContrasena()).thenReturn(new char[0]);
        btnIniciarSesion.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnIniciarSesion.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), anyString()));
        }
        verify(empleadoDao, never()).autenticar(any(), any());
        assertTrue(btnIniciarSesion.isEnabled());
    }

    @Test
    void iniciarSesion_credencialesInvalidas_muestraErrorYLimpiaContrasena() {
        when(vista.getUsuario()).thenReturn("testuser");
        when(vista.getContrasena()).thenReturn("malaClave".toCharArray());
        when(empleadoDao.autenticar("testuser", "malaClave")).thenReturn(null);
        btnIniciarSesion.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnIniciarSesion.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), eq("Usuario o contraseña incorrectos."), eq("Error de acceso"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(vista).limpiarContrasena();
        assertTrue(btnIniciarSesion.isEnabled());
    }

    @Test
    void iniciarSesion_exitosaSinCajaAbierta_navegaAAperturaDeCaja() {
        when(vista.getUsuario()).thenReturn("testuser");
        when(vista.getContrasena()).thenReturn("buenaClave".toCharArray());
        when(empleadoDao.autenticar("testuser", "buenaClave")).thenReturn(empleadoDePrueba("Sales", false));
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        btnIniciarSesion.doClick();

        verify(vista).limpiarFormulario();
        verify(aperturaCajaPanel).resetear();
        verify(aperturaCajaPanel).setNombreUsuario("Test User");
        verify(mainView).showView("aperturaCaja");
    }

    @Test
    void iniciarSesion_exitosaConCajaAbierta_navegaSegunRol() {
        when(vista.getUsuario()).thenReturn("adminuser");
        when(vista.getContrasena()).thenReturn("buenaClave".toCharArray());
        when(empleadoDao.autenticar("adminuser", "buenaClave")).thenReturn(empleadoDePrueba("Admin", false));
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(new CashSession());

        btnIniciarSesion.doClick();

        verify(mainView).showView("admin");
        verify(mainView, never()).showView("aperturaCaja");
    }

    @Test
    void iniciarSesion_debeCambiarContrasena_navegaAPantallaForzadaSinContinuarElFlujoNormal() {
        when(vista.getUsuario()).thenReturn("testuser");
        when(vista.getContrasena()).thenReturn("temporal1".toCharArray());
        when(empleadoDao.autenticar("testuser", "temporal1")).thenReturn(empleadoDePrueba("Sales", true));

        btnIniciarSesion.doClick();

        verify(mainView).showView("cambiarContrasena");
        verify(mainView, never()).showView("aperturaCaja");
        verify(mainView, never()).showView("employee");
        verify(cashSessionDao, never()).obtenerSesionAbierta();
    }

    @Test
    void continuarFlujoPostLogin_conCajaAbierta_navegaAlDashboardDelRolActivo() {
        Empleado admin = empleadoDePrueba("Admin", false);
        Sesion.getInstancia().iniciarSesion(admin);
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(new CashSession());

        controller.continuarFlujoPostLogin();

        verify(mainView).showView("admin");
    }
}
