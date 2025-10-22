package JK.pfm.controller;

import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.dto.filters.ReccurringExpenseFilter;
import JK.pfm.model.RecurringExpense;
import JK.pfm.service.RecurringExpenseService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    
    public RecurringExpenseController(RecurringExpenseService service){
        this.recurringExpenseService = service;
    }
    
    
    /**
     * Retrieves recurring expenses for the currently authenticated user, filtered by the supplied criteria.
     *
     * <p>Returns a {@code 200 OK} response containing a (possibly empty) list of {@link RecurringExpense} entries.
     * If no filters are provided, all recurring expenses for the user are returned.</p>
     *
     * @param filter optional constraints such as date range, category, and account
     * @return {@code ResponseEntity} with the filtered list of {@link RecurringExpense}
     * @implNote Delegates to {@link RecurringExpenseService#getRecurringExpensesByFilters(ReccurringExpenseFilter)}.
     */
    @GetMapping
    public ResponseEntity<List<RecurringExpense>> getRecurringExpenses(@Valid @ModelAttribute ReccurringExpenseFilter filter) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByFilters(filter));
    }
    
	/**
	 * Creates a new recurring expense for the authenticated user.
	 *
	 * <p>Responds with {@code 201 Created} and the created {@link RecurringExpense}
	 * in the response body. The {@code Location} header points to {@code /api/recurring-expenses/{id}}.</p>
	 *
	 * @param request a {@link RecurringExpenseCreation} containing details of the recurring expense
	 * @return {@code ResponseEntity} containing the saved {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#saveRecurringExpense(RecurringExpenseCreation)}.
	 */
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@Valid @RequestBody RecurringExpenseCreation request) {
        var saved = recurringExpenseService.saveRecurringExpense(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(saved.getId())
        .toUri();
        return ResponseEntity.created(uri).body(saved);
    }
    
	/**
	 * Retrieves all upcoming recurring expense payments for the authenticated user.
	 *
	 * <p>Used to display the next scheduled payments based on each expense's due date.</p>
	 *
	 * <p>Responds with {@code 200 OK} and a (possibly empty) list of {@link RecurringExpense}.</p>
	 *
	 * @return {@code ResponseEntity} containing a list of upcoming recurring expenses
	 * @implNote Delegates to {@link RecurringExpenseService#getUpcommingRecurringExpense()}.
	 */
    @GetMapping("/next-payments")
    public ResponseEntity<List<RecurringExpense>> getNextPayements() {
        List<RecurringExpense> payments = recurringExpenseService.getUpcommingRecurringExpense();
        return ResponseEntity.ok(payments);  

    }
    
	/**
	 * Deletes a recurring expense by its identifier.
	 *
	 * <p>Responds with {@code 204 No Content} if the deletion succeeds.</p>
	 *
	 * @param id the ID of the recurring expense to delete
	 * @implNote Delegates to {@link RecurringExpenseService#deleteRecurringExpense(Long)}.
	 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
    }
    
	/**
	 * Updates the amount of a recurring expense.
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link RecurringExpense} object.</p>
	 *
	 * @param id the ID of the recurring expense to update
	 * @param request a {@link UpdatePaymentAmountDto} containing the new amount
	 * @return {@code ResponseEntity} containing the updated {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#updateRecurringExpenseAmount(Long, UpdatePaymentAmountDto)}.
	 */
    @PatchMapping("/amount/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAmount(@PathVariable Long id,@Valid @RequestBody UpdatePaymentAmountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAmount(id, request));
    }
    
	/**
	 * Updates the next due date for a recurring expense.
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link RecurringExpense} object.</p>
	 *
	 * @param id the ID of the recurring expense to update
	 * @param request a {@link UpdatePaymentNextDueDateDto} containing the new due date
	 * @return {@code ResponseEntity} containing the updated {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#updateRecurringExpenseNextDueDate(Long, UpdatePaymentNextDueDateDto)}.
	 */
    @PatchMapping("/name/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseNextDueDate(
            @PathVariable Long id, 
            @Valid @RequestBody UpdatePaymentNextDueDateDto request) {

        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseNextDueDate(id, request));
    }
    
	/**
	 * Updates the account associated with a recurring expense.
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link RecurringExpense} object.</p>
	 *
	 * @param id the ID of the recurring expense to update
	 * @param request a {@link UpdateRecurringExpenseAccountDto} containing the new account reference
	 * @return {@code ResponseEntity} containing the updated {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#updateRecurringExpenseAccount(Long, UpdateRecurringExpenseAccountDto)}.
	 */
    @PatchMapping("/account/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAccount(@PathVariable Long id, @Valid @RequestBody UpdateRecurringExpenseAccountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAccount(id, request));
    }
    
	/**
	 * Pauses an active recurring expense.
	 *
	 * <p>Prevents the expense from being processed until it is resumed.</p>
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link RecurringExpense} object reflecting the paused state.</p>
	 *
	 * @param id the ID of the recurring expense to pause
	 * @return {@code ResponseEntity} containing the updated {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#pauseRecurringExpense(Long)}.
	 */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringExpense> pauseRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.pauseRecurringExpense(id));
    }

	/**
	 * Resumes a previously paused recurring expense.
	 *
	 * <p>Reactivates the recurring payment, allowing future due dates to be processed again.</p>
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link RecurringExpense} object reflecting the active state.</p>
	 *
	 * @param id the ID of the recurring expense to resume
	 * @return {@code ResponseEntity} containing the updated {@link RecurringExpense}
	 * @implNote Delegates to {@link RecurringExpenseService#resumeRecurringExpense(Long)}.
	 */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringExpense> resumeRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.resumeRecurringExpense(id));
    }
}
