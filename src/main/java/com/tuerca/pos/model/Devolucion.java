package com.tuerca.pos.model;

import java.sql.Timestamp;

/**
 * Modela una fila de {@code ProductReturn} (FN.5): la devolución de una línea
 * específica de venta ({@code SaleDetail}), no de la venta completa.
 */
public class Devolucion {

    private int idReturn;
    private int idSaleDetail;
    private int idUserAccount;
    private String returnReason;
    private Timestamp returnDateTime;
    private double refundAmount;

    public int getIdReturn() {
        return idReturn;
    }

    public void setIdReturn(int idReturn) {
        this.idReturn = idReturn;
    }

    public int getIdSaleDetail() {
        return idSaleDetail;
    }

    public void setIdSaleDetail(int idSaleDetail) {
        this.idSaleDetail = idSaleDetail;
    }

    public int getIdUserAccount() {
        return idUserAccount;
    }

    public void setIdUserAccount(int idUserAccount) {
        this.idUserAccount = idUserAccount;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public Timestamp getReturnDateTime() {
        return returnDateTime;
    }

    public void setReturnDateTime(Timestamp returnDateTime) {
        this.returnDateTime = returnDateTime;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }
}
