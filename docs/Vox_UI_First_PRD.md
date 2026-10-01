# Vox --- UI-First Product Requirements Document

**Product:** Vox\
**Platform:** Android\
**Phase:** UI Prototype / V1 Frontend\
**UI:** Jetpack Compose + Material 3\
**Architecture target:** Clean Architecture + MVVM\
**Primary goal:** Build a production-quality Android UI for an
offline-first, voice-first personal finance and shared-expense
application.

------------------------------------------------------------------------

## 1. Product Vision

Vox is a personal finance assistant that eventually allows users to
manage money through natural interactions such as:

> "Hey Vox, spent ₹400 on groceries."

> "How much did I spend on food this month?"

> "Split this bill between me, Rahul and Riya."

For the current milestone, **only the complete Android UI and navigation
experience will be implemented**.

All data should initially come from repositories backed by mock/fake
data rather than embedding sample values directly into Composables. This
allows fake repositories to later be replaced with Room/API
implementations without rewriting UI code.

------------------------------------------------------------------------

## 2. V1 UI Scope

The prototype contains these major experiences:

  Area                      Purpose
  ------------------------- --------------------------------
  Onboarding/Auth           Login/signup experience
  Home                      Financial dashboard
  Vox                       Voice/manual transaction entry
  Activity                  Complete transaction history
  Groups                    Shared expenses
  Receipt Scanner           Receipt capture/import
  Split Bill                Equal/item-based splitting
  Recurring                 Recurring payments/reminders
  Transaction Details       Inspect/edit a transaction
  Profile/Settings          Preferences and account
  Notifications/Attention   Uncategorized transactions

Actual authentication, OCR, speech recognition, SMS reading, backend
sync and AI processing are **out of scope for this UI milestone**.

------------------------------------------------------------------------

## 3. Engineering Standards

The UI prototype should be treated as production code even while using
fake data.

### 3.1 No User-Facing Hardcoded Strings

Never:

``` kotlin
Text("Recent transactions")
```

Instead:

``` kotlin
Text(
    text = stringResource(R.string.recent_transactions)
)
```

`res/values/strings.xml`:

``` xml
<string name="recent_transactions">Recent transactions</string>
<string name="spending_this_month">Spent this month</string>
<string name="upcoming_payments">Upcoming payments</string>
```

Dynamic strings should use placeholders:

``` xml
<string name="amount_due">%1$s due</string>
<string name="due_in_days">Due in %1$d days</string>
```

This applies to labels, errors, accessibility descriptions, buttons,
empty states, dialogs, and notifications.

### 3.2 Consistent Design Tokens

Avoid arbitrary dimensions and colors scattered through screens.

``` kotlin
object VoxSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}
```

``` kotlin
object VoxRadius {
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
}
```

Use `MaterialTheme` for typography, colors, and shapes rather than
hardcoded `Color(...)` values inside screens.

------------------------------------------------------------------------

## 4. Package Structure

Use feature-based packaging rather than grouping everything into generic
`activities`, `fragments`, `models`, or `utils` packages.

``` text
com.vox.finance

├── app
│   ├── VoxApplication.kt
│   └── MainActivity.kt
├── navigation
│   ├── VoxNavHost.kt
│   ├── VoxDestination.kt
│   └── VoxNavigation.kt
├── core
│   ├── designsystem
│   │   ├── component
│   │   ├── theme
│   │   ├── icon
│   │   └── typography
│   ├── model
│   ├── common
│   └── ui
├── feature
│   ├── auth
│   ├── home
│   ├── activity
│   ├── vox
│   ├── groups
│   ├── receipt
│   ├── split
│   ├── recurring
│   ├── transaction
│   └── settings
├── data
│   ├── repository
│   └── fake
└── domain
    ├── model
    └── repository
```

Example feature:

``` text
feature/home/

├── HomeScreen.kt
├── HomeViewModel.kt
├── HomeUiState.kt
├── HomeAction.kt
└── components/
    ├── SpendingSummaryCard.kt
    ├── SpendingChart.kt
    ├── UpcomingPaymentCard.kt
    ├── GroupSummaryCard.kt
    └── AttentionCard.kt
```

------------------------------------------------------------------------

