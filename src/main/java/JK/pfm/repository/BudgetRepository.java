package JK.pfm.repository;

import JK.pfm.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    // You can add methods like findByCategoryId, findByStartDateBetween, etc.
}
