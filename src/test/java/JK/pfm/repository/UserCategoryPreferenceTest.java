
package JK.pfm.repository;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
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
class UserCategoryPreferenceTest {

  @Autowired TestEntityManager em;
  @Autowired UserCategoryPreferenceRepository repo;

  private User user;
    private User other;
    private Category cat1;
    private Category cat2;
    private Category cat3;

    
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
        cat2 = new Category("cat2");
        cat3 = new Category("cat3");
        em.persist(cat1);
        em.persist(cat2);
        em.persist(cat3);
        em.flush();
        
    }
    
    @Test
    void findByUserId(){
        var valid1 = new UserCategoryPreference(user, cat1);
        var valid2 = new UserCategoryPreference(user, cat2);
        var invalid = new UserCategoryPreference(other, cat1);
        
        em.persist(valid1);
        em.persist(valid2);
        em.persist(invalid);
        em.flush();
        em.clear();
        
        List<UserCategoryPreference> pref = repo.findByUserId(user.getId());
        List<String> names = pref.stream()
        .map(p -> p.getCategory().getName())
        .collect(Collectors.toList());
        
        assertEquals(2, pref.size());
        assertThat(names)
        .containsExactlyInAnyOrder(cat1.getName(), cat2.getName())
        .doesNotContain(cat3.getName());
 
    }
    
    @Test
    void existsByUserIdAndCategoryId_true(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        assertTrue(repo.existsByUserIdAndCategoryId(user.getId(), cat1.getId()));
    }
    @Test
    void existsByUserIdAndCategoryId_false(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        assertFalse(repo.existsByUserIdAndCategoryId(user.getId(), cat2.getId()));
    }
    
    @Test
    void findByUserIdAndCategoryId_found(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        Optional<UserCategoryPreference> found = repo.findByUserIdAndCategoryId(user.getId(), cat1.getId());
        
        assertTrue(found.isPresent(), "Preference should be found");
        UserCategoryPreference pref = found.get();
        assertEquals(valid1.getId(), pref.getId(), "IDs should match");
    }
    
    @Test
    void findByUserIdAndCategoryId_notFound(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        Optional<UserCategoryPreference> found = repo.findByUserIdAndCategoryId(user.getId(), cat2.getId());
        
        assertFalse(found.isPresent(), "Preference should not be found");

    }
    
    @Test
    void findByUserIdAndActiveTrue(){
        var valid1 = new UserCategoryPreference(user, cat1);
        var valid2 = new UserCategoryPreference(user, cat2);
        var invalid = new UserCategoryPreference(user, cat3);
        invalid.setActive(false);
        
        em.persist(valid1);
        em.persist(valid2);
        em.persist(invalid);
        em.flush();
        em.clear();
        
        List<UserCategoryPreference> pref = repo.findByUserIdAndActiveTrue(user.getId());
        List<String> names = pref.stream()
        .map(p -> p.getCategory().getName())
        .collect(Collectors.toList());
        
        assertEquals(2, pref.size());
        assertThat(names)
        .containsExactlyInAnyOrder(cat1.getName(), cat2.getName())
        .doesNotContain(cat3.getName());
 
    }
    
    @Test
    void existsByUserIdAndCategoryName_true(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        assertTrue(repo.existsByUserAndCategory_NameIgnoreCase(user, cat1.getName()));
    }
    @Test
    void existsByUserIdAndCategoryName_false(){
        var valid1 = new UserCategoryPreference(user, cat1);
        em.persist(valid1);
        em.flush();
        em.clear();
        assertFalse(repo.existsByUserAndCategory_NameIgnoreCase(user, cat2.getName()));
    }
    
    @Test
    void findCompleteCategoryListForUser(){
        var valid1 = new UserCategoryPreference(user, cat1);
        var valid2 = new UserCategoryPreference(user, cat2);
        valid2.setActive(false);
        var invalid = new UserCategoryPreference(other, cat3);
        
        em.persist(valid1);
        em.persist(valid2);
        em.persist(invalid);
        em.flush();
        em.clear();
        
        List<CategoryListDto> pref = repo.findCategoryListDtoByUserId(user.getId());
        List<String> names = pref.stream()
        .map(p -> p.getName())
        .collect(Collectors.toList());
        
        assertEquals(2, pref.size());
        assertThat(names)
        .containsExactlyInAnyOrder(cat1.getName(), cat2.getName())
        .doesNotContain(cat3.getName());
    }
    
    @Test
    void findActiveByUserWithCategory(){
        var valid1 = new UserCategoryPreference(user, cat1);
        var valid2 = new UserCategoryPreference(user, cat2);
        var invalid = new UserCategoryPreference(user, cat3);
        invalid.setActive(false);
        
        em.persist(valid1);
        em.persist(valid2);
        em.persist(invalid);
        em.flush();
        em.clear();
        
        List<UserCategoryPreference> pref = repo.findActiveByUserWithCategory(user.getId());
        List<String> names = pref.stream()
        .map(p -> p.getCategory().getName())
        .collect(Collectors.toList());
        
        assertEquals(2, pref.size());
        assertThat(names)
        .containsExactlyInAnyOrder(cat1.getName(), cat2.getName())
        .doesNotContain(cat3.getName());
    }
    
}
