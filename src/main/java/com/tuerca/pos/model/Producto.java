/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.model;

/**
 *
 * @author mannycalderon
 */
public class Producto {
    private int idProduct;
    private int idEntrepreneur; // FK
    private String brandName;   // Para mostrar en la tabla sin hacer otra consulta
    private String fullProductCode;
    private String productDescription;
    private String department;
    private double currentPrice;
    private int currentStock;
    private int minStockAlert;
    private boolean active;

    public int getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(int idProduct) {
        this.idProduct = idProduct;
    }

    public int getIdEntrepreneur() {
        return idEntrepreneur;
    }

    public void setIdEntrepreneur(int idEntrepreneur) {
        this.idEntrepreneur = idEntrepreneur;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getFullProductCode() {
        return fullProductCode;
    }

    public void setFullProductCode(String fullProductCode) {
        this.fullProductCode = fullProductCode;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getMinStockAlert() {
        return minStockAlert;
    }

    public void setMinStockAlert(int minStockAlert) {
        this.minStockAlert = minStockAlert;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
}
