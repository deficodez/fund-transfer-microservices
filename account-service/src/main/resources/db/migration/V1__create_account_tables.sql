-- Amounts are stored as minor units (paise/cents) in BIGINT. Never floats, never implicit rounding.
create table account (
    id              uuid         primary key,
    account_number  varchar(20)  not null unique,
    owner_name      varchar(120) not null,
    currency        varchar(3)   not null,
    balance_minor   bigint       not null check (balance_minor >= 0),
    status          varchar(16)  not null,
    version         bigint       not null default 0,   -- optimistic locking (@Version)
    created_at      timestamptz  not null,
    updated_at      timestamptz  not null
);

-- One row per applied operation. The unique constraint is the idempotency guard:
-- the same (account, transfer, type) can only ever be applied once.
create table account_operation (
    id                   uuid        primary key,
    account_id           uuid        not null references account (id),
    transfer_id          uuid        not null,
    type                 varchar(20) not null,
    amount_minor         bigint      not null check (amount_minor > 0),
    balance_after_minor  bigint      not null,
    created_at           timestamptz not null,
    constraint uq_account_transfer_type unique (account_id, transfer_id, type)
);

create index idx_account_operation_account_created
    on account_operation (account_id, created_at desc);
