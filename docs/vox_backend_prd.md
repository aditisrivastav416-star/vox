# Vox Backend Product Requirements Document (PRD)

**Product:** Vox --- Personal Finance & Shared Expense Assistant\
**Document scope:** Backend platform only\
**Status:** v1 Planning\
**Primary client:** Android\
**Architecture direction:** Production-ready modular monolith with clear
extraction boundaries\
**Initial stack:** FastAPI, PostgreSQL, Redis (only where justified),
background workers/queue, object storage\
**Design source:** Vox Figma product UI

------------------------------------------------------------------------

## 1. Product Overview

Vox is a personal finance application for tracking everyday spending,
understanding spending patterns, recording transactions through natural
language/voice, scanning receipts, managing shared expenses with groups,
and tracking recurring payments.

The backend must serve as the authoritative cloud system for user
identity, financial records, shared-expense state, recurring-payment
rules, analytics, receipt processing, AI-assisted commands, and
synchronization across Android clients.

Vox is intended to become a real consumer application rather than a
demo. The backend should therefore be designed for correctness,
security, observability, horizontal growth, and safe schema/API
evolution from the beginning, without prematurely introducing
microservices or infrastructure that current scale does not justify.

------------------------------------------------------------------------

## 2. Goals

The backend must:

-   Provide secure authentication and user isolation.
-   Store personal transactions, income, categories, budgets, groups,
    shared expenses, settlements, and recurring payments.
-   Power the Home dashboard and Activity views shown in the Vox UI.
-   Support offline-first Android clients with reliable synchronization
    and idempotent writes.
-   Support natural-language/voice financial commands through a
    constrained AI tool layer.
-   Process receipt images into editable transaction drafts.
-   Generate spending analytics for time periods such as 7 days, month,
    year, and arbitrary ranges.
-   Deliver recurring-payment and shared-expense notifications.
-   Remain maintainable as usage grows from early beta users to
    materially larger traffic.
-   Provide measurable reliability, latency, AI quality, and operational
    health.

------------------------------------------------------------------------

## 3. Non-Goals for Initial Release

V1 will not:

-   Connect directly to banks or automatically import bank transactions.
-   Move real money or act as a payment processor.
-   Store card/bank credentials.
-   Provide investment, tax, lending, or regulated financial advice.
-   Use microservices solely for theoretical scale.
-   Require Kafka, Kubernetes, event sourcing, or CQRS at launch.
-   Allow an LLM to directly mutate financial records without
    deterministic validation/business logic.

------------------------------------------------------------------------

## 4. Proposed System Architecture

Vox should begin as a **modular monolith**.

``` text
Android App / Widget
        |
        | HTTPS / JSON
        v
   API / FastAPI
        |
        +---------------------------+
        | Modular Domain Layer      |
        |                           |
        | Auth                      |
        | Users                     |
        | Transactions              |
        | Budgets / Analytics       |
        | Groups / Settlements      |
        | Recurring Payments        |
        | Receipts                  |
        | Vox Assistant             |
        | Notifications             |
        +---------------------------+
          |       |       |       |
          v       v       v       v
     PostgreSQL  Redis   Queue   Object Storage
                           |
                           v
                        Workers
                      /    |     \
                     AI   OCR   Notifications
```

Domain boundaries must be explicit even though they initially run in one
deployable backend. A high-load domain can later be extracted without
redesigning the entire application.

------------------------------------------------------------------------

## 5. Backend Modules

### 5.1 Authentication & Identity

Required capabilities:

-   Email/password registration.
-   Email/password login.
-   Secure password hashing.
-   Access/session token strategy.
-   Refresh/session renewal.
-   Logout and session revocation.
-   Current-user endpoint.
-   Account deletion.
-   Password reset.
-   Optional email verification.
-   Support future Google/OIDC sign-in without redesigning user
    identity.

The login UI currently presents email/password authentication; the
backend must never rely on client-side authorization checks.

### 5.2 Users & Preferences

Store:

-   User ID.
-   Display name.
-   Email.
-   Preferred currency.
-   Time zone.
-   Locale.
-   Profile metadata required by the UI.
-   Notification preferences.
-   Account creation/update timestamps.

Currency must not be inferred from UI formatting. Monetary records must
retain their currency explicitly.

### 5.3 Transactions

