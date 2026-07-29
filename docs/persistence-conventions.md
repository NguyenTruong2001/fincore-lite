# FINCORE LITE Persistence Conventions

## Database objects

- Database object: `snake_case`
- Java class: `PascalCase`
- Java field: `camelCase`
- Constant: `UPPER_SNAKE_CASE`

## Tables

Domain tables use plural nouns:

- `customers`
- `accounts`
- `account_holds`
- `transfers`
- `ledger_transactions`
- `ledger_entries`
- `idempotency_records`
- `outbox_events`

The existing technical table `system_metadata` keeps its current name.

## Columns

Columns use descriptive `snake_case` names:

- `customer_id`
- `source_account_id`
- `available_balance`
- `business_date`
- `created_at`

Avoid unclear abbreviations such as `cust_id`, `amt`, `bal`, and `val`.

## Constraints

- Primary key: `pk_<table>`
- Foreign key: `fk_<source_table>_<target_table>`
- Unique constraint: `uk_<table>_<business_columns>`
- Check constraint: `ck_<table>_<rule>`

Examples:

- `pk_accounts`
- `fk_accounts_customers`
- `uk_accounts_account_number`
- `ck_account_holds_amount_positive`

## Indexes

Index naming:

`ix_<table>_<columns>`

Examples:

- `ix_transfers_source_account_id_created_at`
- `ix_outbox_events_status_created_at`

Indexes are created from known query patterns, not automatically for every
column or foreign key.

## Identifier conventions

Primary key, business reference, and idempotency key are separate concerns.

Aggregate identifiers generally use application-generated PostgreSQL UUID
values. UUID values must not be persisted as VARCHAR.

Natural keys are allowed only when stable and appropriate, such as
`system_metadata.metadata_key`.

## Time conventions

- Absolute technical timestamp: PostgreSQL `TIMESTAMPTZ`, Java `Instant`
- Business date: PostgreSQL `DATE`, Java `LocalDate`
- Application time source: injected `Clock`

`LocalDateTime` is not used for technical audit timestamps.

## Lifecycle conventions

Created-only entities extend `CreatedAuditEntity`.

Mutable entities extend `MutableAuditEntity`.

Audit superclasses do not contain:

- Entity identifiers
- Soft-delete fields
- Domain state
- Business behavior

Financial history does not use generic soft delete.