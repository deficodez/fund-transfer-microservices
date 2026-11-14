package com.defi.fundtransfer.account.service;

import com.defi.fundtransfer.account.PostgresIntegrationTest;
import com.defi.fundtransfer.account.domain.Account;
import com.defi.fundtransfer.account.domain.OperationType;
import com.defi.fundtransfer.account.domain.exception.IdempotencyConflictException;
import com.defi.fundtransfer.account.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceIT extends PostgresIntegrationTest {

    @Autowired
    AccountService service;

    private Account newAccount(long balanceMinor) {
        return service.open(new OpenAccountCommand("Defi", "INR", balanceMinor));
    }

    @Test
    void opensAccountWithGeneratedNumberAndInitialBalance() {
        Account a = newAccount(50_000);

        assertThat(a.getAccountNumber()).hasSize(12).containsOnlyDigits();
        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(50_000);
    }

    @Test
    void debitReducesBalanceAndRecordsOperation() {
        Account a = newAccount(10_000);

        OperationResult r = service.debit(a.getId(), UUID.randomUUID(), 2_500);

        assertThat(r.replayed()).isFalse();
        assertThat(r.balanceAfterMinor()).isEqualTo(7_500);
        assertThat(service.history(a.getId(), PageRequest.of(0, 10)).getContent())
                .singleElement()
                .satisfies(op -> assertThat(op.getType()).isEqualTo(OperationType.DEBIT));
    }

    @Test
    void insufficientFundsIsRejectedAndBalanceUntouched() {
        Account a = newAccount(1_000);

        assertThatThrownBy(() -> service.debit(a.getId(), UUID.randomUUID(), 1_001))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(1_000);
    }

    @Test
    void replayingSameTransferIsIdempotent() {
        Account a = newAccount(10_000);
        UUID transferId = UUID.randomUUID();

        OperationResult first = service.debit(a.getId(), transferId, 3_000);
        OperationResult second = service.debit(a.getId(), transferId, 3_000);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.balanceAfterMinor()).isEqualTo(first.balanceAfterMinor());
        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(7_000);
    }

    @Test
    void replayWithDifferentAmountIsAConflict() {
        Account a = newAccount(10_000);
        UUID transferId = UUID.randomUUID();
        service.debit(a.getId(), transferId, 3_000);

        assertThatThrownBy(() -> service.debit(a.getId(), transferId, 4_000))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void reversalRestoresBalanceExactlyOnce() {
        Account a = newAccount(10_000);
        UUID transferId = UUID.randomUUID();
        service.debit(a.getId(), transferId, 4_000);

        OperationResult first = service.reverseDebit(a.getId(), transferId);
        OperationResult second = service.reverseDebit(a.getId(), transferId);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(10_000);
    }

    @Test
    void reversalWithoutDebitIsANoOp() {
        Account a = newAccount(10_000);

        OperationResult r = service.reverseDebit(a.getId(), UUID.randomUUID());

        assertThat(r.amountMinor()).isZero();
        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(10_000);
    }

    @Test
    void concurrentDebitsOnOneAccountNeverLoseUpdates() throws Exception {
        Account a = newAccount(100_000);
        int threads = 20;

        List<Callable<OperationResult>> jobs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            jobs.add(() -> service.debit(a.getId(), UUID.randomUUID(), 1_000));
        }
        runAll(jobs);

        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(100_000 - threads * 1_000L);
        assertThat(service.history(a.getId(), PageRequest.of(0, 100)).getTotalElements()).isEqualTo(threads);
    }

    @Test
    void concurrentDebitsCannotOverdraw() throws Exception {
        Account a = newAccount(5_000);   // enough for exactly 5 of the 1_000 debits

        List<Callable<Boolean>> jobs = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            jobs.add(() -> {
                try {
                    service.debit(a.getId(), UUID.randomUUID(), 1_000);
                    return true;
                } catch (InsufficientFundsException e) {
                    return false;
                }
            });
        }
        long succeeded = runAll(jobs).stream().filter(Boolean::booleanValue).count();

        assertThat(succeeded).isEqualTo(5);
        assertThat(service.get(a.getId()).getBalanceMinor()).isZero();
    }

    @Test
    void sameTransferRetriedConcurrentlyIsAppliedOnce() throws Exception {
        Account a = newAccount(10_000);
        UUID transferId = UUID.randomUUID();

        List<Callable<OperationResult>> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jobs.add(() -> service.debit(a.getId(), transferId, 2_000));
        }
        List<OperationResult> results = runAll(jobs);

        assertThat(results.stream().filter(r -> !r.replayed()).count()).isEqualTo(1);
        assertThat(service.get(a.getId()).getBalanceMinor()).isEqualTo(8_000);
    }

    private static <T> List<T> runAll(List<Callable<T>> jobs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<T> out = new ArrayList<>();
            for (Future<T> f : pool.invokeAll(jobs)) {
                out.add(f.get());
            }
            return out;
        } finally {
            pool.shutdownNow();
        }
    }
}