The transaction service is the core financial ledger for Vox.

Support:

-   Expense.
-   Income.
-   Refund/adjustment where required.
-   Manual entry.
-   Voice-created entry.
-   Receipt-created entry.
-   Recurring-payment-generated entry.
-   Shared/group-linked entry.

Each transaction should support, at minimum:

-   Stable UUID.
-   Owner.
-   Type.
-   Amount in minor currency units.
-   Currency.
-   Merchant/title/description.
-   Category.
-   Transaction timestamp/date.
-   Source.
-   Notes.
-   Group/shared-expense relationship where applicable.
-   Receipt relationship where applicable.
-   Client-generated idempotency identifier.
-   Created/updated timestamps.
-   Sync/version metadata.

Operations:

-   Create.
-   Read.
-   Edit.
-   Delete/archive.
-   Paginated history.
-   Filter by date range.
-   Filter by category.
-   Filter by type.
-   Search.
-   Incremental synchronization.

### 5.4 Categories

Provide system categories such as:

-   Food & drink.
-   Shopping.
-   Transport.
-   Bills/utilities.
-   Entertainment.
-   Housing.
-   Health.
-   Education.
-   Travel.
-   Income.
-   Other.

The model should allow category evolution and eventual user-defined
categories without requiring a transaction schema rewrite.

### 5.5 Budgets

The Home UI displays a monthly budget, amount spent, percentage used,
and remaining amount.

Support:

-   User monthly budget.
-   Effective period.
-   Currency.
-   Amount spent.
-   Amount remaining.
-   Percentage consumed.
-   Future extension to category-specific budgets.

Budget calculations must be server-verifiable even if Android caches
equivalent calculations locally.

### 5.6 Analytics

The backend must power the dashboard's total-spend and
category-breakdown experiences.

Required aggregations:

-   Total expenses over a date range.
-   Total income over a date range.
-   Spending by category.
-   Percentage by category.
-   Budget consumed.
-   Budget remaining.
-   Period-over-period comparison.
-   Time-series spending data.
-   Recent transactions.

Supported periods should include:

-   Past 7 days.
-   Current/selected month.
-   Past/current year.
-   Arbitrary start/end range.

Analytics endpoints should return structured values;
presentation-specific strings belong to Android.

At early scale, compute from PostgreSQL with appropriate indexes.
Introduce cached or pre-aggregated results only after profiling shows a
need.

### 5.7 Activity Feed

The Figma Activity experience groups transactions chronologically and
allows editing.

The backend should expose cursor-paginated activity/history ordered
deterministically by transaction time plus a stable tie-breaker.

The feed should support:

-   Expenses.
-   Income.
-   Relevant group/shared-expense activity if the product chooses to
    surface it.
-   Filtering.
-   Search.
-   Editing/deletion through the canonical transaction APIs.

### 5.8 Groups & Shared Expenses

The UI exposes "Your groups" and group creation.

Support:

-   Create group.
-   Rename/update group.
-   Add/invite members.
-   Remove/leave member with settlement safeguards.
-   List user's groups.
-   Group details.
-   Group membership and roles.
-   Shared expenses.
-   Expense participants.
-   Who paid.
-   Equal split.
-   Exact/custom split.
-   Balance calculation.
-   Settlements.
-   Group activity.

Financial invariants must be enforced server-side. For example,
allocated shares must equal the expense total.

A shared expense and its participant allocations must be committed
atomically.

### 5.9 Recurring Payments

The UI contains recurring entries such as subscriptions and monthly
bills.

Support two models:

**Fixed amount** - Rent. - Subscription. - School fee. - Other
predictable payments.

**Variable amount** - Electricity. - Utility bill. - Other payments
whose amount must be supplied for each occurrence.

A recurring rule should capture:

-   Owner.
-   Name.
-   Category.
-   Currency.
-   Fixed/variable type.
-   Amount when fixed.
-   Frequency.
-   Recurrence rule/day.
-   Next due date.
-   Reminder configuration.
-   Active/paused state.

The server, not the Android device, is responsible for determining due
occurrences.

For variable bills, Vox should create a due occurrence/reminder and ask
the user for the amount rather than inventing a transaction value.

Background processing must be idempotent so retries cannot generate
duplicate occurrences.

### 5.10 Receipt Processing

