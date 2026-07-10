package com.tuerca.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sesión de caja: apertura/arqueo/corte del fondo fijo del día.
 * Solo puede haber una con sessionStatus = "Abierta" a la vez.
 */
public class CashSession {

    private int idCashSession;
    private int idUserAccount;
    private LocalDateTime openingDateTime;
    private LocalDateTime closingDateTime;
    private BigDecimal initialCashAmount;
    private BigDecimal finalCashAmount;
    private BigDecimal theoricalAmount;
    private BigDecimal cashDifference;
    private String sessionStatus;

    // nombre de quien abrió la caja, para mostrar en UI (no es columna propia, viene de un JOIN)
    private String nombreUsuarioApertura;

    public CashSession() {
    }

    public int getIdCashSession() {
        return idCashSession;
    }

    public void setIdCashSession(int idCashSession) {
        this.idCashSession = idCashSession;
    }

    public int getIdUserAccount() {
        return idUserAccount;
    }

    public void setIdUserAccount(int idUserAccount) {
        this.idUserAccount = idUserAccount;
    }

    public LocalDateTime getOpeningDateTime() {
        return openingDateTime;
    }

    public void setOpeningDateTime(LocalDateTime openingDateTime) {
        this.openingDateTime = openingDateTime;
    }

    public LocalDateTime getClosingDateTime() {
        return closingDateTime;
    }

    public void setClosingDateTime(LocalDateTime closingDateTime) {
        this.closingDateTime = closingDateTime;
    }

    public BigDecimal getInitialCashAmount() {
        return initialCashAmount;
    }

    public void setInitialCashAmount(BigDecimal initialCashAmount) {
        this.initialCashAmount = initialCashAmount;
    }

    public BigDecimal getFinalCashAmount() {
        return finalCashAmount;
    }

    public void setFinalCashAmount(BigDecimal finalCashAmount) {
        this.finalCashAmount = finalCashAmount;
    }

    public BigDecimal getTheoricalAmount() {
        return theoricalAmount;
    }

    public void setTheoricalAmount(BigDecimal theoricalAmount) {
        this.theoricalAmount = theoricalAmount;
    }

    public BigDecimal getCashDifference() {
        return cashDifference;
    }

    public void setCashDifference(BigDecimal cashDifference) {
        this.cashDifference = cashDifference;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public boolean isAbierta() {
        return "Abierta".equalsIgnoreCase(sessionStatus);
    }

    public String getNombreUsuarioApertura() {
        return nombreUsuarioApertura;
    }

    public void setNombreUsuarioApertura(String nombreUsuarioApertura) {
        this.nombreUsuarioApertura = nombreUsuarioApertura;
    }
}
