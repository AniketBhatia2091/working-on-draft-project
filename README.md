# DayKhata

DayKhata is an Android daily expense logging application built using **Kotlin**, **Jetpack Compose**, **Room (with KSP)**, **DataStore Preferences**, and **MVVM Clean Architecture**.

DayKhata solves a common personal finance dilemma: logging expenses with dual perspective — what was actually spent versus what is shown to family, while actively keeping personal spending in check through an automated cut-off threshold.

---

## Core Concept & Dual Accounting

Each expense in DayKhata is entered once with two amounts and two labels:
- **`realAmount`**: The actual amount spent (private).
- **`homeAmount`**: The amount shown to family (defaults to `realAmount` if left blank).
- **`homeLabel`**: Label visible to family (e.g., "Groceries").
- **`personalLabel`**: Private note for yourself (e.g., "Groceries + snacks", private).

### Financial Formulas
For a given period budget (e.g., ₹15,000):
- **`homeSpent`** = $\sum \text{homeAmount}$
- **`realSpent`** = $\sum \text{realAmount}$
- **`homeBalance`** = $\text{budget} - \text{homeSpent}$
- **`realBalance`** = $\text{budget} - \text{realSpent}$
- **`savings`** = $\text{homeSpent} - \text{realSpent}$ *(strictly independent of the budget)*
- **`cutOffReached`** = $\text{homeBalance} \le \text{cutOffLimit}$

> **Family Transparency Rule**: The cut-off warning uses the **HOME balance only**. The real balance never triggers it. All home-facing outputs strictly read `homeAmount` and `homeLabel`, never leaking private data.

---

## Phase Plan

- **Phase 1 (Current Phase)**: Core Foundation
  - Project architecture, package structure (`data/`, `domain/`, `ui/`, `util/`).
  - Room entity `Expense` + DAO (CRUD, period queries, date queries).
  - DataStore Preferences for settings (`budget`, `cutOffLimit`, `periodType`, `periodStartDate`).
  - Pure Kotlin `ExpenseCalculator` domain logic with comprehensive unit tests.
  - Jetpack Compose UI:
    - **Dashboard**: Home balance, cut-off progress bar, today's home total, cut-off warning banner, and Personal mode toggle.
    - **Add / Edit Expense**: Input validation, optional home amount defaulting to real amount, and cut-off confirmation alert before saving.
    - **Daily List**: Date navigation, expense list with home amounts by default, and real amounts/savings in Personal mode, with edit and delete actions.
    - **Settings Screen**: Period budget, cut-off limit, and period type (Weekly/Monthly) configuration.
- **Phase 2**: Weekly Charts & Analytics *(Upcoming)*
- **Phase 3**: Savings Deep Dive & Targets *(Upcoming)*
- **Phase 4**: Family WhatsApp Sharing (Home-facing summary generation) *(Upcoming)*
- **Phase 5**: App Lock & Biometrics for Personal Mode *(Upcoming)*

---

## Phase 1 Deliverables

1. **Clean MVVM & Package Layout**:
   - `com.daykhata.app.data.local`: Room Entity `Expense`, `ExpenseDao`, `AppDatabase`, `SettingsDataStore`.
   - `com.daykhata.app.data.repository`: `ExpenseRepository`, `SettingsRepository`.
   - `com.daykhata.app.domain`: `ExpenseCalculator`, `Settings`, `ExpenseSummary`, `PeriodType`.
   - `com.daykhata.app.ui`: Compose screens (`Dashboard`, `AddExpense`, `DailyList`, `Settings`), ViewModels, Navigation, Material 3 Theme.
   - `com.daykhata.app.util`: `DateUtils` for period and date calculations.
2. **Domain Logic & Unit Tests**:
   - `ExpenseCalculatorTest`:
     - Verification that blank/null home amount defaults to real amount.
     - Verification that cut-off triggers strictly on home balance and never on real balance.
     - Verification that savings is never affected by the budget value.
     - Verification that today's home total reads strictly `homeAmount`.
3. **Cut-off Protection**:
   - Dashboard alert banner when `homeBalance <= cutOffLimit`.
   - Modal confirmation before adding an expense when the cut-off limit has been reached.

---

## Tech Stack & Configuration

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34
- **Local Persistence**: Room 2.6.1 with KSP (`com.google.devtools.ksp`)
- **Key-Value Storage**: AndroidX DataStore Preferences 1.1.1
- **Navigation**: Jetpack Navigation Compose 2.8.3
- **Architecture**: MVVM + Clean Architecture with Coroutines & StateFlow

---

## Building & Testing

To run unit tests:
```bash
./gradlew testDebugUnitTest
```

To build debug APK:
```bash
./gradlew assembleDebug
```
Output APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
