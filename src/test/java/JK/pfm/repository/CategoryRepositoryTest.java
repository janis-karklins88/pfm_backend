/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JK.pfm.repository;

import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
class CategoryRepositoryTest {

  @Autowired TestEntityManager em;
  @Autowired CategoryRepository repo;

  @Test
  void findByIsDefaultTrue_returnsOnlyDefaults() {
    Category c1 = new Category("A"); c1.setIsDefault(true);
    Category c2 = new Category("B"); c2.setIsDefault(false);
    Category c3 = new Category("C"); c3.setIsDefault(true);
    em.persist(c1); em.persist(c2); em.persist(c3);
    em.flush(); em.clear();

    List<Category> defaults = repo.findByIsDefaultTrue();
    List<String> names = defaults.stream().map(Category::getName).toList();

    assertEquals(2, defaults.size());
    assertTrue(names.containsAll(List.of("A","C")));
    assertFalse(names.contains("B"));
  }

  @Test
  void findByName_workAsExpected() {
    Category cat = new Category("Groceries");
    cat.setIsDefault(false);
    cat = em.persistAndFlush(cat);

    Optional<Category> byName  = repo.findByName("Groceries");
    
    assertTrue(byName.isPresent());
    assertEquals(cat.getId(), byName.get().getId());

    assertTrue(repo.findByName("Nope").isEmpty());
  }
  
  @Test
  void findIdByName_workAsExpected() {
    Category cat = new Category("Groceries");
    cat.setIsDefault(false);
    cat = em.persistAndFlush(cat);

    Optional<Long> idByName = repo.findIdByName("Groceries");

    assertTrue(idByName.isPresent());
    assertEquals(cat.getId(), idByName.get());

    assertTrue(repo.findIdByName("Nope").isEmpty());
  }
}
