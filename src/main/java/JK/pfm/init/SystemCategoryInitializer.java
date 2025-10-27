package JK.pfm.init;

import JK.pfm.model.Category;
import JK.pfm.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Seeds and maintains system-wide {@link Category} records at application startup.
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Creates or updates a fixed set of user-default categories (visible to every user).</li>
 *   <li>Creates or updates system-only categories (not user-selectable).</li>
 *   <li>Idempotent: re-runs safely; existing categories are updated if the {@code isDefault} flag differs.</li>
 * </ul>
 *
 * <p>Transactionality: the seeding runs within a single transaction.</p>
 *
 * <p><strong>Deployment note:</strong> In production you may want to guard this runner by a Spring profile
 * (e.g., {@code @Profile("init")}) or a property flag to avoid unintended edits.</p>
 */
@Component
public class SystemCategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepo;

    public SystemCategoryInitializer(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Categories visible to every user
        List<String> userDefaultNames = List.of(
            "Food",
            "Housing",
            "Transportation",
            "Household supplies",
            "Rent",
            "Eating out",
            "Entertainment",
            "Trips",
            "Parties",
            "Subscriptions",
            "Bills",
            "Personal Care",
            "Salary",
            "Misc"
        );

        // System‐only categories (not user‐selectable)
        List<String> systemNames = List.of(
            "Opening Balance",
            "Fund Transfer",
            "Savings"
        );

        // Seed or update user‐default categories
        userDefaultNames.forEach(name -> upsertCategory(name, true));

        // Seed or update system categories
        systemNames.forEach(name -> upsertCategory(name, false));
    }

    private void upsertCategory(String name, boolean isDefault) {
        Optional<Category> existing = categoryRepo.findByName(name);
        if (existing.isPresent()) {
            Category cat = existing.get();
            // If the flag is wrong, correct it
            if (cat.getIsDefault() != isDefault) {
                cat.setIsDefault(isDefault);
                categoryRepo.save(cat);
            }
        } else {
            Category cat = new Category();
            cat.setName(name);
            cat.setIsDefault(isDefault);
            categoryRepo.save(cat);
        }
    }
}
