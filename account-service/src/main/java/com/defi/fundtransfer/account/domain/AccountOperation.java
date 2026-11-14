package com.defi.fundtransfer.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of one applied balance change. Doubles as the idempotency key store:
 * unique (account_id, transfer_id, type) in the database means a retried command cannot apply twice.
 *
 * <p>Deliberately holds {@code accountId} as a plain UUID instead of a {@code @ManyToOne Account}.
 * That keeps this table cheap to read (no accidental join, no lazy-loading trap). You will add the
 * association in Phase 2 on purpose, to reproduce and then fix the N+1 problem.
 */
@Entity
@Table(name = "account_operation")
public class AccountOperation {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private OperationType type;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(name = "balance_after_minor", nullable = false, updatable = false)
    private long balanceAfterMinor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountOperation() {
        // JPA
    }

    public AccountOperation(UUID accountId, UUID transferId, OperationType type, long amountMinor, long balanceAfterMinor) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.transferId = transferId;
        this.type = type;
        this.amountMinor = amountMinor;
        this.balanceAfterMinor = balanceAfterMinor;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getTransferId() { return transferId; }
    public OperationType getType() { return type; }
    public long getAmountMinor() { return amountMinor; }
    public long getBalanceAfterMinor() { return balanceAfterMinor; }
    public Instant getCreatedAt() { return createdAt; }
}
