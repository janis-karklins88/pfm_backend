
package JK.pfm.repository;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.dto.ExpenseByAccountDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
  "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TransactionRepositoryTest {

  @Autowired TestEntityManager em;
  @Autowired TransactionRepository repo;

  private User user;
    private User other;
    private Category cat1;
    private Category cat2;
    private Category cat3;
    private Account acc1;
    private Account acc2;

    
    @BeforeEach
    void setUp() {
        // correct user
        user = new User();
        user.setUsername("user1");
        user.setPassword("pw");
        user = em.persistAndFlush(user);
        //incorrect user
        other = new User();
        other.setUsername("user2");
        other.setPassword("pw");
        other = em.persistAndFlush(other);
        
        //categories
        cat1 = new Category("cat1");
        cat1.setIsDefault(false);
        cat2 = new Category("cat2");
        cat1.setIsDefault(true);
        cat3 = new Category("cat3");
        cat1.setIsDefault(false);
        em.persist(cat1);
        em.persist(cat2);
        em.persist(cat3);
              
        //
        acc1 = new Account("acc1", new BigDecimal("100"), user);
        acc1.setVersion(0L);
        acc2 = new Account("acc2", new BigDecimal("100"), user);
        acc2.setVersion(0L);
        em.persist(acc1);
        em.persist(acc2);
        
        em.flush();

    }
    
    @Test
    void findByCategoryId(){
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc2, cat1, "Expense", "txn2");
        var invalid = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat2, "Expense", "txn3");
        em.persist(txn1);
        em.persist(txn2);
        em.persist(invalid);
        em.flush();
        em.clear();
        
        List<Transaction> list = repo.findByCategoryId(cat1.getId());
        
        List<String> names = list.stream()
        .map(p -> p.getDescription())
        .collect(Collectors.toList());
        
        assertEquals(2, list.size());
        assertThat(names)
        .containsExactlyInAnyOrder(txn1.getDescription(), txn2.getDescription())
        .doesNotContain(invalid.getDescription());
    }
    
    @Test
    void findLast5txnDesc(){
        List<Long> accountIds = List.of(acc1.getId());
        
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now().minusDays(1), new BigDecimal("10"), acc1, cat1, "Expense", "txn2");
        var txn3 = new Transaction(LocalDate.now().minusDays(2), new BigDecimal("10"), acc1, cat1, "Expense", "txn3");
        var txn4 = new Transaction(LocalDate.now().minusDays(2), new BigDecimal("10"), acc1, cat1, "Expense", "txn4");
        var txn5 = new Transaction(LocalDate.now().minusDays(4), new BigDecimal("10"), acc1, cat1, "Expense", "txn5");
        var invalid1 = new Transaction(LocalDate.now().minusDays(5), new BigDecimal("10"), acc1, cat1, "Expense", "too old");
        var invalid2 = new Transaction(LocalDate.now().minusDays(1), new BigDecimal("10"), acc2, cat1, "Expense", "other users account");
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(invalid1);
        em.persist(invalid2);
        em.flush();
        em.clear();
        
        List<Transaction> txns = repo.findTop5ByAccountIdInOrderByIdDesc(accountIds);
        List<String> names = txns.stream()
        .map(p -> p.getDescription())
        .collect(Collectors.toList());
        
        assertEquals(5, txns.size());
        assertThat(names)
                .doesNotContain(invalid1.getDescription(), invalid2.getDescription())
                .containsExactly(
                    txn1.getDescription(),
                    txn2.getDescription(),
                    txn4.getDescription(),
                    txn3.getDescription(),
                    txn5.getDescription()
                );
    }
    
    @Test
    void sumByTypeDate(){
        List<Long> accountIds = List.of(acc1.getId());
        
        //valid
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat1, "Expense", "txn2");
        
        //invalid dates
        var txn3 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("10"), acc1, cat1, "Expense", "before date range");
        var txn4 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("10"), acc1, cat1, "Expense", "after date range");
        
        //invalid type
        var txn5 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Deposit", "Depost");
        
        //invalid account
        var txn6 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc2, cat1, "Expense", "other account");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(txn6);
        em.flush();
        em.clear();
        
        BigDecimal sum = repo.sumByTypeAndDate("Expense", accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        
        assertEquals(new BigDecimal("2.00"), sum);
    }
    
    @Test
    void sumByTypeDateAndCategory(){
        List<Long> accountIds = List.of(acc1.getId());
        
        //valid
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat1, "Expense", "txn2");
        
        //invalid dates
        var txn3 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("10"), acc1, cat1, "Expense", "before date range");
        var txn4 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("10"), acc1, cat1, "Expense", "after date range");
        
        //invalid type
        var txn5 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Deposit", "Depost");
        
        //invalid account
        var txn6 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc2, cat1, "Expense", "other account");
        
        //invalid category
        var txn7 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat2, "Expense", "wrong category");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(txn6);
        em.persist(txn7);
        em.flush();
        em.clear();
        
        BigDecimal sum = repo.sumByTypeAndDateAndCategory(
                "Expense", 
                accountIds, 
                LocalDate.now().minusDays(5), 
                LocalDate.now().plusDays(5), 
                cat1.getId()
            );
        
        assertEquals(new BigDecimal("2.00"), sum);
    }
    
    @Test
    void sumExpenseByCategory(){
        List<Long> accountIds = List.of(acc1.getId());
        
        //valid
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn2");
        var txn3 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc1, cat2, "Expense", "txn3");
        
        //invalid dates
        var txn4 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("50"), acc1, cat1, "Expense", "before start");
        var txn5 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("50"), acc1, cat1, "Expense", "after end");
        //invalid acc
        var txn6 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc2, cat1, "Expense", "wrong account");
        //excluded category
        var sysCat = new Category("Savings");
        sysCat.setIsDefault(false);
        em.persist(sysCat);
        var txn7 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, sysCat, "Expense", "excluded category");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(txn6);
        em.persist(txn7);
        em.flush();
        em.clear();
        
        List<ExpenseByCategoryDTO> breakdown = repo.findExpensesByCategory(accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        
        assertThat(breakdown)
            .hasSize(2)
            .extracting(
                ExpenseByCategoryDTO::getCategoryName,
                ExpenseByCategoryDTO::getTotalAmount
            )
            .containsExactlyInAnyOrder(
                tuple("cat1", new BigDecimal("20.00")),
                tuple("cat2", new BigDecimal("1.00"))
            );
        
    }
    
    @Test
    void sumExpenseByAccount(){
        List<Long> accountIds = List.of(acc1.getId(), acc2.getId());
        
        //valid
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat2, "Expense", "txn2");
        var txn3 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc2, cat3, "Expense", "txn3");
        
        //invalid dates
        var txn4 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("50"), acc1, cat1, "Expense", "before start");
        var txn5 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("50"), acc1, cat1, "Expense", "after end");
        //invalid acc
        Account acc3 = new Account ("acc3", new BigDecimal("100"), other);
        em.persist(acc3);
        var txn6 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc3, cat1, "Expense", "wrong account");
        //excluded category
        var sysCat = new Category("Savings");
        sysCat.setIsDefault(false);
        em.persist(sysCat);
        var txn7 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, sysCat, "Expense", "excluded category");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(txn6);
        em.persist(txn7);
        em.flush();
        em.clear();
        
        List<ExpenseByAccountDTO> breakdown = repo.findExpensesByAccount(accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        
        assertThat(breakdown)
            .hasSize(2)
            .extracting(
                ExpenseByAccountDTO::getAccountName,
                ExpenseByAccountDTO::getTotalAmount
            )
            .containsExactlyInAnyOrder(
                tuple("acc1", new BigDecimal("20.00")),
                tuple("acc2", new BigDecimal("1.00"))
            );
        
    }
    
    @Test
    void findByDateRange(){
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc2, cat1, "Expense", "txn2");
        var txn3 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("10"), acc2, cat1, "Expense", "before start");
        var txn4 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("10"), acc2, cat1, "Expense", "after end");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.flush();
        em.clear();
        
        List<Transaction> list = repo.findByDateBetween(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        
        List<String> names = list.stream()
        .map(p -> p.getDescription())
        .collect(Collectors.toList());
        
        assertEquals(2, list.size());
        assertThat(names)
        .containsExactlyInAnyOrder(txn1.getDescription(), txn2.getDescription())
        .doesNotContain(txn3.getDescription(), txn4.getDescription());
    }
    
    @Test
    void existsByAccountIdInAndDateBetween_true(){
        List<Long> accountIds = List.of(acc1.getId());
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");      
        em.persist(txn1);
        em.flush();
        em.clear();

        assertTrue(repo.existsByAccountIdInAndDateBetween(accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)));
    }
    
    @Test
    void existsByAccountIdInAndDateBetween_falseOutsideRange(){
        List<Long> accountIds = List.of(acc1.getId());
        var txn1 = new Transaction(LocalDate.now().minusDays(10), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");      
        em.persist(txn1);
        em.flush();
        em.clear();

        assertFalse(repo.existsByAccountIdInAndDateBetween(accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)));
    }
    
    @Test
    void existsByAccountIdInAndDateBetween_falseWrongAcc(){
        List<Long> accountIds = List.of(acc2.getId());
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Expense", "txn1");      
        em.persist(txn1);
        em.flush();
        em.clear();

        assertFalse(repo.existsByAccountIdInAndDateBetween(accountIds, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)));
    }
    
    @Test
    void getAccountBalanceUpTo(){
        //3 valid(1 depo, 2 expense), 1 wrong user, 1 after cutoff, 1 with sys cat
        var txn1 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, cat1, "Deposit", "txn1");
        var txn2 = new Transaction(LocalDate.now(), new BigDecimal("2"), acc1, cat2, "Expense", "txn2");
        var txn3 = new Transaction(LocalDate.now(), new BigDecimal("3"), acc2, cat3, "Expense", "txn3");
        
        Account acc3 = new Account ("acc3", new BigDecimal("100"), other);
        em.persist(acc3);
        var txn4 = new Transaction(LocalDate.now(), new BigDecimal("1"), acc3, cat1, "Expense", "incorrect user");
        
        var txn5 = new Transaction(LocalDate.now().plusDays(10), new BigDecimal("50"), acc1, cat1, "Expense", "after cutoff");
        
        var sysCat = new Category("Savings");
        sysCat.setIsDefault(false);
        em.persist(sysCat);
        var txn6 = new Transaction(LocalDate.now(), new BigDecimal("10"), acc1, sysCat, "Expense", "excluded category");
        
        em.persist(txn1);
        em.persist(txn2);
        em.persist(txn3);
        em.persist(txn4);
        em.persist(txn5);
        em.persist(txn6);
        em.flush();
        em.clear();     
        
        BigDecimal sum = repo.getAccountBalanceUpTo(user.getId(), LocalDate.now().plusDays(5));
        assertEquals(new BigDecimal("5.00"), sum);
    }
    
    @Test
    void netSavingsMonthlyBalance_filtersAndSumsCorrectly() {
    List<Long> accountIds = List.of(acc1.getId());
    LocalDate start = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
    LocalDate end   = start.with(TemporalAdjusters.lastDayOfMonth());

    // make sure there's a “Savings” category
    Category cat = new Category("Fund Transfer");
    em.persist(cat);

    // 2 valid savings transactions (one deposit, one withdrawal)
    var txn1 = new Transaction(
        start, 
        new BigDecimal("100"), 
        acc1, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );

    var txn2 = new Transaction(
        start.plusDays(5), 
        new BigDecimal("30"), 
        acc1, 
        cat, 
        "Deposit", 
        "Withdraw from savings"
    );

    // excluded: before date range
    var txn3 = new Transaction(
        start.minusDays(1), 
        new BigDecimal("10"), 
        acc1, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );

    // excluded: after date range
    var txn4 = new Transaction(
        end.plusDays(1), 
        new BigDecimal("10"), 
        acc1, 
        cat, 
        "Deposit", 
        "Withdraw from savings"
    );

    // excluded: wrong account
    var txn5 = new Transaction(
        start.plusDays(2), 
        new BigDecimal("20"), 
        acc2, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );

    // excluded: non‐savings category
    Category otherCat = new Category("Other");
    otherCat.setIsDefault(false);
    em.persist(otherCat);
    var txn6 = new Transaction(
        start.plusDays(2),
        new BigDecimal("20"),
        acc1,
        otherCat,
        "Expense",
        "Deposit to savings"
    );

    em.persist(txn1);
    em.persist(txn2);
    em.persist(txn3);
    em.persist(txn4);
    em.persist(txn5);
    em.persist(txn6);
    em.flush();
    em.clear();

    BigDecimal sum = repo.netSavingsMonthlyBalance(accountIds, start, end);
    // expected: 100 (deposit) – 30 (withdraw) = 70
    assertEquals(new BigDecimal("70.00"), sum);
}
    
    @Test
    void netSavingsBalanceUpToCutoffDate() {
    LocalDate today = LocalDate.now();

    Category cat = new Category("Fund Transfer");
    em.persist(cat);

    // 2 valid savings transactions (one deposit, one withdrawal)
    var txn1 = new Transaction(
        today.minusDays(5), 
        new BigDecimal("100"), 
        acc1, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );

    var txn2 = new Transaction(
        today, 
        new BigDecimal("30"), 
        acc1, 
        cat, 
        "Deposit", 
        "Withdraw from savings"
    );

    // excluded: after cutoff
    var txn3 = new Transaction(
        today.plusDays(1), 
        new BigDecimal("10"), 
        acc1, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );


    // excluded: wrong account
    Account acc3 = new Account ("acc3", new BigDecimal("100"), other);
        em.persist(acc3);
    var txn5 = new Transaction(
        today, 
        new BigDecimal("20"), 
        acc3, 
        cat, 
        "Expense", 
        "Deposit to savings"
    );

    // excluded: non‐savings category
    Category otherCat = new Category("Other");
    otherCat.setIsDefault(false);
    em.persist(otherCat);
    var txn6 = new Transaction(
        today,
        new BigDecimal("20"),
        acc1,
        otherCat,
        "Expense",
        "Deposit to savings"
    );

    em.persist(txn1);
    em.persist(txn2);
    em.persist(txn3);
    em.persist(txn5);
    em.persist(txn6);
    em.flush();
    em.clear();

    BigDecimal sum = repo.getSavingsBalanceUpTo(user.getId(), today);
    // expected: 100 (deposit) – 30 (withdraw) = 70
    assertEquals(new BigDecimal("70.00"), sum);
    }
    
    
    
}
