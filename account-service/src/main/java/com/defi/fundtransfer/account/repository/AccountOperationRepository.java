package com.defi.fundtransfer.account.repository;

import com.defi.fundtransfer.account.domain.AccountOperation;
import com.defi.fundtransfer.account.domain.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, UUID> {

    Optional<AccountOperation> findByAccountIdAndTransferIdAndType(UUID accountId, UUID transferId, OperationType type);

    Page<AccountOperation> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
