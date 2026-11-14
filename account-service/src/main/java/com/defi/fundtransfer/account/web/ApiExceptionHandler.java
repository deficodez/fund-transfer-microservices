package com.defi.fundtransfer.account.web;

import com.defi.fundtransfer.account.domain.exception.AccountNotActiveException;
import com.defi.fundtransfer.account.domain.exception.AccountNotFoundException;
import com.defi.fundtransfer.account.domain.exception.IdempotencyConflictException;
import com.defi.fundtransfer.account.domain.exception.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain failures to RFC 7807 problem responses so callers (and later the Saga) can branch on status. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", ex.getMessage());
    }

    /** Business rejection: the Saga treats this as a non-retryable failure and starts compensation. */
    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds", ex.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail notActive(AccountNotActiveException ex) {
        return problem(HttpStatus.CONFLICT, "Account not active", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail idempotencyConflict(IdempotencyConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
