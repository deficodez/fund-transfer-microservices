package com.defi.fundtransfer.account.service;

import com.defi.fundtransfer.account.domain.OperationType;

import java.util.UUID;

/**
 * Outcome of a money-moving command.
 *
 * @param replayed true when the command had already been applied and this is the recorded result
 *                 (an idempotent retry), false when this call changed the balance
 */
public record OperationResult(
        UUID accountId,
        UUID transferId,
        OperationType type,
        long amountMinor,
        long balanceAfterMinor,
        boolean replayed) {
}