## 5. Navigation Architecture

Primary bottom navigation:

``` text
╭────────────────────────╮
│  Home    🎙   Activity │
╰────────────────────────╯
```

The center Vox action should be visually dominant.

-   **Home:** Primary financial dashboard.
-   **Vox:** Central voice/manual/add action.
-   **Activity:** Complete transaction history.
-   **Groups, Recurring, Scanner, Settings:** Secondary destinations
    reached from primary surfaces.

------------------------------------------------------------------------

## 6. Authentication UI

### Welcome

``` text
VOX

Your money.
Just ask.

Track spending, split bills and
understand your finances naturally.

[ Continue with Google ]

[ Continue with phone ]

Skip for now
```

For the UI prototype, authentication actions navigate into the
application using fake authenticated state.

### Profile Setup

Collect:

-   Name
-   Profile picture
-   Default currency
-   Optional phone number

Currency must not be assumed to always be INR in the domain/UI
architecture.

------------------------------------------------------------------------

## 7. Home Screen

Home is the primary financial dashboard.

It should contain:

-   Greeting/profile entry
-   Current month selector
-   Monthly spending
-   Monthly income
-   Net cash flow
-   Spending visualization
-   Category breakdown
-   Upcoming recurring payments
-   Groups
-   Transactions needing attention

Example:

``` text
Good morning, Aditi                     ◯

October 2026

₹18,420
Spent this month

↓ 12% from last month

╭────────────────────────────────────╮
│ Spending                           │
│                                    │
│ Food            ₹6,240             │
│ Shopping        ₹4,120             │
│ Travel          ₹2,800             │
│ Bills           ₹2,100             │
│                                    │
│        View breakdown →            │
╰────────────────────────────────────╯

Upcoming                           See all

🏠 Rent
₹18,000                         Oct 5

⚡ Electricity
Amount needed                   Oct 8

Groups                             +

╭───────────────╮ ╭───────────────╮
│ Goa Trip      │ │ Flat 402      │
│ +₹2,450       │ │ -₹740         │
│ you're owed   │ │ you owe       │
╰───────────────╯ ╰───────────────╯

Needs attention

₹1,450
XYZ Enterprises

Categorize this transaction

[Food] [Shopping] [Other]
```

### Financial Summary Modes

Support:

``` text
[ Spending ] [ Income ] [ Net ]
```

The visualization should update according to the selected mode.

Charts should be reusable Compose components consuming UI models rather
than repositories directly.

------------------------------------------------------------------------

## 8. Vox Interaction UI

Pressing the central Vox button opens a modal/full-screen interaction.

Initial:

``` text
              ✕

              ◉

          What happened?

Try saying:

"Spent ₹400 on groceries"

"Got ₹5,000 as a gift"

"How much did I spend on food?"

        [⌨ Type instead]
```

Listening:

``` text
              ✕

             ))) 🎙 (((

             Listening...

       ━━━╸━━━━╸━━━╸━━━━╸━━

       [ Cancel ]
```

Recognized:

``` text
"Spent ₹400 on groceries"

────────────────────────────

I found:

Expense
₹400
Groceries
Today

       [ Confirm ]

       Edit details
```

For the UI milestone, confirmation may update fake state.

------------------------------------------------------------------------

## 9. Quick Add

The Vox action should provide access to:

``` text
Add

🎙 Voice transaction
＋ Add manually
▣ Scan receipt
👥 Split bill
```

### Manual Transaction Form

``` text
Add transaction

[ Expense ] [ Income ]

Amount
[________________]

Description
[__________________]

Category
[ Groceries       ▼ ]

Date
[ Today           ▼ ]

Payment method
[ UPI             ▼ ]

[ Add transaction ]
```

------------------------------------------------------------------------

## 10. Receipt Scanner

Scanner UI:

``` text
Scan receipt

┌───────────────────────────┐
│                           │
│       Receipt area        │
│                           │
└───────────────────────────┘

Position receipt inside frame

           ◉

Upload from gallery
```

Camera/OCR behavior may be simulated during this milestone.

### Receipt Result

