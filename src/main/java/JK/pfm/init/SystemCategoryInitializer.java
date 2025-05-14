package JK.pfm.init;

import JK.pfm.model.Category;
import JK.pfm.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
