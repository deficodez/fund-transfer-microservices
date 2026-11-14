package com.defi.fundtransfer.account.web;

import com.defi.fundtransfer.account.domain.Account;
import com.defi.fundtransfer.account.service.AccountService;
import com.defi.fundtransfer.account.service.OpenAccountCommand;
import com.defi.fundtransfer.account.service.OperationResult;
import com.defi.fundtransfer.account.web.dto.AccountResponse;
import com.defi.fundtransfer.account.web.dto.OpenAccountRequest;
import com.defi.fundtransfer.account.web.dto.OperationRequest;
import com.defi.fundtransfer.account.web.dto.OperationResponse;
import com.defi.fundtransfer.account.web.dto.ReverseRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST surface used for manual testing today. In Phase 3 the Transfer service drives debit/credit/reverse
 * through Kafka commands, and these endpoints stay as an operational/debug interface.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest req) {
        Account created = service.open(new OpenAccountCommand(req.ownerName(), req.currency(), req.initialBalanceMinor()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(AccountResponse.from(created));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(service.get(id));
    }

    @GetMapping("/{id}/operations")
    public Page<OperationResponse> history(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.history(id, PageRequest.of(page, Math.min(size, 100))).map(OperationResponse::from);
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<OperationResult> debit(@PathVariable UUID id, @Valid @RequestBody OperationRequest req) {
        return respond(service.debit(id, req.transferId(), req.amountMinor()));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<OperationResult> credit(@PathVariable UUID id, @Valid @RequestBody OperationRequest req) {
        return respond(service.credit(id, req.transferId(), req.amountMinor()));
    }

    @PostMapping("/{id}/debit/reverse")
    public ResponseEntity<OperationResult> reverse(@PathVariable UUID id, @Valid @RequestBody ReverseRequest req) {
        return respond(service.reverseDebit(id, req.transferId()));
    }

    /** 201 when the balance actually changed, 200 when this was an idempotent replay. */
    private static ResponseEntity<OperationResult> respond(OperationResult result) {
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
}
