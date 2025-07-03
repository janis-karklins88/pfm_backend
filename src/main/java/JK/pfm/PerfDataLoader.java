
package JK.pfm;

import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.UserService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("perf")
@Order(Ordered.LOWEST_PRECEDENCE)
public class PerfDataLoader implements CommandLineRunner {
    private final UserRepository      userRepo;
  private final AccountRepository   accountRepo;
  private final CategoryRepository  categoryRepo;
  private final TransactionRepository txnRepo;
  private final PasswordEncoder     passwordEncoder;
  @Autowired private UserService userService;

  public PerfDataLoader(UserRepository userRepo,
                        AccountRepository accountRepo,
                        CategoryRepository categoryRepo,
                        TransactionRepository txnRepo,
                        PasswordEncoder passwordEncoder) {
    this.userRepo        = userRepo;
    this.accountRepo     = accountRepo;
    this.categoryRepo    = categoryRepo;
    this.txnRepo         = txnRepo;
    this.passwordEncoder = passwordEncoder;
  }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("🔹 [perf] Starting PerfDataLoader...");
    // ── 1) Create the perf user 
    String username = "perfUser";
    Optional<User> maybe = userRepo.findByUsername(username);
    User perfUser;
    if (maybe.isEmpty()) {
    var dto = new UserRegistrationDto(username, "Password123");
    perfUser = userService.saveUser(dto);   // this seeds prefs & settings
    } else {
    perfUser = maybe.get();
    }

    // ── 2) Grab the default categories that were seeded by SystemCategoryInitializer ───
    List<Category> cats = categoryRepo.findByIsDefaultTrue();

    // ── 3) Create 10 accounts for that user ───────────────────────────────────────
    for (int i = 1; i <= 10; i++) {
      Account acct = new Account("PerfAcct" + i, BigDecimal.valueOf(10_000), perfUser);
      acct = accountRepo.save(acct);

      // ── 4) Generate 10 000 transactions per account ──────────────────────────
      LocalDate start = LocalDate.now().minusYears(1);
      for (int j = 0; j < 10_000; j++) {
        LocalDate date = start.plusDays(ThreadLocalRandom
          .current().nextInt(365));
        BigDecimal amount = BigDecimal.valueOf(
          ThreadLocalRandom.current().nextDouble(5, 500))
          .setScale(2, RoundingMode.HALF_UP);

        // pick a random default category
        Category cat = cats.get(ThreadLocalRandom
          .current().nextInt(cats.size()));
        // Then for each random transaction:
        String type = cat.getName().equals("Salary")
        ? "Deposit"
        : "Expense";

        Transaction t = new Transaction(date, amount, acct, cat, type, "perf");
        txnRepo.save(t);
      }
    }

    System.out.println("✅ PerfDataLoader complete: 10 accounts × 10 000 txns each.");
  }
}