The Scan experience requires an asynchronous receipt pipeline.

``` text
Android
   |
   | request upload
   v
Backend
   |
   +--> Object Storage
   |
   +--> Receipt Job
           |
           v
        OCR / Vision
           |
           v
    Structured Extraction
           |
           v
      Draft Transaction
           |
           v
    User Reviews / Confirms
```

Extract where possible:

-   Merchant.
-   Date.
-   Currency.
-   Subtotal.
-   Tax.
-   Total.
-   Line items.
-   Suggested category.

Receipt extraction must create a **draft**, not silently commit an
uncertain financial transaction.

Store extraction confidence/metadata needed for debugging and evaluation
without retaining unnecessary sensitive model prompts/logs.

### 5.11 Vox AI Assistant

The record/voice interaction should be backed by a constrained assistant
layer.

Example:

``` text
"Spent 400 on groceries yesterday"
              |
              v
        Vox Assistant
              |
              v
       Structured Tool Call
              |
              v
{
  "tool": "create_transaction",
  "arguments": {
    "amount": 400,
    "type": "expense",
    "category": "groceries",
    "date": "..."
  }
}
              |
              v
       Schema Validation
              |
              v
       Business Service
              |
              v
          PostgreSQL
```

Initial tools:

-   `create_transaction`
-   `update_transaction`
-   `search_transactions`
-   `get_spending_summary`
-   `get_category_breakdown`
-   `create_recurring_payment`
-   `get_upcoming_payments`
-   `get_group_balance`
-   `create_shared_expense`

Rules:

-   LLM output is untrusted input.
-   Tool arguments must pass strict schemas.
-   Authorization happens after tool selection and before execution.
-   Ambiguous/destructive actions require clarification or confirmation.
-   Financial calculations should be performed by deterministic
    application/database code rather than trusted to free-form model
    output.
-   AI provider calls must have timeout, retry, quota, and
    graceful-degradation handling.
-   Store enough telemetry to evaluate model quality without
    unnecessarily storing sensitive user content.

### 5.12 Notifications

Support server-originated notification events for:

-   Upcoming recurring payments.
-   Variable recurring payment requiring an amount.
-   Group expense added.
-   Group invitation.
-   Settlement-related activity where appropriate.

Use push notifications through the Android notification infrastructure.

The backend must store device tokens, handle invalid tokens, and prevent
duplicate notification delivery where possible.

------------------------------------------------------------------------

## 6. Offline-First Android Synchronization

Android should be capable of recording transactions without
connectivity. Therefore the API must explicitly support offline
synchronization.

### Client write flow

``` text
Android creates local transaction
        |
        | client-generated UUID
        v
Room = immediate local source of truth
        |
        | connectivity available
        v
WorkManager
        |
        | idempotent request
        v
Backend
        |
        v
PostgreSQL
        |
        v
Server acknowledgement/version
        |
        v
Room updated as synced
```

Requirements:

-   Client-generated operation/transaction identifiers.
-   Idempotent mutation endpoints.
-   Duplicate retry must not create duplicate financial records.
-   Server-generated `updated_at` or version marker.
-   Incremental pull/sync mechanism.
-   Tombstone/deletion strategy where synchronization requires it.
-   Explicit conflict policy.
-   Network retries must be safe.
-   Android must be able to recover if the server commits a transaction
    but the response is lost.

For V1, conflict handling can use server-authoritative
last-write/version checking for ordinary transaction edits, while
high-risk shared-expense operations should reject stale updates rather
than silently overwrite them.

------------------------------------------------------------------------

## 7. API Design

Use versioned REST APIs:

``` text
/api/v1/...
```

Representative endpoint groups:

``` text
/auth/*
/users/me

/transactions
/transactions/{id}
/transactions/sync

/categories

/budgets
/analytics/summary
/analytics/categories
/analytics/timeline

/groups
/groups/{id}
/groups/{id}/members
/groups/{id}/expenses
/groups/{id}/balances
/groups/{id}/settlements

/recurring-payments
/recurring-payments/{id}
/recurring-payments/{id}/occurrences

/receipts
/receipts/{id}

/assistant/messages
/assistant/confirm

/devices
/notification-preferences
```

This is an API surface guide, not a requirement to create one endpoint
for every UI interaction.

### API conventions

