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
    
    
    // Retrieve all recurring expenses
    /**
    * @param filter controls date range, category and account filters
    * @return all matching recurring expenses for the current user
    */
    @GetMapping
    public ResponseEntity<List<RecurringExpense>> getRecurringExpenses(@Valid @ModelAttribute ReccurringExpenseFilter filter) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByFilters(filter));
    }
    
    // Create a new recurring expense
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@Valid @RequestBody RecurringExpenseCreation request) {
        var saved = recurringExpenseService.saveRecurringExpense(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(saved.getId())
        .toUri();
        return ResponseEntity.created(uri).body(saved);
    }
    
    //next payments
    @GetMapping("/next-payments")
    public ResponseEntity<List<RecurringExpense>> getNextPayements() {
        List<RecurringExpense> payments = recurringExpenseService.getUpcommingRecurringExpense();
        return ResponseEntity.ok(payments);  

    }
    
    // Delete a recurring expense by ID
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
    }
    
    //edit amount
    @PatchMapping("/amount/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAmount(@PathVariable Long id,@Valid @RequestBody UpdatePaymentAmountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAmount(id, request));
    }
    
    //edit nextduedate
    @PatchMapping("/name/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseNextDueDate(
            @PathVariable Long id, 
            @Valid @RequestBody UpdatePaymentNextDueDateDto request) {

        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseNextDueDate(id, request));
    }
    
    //change account
    @PatchMapping("/account/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAccount(@PathVariable Long id, @Valid @RequestBody UpdateRecurringExpenseAccountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAccount(id, request));
    }
    
    // Pause a recurring expense
    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringExpense> pauseRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.pauseRecurringExpense(id));
    }

    // Resume a recurring expense
    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringExpense> resumeRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.resumeRecurringExpense(id));
    }
}
