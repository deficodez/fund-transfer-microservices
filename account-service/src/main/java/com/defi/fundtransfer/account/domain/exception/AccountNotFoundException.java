package com.defi.fundtransfer.account.domain.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}
