/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.model;

import java.sql.Date;

/**
 *
 * @author mannycalderon
 */
public class Venta {
    private int id;
    private int idUsuario;     // Quién vendió
    private double total;      // Monto total final
    private String metodoPago; // Efectivo o Transferencia
    private Date fecha;        // Momento de la venta

    public Venta() {}

    // Constructor útil para el registro
    public Venta(int idUsuario, double total, String metodoPago) {
        this.idUsuario = idUsuario;
        this.total = total;
        this.metodoPago = metodoPago;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
}
