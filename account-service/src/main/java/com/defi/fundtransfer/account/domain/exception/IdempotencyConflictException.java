package com.defi.fundtransfer.account.domain.exception;

import java.util.UUID;

/** The same transferId was replayed with a different amount: a client bug, never silently accepted. */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(UUID transferId, long originalAmount, long newAmount) {
        super("Transfer %s was already applied with amount %d, got %d".formatted(transferId, originalAmount, newAmount));
    }
}
