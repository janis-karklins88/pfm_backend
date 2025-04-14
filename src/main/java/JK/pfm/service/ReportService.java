package JK.pfm.service;

import JK.pfm.dto.BalanceBreakdownDTO;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.util.AccountSpecifications;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private SavingsGoalService savingsGoalsService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private SavingsGoalService savingsGoalService;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;

    }

    //total spending and income
    public Map<String, BigDecimal> getSpendingAndIncomeSummary(LocalDate start, LocalDate end) {
        Validations.checkDate(start);
        Validations.checkDate(end);
        //get user accounts
        Long userId = SecurityUtil.getUserId();        
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = new ArrayList<>();
        for(Account account : accounts){
            Long id = account.getId();
            accountIds.add(id);
        }
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDate("Expense", accountIds, start, end);
        BigDecimal totalIncome = transactionRepository.sumByTypeAndDate("Deposit", accountIds, start, end);

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalSpending", totalSpending);
        summary.put("totalIncome", totalIncome);
        return summary;
    }

    //get total user balance
    public BigDecimal getTotalUserBalance(){
        BigDecimal sum = accountService.getTotalBalance().add(savingsGoalsService.getTotalBalance());
        return sum;
    }
    
    
    //expenses breaked down by category
    public List<ExpenseByCategoryDTO> getSpendingByCategory(LocalDate start, LocalDate end) {
        
        Long userId = SecurityUtil.getUserId();
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = new ArrayList<>();
        for(Account acc : accounts){
            Long id = acc.getId();
            accountIds.add(id);
        }
        // If no accounts exist for the user, return an empty map
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return transactionRepository.findExpensesByCategory(accountIds, start, end);
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
    
    //calculate monthly cashflow
    public List<CashFlowDTO> getMonthlyCashFlow() {
        
       List<CashFlowDTO> cashFlowList = new ArrayList<>();
       LocalDate today = LocalDate.now();
       
       for (int i = 6; i >= 0; i--) {

        LocalDate targetMonth = today.minusMonths(i);
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        
        Map<String, BigDecimal> summary = getSpendingAndIncomeSummary(startOfMonth, endOfMonth);
        BigDecimal inflow = summary.getOrDefault("totalIncome", BigDecimal.ZERO);
        BigDecimal outflow = summary.getOrDefault("totalSpending", BigDecimal.ZERO);
        BigDecimal netFlow = inflow.subtract(outflow);
        
        String monthLabel = startOfMonth.getMonth().toString();
        
        CashFlowDTO dto = new CashFlowDTO(monthLabel, inflow, outflow, netFlow);
        cashFlowList.add(dto);
    }
       

       return cashFlowList;
    }
    
    //get balance breakdown
    public List<BalanceBreakdownDTO> getBalanceBreakdown(){
        Long userId = SecurityUtil.getUserId();
        List<Account> accounts = accountRepository.findAll(AccountSpecifications.belongsToUser(userId));
        List<BalanceBreakdownDTO> breakdown = new ArrayList<>();
        
        for(Account acc : accounts){
            BalanceBreakdownDTO balanceBreakdownDTO = new BalanceBreakdownDTO(acc.getName(), acc.getAmount());
            breakdown.add(balanceBreakdownDTO);
        }
        
        BigDecimal savingsBalance = savingsGoalService.getTotalBalance();
        BalanceBreakdownDTO balanceBreakdownDTO = new BalanceBreakdownDTO("Savings", savingsBalance);
        breakdown.add(balanceBreakdownDTO);
        
        return breakdown;
    }
    
    //get expense and prediction
    public Map<String, BigDecimal> getExpenseAndPrediction(){
        //get user accounts
        Long userId = SecurityUtil.getUserId();        
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = new ArrayList<>();
        for(Account account : accounts){
            Long id = account.getId();
            accountIds.add(id);
        }
        
        //get monthly expense
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        
        BigDecimal sumExpenses = BigDecimal.ZERO;
        BigDecimal recentExpenses = BigDecimal.ZERO;
        
        for (int i = 9; i >= 0; i--) {

        LocalDate targetMonth = today.minusMonths(i);
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        String monthLabel = startOfMonth.getMonth().toString().substring(0, 3);
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDate("Expense", accountIds, startOfMonth, endOfMonth);
        breakdown.put(monthLabel, totalSpending != null ? totalSpending : BigDecimal.ZERO);
        //for calculating average total and recent expenses 
        sumExpenses = sumExpenses.add(totalSpending);
        if(i > 7){
            recentExpenses = recentExpenses.add(totalSpending);
        }
        }
        
        //calculate for how many months there is data
        int monthsWithData = (int) breakdown.values().stream()
        .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
        .count();
        
        //calculating averages and trend factor
        BigDecimal totalAverage = sumExpenses.divide(new BigDecimal(monthsWithData), 2, RoundingMode.HALF_UP);
        BigDecimal recentAverage = sumExpenses.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP);
        BigDecimal trendFactor = recentAverage.divide(totalAverage, 2, RoundingMode.HALF_UP);
        
        
        
        //set predicted expense
        BigDecimal predictedExpense;
        if(monthsWithData < 6){
            predictedExpense = totalAverage;
        } else {
            predictedExpense = totalAverage.multiply(trendFactor);
        }
        
        
        // Add prediction for next 2 months
        LocalDate nextMonth = today.plusMonths(1);
        String nextMonthLabel = nextMonth.getMonth().toString().substring(0, 3);
        breakdown.put(nextMonthLabel, predictedExpense);
    
        LocalDate nextNextMonth = today.plusMonths(2);
        String nextNextMonthLabel = nextNextMonth.getMonth().toString().substring(0, 3);
        breakdown.put(nextNextMonthLabel, predictedExpense);
        
        

        return breakdown;
    }
}

