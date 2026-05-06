/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.view.GestionEmprendedores;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoEmprendedor;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mannycalderon
 */
public class EmprendedorController {
    private GestionEmprendedores vistaGestion;
    private NuevoEmprendedor vistaRegistro;
    private EmprendedorDAO dao;
    private MainView mainView;
    
    public EmprendedorController(NuevoEmprendedor vReg, GestionEmprendedores vGest, MainView main) {
        this.vistaRegistro = vReg;
        this.vistaGestion = vGest;
        this.mainView = main;
        this.dao = new EmprendedorDAO();
        
        initTablaAcciones();
        initListeners();
    }
    
    private void initListeners() {
        this.vistaRegistro.getBtnRegistrar().addActionListener(e -> registrarEmprendedor());
        
        
        // AGREGAMOS EL LISTENER DEL MOUSE
        vistaGestion.getTablaEmprendedores().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaEmprendedores().columnAtPoint(e.getPoint());
                if (col == 7) {
                    vistaGestion.getTablaEmprendedores().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaEmprendedores().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }
    
    private void initTablaAcciones() {
        // Definimos qué pasa cuando se pulsan los botones
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                // Obtenemos el ID del empleado de la fila seleccionada (columna 0)
                int id = (int) vistaGestion.getTablaEmprendedores().getValueAt(row, 0);
                //prepararEdicion(id);
            }

            @Override
            public void onEliminar(int row) {
                int id = (int) vistaGestion.getTablaEmprendedores().getValueAt(row, 0);
                confirmarEliminacion(id, row);
            }
        };

        // Aplicamos el Render y el Editor a la columna 7 (Acciones)
        vistaGestion.getTablaEmprendedores().getColumnModel().getColumn(7).setCellRenderer(new AccionesRender());
        vistaGestion.getTablaEmprendedores().getColumnModel().getColumn(7).setCellEditor(new AccionesEditar(evento));

        // Tip: Aumenta un poco el alto de las filas para que los botones luzcan mejor
        vistaGestion.getTablaEmprendedores().setRowHeight(40);
    }
    
    // registrar nuevo emprendimiento
    private void registrarEmprendedor() {
        // 1. Extraer datos de la vista
        String marca = vistaRegistro.getBrandName();
        String contacto = vistaRegistro.getContactName();
        String tel = vistaRegistro.getContactPhone();
        String correo = vistaRegistro.getEmail();
        String rentaStr = vistaRegistro.getRent();
        java.util.Date fechaUtil = vistaRegistro.getFechaSeleccionada();

        // 2. Validación simple
        if (marca.isEmpty() || contacto.isEmpty() || rentaStr.isEmpty() || fechaUtil == null) {
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, completa los campos obligatorios.");
            return;
        }
        
        // validar emprendimiento
        if(marca.isEmpty()){
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, ingresa el nombre del emprendimiento.");
            return;
        }
        
        // validar emprendedor 
        if(contacto.isEmpty()){
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, ingresa el nombre del emprendedor.");
            return;
        }
        
        // validar emprendedor 
        if(rentaStr.isEmpty()){
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, ingresa el monto de la renta mensual.");
            return;
        }
        
        // validar fecha de contrato 
        if(fechaUtil == null){
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, ingresa la fecha de contrato.");
            return;
        }
        
        // convertir a java.sql.ddate para que mariadb lo acepte
        java.sql.Date fechaSQL = new java.sql.Date(fechaUtil.getTime());

        try {
            // 3. Empaquetar datos
            Emprendedor emp = new Emprendedor();
            emp.setMarca(marca);
            emp.setNombreContacto(contacto);
            emp.setTelefono(tel);
            emp.setEmail(correo);
            emp.setRentaMensual(Double.parseDouble(rentaStr));
            emp.setFechaContrato(new java.sql.Date(fechaSQL.getTime()));

            // 4. Ejecutar registro
            if (dao.registrar(emp)) {
                JOptionPane.showMessageDialog(vistaRegistro, "¡Emprendedor registrado con éxito!");
                vistaRegistro.limpiarFormulario(); // El método que ya ajustamos con setDate(null)
                cargarTabla();
                mainView.showView("entrepreneur"); // Regresar a la tabla
            } else {
                JOptionPane.showMessageDialog(vistaRegistro, "Error al guardar en la base de datos.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaRegistro, "La renta debe ser un número válido.");
        }
    }

    // consultar emprendedores
    public void cargarTabla() {
        DefaultTableModel modelo = (DefaultTableModel) vistaGestion.getTablaEmprendedores().getModel();
        modelo.setRowCount(0); // Limpiar tabla

        List<Emprendedor> lista = dao.listar();
        for (Emprendedor e : lista) {
            modelo.addRow(new Object[]{
                e.getId(),
                e.getMarca(),
                e.getNombreContacto(),
                e.getTelefono(),
                e.getEmail(),
                e.getFechaContrato(),
                "$" + e.getRentaMensual(),
                "" // Espacio para los botones de acción
                    
            });
        }
        initTablaAcciones();
    }
    
    private void confirmarEliminacion(int id, int row) {
        // 1. Notificación de confirmación
        int confirm = JOptionPane.showConfirmDialog(
            mainView, 
            "¿Estás seguro de que deseas eliminar al emprendimiento con ID: " + id + "?\n" +
            "Esta acción lo eliminará de la lista de gestión.",
            "Confirmar Eliminación Lógica", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // 2. Realizar la petición
            if (dao.eliminarLogico(id)) {
                // 3. Notificación de éxito
                JOptionPane.showMessageDialog(mainView, "Emprendimiento desactivado con éxito.");

                // 4. Refrescar estado (la tabla volverá a consultar solo los activos)
                cargarTabla(); 
            } else {
                JOptionPane.showMessageDialog(mainView, "Error al intentar desactivar al Emprendimiento.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
