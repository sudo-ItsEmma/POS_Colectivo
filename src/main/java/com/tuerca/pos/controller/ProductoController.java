/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.view.GestionProductos;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoProducto;
import java.util.List;

/**
 *
 * @author mannycalderon
 */
public class ProductoController {
    private GestionProductos vistaGestion;
    private NuevoProducto vistaRegistro;
    private MainView mainView;
    private ProductoDAO productoDao;
    
    // EL CONSTRUCTOR: Es el corazón de la conexión
    public ProductoController(GestionProductos vistaGestion, NuevoProducto vistaRegistro, MainView mainView) {
        this.vistaGestion = vistaGestion;
        this.vistaRegistro = vistaRegistro;
        this.mainView = mainView;
        this.productoDao = new ProductoDAO();

        // 1. Cargamos los datos iniciales al arrancar
        cargarCombos();
        
        // 2. Aquí conectaremos los botones y eventos más adelante
        initListeners();
    }

    private void initListeners() {
        // Por ejemplo, cuando cambien la selección del combo:
        // vistaGestion.getCbFiltroEmprendedor().addActionListener(e -> filtrarTabla());
    }
    
    
    public void cargarCombos() {
        EmprendedorDAO empDao = new EmprendedorDAO();
        List<Emprendedor> lista = empDao.listarNombresYId();

        // Limpiar y llenar combo de la tabla (filtro)
        vistaGestion.getCbFiltroEmprendedor().removeAllItems();
        vistaGestion.getCbFiltroEmprendedor().addItem("--- Todos ---");
        
        // Limpiar y llenar combo del registro (obligatorio)
        vistaRegistro.getCbEmprendedor().removeAllItems();
        vistaRegistro.getCbEmprendedor().addItem("Selecciona un emprendedor...");

        for (Emprendedor emp : lista) {
            vistaGestion.getCbFiltroEmprendedor().addItem(emp);
            vistaRegistro.getCbEmprendedor().addItem(emp);
        }
    }
    
    public int getSelectedEntrepreneurId() {
        Object seleccion = vistaGestion.getCbFiltroEmprendedor().getSelectedItem();

        if (seleccion instanceof Emprendedor) {
            return ((Emprendedor) seleccion).getId();
        }
        return 0; // Si seleccionó "--- Todos ---"
    }
}
