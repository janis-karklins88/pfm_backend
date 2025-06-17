
package JK.pfm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashFlowDTO {
    private String month;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal netFlow;
    
    public CashFlowDTO() {

    }

    public CashFlowDTO(String month, BigDecimal inflow, BigDecimal outflow, BigDecimal netFlow) {
        this.month = month;
        this.inflow = inflow;
        this.outflow = outflow;
        this.netFlow = netFlow;
    }

    // Getters and setters

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
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

