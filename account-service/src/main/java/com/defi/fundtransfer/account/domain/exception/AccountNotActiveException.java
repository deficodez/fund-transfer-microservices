package com.defi.fundtransfer.account.domain.exception;

import com.defi.fundtransfer.account.domain.AccountStatus;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(UUID accountId, AccountStatus status) {
        super("Account %s is %s and cannot move money".formatted(accountId, status));
    }
}
