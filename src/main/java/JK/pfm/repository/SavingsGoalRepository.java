package JK.pfm.repository;

import JK.pfm.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    // You can add custom query methods if needed.
}