``` text
Receipt found

Social
October 1

Pasta                      ₹650
Pizza                      ₹900
Coke                       ₹300
Tax                        ₹185

──────────────────────────────
Total                    ₹2,035

[ Add transaction ]

[ Split this bill ]
```

------------------------------------------------------------------------

## 11. Split Bill

``` text
Split ₹2,035

How would you like to split?

╭──────────────────────────────╮
│ 👥 Split equally             │
│ Everyone pays the same share │
╰──────────────────────────────╯

╭──────────────────────────────╮
│ 🍽 Split by items            │
│ Assign what everyone ordered │
╰──────────────────────────────╯
```

### Equal Split

``` text
Who's splitting?

✓ You
✓ Rahul
✓ Aditi
✓ Riya

4 people

₹508.75 each

[ Confirm split ]
```

### Item Split

``` text
Who had what?

Pasta                         ₹650
[ You ▼ ]

Pizza                         ₹900
[ Rahul + Aditi ▼ ]

Coke                          ₹300
[ You + Rahul ▼ ]

Tax                           ₹185
Split proportionally

[ Calculate split ]
```

Voice-based assignment can be integrated later.

------------------------------------------------------------------------

## 12. Groups

Groups should be prominently accessible from Home.

``` text
Groups                               +

You are owed
₹4,840

You owe
₹1,240

Goa Trip 🌴
You are owed ₹2,450

Rahul                     +₹1,200
Riya                        +₹750
Aditi                       +₹500

Flat 402 🏠
You owe ₹740

College Friends
Settled up
```

### Create Group

``` text
Create group

Group name
[ Goa Trip ]

Group type
[ Trip ] [ Home ] [ Friends ] [ Other ]

Add people
[ Search name / phone / email ]

Rahul                           +
Aditi                           +
Riya                            +

[ Create group ]
```

------------------------------------------------------------------------

## 13. Group Details

``` text
← Goa Trip 🌴                        ⋮

You are owed

₹2,450

Rahul owes you                  ₹1,200
Riya owes you                     ₹750
Aditi owes you                    ₹500

[ Add expense ]       [ Settle up ]

Expenses

Dinner at Thalassa               ₹4,800
You paid
Your share ₹1,200

Taxi                              ₹1,200
Rahul paid
Your share ₹300

Hotel                            ₹12,000
You paid
Your share ₹3,000
```

------------------------------------------------------------------------

## 14. Recurring Payments

Accessible through Home → Upcoming → See all.

``` text
Recurring payments                       +

Upcoming

🏠 Rent
₹18,000
Monthly • 5th
Due in 4 days

⚡ Electricity
Ask for amount
Monthly • 8th
Due in 7 days

📺 Netflix
₹649
Monthly • 14th

🎓 School fees
₹36,000
Quarterly
```

### Create Recurring Payment

``` text
New recurring payment

Name
[ Rent ]

Amount

○ Fixed amount
● Ask me every time

[ Amount ]

Repeats
[ Monthly ▼ ]

Due
[ 5th ▼ ]

Remind me

[✓] 3 days before
[✓] On due date

[ Save ]
```

If `Fixed amount` is selected, amount is required.

If `Ask me every time` is selected, the fixed amount input should not be
required.

Initial recurrence options:

-   Daily
-   Weekly
-   Monthly
-   Quarterly
-   Yearly
-   Custom

------------------------------------------------------------------------

## 15. Activity

Activity is the authoritative transaction-history UI.

``` text
Activity

🔍 Search transactions

[ All ] [ Expenses ] [ Income ] [ Groups ]

TODAY

Swiggy                            -₹740
Food • UPI                        8:42 PM

Bonus                          +₹20,000
Income • Bank                     3:21 PM

Uber                              -₹284
Transport • UPI                   9:12 AM

YESTERDAY

Electricity                    -₹2,410
Bills • UPI

Dinner                          -₹1,200
Goa Trip • Shared
```

Use `LazyColumn` for transaction lists.

Filtering should support:

-   All
-   Expenses
-   Income
-   Shared/group expenses

Search UI should be designed to eventually support merchant, category,
date, payment method, and natural-language queries.

------------------------------------------------------------------------

## 16. Transaction Details

