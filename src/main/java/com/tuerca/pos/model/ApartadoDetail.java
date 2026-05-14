/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.model;

/**
 *
 * @author mannycalderon
 */
public class ApartadoDetail {
    private int idBookingDetail;
    private int idBooking;
    private int idProduct;
    private int quantity;
    private double unitPrice;
    private double subtotalDetail;

    public int getIdBookingDetail() {
        return idBookingDetail;
    }

    public void setIdBookingDetail(int idBookingDetail) {
        this.idBookingDetail = idBookingDetail;
    }

    public int getIdBooking() {
        return idBooking;
    }

    public void setIdBooking(int idBooking) {
        this.idBooking = idBooking;
    }

    public int getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(int idProduct) {
        this.idProduct = idProduct;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubtotalDetail() {
        return subtotalDetail;
    }

    public void setSubtotalDetail(double subtotalDetail) {
        this.subtotalDetail = subtotalDetail;
    }
}
