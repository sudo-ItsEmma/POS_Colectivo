package com.tuerca.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un arqueo de caja: comparación puntual entre el saldo teórico y el
 * efectivo contado. A diferencia del corte de caja (único por sesión,
 * columnas en {@link CashSession}), puede haber varios arqueos en la
 * misma sesión, ya que FN.7 permite hacerlo "en cualquier momento del
 * día, sin bloquear ventas".
 */
public class CashCount {

    private int idCashCount;
    private int idCashSession;
    private int idUserAccount;
    private LocalDateTime countDateTime;
    private BigDecimal theoricalAmount;
    private BigDecimal countedAmount;
    private BigDecimal cashDifference;
    private String justificationComment;

    public CashCount() {
    }

    public int getIdCashCount() {
        return idCashCount;
    }

    public void setIdCashCount(int idCashCount) {
        this.idCashCount = idCashCount;
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

    public LocalDateTime getCountDateTime() {
        return countDateTime;
    }

    public void setCountDateTime(LocalDateTime countDateTime) {
        this.countDateTime = countDateTime;
    }

    public BigDecimal getTheoricalAmount() {
        return theoricalAmount;
    }

    public void setTheoricalAmount(BigDecimal theoricalAmount) {
        this.theoricalAmount = theoricalAmount;
    }

    public BigDecimal getCountedAmount() {
        return countedAmount;
    }

    public void setCountedAmount(BigDecimal countedAmount) {
        this.countedAmount = countedAmount;
    }

    public BigDecimal getCashDifference() {
        return cashDifference;
    }

    public void setCashDifference(BigDecimal cashDifference) {
        this.cashDifference = cashDifference;
    }

    public String getJustificationComment() {
        return justificationComment;
    }

    public void setJustificationComment(String justificationComment) {
        this.justificationComment = justificationComment;
    }
}
