package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.service.BudgetService;
import java.math.BigDecimal;
import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.dto.filters.DateRangeFilter;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    public BudgetController(BudgetService service){
        this.budgetService = service;
    }

    
    /**
     * Retrieves all budgets for the authenticated user within an optional date range filter.
     *
     * <p>If the {@link DateRangeFilter} is provided, only budgets within that
     * start and end date are returned. Otherwise, all budgets are listed.</p>
     *
     * <p>Responds with {@code 200 OK} and a list of {@link Budget} objects.</p>
     *
     * @param filter an optional {@link DateRangeFilter} specifying the time range
     * @return {@code ResponseEntity} containing the list of budgets
     * @implNote Delegates to {@link BudgetService#getAllBudgets(DateRangeFilter)}.
     */
    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets(@Valid @ModelAttribute DateRangeFilter filter) {
    return ResponseEntity.ok(budgetService.getAllBudgets(filter));
}

    /**
     * Creates a new budget for the authenticated user.
     *
     * <p>Responds with {@code 201 Created} and the newly created {@link Budget} object.
     * The {@code Location} header of the response points to {@code /api/budgets/{id}}.</p>
     *
     * @param request a {@link BudgetCreationRequest} containing details of the budget
     * @return {@code ResponseEntity} containing the saved {@link Budget} and {@code 201 Created} status
     * @implNote The resource URI is constructed dynamically using {@link ServletUriComponentsBuilder}.
     */
    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetCreationRequest request) {
        var budget = budgetService.saveBudget(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(budget.getId())
        .toUri();
        return ResponseEntity.created(uri).body(budget);
    }
    /**
    * Deletes a specific budget by its identifier.
     *
     * <p>Responds with {@code 204 No Content} if the deletion is successful.
     * If the specified budget does not belong to the user, an access error is returned.</p>
     *
     * @param id the unique identifier of the budget to delete
     * @return an empty {@code ResponseEntity} with {@code 204 No Content} status
     * @implNote Delegates to {@link BudgetService#deleteBudgetForUser(Long)} to ensure user-level security.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudgetForUser(id);
        return ResponseEntity.noContent().build();
}

    

    /**
     * Updates the amount of an existing budget.
     *
     * <p>Responds with {@code 200 OK} and the updated {@link Budget} object.</p>
     *
     * @param id the ID of the budget to update
     * @param request a {@link UpdateBudgetAmountDto} containing the new budget amount
     * @return {@code ResponseEntity} containing the updated {@link Budget}
     * @implNote Delegates to {@link BudgetService#updateBudgetAmount(Long, UpdateBudgetAmountDto)}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Budget> updateBudgetAmount(@PathVariable Long id, 
            @Valid @RequestBody UpdateBudgetAmountDto request) {
        return ResponseEntity.ok(budgetService.updateBudgetAmount(id, request));
    }
    
    /**
     * Retrieves the total amount spent for a specific budget.
     *
     * <p>Responds with {@code 200 OK} and a {@link BigDecimal} value representing
     * the total expenditures recorded under the specified budget.</p>
     *
     * @param id the ID of the budget for which to calculate total spending
     * @return {@code ResponseEntity} containing the total spent amount as a {@link BigDecimal}
     * @implNote Delegates to {@link BudgetService#getTotalSpentOnBudget(Long)}.
     */
    @GetMapping("/spent/{id}")
    public ResponseEntity<BigDecimal> getTotalSpentOnBudget(@PathVariable Long id){
        BigDecimal totalSpent = budgetService.getTotalSpentOnBudget(id);
        return ResponseEntity.ok(totalSpent);
    }
    
    /**
     * Updates the monthly active status of a budget.
     *
     * <p>Marks the specified budget as either active or inactive for the current month.</p>
     *
     * <p>Responds with {@code 200 OK} and the updated {@link Budget} reflecting
     * the new monthly active state.</p>
     *
    * @param budgetId the ID of the budget to update
    * @param active {@code true} to set as active, {@code false} to deactivate
    * @return {@code ResponseEntity} containing the updated {@link Budget}
    * @implNote Delegates to {@link BudgetService#updateMonthlyStatus(Long, boolean)}.
    */
    @PatchMapping("/{budgetId}/monthly")
    public ResponseEntity<Budget> updateMonthlyStatus(@PathVariable Long budgetId, 
                                                      @RequestParam boolean active) {
        Budget budget = budgetService.updateMonthlyStatus(budgetId, active);
        return ResponseEntity.ok(budget);
    }
}