``` text
Expense

             -₹740

              Swiggy

             October 1
              8:42 PM

Category
Food

Payment method
UPI

Source
Detected from SMS

Group
None

Notes
—

[ Edit transaction ]

Delete transaction
```

------------------------------------------------------------------------

## 17. Empty, Loading and Error States

Every major screen must explicitly support loading, success, empty, and
error conditions where relevant.

Example:

``` kotlin
sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val data: HomeUiModel
    ) : HomeUiState

    data class Error(
        val message: UiText
    ) : HomeUiState
}
```

### Groups Empty State

``` text
No groups yet

Create a group to split expenses
with friends and keep track of
who owes whom.

[ Create your first group ]
```

### Activity Empty State

``` text
Nothing here yet

Your transactions will appear here.

[ Add transaction ]
```

------------------------------------------------------------------------

## 18. UI State Management

Follow unidirectional data flow:

``` text
User
 │
Action
 ↓
ViewModel
 │
StateFlow<UiState>
 │
State
 ↓
Compose UI
```

Example:

``` kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val summary: SpendingSummaryUi? = null,
    val upcomingPayments: List<PaymentUi> = emptyList(),
    val groups: List<GroupUi> = emptyList(),
    val transactionsNeedingAttention: List<TransactionUi> = emptyList()
)
```

Actions:

``` kotlin
sealed interface HomeAction {

    data object OpenProfile : HomeAction

    data object OpenActivity : HomeAction

    data class OpenGroup(
        val groupId: String
    ) : HomeAction

    data class CategorizeTransaction(
        val transactionId: String,
        val categoryId: String
    ) : HomeAction
}
```

Screen:

``` kotlin
HomeScreen(
    state = state,
    onAction = viewModel::onAction
)
```

Business logic should not be embedded inside Composable `onClick`
handlers.

------------------------------------------------------------------------

## 19. Compose Component Architecture

Screens should primarily compose reusable components.

Example:

``` text
HomeScreen
├── VoxTopBar
├── MonthlySummary
├── SpendingBreakdownCard
├── UpcomingPaymentsSection
│   └── UpcomingPaymentItem
├── GroupsSection
│   └── GroupCard
├── NeedsAttentionSection
│   └── TransactionAttentionCard
└── VoxFloatingNavigation
```

Avoid extremely large screen files containing every UI element.

------------------------------------------------------------------------

## 20. Compose Previews

Reusable UI components should have previews.

``` kotlin
@Preview(showBackground = true)
@Composable
private fun GroupCardPreview() {
    VoxTheme {
        GroupCard(
            group = GroupUi(
                name = "Goa Trip",
                balance = MoneyUi(
                    amount = 245000,
                    currency = "INR"
                )
            ),
            onClick = {}
        )
    }
}
```

Substantial preview/sample data should live in dedicated preview
fixtures rather than production logic.

------------------------------------------------------------------------

## 21. Money Handling

Do not scatter currency symbols or formatted monetary strings throughout
screens.

Domain representation:

``` kotlin
data class Money(
    val amountMinor: Long,
    val currencyCode: String
)
```

Example:

``` text
₹400.25

amountMinor = 40025
currencyCode = "INR"
```

All formatting should go through a centralized `MoneyFormatter`.

This allows the same UI to support INR, USD, EUR, and other currencies
later without screen changes.

------------------------------------------------------------------------

## 22. Date Handling

Screens should not manually construct date strings.

Use a centralized formatter abstraction:

``` kotlin
interface VoxDateFormatter {

    fun transactionDate(
        instant: Instant
    ): String

    fun dueDate(
        date: LocalDate
    ): String
}
```

This enables consistent presentation such as:

``` text
Today
Yesterday
Oct 1
Sep 28
```

and makes localization easier later.

------------------------------------------------------------------------

## 23. Accessibility

Accessibility should be built in from the beginning.

Requirements:

-   Meaningful icons receive appropriate semantics/content descriptions.
-   Decorative icons should not produce unnecessary TalkBack output.
-   Interactive touch targets should be at least approximately 48dp.
-   Information must not be represented by color alone.
-   Font scaling should not break important layouts.
-   Buttons and controls should have clear accessible labels.
-   Dynamic content should expose meaningful semantics.

Instead of communicating balances only with green/red colors, display
explicit labels such as:

