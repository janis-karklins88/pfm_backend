package JK.pfm.service;

import JK.pfm.dto.BalanceBreakdownDTO;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.ChangesVsLastMonthDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.dto.ExpenseByAccountDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
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
import java.util.Optional;
import org.springframework.stereotype.Service;


@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final SavingsGoalService savingsGoalsService;
    private final AccountRepository accountRepository;
    private final SavingsGoalService savingsGoalService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final AccountUtil accountUtil;

    public ReportService(
        TransactionRepository transactionRepository,
        AccountService accountService,
        SavingsGoalService savingsGoalsService,
        AccountRepository accountRepository,
        SavingsGoalService savingsGoalService,
        SavingsGoalRepository savingsGoalRepository,
        AccountUtil accountUtil
    ) {
        this.transactionRepository    = transactionRepository;
        this.accountService           = accountService;
        this.savingsGoalsService      = savingsGoalsService;
        this.accountRepository        = accountRepository;
        this.savingsGoalService       = savingsGoalService;
        this.savingsGoalRepository    = savingsGoalRepository;
        this.accountUtil              = accountUtil;
    }


    /*********************************GET TOTAL SPENDING AND INCOME***************************************/
    
    public Map<String, BigDecimal> getSpendingAndIncomeSummary(DateRangeFilter filter) {

        //get user accounts    
        List<Long> accountIds = accountUtil.getUserAccountIds();
        //set dates, if not provided, set wide range
        LocalDate start = Optional.ofNullable(filter.getStartDate())
                              .orElse(LocalDate.of(1900, 1, 1));
        LocalDate end   = Optional.ofNullable(filter.getEndDate())
                              .orElse(LocalDate.now());
        
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDate("Expense", accountIds, start, end);
        BigDecimal totalIncome = transactionRepository.sumByTypeAndDate("Deposit", accountIds, start, end);

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalSpending", totalSpending);
        summary.put("totalIncome", totalIncome);
        return summary;
    }

    /*********************************GET TOTALUSER BALANCE****************************************/
    
    public BigDecimal getTotalUserBalance(){
        BigDecimal sum = accountService.getTotalBalance().add(savingsGoalsService.getTotalBalance());
        return sum;
    }
    
    
    /*********************************EXPENSE BREAKDOWN BY CATEGORY****************************************/
    /**
    * Retrieves the total expenses grouped by category for the current user
    * between the given start and end dates.
    *
    * @param getStartDate() the first date (inclusive) of the period to query
    * @param getEndDate()   the last date (inclusive) of the period to query
    * @return      a List of ExpenseByCategoryDTO, one entry per category;
    *              returns an empty list if the user has no accounts
    */
    public List<ExpenseByCategoryDTO> getSpendingByCategory(DateRangeFilter filter) {
        List<Long> accountIds = accountUtil.getUserAccountIds();
        // If no accounts exist for the user, return an empty map
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return transactionRepository.findExpensesByCategory(accountIds, filter.getStartDate(), filter.getEndDate());
    }
    
    
    /*********************************EXPENSE BREAKDOWN BY Account****************************************/
    /**
    * Retrieves the total expenses grouped by accounts for the current user
    * between the given start and end dates.
    *
    * @param start the first date (inclusive) of the period to query
    * @param end   the last date (inclusive) of the period to query
    * @return      a List of ExpenseByAccountDTO, one entry per account;
    *              returns an empty list if the user has no accounts
    */
    public List<ExpenseByAccountDTO> getSpendingByAccount(DateRangeFilter filter) {
        List<Long> accountIds = accountUtil.getUserAccountIds();
        // If no accounts exist for the user, return an empty map
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return transactionRepository.findExpensesByAccount(accountIds, filter.getStartDate(), filter.getEndDate());
    }
    
    
    
    
    /*********************************GET DAILY TRENDS****************************************/
    
    public List<DailyTrend> getDailyTrends(DateRangeFilter filter) {
        // Retrieve daily transaction data grouped by date and type.
        List<Object[]> results = transactionRepository.getDailyTrends(filter.getStartDate(), filter.getEndDate());
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
    
    /*********************************GET MONTHLY CASHFLOW****************************************/
    
    public List<CashFlowDTO> getMonthlyCashFlow() {
        
       List<CashFlowDTO> cashFlowList = new ArrayList<>();
       LocalDate today = LocalDate.now();
       
       for (int i = 6; i >= 0; i--) {

        LocalDate targetMonth = today.minusMonths(i);
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        DateRangeFilter filter = new DateRangeFilter(startOfMonth, endOfMonth);
        Map<String, BigDecimal> summary = getSpendingAndIncomeSummary(filter);
        BigDecimal inflow = summary.getOrDefault("totalIncome", BigDecimal.ZERO);
        BigDecimal outflow = summary.getOrDefault("totalSpending", BigDecimal.ZERO);
        BigDecimal netFlow = inflow.subtract(outflow);
        
        String monthLabel = startOfMonth.getMonth().toString();
        
        CashFlowDTO dto = new CashFlowDTO(monthLabel, inflow, outflow, netFlow);
        cashFlowList.add(dto);
    }
       

       return cashFlowList;
    }
    
    /*********************************GETBALANCE BREAKDOWN****************************************/
    
    public List<BalanceBreakdownDTO> getBalanceBreakdown(){
        Long userId = SecurityUtil.getUserId();
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(userId);
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
    
    /*********************************GET EXPENSE AND PREDICTIONS****************************************/
    
    public Map<String, BigDecimal> getExpenseAndPrediction(){
        //get user accounts
        List<Long> accountIds = accountUtil.getUserAccountIds();
        
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
        if(i > 0 && i <= 3){
            recentExpenses = recentExpenses.add(totalSpending);
        }
        }
        
        //calculate for how many months there is data
        int monthsWithData = (int) breakdown.values().stream()
        .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
        .count();
        
         //calculating averages and trend factor
        BigDecimal totalAverage = monthsWithData > 0 
            ? sumExpenses.divide(new BigDecimal(monthsWithData), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal recentAverage = recentExpenses.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP);
        BigDecimal trendFactor = BigDecimal.ONE;
            if (totalAverage.compareTo(BigDecimal.ZERO) != 0) {
                trendFactor = recentAverage.divide(totalAverage, 2, RoundingMode.HALF_UP);
            }
        
        
        
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
    
    /*********************************GET EXPENSE AND PREDICTION FOR SPECIFIC CATEGORY****************************************/
    
    public Map<String, BigDecimal> getExpenseForCategory(Long categoryId){
        //get user accounts
        List<Long> accountIds = accountUtil.getUserAccountIds();
        
        //get monthly expense
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        
        BigDecimal sumExpenses = BigDecimal.ZERO;
        BigDecimal recentExpenses = BigDecimal.ZERO;
        int monthsWithData = 0;
        
        for (int i = 9; i >= 0; i--) {

        LocalDate targetMonth = today.minusMonths(i);
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        String monthLabel = startOfMonth.getMonth().toString().substring(0, 3);
        
        BigDecimal totalSpending = transactionRepository.sumByTypeAndDateAndCategory("Expense", accountIds, startOfMonth, endOfMonth, categoryId);
        breakdown.put(monthLabel, totalSpending != null ? totalSpending : BigDecimal.ZERO);
        //for calculating average total and recent expenses 
        sumExpenses = sumExpenses.add(totalSpending);
        if(i > 0 && i <= 3){
            recentExpenses = recentExpenses.add(totalSpending);
        }
        // Check if there were any transactions in this month (regardless of category)
        boolean hasTransactions = transactionRepository
            .existsByAccountIdInAndDateBetween(accountIds, startOfMonth, endOfMonth);
        if (hasTransactions) {
            monthsWithData++;  // Count this month as valid data
        }
        }
               
        //calculating averages and trend factor
        BigDecimal totalAverage = monthsWithData > 0 
            ? sumExpenses.divide(new BigDecimal(monthsWithData), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal recentAverage = recentExpenses.divide(new BigDecimal(3), 2, RoundingMode.HALF_UP);
        BigDecimal trendFactor = BigDecimal.ONE;
            if (totalAverage.compareTo(BigDecimal.ZERO) != 0) {
                trendFactor = recentAverage.divide(totalAverage, 2, RoundingMode.HALF_UP);
            }
        
        
        
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
    
        /**
    * Calculates month-over-month percentage changes for:
    *   • Income
    *   • Expense
    *   • Savings balance
    *   • Total balance (accounts + savings)
    *   • Account balance (accounts only)
    *
    * @return a list of ChangesVsLastMonthDTO with whole-number percentage changes
    */
    public List<ChangesVsLastMonthDTO> getChanges() {
        List<ChangesVsLastMonthDTO> changes = new ArrayList<>();

        // 1) Compute date ranges
        LocalDate today           = LocalDate.now();
        LocalDate lastMonth       = today.minusMonths(1);
        LocalDate startLastMonth  = lastMonth.withDayOfMonth(1);
        LocalDate endLastMonth    = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        LocalDate startThisMonth  = today.withDayOfMonth(1);
        LocalDate endThisMonth    = today.withDayOfMonth(today.lengthOfMonth());

        // 2) Income & Expense
        DateRangeFilter filterThisMonth = new DateRangeFilter(startThisMonth, endThisMonth);
        DateRangeFilter filterLastMonth = new DateRangeFilter(startLastMonth, endLastMonth);
        Map<String, BigDecimal> thisM = getSpendingAndIncomeSummary(filterThisMonth);
        Map<String, BigDecimal> lastM = getSpendingAndIncomeSummary(filterLastMonth);

        BigDecimal thisIncome  = thisM.getOrDefault("totalIncome",  BigDecimal.ZERO);
        BigDecimal lastIncome  = lastM.getOrDefault("totalIncome",  BigDecimal.ZERO);
        BigDecimal incomePct   = calculatePercentChange(thisIncome, lastIncome);

        BigDecimal thisExpense = thisM.getOrDefault("totalSpending", BigDecimal.ZERO);
        BigDecimal lastExpense = lastM.getOrDefault("totalSpending", BigDecimal.ZERO);
        BigDecimal expensePct  = calculatePercentChange(thisExpense, lastExpense);

        changes.add(new ChangesVsLastMonthDTO("Income",  incomePct));
        changes.add(new ChangesVsLastMonthDTO("Expense", expensePct));

        // 3) Savings
        Long userId            = SecurityUtil.getUserId();
        BigDecimal currentSav  = savingsGoalRepository.getTotalBalanceByUserId(userId);
        BigDecimal lastSav     = savingsGoalRepository.getLastMonthBalanceByUserId(userId);
        BigDecimal savingsPct  = calculatePercentChange(currentSav, lastSav);
        changes.add(new ChangesVsLastMonthDTO("Savings", savingsPct));

        // 4) Total Balance = accounts + savings
        BigDecimal currentTotal   = getTotalUserBalance();
        BigDecimal lastAccounts   = transactionRepository.getAccountBalanceUpTo(userId, endLastMonth);
        BigDecimal lastTotal      = lastAccounts.add(lastSav);
        BigDecimal totalPct       = calculatePercentChange(currentTotal, lastTotal);
        changes.add(new ChangesVsLastMonthDTO("totalBalance", totalPct));

        // 5) Account Balance only
        BigDecimal accountPct     = calculatePercentChange(currentTotal, lastAccounts);
        changes.add(new ChangesVsLastMonthDTO("accountBalance", accountPct));

        return changes;
}

    //helper method for calculating changes in percentages
    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
     if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(100);
        }
        BigDecimal diff = current.subtract(previous);
        return diff
            .multiply(BigDecimal.valueOf(100))
            .divide(previous.abs(), 0, RoundingMode.HALF_UP);
    }

}

