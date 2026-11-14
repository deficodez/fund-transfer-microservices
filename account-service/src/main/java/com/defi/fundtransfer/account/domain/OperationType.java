package com.defi.fundtransfer.account.domain;

public enum OperationType {
    DEBIT,
    CREDIT,
    /** Compensation for a previously applied DEBIT of the same transfer. */
    DEBIT_REVERSAL
}
