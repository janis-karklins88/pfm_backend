package JK.pfm.service;

import JK.pfm.dto.BudgetVsActualDTO;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;


@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public ReportService(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    //total spending and income
    public Map<String, BigDecimal> getSpendingAndIncomeSummary(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDate("Expense", start, end);
        BigDecimal totalIncome = transactionRepository.sumByTypeAndDate("Income", start, end);

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalSpending", totalSpending);
        summary.put("totalIncome", totalIncome);
        return summary;
    }
    
    //net savings calculation
    public BigDecimal calculateNetSavings(LocalDate start, LocalDate end){
        Validations.checkDate(start);
        Validations.checkDate(end);
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDate("Expense", start, end);
        BigDecimal totalIncome = transactionRepository.sumByTypeAndDate("Income", start, end);
        return totalIncome.subtract(totalSpending);
    }
    
    //expenses breaked down by category
    public Map<String, BigDecimal> getSpendingByCategory(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        
        List<Object[]> results = transactionRepository.sumExpensesByCategory(start, end);
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (Object[] result : results) {
            String category = (String) result[0];
            BigDecimal total = (BigDecimal) result[1];
            breakdown.put(category, total);
        }
        return breakdown;
    }
    
    //getting daily trends, for now time unit is day, if trend is too granular, time unit should be increased
    public List<DailyTrend> getDailyTrends(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        
        // Retrieve daily transaction data grouped by date and type.
        List<Object[]> results = transactionRepository.getDailyTrends(start, end);
        Map<LocalDate, DailyTrend> trendMap = new HashMap<>();

        //populating map
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            String type = (String) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            //at default expense and income is 0
            DailyTrend dailyTrend = trendMap.getOrDefault(date, new DailyTrend(date, BigDecimal.ZERO, BigDecimal.ZERO));
            if ("Expense".equalsIgnoreCase(type)) {
                dailyTrend.setTotalExpense(dailyTrend.getTotalExpense().add(amount));
            } else if ("Income".equalsIgnoreCase(type)) {
                dailyTrend.setTotalIncome(dailyTrend.getTotalIncome().add(amount));
            }
            trendMap.put(date, dailyTrend);
        }

        // Convert map values to a sorted list by date
        List<DailyTrend> trends = new ArrayList<>(trendMap.values());
        trends.sort(Comparator.comparing(DailyTrend::getDate));
        return trends;
    }
    
    //calculate net daily cashflow
    public List<CashFlowDTO> getDailyCashFlow(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        
       List<CashFlowDTO> dailyCashFlow = new ArrayList<>();
       //getting daily income/expense
       List<DailyTrend> dailyIncomeExpense = getDailyTrends(start, end);
       
       for(DailyTrend row : dailyIncomeExpense){
           BigDecimal inflow = row.getTotalIncome();
           BigDecimal outflow = row.getTotalExpense();
           BigDecimal netflow = inflow.subtract(outflow);
           LocalDate date = row.getDate();
           //setting CashFlowDTO
           CashFlowDTO netDailyFlow = new CashFlowDTO(date, inflow, outflow, netflow);
           dailyCashFlow.add(netDailyFlow);
       }
       return dailyCashFlow;
    }
    
    //budget vs actual spending
    public List<BudgetVsActualDTO> getBudgetVsActual(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        
        // Retrieve actual spending by category
        List<Object[]> transactionData = transactionRepository.sumExpensesByCategory(start, end);
        Map<String, BigDecimal> actualByCategory = new HashMap<>();
        for (Object[] row : transactionData) {
            String category = (String) row[0];
            BigDecimal actual = (BigDecimal) row[1];
            actualByCategory.put(category, actual);
        }
        
        // Retrieve budgets
        List<Budget> budgets = budgetRepository.findAllBudgets();
        Map<String, BigDecimal> budgetByCategory = new HashMap<>();
        for (Budget budget : budgets) {
            Category category = budget.getCategory();
            budgetByCategory.put(category.getName(), budget.getAmount());
        }
        
        // Create a set of all categories (from either budgets or transactions)
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(budgetByCategory.keySet());
        allCategories.addAll(actualByCategory.keySet());
        
        // Build the list of DTOs
        List<BudgetVsActualDTO> report = new ArrayList<>();
        for (String category : allCategories) {
            BigDecimal budgeted = budgetByCategory.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal actual = actualByCategory.getOrDefault(category, BigDecimal.ZERO);
            report.add(new BudgetVsActualDTO(category, budgeted, actual));
        }
        
        return report;
    }
    
}