``` text
+₹2,450
You're owed
```

and:

``` text
-₹740
You owe
```

------------------------------------------------------------------------

## 24. Theme and Design System

Centralize design primitives under:

``` text
core/designsystem/

Color.kt
Theme.kt
Type.kt
Shape.kt
Spacing.kt
VoxIcons.kt
```

Components should generally consume:

``` kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurface
MaterialTheme.typography.titleLarge
MaterialTheme.shapes.medium
```

Both light and dark themes should work from the beginning.

------------------------------------------------------------------------

## 25. Mock Data Architecture

Do not declare fake transaction/group/payment lists directly inside
screens.

Use:

``` text
FakeTransactionRepository
FakeGroupRepository
FakeRecurringPaymentRepository
        ↓
     ViewModels
        ↓
    StateFlow
        ↓
       UI
```

Later:

``` text
FakeTransactionRepository
            ↓ replace
RoomTransactionRepository
```

The UI should not need to change when the real data layer is introduced.

------------------------------------------------------------------------

## 26. UI Testing Requirements

Important flows should be testable from the UI milestone:

-   Home → Group → Group details
-   Home → Upcoming → Recurring payments
-   Vox → Manual transaction
-   Vox → Scan receipt → Receipt result
-   Receipt → Split bill
-   Activity → Transaction details
-   Group → Add expense → Split
-   Recurring → Create recurring payment
-   Home → Needs attention → Categorize transaction

Tests should target semantic elements rather than screen coordinates.

------------------------------------------------------------------------

## 27. Best-Practice Requirements

The implementation must follow these rules:

1.  No user-facing strings hardcoded inside Kotlin classes or
    Composables.
2.  No arbitrary colors scattered across feature code.
3.  Avoid arbitrary spacing/radius values where design-system tokens
    exist.
4.  No fake/sample data directly inside production screens.
5.  No business logic inside Composables.
6.  ViewModels expose immutable UI state.
7.  UI communicates actions/events back to ViewModels.
8.  Use `StateFlow` and lifecycle-aware collection.
9.  Use feature-based packages.
10. Keep domain models separate from UI-specific models where
    appropriate.
11. Centralize money formatting.
12. Centralize date formatting.
13. Do not assume INR throughout the architecture.
14. Use `LazyColumn`/lazy containers for potentially large lists.
15. Create reusable Compose components rather than giant screen files.
16. Add Compose previews for reusable components.
17. Design all major screens for loading, empty, success, and error
    states.
18. Build accessibility semantics into components.
19. Support Material 3 light and dark themes.
20. Keep navigation logic outside individual UI components.
21. Pass IDs through navigation rather than large domain objects where
    appropriate.
22. Use stable keys for lazy lists.
23. Hoist reusable component state where appropriate.
24. Avoid unnecessary mutable state inside Composables.
25. Keep the presentation layer independent enough that fake
    repositories can later be replaced by Room/API-backed repositories.

------------------------------------------------------------------------

## 28. UI Milestone Acceptance Criteria

The UI milestone is complete when a user can navigate this complete
fake-data journey:

``` text
Launch
  ↓
Onboarding
  ↓
Login
  ↓
Home
  │
  ├── Spending analytics
  │
  ├── Groups
  │     ↓
  │   Group details
  │     ↓
  │   Add expense
  │     ↓
  │   Split bill
  │
  ├── Upcoming payments
  │     ↓
  │   Recurring payments
  │     ↓
  │   Create reminder
  │
  └── Vox
        │
        ├── Voice UI
        ├── Manual transaction
        └── Scan receipt
                ↓
              Receipt
                ↓
             Split bill

Activity
   ↓
Transaction
   ↓
Edit transaction
```

Everything may use fake data during this milestone, but the UI should
behave like the real application.

The objective is **not** to build a disposable visual prototype. The
milestone should produce the real Android presentation layer using
proper navigation, ViewModels, StateFlow, reusable Compose components,
Android resources, formatting abstractions, accessibility, fake
repositories, and a centralized design system.

Future implementations of Room, authentication, OCR, voice interaction,
SMS detection, backend synchronization, and AI should plug underneath
this presentation layer without requiring the screens to be rebuilt.
