package com.tuerca.pos.model;

import java.sql.Date;

/**
 * Modela un pago a emprendedor ({@code Settlement}, FN.9): el resultado de
 * liquidar un conjunto de ventas pendientes de un emprendedor en un periodo.
 */
public class Settlement {

    private int idSettlement;
    private int idEntrepreneur;
    private int idUserAccount;
    private Date settlementDate;
    private Date periodStartDate;
    private Date periodEndDate;
    private double grossAmount;
    private double totalDiscounts;
    private double rentDiscount;
    private double otherDiscounts;
    private double netAmountPaid;

    public int getIdSettlement() {
        return idSettlement;
    }

    public void setIdSettlement(int idSettlement) {
        this.idSettlement = idSettlement;
    }

    public int getIdEntrepreneur() {
        return idEntrepreneur;
    }

    public void setIdEntrepreneur(int idEntrepreneur) {
        this.idEntrepreneur = idEntrepreneur;
    }

    public int getIdUserAccount() {
        return idUserAccount;
    }

    public void setIdUserAccount(int idUserAccount) {
        this.idUserAccount = idUserAccount;
    }

    public Date getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(Date settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Date getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(Date periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public Date getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(Date periodEndDate) {
        this.periodEndDate = periodEndDate;
    }

    public double getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(double grossAmount) {
        this.grossAmount = grossAmount;
    }

    public double getTotalDiscounts() {
        return totalDiscounts;
    }

    public void setTotalDiscounts(double totalDiscounts) {
        this.totalDiscounts = totalDiscounts;
    }

    public double getRentDiscount() {
        return rentDiscount;
    }

    public void setRentDiscount(double rentDiscount) {
        this.rentDiscount = rentDiscount;
    }

    public double getOtherDiscounts() {
        return otherDiscounts;
    }

    public void setOtherDiscounts(double otherDiscounts) {
        this.otherDiscounts = otherDiscounts;
    }

    public double getNetAmountPaid() {
        return netAmountPaid;
    }

    public void setNetAmountPaid(double netAmountPaid) {
        this.netAmountPaid = netAmountPaid;
    }
}
