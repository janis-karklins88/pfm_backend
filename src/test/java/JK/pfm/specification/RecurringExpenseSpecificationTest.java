package JK.pfm.specification;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.User;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.specifications.RecurringExpenseSpecifications;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class RecurringExpenseSpecificationTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private RecurringExpenseRepository repo;

    private User user1, user2;
    private Account acct1, acct2;
    private Category cat1, cat2;

    @BeforeEach
    void setUp() {
        user1 = new User(); 
        user1.setPassword("password123");
        user1.setUsername("username");
        em.persist(user1);
        
        user2 = new User();
        user2.setPassword("password123");
        user2.setUsername("username2");
        em.persist(user2);

        acct1 = new Account("A1", BigDecimal.ZERO, user1); em.persist(acct1);
        acct2 = new Account("A2", BigDecimal.ZERO, user2); em.persist(acct2);

        cat1 = new Category("Cat1"); em.persist(cat1);
        cat2 = new Category("Cat2"); em.persist(cat2);

        // RE1: belongsTo user1, nextDueDate=2025-06-01, cat1, acct1
        persistRecurring(acct1, cat1, LocalDate.of(2025,6,1));
        // RE2: belongsTo user1, nextDueDate=2025-06-15, cat2, acct1
        persistRecurring(acct1, cat2, LocalDate.of(2025,6,15));
        // RE3: belongsTo user1, nextDueDate=2025-07-01, cat1, acct1
        persistRecurring(acct1, cat1, LocalDate.of(2025,7,1));
        // RE4: belongsTo user2, nextDueDate=2025-06-10, cat1, acct2
        persistRecurring(acct2, cat1, LocalDate.of(2025,6,10));
    }

    private void persistRecurring(Account acct, Category cat, LocalDate due) {
        RecurringExpense re = new RecurringExpense();
        re.setAccount(acct);
        re.setCategory(cat);
        re.setAmount(BigDecimal.TEN);
        re.setNextDueDate(due);
        em.persist(re);
    }

    @Test
    void belongsToUser_filtersCorrectly() {
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .belongsToUser(user1.getId());

        List<RecurringExpense> results = repo.findAll(spec);

        // Should include RE1, RE2, RE3 but not RE4
        assertThat(results).hasSize(3)
            .allMatch(re -> re.getAccount().getUser().getId().equals(user1.getId()));
    }

    @Test
    void dateBetween_returnsOnlyInRange() {
        // range June 1–June 30 should pick RE1, RE2, RE4 (all due in June)
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .dateBetween(LocalDate.of(2025,6,1), LocalDate.of(2025,6,30));

        List<RecurringExpense> results = repo.findAll(spec);

        assertThat(results).hasSize(3)
            .extracting(RecurringExpense::getNextDueDate)
            .allMatch(d -> !d.isBefore(LocalDate.of(2025,6,1)) &&
                           !d.isAfter(LocalDate.of(2025,6,30)));
    }

    @Test
    void dateGreaterThanOrEqual_filtersCorrectly() {
        // >= July 1 picks only RE3
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .dateGreaterThanOrEqual(LocalDate.of(2025,7,1));

        List<RecurringExpense> results = repo.findAll(spec);
        assertThat(results).hasSize(1)
            .allMatch(re -> re.getNextDueDate().compareTo(LocalDate.of(2025,7,1)) >= 0);
    }

    @Test
    void dateLessThanOrEqual_filtersCorrectly() {
        // <= June 10 picks RE1 (6/1), RE2 (6/15 is out), RE4 (6/10)
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .dateLessThanOrEqual(LocalDate.of(2025,6,10));

        List<RecurringExpense> results = repo.findAll(spec);
        assertThat(results).hasSize(2)
            .allMatch(re -> re.getNextDueDate().compareTo(LocalDate.of(2025,6,10)) <= 0);
    }

    @Test
    void categoryEquals_filtersCorrectly() {
        // cat1 picks RE1, RE3, RE4 (two for user1, one for user2)
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .categoryEquals(cat1.getId());

        List<RecurringExpense> results = repo.findAll(spec);
        assertThat(results).hasSize(3)
            .allMatch(re -> re.getCategory().getId().equals(cat1.getId()));
    }

    @Test
    void accountEquals_filtersCorrectly() {
        // acct2 picks only RE4
        Specification<RecurringExpense> spec = RecurringExpenseSpecifications
            .accountEquals(acct2.getId());

        List<RecurringExpense> results = repo.findAll(spec);
        assertThat(results).hasSize(1)
            .allMatch(re -> re.getAccount().getId().equals(acct2.getId()));
    }

    @Test
    void combinedSpecifications_workTogether() {
        // user1 AND cat1 AND in June  
        Specification<RecurringExpense> spec = Specification
            .where(RecurringExpenseSpecifications.belongsToUser(user1.getId()))
            .and(RecurringExpenseSpecifications.categoryEquals(cat1.getId()))
            .and(RecurringExpenseSpecifications.dateBetween(
                LocalDate.of(2025,6,1),
                LocalDate.of(2025,6,30)
            ));

        List<RecurringExpense> results = repo.findAll(spec);

        // Should pick only RE1 (user1, cat1, due 6/1) 
        assertThat(results).hasSize(1)
            .allMatch(re -> re.getNextDueDate().equals(LocalDate.of(2025,6,1)) &&
                            re.getAccount().getUser().getId().equals(user1.getId()));
    }
}
