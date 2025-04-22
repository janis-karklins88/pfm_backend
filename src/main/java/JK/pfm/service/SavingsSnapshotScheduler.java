
package JK.pfm.service;

import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SavingsSnapshotScheduler {

    private final SavingsGoalRepository savingsGoalRepo;
    private final TransactionRepository txnRepo;

    public SavingsSnapshotScheduler(SavingsGoalRepository savingsGoalRepo,
                                    TransactionRepository txnRepo) {
        this.savingsGoalRepo = savingsGoalRepo;
        this.txnRepo         = txnRepo;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void runAtStartup() {
        snapshotLastMonthAmounts();
    }

    /**
     * Runs at 00:00 on the 1st of each month.
     * For each user, computes the cumulative savings balance
     * as of the last day of the previous month (LocalDate),
     * then writes it into SavingsGoal.lastMonthAmount.
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void snapshotLastMonthAmounts() {
        LocalDate cutoffDate = LocalDate.now()
            .minusMonths(1)
            .with(TemporalAdjusters.lastDayOfMonth());

        // for each user with savings goals
        for (Long userId : savingsGoalRepo.findDistinctUserIds()) {
            BigDecimal cumulative = txnRepo.getSavingsBalanceUpTo(userId, cutoffDate);
            savingsGoalRepo.updateLastMonthAmountByUserId(userId, cumulative);
        }
    }
}

