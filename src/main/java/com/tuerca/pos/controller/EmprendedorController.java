/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoEmprendedor;
import javax.swing.JOptionPane;

/**
 *
 * @author mannycalderon
 */
public class EmprendedorController {
    private NuevoEmprendedor vistaRegistro;
    private EmprendedorDAO dao;
    private MainView mainView;
    
    public EmprendedorController(NuevoEmprendedor vReg, MainView main) {
        this.vistaRegistro = vReg;
        this.mainView = main; // <--- Se asigna aquí
        this.dao = new EmprendedorDAO();
        
        initListeners();
    }
    
    private void initListeners() {
        this.vistaRegistro.getBtnRegistrar().addActionListener(e -> registrarEmprendedor());
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
                mainView.showView("entrepreneur"); // Regresar a la tabla
            } else {
                JOptionPane.showMessageDialog(vistaRegistro, "Error al guardar en la base de datos.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaRegistro, "La renta debe ser un número válido.");
        }
    }

    
}
