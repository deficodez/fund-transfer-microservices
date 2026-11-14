package com.defi.fundtransfer.account.domain;

import com.defi.fundtransfer.account.domain.exception.AccountNotActiveException;
import com.defi.fundtransfer.account.domain.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root. All balance changes go through {@link #debit(long)} / {@link #credit(long)}
 * so the invariants (never negative, only ACTIVE accounts move money) live in one place.
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20, updatable = false)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    /** Wrapper type on purpose: null means "new" to Spring Data, so save() issues persist() not merge(). */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
        // JPA
    }

    public static Account open(String accountNumber, String ownerName, String currency, long initialBalanceMinor) {
        if (initialBalanceMinor < 0) {
            throw new IllegalArgumentException("initial balance cannot be negative");
        }
        Account a = new Account();
        a.id = UUID.randomUUID();
        a.accountNumber = accountNumber;
        a.ownerName = ownerName;
        a.currency = currency;
        a.balanceMinor = initialBalanceMinor;
        a.status = AccountStatus.ACTIVE;
        a.createdAt = Instant.now();
        a.updatedAt = a.createdAt;
        return a;
    }

    public void debit(long amountMinor) {
        requirePositive(amountMinor);
        requireActive();
        if (balanceMinor < amountMinor) {
            throw new InsufficientFundsException(id, balanceMinor, amountMinor);
        }
        balanceMinor -= amountMinor;
        touch();
    }

    public void credit(long amountMinor) {
        requirePositive(amountMinor);
        requireActive();
        balanceMinor += amountMinor;
        touch();
    }

    /** Used by compensation: giving money back must work even if the account was frozen after the debit. */
    public void restore(long amountMinor) {
        requirePositive(amountMinor);
        balanceMinor += amountMinor;
        touch();
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id, status);
        }
    }

    private static void requirePositive(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public long getBalanceMinor() { return balanceMinor; }
    public AccountStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
