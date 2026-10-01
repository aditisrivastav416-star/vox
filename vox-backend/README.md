# Vox backend

NestJS/PostgreSQL API for the Vox Android app. All routes start with `/api/v1`.

## Run

```sh
cp .env.example .env
docker compose up -d postgres
npm install
npm run db:migrate
npm run start:dev
npm run worker
```

Set `DATABASE_URL` and use HTTPS in production. The sample database credentials are for local use only.

## Android contract

The Android app currently uses `Fake*Repository` classes and has no HTTP client. Replace those repositories with network/local Room adapters to consume these endpoints. Android's `CURRENT_USER_ID = "me"` is a local display alias; API IDs are UUIDs returned by `/auth/register` or `/auth/login`.

- `POST /auth/register` or `/auth/login`: `{ "email", "password", "name"? }` returns `{ token, user }`. Send `Authorization: Bearer <token>`.
- `GET/PUT /users/me`: `UserProfile` fields map to `name`, `currencyCode`, `phone`.
- `POST /transactions`: send the Android `Transaction` fields flattened: `id`, `type`, `amountMinor` as a decimal string, `currencyCode`, `title`, `category`, `paymentMethod`, `timestamp` (ISO 8601), `source`, `groupId`, `notes`. The response also includes `amount: { amountMinor, currencyCode }` for direct model mapping and a `version`.
- `PUT /transactions/:id`: same payload plus the last server `version`. `DELETE /transactions/:id?version=N` makes a sync tombstone. `GET /transactions/sync?cursor=...` returns changes, including tombstones, and a `nextCursor` for the next pull.
- `GET /transactions` gives cursor-paginated Activity items, with `type`, `category`, `from`, `to`, and `search` filters.
- `GET /analytics/summary`, `/analytics/categories`, `/analytics/timeline` power Home. Use `period=7d|month|year` or `from` and `to`.
- `PUT /budgets` and `GET /budgets` store monthly budget values.
- `POST /groups`, `GET /groups`, `GET /groups/:id`, `POST /groups/:id/expenses`, `GET /groups/:id/balances`, `POST /groups/:id/settlements` serve groups. Share keys are server user UUIDs and values are minor-unit strings.
- `POST/PUT /recurring-payments`, `GET /recurring-payments`, `GET /recurring-payments/:id/occurrences` serve recurring rules. Null `amountMinor` means a variable bill.

Amounts use integer minor units as strings over JSON to preserve Kotlin `Long` precision. Timestamps are UTC ISO 8601; recurring dates use `YYYY-MM-DD`. Category, type, source, method, group type and frequency strings match Android enum names.

Receipt OCR, assistant AI, push notifications, and password reset need external providers and are not configured. The worker creates due occurrences but never fabricates variable amounts.
