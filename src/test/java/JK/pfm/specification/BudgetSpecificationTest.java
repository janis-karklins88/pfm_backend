package JK.pfm.specification;

import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.specifications.BudgetSpecifications;
import JK.pfm.specifications.TransactionSpecifications;
import JK.pfm.util.JWTUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class BudgetSpecificationTest {

    @Autowired
    private BudgetRepository repo;

    private Category cat1;
    private User user;
    
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean 
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        user = new User(); 
        user.setPassword("password123");
        user.setUsername("username");
        em.persist(user);
        
        User user2 = new User();
        user2.setPassword("password123");
        user2.setUsername("username2");
        em.persist(user2);


        cat1 = new Category("Food");
        em.persist(cat1);
        
        // Budgets for user1
        // B1: Jan 1–Jan 31, user1
        persistBudget( user, LocalDate.of(2025,1,1),  LocalDate.of(2025,1,31), BigDecimal.TEN);
        // B2: Feb 1–Mar 1,   user1
        persistBudget( user, LocalDate.of(2025,2,1),  LocalDate.of(2025,3,1),  BigDecimal.valueOf(20));
        // B3: Mar 1–Mar 31,  user1
        persistBudget( user, LocalDate.of(2025,3,1),  LocalDate.of(2025,3,31), BigDecimal.valueOf(30));
        // B4: Apr 1–Apr 30,  user2 (different user)
        persistBudget( user2, LocalDate.of(2025,4,1),  LocalDate.of(2025,4,30), BigDecimal.valueOf(40));
    
    }
    
    private void persistBudget(User u, LocalDate start, LocalDate end, BigDecimal amt) {
        Budget b = new Budget();
        b.setUser(u);
        b.setCategory(cat1);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setAmount(amt);
        em.persist(b);
    }

    @Autowired
    private TestEntityManager em;
    
    @Test
    void belongsToUser_filtersCorrectly() {
        Specification<Budget> spec = BudgetSpecifications.belongsToUser(user.getId());
        List<Budget> results = repo.findAll(spec);
        // Should include B1, B2, B3 but not B4
        assertThat(results).hasSize(3)
            .allMatch(b -> b.getUser().getId().equals(user.getId()));
    }

    @Test
    void activeBetween_findsBudgetsOverlappingWindow() {
        // Window: Feb 15 – Mar 15 should catch B2 (Feb–Mar) and B3 (Mar)
        Specification<Budget> spec = BudgetSpecifications.activeBetween(
            LocalDate.of(2025,2,15),
            LocalDate.of(2025,3,15)
        );
        List<Budget> results = repo.findAll(spec);
        // all endDates in range?
        assertThat(results)
        .extracting(Budget::getEndDate)
        .allMatch(ed -> !ed.isBefore(LocalDate.of(2025, 2, 15)));

        // all startDates in range?
        assertThat(results)
        .extracting(Budget::getStartDate)
        .allMatch(sd -> !sd.isAfter(LocalDate.of(2025, 3, 15)));
    }

    @Test
    void startDateOnOrAfter_filtersCorrectly() {
        // Only B2 (start Feb 1) and B3 (start Mar 1) have startDate >= Feb 1
        Specification<Budget> spec = BudgetSpecifications.startDateOnOrAfter(LocalDate.of(2025,2,1));
        List<Budget> results = repo.findAll(spec);
        assertThat(results)
            .extracting(Budget::getStartDate)
            .allMatch(d -> !d.isBefore(LocalDate.of(2025,2,1)));
    }

    @Test
    void endDateOnOrBefore_filtersCorrectly() {
        // Only B1 (end Jan 31) and B2 (end Mar 1) have endDate <= Mar 1
        Specification<Budget> spec = BudgetSpecifications.endDateOnOrBefore(LocalDate.of(2025,3,1));
        List<Budget> results = repo.findAll(spec);
        assertThat(results)
            .extracting(Budget::getEndDate)
            .allMatch(d -> !d.isAfter(LocalDate.of(2025,3,1)));
    }

    @Test
    void combinedSpecifications_workTogether() {
        // Combine: belongsToUser(user1) AND startDate >= Feb 1
        Specification<Budget> spec = Specification
            .where(BudgetSpecifications.belongsToUser(user.getId()))
            .and(BudgetSpecifications.startDateOnOrAfter(LocalDate.of(2025,2,1)));

        List<Budget> results = repo.findAll(spec);
        // Expect B2 and B3 only
        assertThat(results)
            .extracting(Budget::getStartDate)
            .containsExactlyInAnyOrder(
                LocalDate.of(2025,2,1),
                LocalDate.of(2025,3,1)
            );
    }

 
}