-   JSON.
-   UUID resource identifiers.
-   UTC timestamps over the wire.
-   Explicit currency codes.
-   Monetary amounts represented safely (prefer integer minor units
    internally/API contract).
-   Consistent error envelope.
-   Machine-readable error codes.
-   Cursor pagination for growing feeds.
-   Idempotency keys/client operation IDs for retryable mutations.
-   API versioning from initial public release.

------------------------------------------------------------------------

## 8. Core Data Model

Primary entities:

``` text
User
 |
 +-- UserPreference
 +-- Device
 +-- Budget
 +-- Transaction ---- Category
 |       |
 |       +---- Receipt
 |
 +-- RecurringRule
 |       |
 |       +---- RecurringOccurrence
 |
 +-- GroupMembership ---- Group
                            |
                            +---- SharedExpense
                                      |
                                      +---- ExpenseShare
                            |
                            +---- Settlement

AssistantConversation
 |
 +-- AssistantMessage
 +-- AssistantAction
```

Exact schemas belong in the technical design/database specification
rather than the PRD.

------------------------------------------------------------------------

## 9. Database Requirements

Use PostgreSQL as the system of record.

Requirements:

-   Foreign-key integrity.
-   Unique constraints for invariants/idempotency.
-   Transactions for multi-table financial operations.
-   Appropriate indexes based on real access paths.
-   Migration tooling.
-   Connection pooling.
-   Database backups.
-   Restore procedure.
-   Query monitoring.
-   No floating-point storage for money.

Likely early indexes include owner + transaction date, group membership,
recurring next-due date, and synchronization/version fields.

Do not add indexes mechanically; verify query plans as real data grows.

------------------------------------------------------------------------

## 10. Redis & Caching

Redis is optional at initial development and should be introduced for
concrete requirements.

Potential uses:

-   Rate limiting.
-   Short-lived analytics cache.
-   Distributed idempotency metadata where DB constraints alone are
    insufficient.
-   Short-lived application state.
-   Queue infrastructure depending on worker choice.

PostgreSQL remains authoritative.

Cache invalidation must occur when relevant transactions/budgets change.
Correctness is more important than serving stale financial totals
quickly.

------------------------------------------------------------------------

## 11. Background Jobs

Use workers for tasks that should not hold an API request open:

-   Receipt OCR/extraction.
-   Long-running AI operations if necessary.
-   Recurring-payment occurrence generation.
-   Push notifications.
-   Email.
-   Cleanup jobs.
-   Future analytics precomputation.

Jobs must support:

-   Retry policy.
-   Exponential backoff where appropriate.
-   Idempotency.
-   Dead-letter/failure visibility.
-   Structured logging.
-   Correlation/request IDs.

------------------------------------------------------------------------

## 12. Security & Privacy

Minimum production requirements:

-   HTTPS only.
-   Modern password hashing.
-   Server-side authorization on every user-owned resource.
-   Secure session/token lifecycle.
-   Rate limiting for authentication and AI endpoints.
-   Input/schema validation.
-   Object-storage access control.
-   Short-lived signed receipt URLs where required.
-   Secrets outside source control.
-   Sensitive-data log redaction.
-   Account/data deletion.
-   Database backups protected appropriately.
-   Dependency/security scanning in CI.
-   Auditability for sensitive mutations.

A user knowing another transaction/group/receipt UUID must never grant
access to it.

Vox does not require real bank credentials for the designed product;
avoid collecting financial secrets the application does not need.

------------------------------------------------------------------------

## 13. Reliability & Consistency

Financial mutations prioritize correctness over availability.

Requirements:

-   Atomic transaction creation where related records must change
    together.
-   Atomic shared-expense + participant allocation.
-   Idempotent retry handling.
-   Explicit concurrency handling.
-   Optimistic versioning where stale edits are dangerous.
-   Duplicate recurring-job protection.
-   Retryable vs non-retryable errors clearly distinguished.
-   External AI/OCR failure must not corrupt canonical financial
    records.

------------------------------------------------------------------------

## 14. Performance Targets

Initial production targets:

-   Typical read API p95: \< 300 ms excluding third-party AI/OCR calls.
-   Typical write API p95: \< 500 ms excluding third-party services.
-   Dashboard summary should feel interactive and support
    caching/precomputation if real measurements require it.
-   Paginated endpoints must not degrade linearly with total account
    history.
