package com.defi.fundtransfer.account.repository;

import com.defi.fundtransfer.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * SELECT ... FOR UPDATE. Concurrent debits on the same account queue up on the row lock instead of
     * racing and failing with an optimistic-lock exception. Money movement on a hot account is exactly
     * the case where pessimistic locking beats retry-on-conflict.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByAccountNumber(String accountNumber);
}
