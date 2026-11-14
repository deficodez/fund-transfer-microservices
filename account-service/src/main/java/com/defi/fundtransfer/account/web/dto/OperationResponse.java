package com.defi.fundtransfer.account.web.dto;

import com.defi.fundtransfer.account.domain.AccountOperation;
import com.defi.fundtransfer.account.domain.OperationType;

import java.time.Instant;
import java.util.UUID;

public record OperationResponse(
        UUID id,
        UUID transferId,
        OperationType type,
        long amountMinor,
        long balanceAfterMinor,
        Instant createdAt) {

    public static OperationResponse from(AccountOperation op) {
        return new OperationResponse(op.getId(), op.getTransferId(), op.getType(), op.getAmountMinor(),
                op.getBalanceAfterMinor(), op.getCreatedAt());
    }
}
