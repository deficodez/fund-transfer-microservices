package com.defi.fundtransfer.account.web.dto;

import com.defi.fundtransfer.account.domain.Account;
import com.defi.fundtransfer.account.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        String currency,
        long balanceMinor,
        AccountStatus status,
        Instant createdAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getOwnerName(), a.getCurrency(),
                a.getBalanceMinor(), a.getStatus(), a.getCreatedAt());
    }
}
