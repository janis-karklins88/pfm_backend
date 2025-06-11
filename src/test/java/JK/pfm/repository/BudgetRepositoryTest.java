package JK.pfm.repository;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}    