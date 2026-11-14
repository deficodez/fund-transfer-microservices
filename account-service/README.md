# account-service

Owns accounts, balances and an append-only log of idempotent money operations. First of three services
in the fund-transfer system (Account, Transfer, Ledger).

## Run

```bash
docker compose up -d account-db          # from fund-transfer/
cd account-service
mvn spring-boot:run                      # http://localhost:8081
mvn verify                               # integration tests, needs Docker (Testcontainers)
```

## Try it

```bash
# open an account with 10,000.00 (minor units: 1_000_000 paise)
curl -s -X POST localhost:8081/api/v1/accounts -H 'Content-Type: application/json' \
  -d '{"ownerName":"Defi","currency":"INR","initialBalanceMinor":1000000}'

# debit - run it twice with the same transferId: 201 then 200 (replayed), balance moves once
curl -s -i -X POST localhost:8081/api/v1/accounts/<id>/debit -H 'Content-Type: application/json' \
  -d '{"transferId":"11111111-1111-1111-1111-111111111111","amountMinor":250000}'

# compensate
curl -s -X POST localhost:8081/api/v1/accounts/<id>/debit/reverse -H 'Content-Type: application/json' \
  -d '{"transferId":"11111111-1111-1111-1111-111111111111"}'
```

## Design decisions worth being able to explain

| Decision | Why |
|---|---|
| Amounts as `long` minor units | No floating point, no rounding surprises |
| `SELECT ... FOR UPDATE` on debit/credit | Hot account: queue on the row lock instead of retrying optimistic conflicts |
| `@Version` kept as well | Safety net for any write path that forgets to lock |
| `account_operation` unique `(account, transfer, type)` | Idempotency enforced by the database, not just by code |
| Replay with different amount = 409 | A reused key with different data is a client bug |
| `restore()` ignores account status | Compensation must work even if the account was frozen after the debit |
| `open-in-view: false` | Lazy loading from the web layer hides N+1 bugs |
| `AccountOperation.accountId` is a plain UUID | Association is added on purpose in Phase 2 to reproduce N+1 |
| Flyway owns schema, Hibernate `validate` | Schema changes are reviewed migrations |

## Today's exercises (after it runs)

1. Read `AccountServiceIT` top to bottom and predict each result before running it.
2. Remove `@Lock` from `findByIdForUpdate`: which tests fail, and how? (Hint: `@Version` throws.)
3. Remove the unique constraint and the lookup: show a duplicate debit.
4. Turn on `hibernate.generate_statistics` and read the query counts for `history()`.

## Known gaps (planned)

- Late DEBIT after its own reversal is still applied (Phase 3: cancellation marker).
- No auth yet (Phase 4). No Kafka commands yet (Phase 3). No outbox yet (Phase 3).
