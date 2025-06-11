package JK.pfm.repository;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
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
class RecurringExpenseRepositoryTest {


    @Autowired
    private RecurringExpenseRepository repo;
    
    @Autowired
    private TestEntityManager em;
    
    private User user;
    private Category category;
    private Account account;
    private Account validAccount;
    private Account otherAccount;

    
    @BeforeEach
    void setUp() {
        // 1) minimal User
        user = new User();
        user.setUsername("u1");
        user.setPassword("pw");
        user = em.persistAndFlush(user);

        // 2) Category (must set isDefault)
        category = new Category("utilities");
        category.setIsDefault(false);
        category = em.persistAndFlush(category);

        // 3) Account (must set version)
        account = new Account("Acct", BigDecimal.TEN, user);
        account.setVersion(0L);
        account = em.persistAndFlush(account);
        
        //Accounts for 2nd method
        validAccount = new Account("A1", BigDecimal.TEN, user);
        validAccount.setVersion(0L);
        validAccount = em.persistAndFlush(validAccount);

        otherAccount = new Account("A2", BigDecimal.TEN, user);
        otherAccount.setVersion(0L);
        otherAccount = em.persistAndFlush(otherAccount);
    }

    @Test
    void findsOnlyActiveExpensesDueOnOrBeforeGivenDate() {
        LocalDate cutoff = LocalDate.now();
        
        // a) Due before cutoff, active → SHOULD be returned
        RecurringExpense dueAndActive = new RecurringExpense();
        dueAndActive.setAccount(account);
        dueAndActive.setCategory(category);
        dueAndActive.setName("dueActive");
        dueAndActive.setAmount(BigDecimal.ONE);
        dueAndActive.setStartDate(cutoff.minusDays(10));
        dueAndActive.setNextDueDate(cutoff.minusDays(1));
        dueAndActive.setActive(true);
        em.persist(dueAndActive);

        // b) Due before cutoff, inactive → should NOT be returned
        RecurringExpense dueButInactive = new RecurringExpense();
        dueButInactive.setAccount(account);
        dueButInactive.setCategory(category);
        dueButInactive.setName("dueInactive");
        dueButInactive.setAmount(BigDecimal.ONE);
        dueButInactive.setStartDate(cutoff.minusDays(10));
        dueButInactive.setNextDueDate(cutoff.minusDays(1));
        dueButInactive.setActive(false);
        em.persist(dueButInactive);

        // c) Due exactly on cutoff, active → SHOULD be returned (boundary = inclusive)
        RecurringExpense exactAndActive = new RecurringExpense();
        exactAndActive.setAccount(account);
        exactAndActive.setCategory(category);
        exactAndActive.setName("exactActive");
        exactAndActive.setAmount(BigDecimal.ONE);
        exactAndActive.setStartDate(cutoff.minusDays(5));
        exactAndActive.setNextDueDate(cutoff);
        exactAndActive.setActive(true);
        em.persist(exactAndActive);

        // d) Due after cutoff, active → should NOT be returned
        RecurringExpense notYetDueAndActive = new RecurringExpense();
        notYetDueAndActive.setAccount(account);
        notYetDueAndActive.setCategory(category);
        notYetDueAndActive.setName("notDueYet");
        notYetDueAndActive.setAmount(BigDecimal.ONE);
        notYetDueAndActive.setStartDate(cutoff.plusDays(1));
        notYetDueAndActive.setNextDueDate(cutoff.plusDays(5));
        notYetDueAndActive.setActive(true);
        em.persist(notYetDueAndActive);

        em.flush();
        em.clear();

        List<RecurringExpense> results =
            repo.findByNextDueDateLessThanEqualAndActiveTrue(cutoff);
        
        assertEquals(2, results.size(), "should return only the active expenses due on/before cutoff");

        List<String> names = results.stream()
                                    .map(RecurringExpense::getName)
                                    .toList();

        assertTrue(names.contains("dueActive"),    "dueActive should be in results");
        assertTrue(names.contains("exactActive"),  "exactActive should be in results");
        assertFalse(names.contains("dueInactive"), "dueInactive should be excluded");
        assertFalse(names.contains("notDueYet"),   "notDueYet should be excluded");

    }
    
    @Test
    void findNext5ActivePaymentsInAscOrder (){
        LocalDate cutoff = LocalDate.now();
        
        // 1) Six matching, active, correct account
        for (int i = 1; i <= 6; i++) {
            RecurringExpense e = new RecurringExpense();
            e.setAccount(validAccount);
            e.setCategory(category);
            e.setName("match" + i);
            e.setAmount(BigDecimal.ONE);
            e.setStartDate(cutoff);
            e.setNextDueDate(cutoff.plusDays(i)); // days 1..6
            e.setActive(true);
            em.persist(e);
        }
        
        //Inactive but would otherwise match
        RecurringExpense inactive = new RecurringExpense();
        inactive.setAccount(validAccount);
        inactive.setCategory(category);
        inactive.setName("inactive");
        inactive.setAmount(BigDecimal.ONE);
        inactive.setStartDate(cutoff);
        inactive.setNextDueDate(cutoff.plusDays(3));
        inactive.setActive(false);
        em.persist(inactive);
        //Wrong acc
        RecurringExpense wrongAcct = new RecurringExpense();
        wrongAcct.setAccount(otherAccount);
        wrongAcct.setCategory(category);
        wrongAcct.setName("wrongAcct");
        wrongAcct.setAmount(BigDecimal.ONE);
        wrongAcct.setStartDate(cutoff);
        wrongAcct.setNextDueDate(cutoff.plusDays(4));
        wrongAcct.setActive(true);
        em.persist(wrongAcct);
        //todays payment not included
        RecurringExpense boundary = new RecurringExpense();
        boundary.setAccount(validAccount);
        boundary.setCategory(category);
        boundary.setName("boundary");
        boundary.setAmount(BigDecimal.ONE);
        boundary.setStartDate(cutoff);
        boundary.setNextDueDate(cutoff);
        boundary.setActive(true);
        em.persist(boundary);
        
        em.flush();
        em.clear();
        
        List<RecurringExpense> results = repo
          .findTop5ByAccountIdInAndNextDueDateAfterAndActiveTrueOrderByNextDueDateAsc(
            List.of(validAccount.getId()), cutoff);
        
        assertEquals(5, results.size(), "Only top 5 should be returned");
        List<LocalDate> dates = results.stream()
                                       .map(RecurringExpense::getNextDueDate)
                                       .toList();
        assertEquals(
          List.of(1,2,3,4,5).stream()
             .map(cutoff::plusDays)
             .toList(),
          dates,
          "The nextDueDates must be cutoff+1 … cutoff+5 in order"
        );
        List<String> names = results.stream()
                                    .map(RecurringExpense::getName)
                                    .toList();
        assertFalse(names.contains("match6"),    "6th match should be dropped");
        assertFalse(names.contains("inactive"),  "inactive should be filtered out");
        assertFalse(names.contains("wrongAcct"), "wrong account should be filtered out");
        assertFalse(names.contains("boundary"),  "boundary date should be filtered out");

    } 
}
