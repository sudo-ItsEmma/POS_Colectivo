package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.CambiarContrasenaPanel;

import javax.swing.JButton;
import javax.swing.JOptionPane;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CambiarContrasenaControllerTest {

    @Mock
    private CambiarContrasenaPanel vista;
    @Mock
    private LoginController loginController;

    private final JButton btnCambiar = new JButton();
    private MockedConstruction<EmpleadoDAO> construccionDao;
    private EmpleadoDAO dao;

    @BeforeEach
    void construirController() {
        when(vista.getBtnCambiar()).thenReturn(btnCambiar);
        construccionDao = mockConstruction(EmpleadoDAO.class);

        new CambiarContrasenaController(vista, loginController);
        dao = construccionDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(55);
        empleado.setId(1);
        empleado.setNombre("Test");
        empleado.setPaterno("User");
        empleado.setUsername("testuser");
        empleado.setIdRole(2);
        empleado.setRoleName("Sales");
        Sesion.getInstancia().iniciarSesion(empleado);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    @Test
    void cambiarContrasena_menosDeOchoCaracteres_muestraAvisoYReactivaBoton() {
        when(vista.getNuevaContrasena()).thenReturn("corta".toCharArray());
        when(vista.getConfirmarContrasena()).thenReturn("corta".toCharArray());
        btnCambiar.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCambiar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), any(), eq("Contraseña inválida"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        verify(dao, never()).cambiarContrasena(anyInt(), any());
        assertTrue(btnCambiar.isEnabled());
    }

    @Test
    void cambiarContrasena_noCoinciden_muestraAvisoYReactivaBoton() {
        when(vista.getNuevaContrasena()).thenReturn("password1".toCharArray());
        when(vista.getConfirmarContrasena()).thenReturn("password2".toCharArray());
        btnCambiar.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCambiar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("Las contraseñas no coinciden."), eq("Contraseña inválida"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        verify(dao, never()).cambiarContrasena(anyInt(), any());
        assertTrue(btnCambiar.isEnabled());
    }

    @Test
    void cambiarContrasena_exitosa_llamaAlDaoYContinuaElFlujoPostLogin() {
        when(vista.getNuevaContrasena()).thenReturn("password1".toCharArray());
        when(vista.getConfirmarContrasena()).thenReturn("password1".toCharArray());
        when(dao.cambiarContrasena(55, "password1")).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCambiar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Contraseña actualizada con éxito.")));
        }
        verify(loginController).continuarFlujoPostLogin();
    }

    @Test
    void cambiarContrasena_elDaoFalla_muestraErrorYReactivaBotonSinContinuar() {
        when(vista.getNuevaContrasena()).thenReturn("password1".toCharArray());
        when(vista.getConfirmarContrasena()).thenReturn("password1".toCharArray());
        when(dao.cambiarContrasena(anyInt(), any())).thenReturn(false);
        btnCambiar.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCambiar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), any(), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(loginController, never()).continuarFlujoPostLogin();
        assertTrue(btnCambiar.isEnabled());
    }
}
