package JK.pfm.repository;

import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;


@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")        
class SavingsGoalRepositoryTest {


    @Autowired
    private SavingsGoalRepository repo;
    
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
    void savingsSumForUser(){
        User otherOther = new User();
        otherOther.setPassword("pw");
        otherOther.setUsername("user");
        em.persist(otherOther);
        
        SavingsGoal goal1 = new SavingsGoal("goal-A", BigDecimal.TEN, "trip", user);
        goal1.setCurrentAmount(BigDecimal.TEN);
        em.persist(goal1);
        
        SavingsGoal goal2 = new SavingsGoal("goal-B", BigDecimal.TEN, "trip", user);
        goal2.setCurrentAmount(BigDecimal.TEN);
        em.persist(goal2);
        
        SavingsGoal otherUsers = new SavingsGoal("goal-C", BigDecimal.TEN, "trip", other);
        otherUsers.setCurrentAmount(BigDecimal.TEN);
        em.persist(otherUsers);
        
        em.flush();
        em.clear();
        
        BigDecimal sum = repo.getTotalBalanceByUserId(user.getId());
        BigDecimal sumNoUser = repo.getTotalBalanceByUserId(otherOther.getId());
        assertEquals(new BigDecimal("20.00"), sum);
        assertEquals(new BigDecimal("0"), sumNoUser);
    }
    
    @Test
    void savingsLastMonthSumForUser(){
        SavingsGoal goal1 = new SavingsGoal("goal-A", BigDecimal.TEN, "trip", user);
        goal1.setlastMonthAmount(BigDecimal.TEN);
        em.persist(goal1);
        
        SavingsGoal goal2 = new SavingsGoal("goal-B", BigDecimal.TEN, "trip", user);
        goal2.setlastMonthAmount(BigDecimal.ONE);
        em.persist(goal2);
        
        SavingsGoal otherUsers = new SavingsGoal("goal-C", BigDecimal.TEN, "trip", other);
        otherUsers.setlastMonthAmount(BigDecimal.TEN);
        em.persist(otherUsers);
        
        em.flush();
        em.clear();
        
        BigDecimal sum = repo.getLastMonthBalanceByUserId(user.getId());
        
        assertEquals(new BigDecimal("10.00"), sum);
    }
    
    @Test
    void savingsGoalDistinctUsers(){
        User otherOther = new User();
        otherOther.setPassword("pw");
        otherOther.setUsername("user");
        em.persist(otherOther);
        
        SavingsGoal goal1 = new SavingsGoal("goal-A", BigDecimal.TEN, "trip", user);
        em.persist(goal1);
        
        SavingsGoal goal2 = new SavingsGoal("goal-B", BigDecimal.TEN, "trip", otherOther);
        em.persist(goal2);
        
        SavingsGoal otherUsers = new SavingsGoal("goal-C", BigDecimal.TEN, "trip", other);
        em.persist(otherUsers);
        
        em.flush();
        em.clear();
        
        List<Long> users = repo.findDistinctUserIds();
        
        assertEquals(3, users.size());
        assertTrue(users.contains(user.getId()));
        assertTrue(users.contains(other.getId()));
        assertTrue(users.contains(otherOther.getId()));
        

    }
    
    @Test
    void updateLastMonthAmount(){
        SavingsGoal goal = new SavingsGoal("goal-A", BigDecimal.TEN, "trip", user);
        goal.setlastMonthAmount(BigDecimal.TEN);
        em.persist(goal);
        
        SavingsGoal goal2 = new SavingsGoal("goal-A", BigDecimal.TEN, "trip", user);
        goal2.setlastMonthAmount(BigDecimal.TEN);
        em.persist(goal2);
        
        SavingsGoal otherUsers = new SavingsGoal("goal-C", BigDecimal.TEN, "trip", other);
        otherUsers.setlastMonthAmount(BigDecimal.TEN);
        em.persist(otherUsers);
        
        em.flush();
        em.clear();
        
        int updates = repo.updateLastMonthAmountByUserId(user.getId(), BigDecimal.ONE);
        assertEquals(2, updates, "2 rows have to be updated");
        
        em.flush();
        em.clear();
        
        SavingsGoal updated = repo.findById(goal.getId()).orElseThrow();
        SavingsGoal updated2 = repo.findById(goal2.getId()).orElseThrow();
        SavingsGoal notUpdated = repo.findById(otherUsers.getId()).orElseThrow();
        
        
        assertEquals(new BigDecimal("1.00"), updated.getlastMonthAmount());
        assertEquals(new BigDecimal("1.00"), updated2.getlastMonthAmount());
        assertEquals(new BigDecimal("10.00"), notUpdated.getlastMonthAmount());
    }
}