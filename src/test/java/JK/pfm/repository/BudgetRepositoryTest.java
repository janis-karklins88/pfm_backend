package JK.pfm.repository;

import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")        
class BudgetRepositoryTest {


    @Autowired
    private BudgetRepository repo;
    
    @Autowired
    private TestEntityManager em;
    
    private User user;
    private User other;

    
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

    }
    
    @Test
    void getTotalSpentOnBudget_filtersByCategoryDateAccount(){
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 1, 31);
        
        Category targetCat   = em.persist(new Category("target"));
        targetCat.setIsDefault(false);
        em.flush();

        Category otherCat    = em.persist(new Category("other"));
        otherCat.setIsDefault(false);
        em.flush();
        
        User u = em.persist(new User("u","pw"));
        Account acct1 = new Account("A1", BigDecimal.ZERO, u);
        acct1.setVersion(0L);
        em.persist(acct1);

        Account acct2 = new Account("A2", BigDecimal.ZERO, u);
        acct2.setVersion(0L);
        em.persist(acct2);

        em.flush();
        
        //valid transactions
        em.persist(new Transaction (start, BigDecimal.valueOf(10), acct1, targetCat, "Expense", "t1"));
        em.persist(new Transaction (start.plusDays(5), BigDecimal.valueOf(5), acct1, targetCat, "Expense", "t2"));
        em.persist(new Transaction (start, BigDecimal.valueOf(30), acct1, targetCat, "Expense", "t3"));
        
        //invalid transactions
        em.persist(new Transaction (start.plusDays(3), BigDecimal.valueOf(10), acct1, otherCat, "Expense", "wrong category"));
        em.persist(new Transaction (start.minusDays(1), BigDecimal.valueOf(10), acct1, targetCat, "Expense", "too early"));
        em.persist(new Transaction (end.plusDays(1), BigDecimal.valueOf(10), acct1, targetCat, "Expense", "too late"));
        em.persist(new Transaction (start.plusDays(2), BigDecimal.valueOf(10), acct2, targetCat, "Expense", "wrong acc"));
        
        em.flush();
        em.clear();
        
        BigDecimal total = repo.getTotalSpentOnBudget(
        targetCat.getId(), start, end, List.of(acct1.getId())
        );
        assertEquals(new BigDecimal("45.00"), total);
        }
    
    @Test
    void findMonthlyBudgets (){
        LocalDate today = LocalDate.now();
        
        Category category   = em.persist(new Category("test-cat"));
        
        // 1) VALID budget
        Budget valid = new Budget(
        BigDecimal.valueOf(100),
        today.minusDays(5),
        today.plusDays(5),
        category,
        user
        );
        valid.setMonthly(true);
        em.persist(valid);
        
        
        // 2) ENDED budget (end < today)
        Budget ended = new Budget(
        BigDecimal.valueOf(200),
        today.minusDays(10),
        today.minusDays(1),
        category,
        user
        );
        ended.setMonthly(true);
        em.persist(ended);

        // 3) NOT-YET-STARTED budget (start > today)
        Budget future = new Budget(
        BigDecimal.valueOf(300),
        today.plusDays(1),
        today.plusDays(10),
        category,
        user
        );
        future.setMonthly(true);
        em.persist(future);

        // 4) INACTIVE (monthly=false but date-range matches)
        Budget invalid = new Budget(
        BigDecimal.valueOf(100),
        today.minusDays(5),
        today.plusDays(5),
        category,
        user
        );
        invalid.setMonthly(false);
        em.persist(invalid);

        em.flush();
        em.clear();
        
        List<Budget> results = repo
        .findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);

        // Only the first one qualifies
        assertEquals(1, results.size(), "Should return exactly 1 budget");
        assertEquals(valid.getId(), results.get(0).getId(), "Only the valid monthly budget is returned");
    }
    
    @Test
    void existsNextMonth_correct(){
        LocalDate s1 = LocalDate.of(2025,6,1);
        Category category   = em.persist(new Category("test-cat"));
                
        // 1) VALID budget
        Budget valid = new Budget(
        BigDecimal.valueOf(100),
        s1,
        s1.plusDays(30),
        category,
        user
        );
        valid.setMonthly(true);
        em.persist(valid);
        em.flush();
        em.clear();
        
        assertTrue(repo.existsByUserAndCategoryAndStartDateAndMonthlyTrue(user, category, s1));
    }
    
    @Test
    void existsNextMonth_incorrectDate(){
        LocalDate s1 = LocalDate.of(2025,6,1);
        Category category   = em.persist(new Category("test-cat"));
        LocalDate s2 = LocalDate.of(2025,7,1);
        
        Budget valid = new Budget(
        BigDecimal.valueOf(100),
        s1,
        s1.plusDays(30),
        category,
        user
        );
        valid.setMonthly(true);
        em.persist(valid);
        em.flush();
        em.clear();
        
        assertFalse(repo.existsByUserAndCategoryAndStartDateAndMonthlyTrue(user, category, s2));
    }
    
    @Test
    void existsNextMonth_incorrectCategory(){
        LocalDate s1 = LocalDate.of(2025,6,1);
        Category category   = em.persist(new Category("test-cat"));
        Category category2   = em.persist(new Category("test1-cat"));
                
        Budget valid = new Budget(
        BigDecimal.valueOf(100),
        s1,
        s1.plusDays(30),
        category2,
        user
        );
        valid.setMonthly(true);
        em.persist(valid);
        em.flush();
        em.clear();
        
        assertFalse(repo.existsByUserAndCategoryAndStartDateAndMonthlyTrue(user, category, s1));
    }
    
    @Test
    void existsNextMonth_incorrectUser(){
        LocalDate s1 = LocalDate.of(2025,6,1);
        Category category   = em.persist(new Category("test-cat"));
                
        Budget valid = new Budget(
        BigDecimal.valueOf(100),
        s1,
        s1.plusDays(30),
        category,
        other
        );
        valid.setMonthly(true);
        em.persist(valid);
        em.flush();
        em.clear();
        
        assertFalse(repo.existsByUserAndCategoryAndStartDateAndMonthlyTrue(user, category, s1));
    }
}    