-   Receipt/AI work may be asynchronous when latency exceeds an
    interactive request budget.

Targets should be measured in production and adjusted from evidence
rather than claimed as achieved before deployment.

------------------------------------------------------------------------

## 15. Scalability Strategy

Scale progressively.

### Stage 1 --- Launch

``` text
FastAPI
   |
   +-- PostgreSQL
   +-- Object Storage
   +-- Worker/Queue
```

### Stage 2 --- Growing usage

``` text
Load Balancer
      |
 FastAPI replicas
      |
 +----+----------+
 |               |
PostgreSQL      Redis
 |               |
Read tuning    Cache/rate limits
      |
 Queue --> Worker replicas
```

### Stage 3 --- Only when measurements justify it

Potentially:

-   Read replicas.
-   Analytics/preaggregation pipeline.
-   Independent AI/receipt workers.
-   Dedicated notification service.
-   Domain extraction into services.
-   Event streaming for genuinely high-volume asynchronous workflows.

No migration to microservices should occur without an identified
operational or scaling bottleneck.

------------------------------------------------------------------------

## 16. Observability

From the first production deployment:

-   Structured JSON logs.
-   Request/correlation IDs.
-   Centralized error reporting.
-   API request rate.
-   p50/p95/p99 latency.
-   4xx/5xx rates.
-   Database latency and slow queries.
-   Worker queue depth.
-   Job retry/failure rate.
-   AI provider latency/errors.
-   AI token/cost metrics.
-   Receipt processing latency/failure rate.
-   Notification delivery outcomes.
-   Android/backend sync failure metrics.

Never log passwords, auth tokens, full sensitive receipts, or
unnecessary personal financial data.

------------------------------------------------------------------------

## 17. AI Evaluation Requirements

Vox should maintain a versioned evaluation dataset for assistant
behavior.

Test examples should cover:

-   Expense creation.
-   Income creation.
-   Relative dates.
-   Ambiguous amounts.
-   Corrections ("actually make that 450").
-   Spending questions.
-   Period comparisons.
-   Recurring payments.
-   Group expenses.
-   Unsupported requests.
-   Malicious/prompt-injection-like inputs.

Track:

-   Intent/tool-selection accuracy.
-   Argument extraction accuracy.
-   Amount accuracy.
-   Date parsing accuracy.
-   Category accuracy.
-   End-to-end action accuracy.
-   Clarification rate.
-   Invalid/hallucinated tool-call rate.
-   Latency.
-   Cost per assistant request.

Model/prompt changes should run against the regression suite before
production deployment.

------------------------------------------------------------------------

## 18. Testing Strategy

### Backend

-   Unit tests for domain logic.
-   API integration tests.
-   PostgreSQL integration tests.
-   Authentication/authorization tests.
-   Migration tests.
-   Pagination tests.
-   Transaction/concurrency tests.
-   Idempotency tests.
-   Recurring-job duplicate tests.
-   Group balance invariant tests.
-   Receipt workflow tests.
-   Notification tests.

### AI

-   Structured-output/schema tests.
-   Tool-selection evaluation.
-   Golden-query regression suite.
-   Ambiguous-query tests.
-   Failure/fallback tests.

Critical financial logic should not require live LLM calls to test.

------------------------------------------------------------------------

## 19. CI/CD

Every backend pull request should run:

``` text
Formatting / lint
        |
Type/static checks
        |
Unit tests
        |
Integration tests
        |
Security/dependency checks
        |
AI regression subset
        |
Build container
```

Deployment environments:

-   Local.
-   Development/test.
-   Staging.
-   Production.

Production database migrations should be controlled and
backwards-compatible where rolling deployments require it.

------------------------------------------------------------------------

## 20. Product-to-Backend Mapping

  -----------------------------------------------------------------------
  UI/Product Capability               Backend Responsibility
  ----------------------------------- -----------------------------------
  Email login                         Auth/session management

  User greeting/account drawer        User/profile service

  Total spent this month              Analytics aggregation

  Monthly budget / % used / remaining Budget + analytics

  Spending breakdown                  Category analytics

  Activity transaction list           Cursor-paginated transaction
                                      history

  Edit transaction                    Authorized transaction mutation

  Scan                                Receipt upload + OCR/extraction

  Record transaction                  Transaction API / Vox Assistant

  Groups                              Groups, memberships, shared
                                      expenses, balances

  Recurring payments                  Recurrence rules, occurrences,
                                      scheduler

  Widget 7D / month / year            Local cached analytics + backend
                                      summary/sync

  Voice financial questions           Assistant tools over authorized
                                      financial services

  Voice financial actions             Structured AI action → validation →
                                      domain service
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 21. Delivery Phases

