package JK.pfm.specification;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.security.CustomUserDetailsService;
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
class TransactionsSpecificationTest {

    @Autowired
    private TransactionRepository repo;

    private Account account1, account2;
    private Category cat1, cat2;
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

        account1 = new Account("a1", BigDecimal.ZERO, user);
        account2 = new Account("a2", BigDecimal.ZERO, user);
        em.persist(account1);
        em.persist(account2);

        cat1 = new Category("Food");
        cat2 = new Category("Rent");
        em.persist(cat1);
        em.persist(cat2);

        // t1: in June, Food, account1, Expense
        em.persist(new Transaction(
            LocalDate.of(2025, 6, 5), new BigDecimal("10"), 
            account1, cat1, "Expense", "t1"
        ));
        // t2: in June, Rent, account2, Deposit
        em.persist(new Transaction(
            LocalDate.of(2025, 6, 20), new BigDecimal("20"),
            account2, cat2, "Deposit", "t2"
        ));
        // t3: in July, Food, account1, Expense
        em.persist(new Transaction(
            LocalDate.of(2025, 7, 1), new BigDecimal("5"), 
            account1, cat1, "Expense", "t3"
        ));
    }

    @Autowired
    private TestEntityManager em;

    @Test
    void dateBetween_returnsOnlyJuneTransactions() {
        Specification<Transaction> juneSpec = TransactionSpecifications
            .dateBetween(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30)
            );

        List<Transaction> results = repo.findAll(juneSpec);

        assertThat(results)
            .extracting(Transaction::getDescription)
            .containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void categoryAndAccount_andType_combinationFiltersCorrectly() {
        Specification<Transaction> spec = Specification
            .where(TransactionSpecifications.categoryEquals(cat1.getId()))
            .and(TransactionSpecifications.accountEquals(account1.getId()))
            .and(TransactionSpecifications.typeEquals("Expense"));

        List<Transaction> results = repo.findAll(spec);

        assertThat(results)
            .extracting(Transaction::getDescription)
            .containsExactly("t1", "t3");  // both Food/Expense, across dates
    }

    @Test
    void belongsToUser_filtersAcrossAccounts() {
        // user has both account1 and account2, so all 3 persisted txns
        Specification<Transaction> userSpec = TransactionSpecifications
            .belongsToUser(user.getId());

        List<Transaction> results = repo.findAll(userSpec);

        assertThat(results).hasSize(3);
    }
}
