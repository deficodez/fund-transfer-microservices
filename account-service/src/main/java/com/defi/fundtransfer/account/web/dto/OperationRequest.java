package com.defi.fundtransfer.account.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OperationRequest(
        @NotNull UUID transferId,
        @Min(value = 1, message = "must be at least 1 minor unit") long amountMinor) {
}
