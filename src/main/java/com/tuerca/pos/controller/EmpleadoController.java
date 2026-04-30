/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.view.NuevoEmpleado;
import javax.swing.JOptionPane;

/**
 *
 * @author mannycalderon
 */
public class EmpleadoController {
    private NuevoEmpleado vista;
    private EmpleadoDAO dao;
    
    // El constructor recibe la instancia del panel
    public EmpleadoController(NuevoEmpleado vista){
        this.vista = vista;
        this.dao = new EmpleadoDAO();
        
        // inicializamos los listeners
        initListeners();
    }
    
    private void initListeners(){
        // capturamos los datos al dar clic en Registrar
        this.vista.getBtnRegistrar().addActionListener(e -> registrarEmpleado());
        // Botón Cancelar
        this.vista.getBtnCancelar().addActionListener(e -> {
            vista.limpiarFormulario();
        });

        // Botón Volver
        this.vista.getBtnBack().addActionListener(e -> {
            vista.limpiarFormulario();
            // Cambiar de vista
        });
    }
    
    private void registrarEmpleado(){
        // capturar datos
        String nombre = vista.getNombre();
        String paterno = vista.getPaterno();
        String materno = vista.getMaterno();
        String numero = vista.getTelefono();
        String contra = vista.getContra();
        String confirma = vista.getConfirmarContra();
        
        // validación de datos
        // nombre, apellido paterno y contraseña no esten vacios
        if(nombre.isEmpty() || paterno.isEmpty() || contra.isEmpty()){
            JOptionPane.showMessageDialog(vista, "Por favor, rellena los campos obligatorios.");
            return;
        }
        
        // si el apellido materno esta vacio agregamos una X
        if(materno.trim().isEmpty()){
            materno = "X";
        }
        
        // contraseña con almenos 8 caracteres
        if(contra.length() < 8){
            JOptionPane.showMessageDialog(vista, "La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        
        // contraseñas iguales
        if(!contra.equals(confirma)){
            JOptionPane.showMessageDialog(vista, "Las contraseñas no coinciden.");
            return;
        }
        
        // procesamos los datos
        // Generamos nombre de usuario
        String rolSeleccionado = vista.getRol(); //Administrador o Vendedor (Admin o Sales)
        String userName = generarUsername(nombre, paterno, materno, rolSeleccionado);
        
        // Empaquetamos para llamar al DAO
        Empleado emp = new Empleado();
        emp.setNombre(nombre);
        emp.setPaterno(paterno);
        emp.setMaterno(materno);
        emp.setTelefono(numero);
        emp.setUsername(userName);
        emp.setPassword(contra); // La contraseña ya validada
        emp.setIdRole(rolSeleccionado.equals("Administrador") ? 1 : 2);
        if (dao.registrar(emp)) {
            JOptionPane.showMessageDialog(vista, "¡Empleado registrados con éxito!");
            vista.limpiarFormulario();
        }
    }
    
    private String generarUsername(String nom, String pat, String mat, String rol){
        String prefijo = rol.equals("Administrador") ? "AD" : "SA";
        String p1 = pat.substring(0,2).toUpperCase();
        String p2 = mat.substring(0,1).toUpperCase();
        String p3 = nom.substring(0,1).toUpperCase();
        int random = (int)(Math.random()*90+10);
        
        return prefijo + p1 + p2 + p3 + random;
    }
}
