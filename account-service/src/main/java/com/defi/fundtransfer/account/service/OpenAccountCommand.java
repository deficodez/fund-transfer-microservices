package com.defi.fundtransfer.account.service;

public record OpenAccountCommand(String ownerName, String currency, long initialBalanceMinor) {
}
