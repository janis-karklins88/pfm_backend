package JK.pfm.repository;

import JK.pfm.model.Account;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
class AccountRepositoryTest {


    @Autowired
    private AccountRepository repo;
    
    @Autowired
    private TestEntityManager em;
    
    private User user;
    private User other;
    private Account activeA;
    private Account activeB;
    private Account activeC;
    private Account inactive;


    
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
    void findByUserIdAndNameAndActiveTrue(){
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);

        activeB = new Account("B-two", BigDecimal.TEN, user);
        activeB.setVersion(0L);
        activeB.setIsActive(true);
        em.persist(activeB);

        inactive = new Account("A-one", BigDecimal.TEN, user);
        inactive.setVersion(0L);
        inactive.setIsActive(false);
        em.persist(inactive);

        em.flush();
        em.clear();
        
        Optional<Account> found = repo.findByUserIdAndNameAndActiveTrue(
        user.getId(), "A-one");
        
        assertTrue(found.isPresent());
        assertEquals(activeA.getId(), found.get().getId());
        }
    
    @Test
    void findByUserIdAndIdAndActiveTrue(){
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);

        activeB = new Account("B-two", BigDecimal.TEN, user);
        activeB.setVersion(0L);
        activeB.setIsActive(true);
        em.persist(activeB);

        inactive = new Account("A-one", BigDecimal.TEN, user);
        inactive.setVersion(0L);
        inactive.setIsActive(false);
        em.persist(inactive);

        em.flush();
        em.clear();
        
        Optional<Account> found = repo.findByUserIdAndIdAndActiveTrue(
        user.getId(), activeA.getId());
        Optional<Account> foundEmpty = repo.findByUserIdAndIdAndActiveTrue(
        user.getId(), inactive.getId());
        
        assertTrue(found.isPresent());
        assertEquals("A-one", found.get().getName());
        assertTrue(foundEmpty.isEmpty());
        }
    
    @Test
    void findByUserIdAndNameAndActiveTrue_returnsEmptyForWrongUser() {
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);

        Optional<Account> result =
        repo.findByUserIdAndNameAndActiveTrue(other.getId(), "A-one");

        assertTrue(result.isEmpty(), "Should not find an account for the wrong user");
    }
    
    @Test
    void findByUserIdAndIdAndActiveTrue_returnsEmptyForWrongUser() {
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);

        Optional<Account> result =
        repo.findByUserIdAndIdAndActiveTrue(other.getId(), activeA.getId());

        assertTrue(result.isEmpty(), "Should not find another user’s account by ID");
    }
    
    @Test
    void findAllUserActiveAccounts(){
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);
        
        activeB = new Account("B-two", BigDecimal.TEN, user);
        activeB.setVersion(0L);
        activeB.setIsActive(true);
        em.persist(activeB);
        
        inactive = new Account("inactive", BigDecimal.TEN, user);
        inactive.setVersion(0L);
        inactive.setIsActive(false);
        em.persist(inactive);
        
        List<Account> list = repo.findByUserIdAndActiveTrue(user.getId());
        
        assertEquals(2, list.size());
        List<String> names = list.stream()
                                    .map(Account::getName)
                                    .toList();
        assertTrue(names.contains("A-one"));
        assertTrue(names.contains("B-two"));
        assertFalse(names.contains("inactive"));
    }
    
    @Test
    void sumAccountBalance(){
        activeA = new Account("A-one", BigDecimal.TEN, user);
        activeA.setVersion(0L);
        activeA.setIsActive(true);
        em.persist(activeA);
        
        activeB = new Account("B-two", BigDecimal.TEN, user);
        activeB.setVersion(0L);
        activeB.setIsActive(true);
        em.persist(activeB);
        
        activeC = new Account("C-one", BigDecimal.TEN, other);
        activeC.setVersion(0L);
        activeC.setIsActive(true);
        em.persist(activeC);
        
        inactive = new Account("inactive", BigDecimal.TEN, user);
        inactive.setVersion(0L);
        inactive.setIsActive(false);
        em.persist(inactive);
        
        em.flush();
        
        BigDecimal sum = repo.getTotalBalanceByUserId(user.getId());
        assertEquals(new BigDecimal("20.00"), sum);
    }


}
