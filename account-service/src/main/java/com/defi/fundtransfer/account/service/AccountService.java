package com.defi.fundtransfer.account.service;

import com.defi.fundtransfer.account.domain.Account;
import com.defi.fundtransfer.account.domain.AccountOperation;
import com.defi.fundtransfer.account.domain.OperationType;
import com.defi.fundtransfer.account.domain.exception.AccountNotFoundException;
import com.defi.fundtransfer.account.domain.exception.IdempotencyConflictException;
import com.defi.fundtransfer.account.repository.AccountOperationRepository;
import com.defi.fundtransfer.account.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final AccountOperationRepository operations;

    public AccountService(AccountRepository accounts, AccountOperationRepository operations) {
        this.accounts = accounts;
        this.operations = operations;
    }

    @Transactional
    public Account open(OpenAccountCommand cmd) {
        Account account = Account.open(nextAccountNumber(), cmd.ownerName(), cmd.currency(), cmd.initialBalanceMinor());
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public Page<AccountOperation> history(UUID accountId, Pageable pageable) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return operations.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }

    @Transactional
    public OperationResult debit(UUID accountId, UUID transferId, long amountMinor) {
        return apply(accountId, transferId, OperationType.DEBIT, amountMinor);
    }

    @Transactional
    public OperationResult credit(UUID accountId, UUID transferId, long amountMinor) {
        return apply(accountId, transferId, OperationType.CREDIT, amountMinor);
    }

    /**
     * Compensation for {@link #debit}. Gives back exactly what was debited for this transfer.
     *
     * <p>If no DEBIT was ever recorded for the transfer this is a no-op: there is nothing to undo.
     * Known hazard to solve in Phase 3: a DEBIT command that arrives <em>after</em> its own reversal
     * (network delay, redelivery) would still be applied. The fix is to record a "cancelled" marker
     * so a late DEBIT is rejected.
     */
    @Transactional
    public OperationResult reverseDebit(UUID accountId, UUID transferId) {
        Account account = lockAccount(accountId);

        Optional<AccountOperation> existingReversal =
                operations.findByAccountIdAndTransferIdAndType(accountId, transferId, OperationType.DEBIT_REVERSAL);
        if (existingReversal.isPresent()) {
            return replay(existingReversal.get());
        }

        Optional<AccountOperation> debit =
                operations.findByAccountIdAndTransferIdAndType(accountId, transferId, OperationType.DEBIT);
        if (debit.isEmpty()) {
            return new OperationResult(accountId, transferId, OperationType.DEBIT_REVERSAL, 0, account.getBalanceMinor(), true);
        }

        long amount = debit.get().getAmountMinor();
        account.restore(amount);
        operations.save(new AccountOperation(accountId, transferId, OperationType.DEBIT_REVERSAL, amount, account.getBalanceMinor()));
        return new OperationResult(accountId, transferId, OperationType.DEBIT_REVERSAL, amount, account.getBalanceMinor(), false);
    }

    private OperationResult apply(UUID accountId, UUID transferId, OperationType type, long amountMinor) {
        // The row lock serialises every operation on this account, so the check-then-insert below is race free.
        Account account = lockAccount(accountId);

        Optional<AccountOperation> existing = operations.findByAccountIdAndTransferIdAndType(accountId, transferId, type);
        if (existing.isPresent()) {
            AccountOperation op = existing.get();
            if (op.getAmountMinor() != amountMinor) {
                throw new IdempotencyConflictException(transferId, op.getAmountMinor(), amountMinor);
            }
            return replay(op);
        }

        if (type == OperationType.DEBIT) {
            account.debit(amountMinor);
        } else {
            account.credit(amountMinor);
        }
        operations.save(new AccountOperation(accountId, transferId, type, amountMinor, account.getBalanceMinor()));
        return new OperationResult(accountId, transferId, type, amountMinor, account.getBalanceMinor(), false);
    }

    private Account lockAccount(UUID accountId) {
        return accounts.findByIdForUpdate(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private static OperationResult replay(AccountOperation op) {
        return new OperationResult(op.getAccountId(), op.getTransferId(), op.getType(), op.getAmountMinor(), op.getBalanceAfterMinor(), true);
    }

    private String nextAccountNumber() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            candidate = sb.toString();
        } while (accounts.existsByAccountNumber(candidate));
        return candidate;
    }
}