### Phase 1 --- Backend Foundation

-   Project structure/config.
-   PostgreSQL.
-   Migrations.
-   Authentication.
-   Users/preferences.
-   Categories.
-   Transactions CRUD.
-   Pagination/filtering.
-   Basic budgets.
-   Basic analytics.
-   Testing foundation.
-   Docker + CI.

**Exit criterion:** Android can authenticate, create/edit/delete/sync
transactions, and render the primary Home/Activity data from real APIs.

### Phase 2 --- Offline Reliability & Production Readiness

-   Idempotent mutations.
-   Incremental sync.
-   Conflict/version handling.
-   Rate limiting.
-   Structured logs/metrics.
-   Staging/production environments.
-   Backups.
-   Error monitoring.
-   Push-device registration.

**Exit criterion:** normal network loss/retry/process-death scenarios do
not create duplicate or corrupt financial records.

### Phase 3 --- Groups

-   Groups/members.
-   Shared expenses.
-   Split logic.
-   Balance computation.
-   Settlements.
-   Group notifications.
-   Concurrency/invariant tests.

**Exit criterion:** multiple users can share expenses while balances
remain mathematically consistent under retries/concurrent updates.

### Phase 4 --- Recurring Payments

-   Fixed and variable recurring rules.
-   Occurrence generation.
-   Scheduler/workers.
-   Reminder notifications.
-   Idempotent job execution.

**Exit criterion:** missed/retried scheduler runs cannot duplicate
occurrences and variable bills never fabricate an amount.

### Phase 5 --- Receipt Intelligence

-   Secure upload.
-   Object storage.
-   Async processing.
-   OCR/vision extraction.
-   Draft transaction.
-   User confirmation.
-   Extraction evaluation.

**Exit criterion:** receipt failures degrade safely and never create
unconfirmed incorrect transactions.

### Phase 6 --- Vox AI Assistant

-   Assistant API.
-   Tool schemas.
-   Financial query tools.
-   Transaction tools.
-   Confirmation/clarification flow.
-   AI telemetry.
-   Evaluation dataset/regression pipeline.

**Exit criterion:** model output cannot bypass authorization/business
rules, and assistant quality is measured with repeatable evaluations.

### Phase 7 --- Scale From Evidence

-   Profile database queries.
-   Add justified indexes.
-   Redis caching where measured.
-   Horizontal API/worker scaling.
-   Preaggregation where necessary.
-   Load testing.
-   Cost optimization.

**Exit criterion:** architectural changes are tied to measured
bottlenecks rather than hypothetical scale.

------------------------------------------------------------------------

## 22. Backend Definition of Done for Public Beta

The backend is ready for a public Vox beta when:

-   Authentication and authorization have security tests.
-   Personal transactions work online and offline through the Android
    sync protocol.
-   Duplicate client retries are safe.
-   Home analytics and Activity are backed by production data.
-   Group financial invariants are tested.
-   Recurring jobs are idempotent.
-   AI actions pass through validated deterministic services.
-   Receipt extraction produces drafts requiring confirmation.
-   Production logging, metrics, crash/error monitoring, and alerts
    exist.
-   Database backup and restore have been tested.
-   Account deletion is implemented.
-   Secrets are managed outside the repository.
-   Staging and production are separate.
-   CI gates critical tests.
-   Privacy-facing data behavior is documented.

------------------------------------------------------------------------

## 23. Engineering Principle

Vox should be built **for evolution, not for imaginary scale**.

The system should have strong domain boundaries, correct financial
invariants, idempotent APIs, reliable offline synchronization,
constrained AI execution, tests, metrics, and operational visibility
from the beginning. Infrastructure complexity should be introduced only
when actual traffic, latency, reliability, team ownership, or cost data
justifies it.

That approach gives Vox a credible production path while also making the
project technically defensible for Android, backend, applied-AI, and
system-design interviews.
