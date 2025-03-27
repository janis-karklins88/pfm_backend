package JK.pfm.repository;

import JK.pfm.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long>, JpaSpecificationExecutor<SavingsGoal> {
    // You can add custom query methods if needed.
}
