package com.defi.fundtransfer.account.domain.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId, long balanceMinor, long requestedMinor) {
        super("Insufficient funds on account %s: balance=%d requested=%d".formatted(accountId, balanceMinor, requestedMinor));
    }
}
