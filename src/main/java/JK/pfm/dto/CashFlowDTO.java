
package JK.pfm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashFlowDTO {
    private LocalDate date;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal netFlow;

    public CashFlowDTO(LocalDate date, BigDecimal inflow, BigDecimal outflow, BigDecimal netFlow) {
        this.date = date;
        this.inflow = inflow;
        this.outflow = outflow;
        this.netFlow = netFlow;
    }

    // Getters and setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getInflow() {
        return inflow;
    }

    public void setInflow(BigDecimal inflow) {
        this.inflow = inflow;
    }

    public BigDecimal getOutflow() {
        return outflow;
    }

    public void setOutflow(BigDecimal outflow) {
        this.outflow = outflow;
    }

    public BigDecimal getNetFlow() {
        return netFlow;
    }

    public void setNetFlow(BigDecimal netFlow) {
        this.netFlow = netFlow;
    }
}

