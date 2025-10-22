package JK.pfm.controller;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.SavingsGoal;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;



@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    
    public SavingsGoalController (SavingsGoalService serv){
        this.savingsGoalService =serv;
    }

	/**
	 * Retrieves all savings goals for the authenticated user.
	 *
	 * <p>Responds with {@code 200 OK} and a list of {@link SavingsGoal}
	 * objects representing each goal created by the current user.</p>
	 *
	 * @return {@code ResponseEntity} containing the list of {@link SavingsGoal}
	 * @implNote Delegates to {@link SavingsGoalService#getAllSavingsGoals(Long)}
	 * using the authenticated user's ID from {@link SecurityUtil#getUserId()}.
	 */
    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAllSavingsGoals() {
        return ResponseEntity.ok(savingsGoalService.getAllSavingsGoals(SecurityUtil.getUserId()));
    }
    
	/**
	 * Retrieves the total balance across all savings goals for the authenticated user.
	 *
	 * <p>Responds with {@code 200 OK} and a single {@link BigDecimal} value
	 * representing the total funds saved across all goals.</p>
	 *
	 * @return {@code ResponseEntity} containing the total savings balance
	 * @implNote Delegates to {@link SavingsGoalService#getTotalBalance()}.
	 */
    @GetMapping("/savings-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        return ResponseEntity.ok(savingsGoalService.getTotalBalance());
    }

	/**
	 * Creates a new savings goal for the authenticated user.
	 *
	 * <p>Responds with {@code 201 Created} and the newly created {@link SavingsGoal}.
	 * The {@code Location} header points to {@code /api/savings-goals/{id}}.</p>
	 *
	 * @param request a {@link SavingGoalCreation} object containing goal details
	 * @return {@code ResponseEntity} containing the created {@link SavingsGoal}
	 * @implNote Delegates to {@link SavingsGoalService#saveSavingsGoal(SavingGoalCreation)}.
	 */
    @PostMapping
    public ResponseEntity<SavingsGoal> createSavingsGoal(@Valid @RequestBody SavingGoalCreation request) {
        var savingGoal = savingsGoalService.saveSavingsGoal(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(savingGoal.getId())
        .toUri();
        return ResponseEntity.created(uri).body(savingGoal);
    }

	/**
	 * Retrieves a specific savings goal by its identifier.
	 *
	 * <p>Responds with {@code 200 OK} and the requested {@link SavingsGoal}
	 * if found, or {@code 404 Not Found} if it does not exist.</p>
	 *
	 * @param id the ID of the savings goal to retrieve
	 * @return {@code ResponseEntity} containing the {@link SavingsGoal}, or {@code 404 Not Found}
	 * @implNote Delegates to {@link SavingsGoalService#getSavingsGoalById(Long)}.
	 */
    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoal> getSavingsGoalById(@PathVariable Long id) {
        return savingsGoalService.getSavingsGoalById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

	/**
	 * Deletes a savings goal by its identifier.
	 *
	 * <p>Responds with {@code 204 No Content} if the deletion succeeds.</p>
	 *
	 * @param id the ID of the savings goal to delete
	 * @implNote Delegates to {@link SavingsGoalService#deleteSavingsGoal(Long)}.
	 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavingsGoal(@PathVariable Long id) {
        savingsGoalService.deleteSavingsGoal(id);
    }
    
	/**
	 * Updates the target amount of a savings goal.
	 *
	 * <p>Responds with {@code 200 OK} and the updated {@link SavingsGoal} object.</p>
	 *
	 * @param id the ID of the savings goal to update
	 * @param request a {@link UpdateSavingsAmountDto} containing the new target amount
	 * @return {@code ResponseEntity} containing the updated {@link SavingsGoal}
	 * @implNote Delegates to {@link SavingsGoalService#updateSavingsGoalAmount(Long, UpdateSavingsAmountDto)}.
	 */
    @PatchMapping("/{id}/amount")
    public ResponseEntity<SavingsGoal> updateSavingGoalAmount(@PathVariable Long id,@Valid @RequestBody UpdateSavingsAmountDto request){
        return ResponseEntity.ok(savingsGoalService.updateSavingsGoalAmount(id, request));
    }
    
	/**
	 * Transfers funds into or out of a specific savings goal.
	 *
	 * <p>Used to record deposits or withdrawals for the goal.
	 * Responds with {@code 200 OK} and the updated {@link SavingsGoal} showing the new balance.</p>
	 *
	 * @param id the ID of the savings goal affected
	 * @param request a {@link SavingsFundTransferDTO} describing the transfer amount and direction
	 * @return {@code ResponseEntity} containing the updated {@link SavingsGoal}
	 * @implNote Delegates to {@link SavingsGoalService#transferFunds(Long, SavingsFundTransferDTO)}.
	 */
    @PatchMapping("/{id}/transfer-funds")
    public ResponseEntity<SavingsGoal> transferFundsSavingsGoal(
            @PathVariable Long id, 
            @Valid @RequestBody SavingsFundTransferDTO request) {
        return ResponseEntity.ok(savingsGoalService.transferFunds(id, request));
    }
    
	/**
	 * Retrieves the user's total net savings deposits by month.
	 *
	 * <p>Responds with {@code 200 OK} and a map where keys represent month labels
	 * and values represent total net deposit amounts (deposits minus withdrawals).</p>
	 *
	 * @return {@code ResponseEntity} containing the monthly net balance map
	 * @implNote Delegates to {@link SavingsGoalService#getNetMonthlyBalance()}.
	 */
    @GetMapping("net-balance")
    public ResponseEntity<Map<String, BigDecimal>> getNetMonthlyBalance() {
        return ResponseEntity.ok(savingsGoalService.getNetMonthlyBalance());
    }
}
