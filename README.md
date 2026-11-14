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

- [x] **Phase 1**: Core Foundation (Room, DataStore, Calculator, Dashboard, Add Expense, Cut-off Logic)
- [x] **Phase 2**: Home/Personal Day View & WhatsApp Family Share Flow
- [x] **Phase 3**: Weekly Log, Canvas Bar Chart & Home Weekly Share
- [ ] **Phase 4**: Dedicated Savings Screen & Goals *(Upcoming)*
- [ ] **Phase 5**: App Lock & Biometrics for Personal Mode *(Upcoming)*

---

## Phase 3 Deliverables & Features

### 1. Weekly Log Screen & Navigation
- **Weekly Tab**: Added a dedicated "Weekly" destination to the bottom navigation bar.
- **Week Selector**:
  - Operates on a standard Monday-to-Sunday week cycle.
  - Date range display (e.g., `10 Nov – 16 Nov`).
  - Controls: "Previous week", "Next week" (disabled on the current week to prevent future navigation), and a quick "This week" reset button.
- **Shared App State**: Uses the same `AppModeState` as Phase 2 for the `Home | Personal` toggle, defaulting to **HOME** on every app launch.

### 2. Day-by-Day List
- Seven rows covering Monday through Sunday with dates and formatted daily totals.
- **Home Mode**: Displays day name, date, and that day's Home Total (or a dash if ₹0).
- **Personal Mode**: Displays day name, date, Real Total, and that day's savings delta (`home - real`).
- **Deep Linking to Daily View**: Tapping any day opens the Daily List screen pre-focused on that selected date.

### 3. Weekly Totals Card
- **Home Mode**:
  - Week Home Total
  - Average per day (`weekHomeTotal / 7`)
  - Highest spending day (`Mon (₹X)` or `–` on zero spend; picks earliest day on tie)
  - Remaining home balance for current budget period
- **Personal Mode**:
  - Week Real Total
  - Week Savings (`weekHomeTotal - weekRealTotal`)
  - Average per day (real)
  - Highest spending day (real)

### 4. Custom Compose Canvas Bar Chart
- **Lightweight Canvas**: Implemented using pure Jetpack Compose Canvas without external dependencies.
- **Type-Safe Data Isolation**:
  - `HomeWeekChartData`: Strictly carries only `homeAmount` totals. Real numbers cannot enter the chart in Home mode by type.
  - `PersonalWeekChartData`: Carries real and home series side-by-side, visually illustrating the private savings gap.
- Includes day-of-week axis labels (`Mon`..`Sun`), value labels, sensible dynamic scaling, and an empty state banner (`"No expenses this week"`).

### 5. Domain Calculator & Home Weekly Share
- **`WeeklySummaryCalculator`**:
  - Aggregates expenses by Monday–Sunday calendar slots.
  - Handles weeks crossing month boundaries, empty weeks, and single-day activity.
  - Tie-breaking: picks the earliest day in the week when amounts are equal.
- **`HomeWeeklyMessageBuilder`**:
  - Formats family-safe weekly text summary using Indian currency notation (`₹14,770`, `₹1,05,000`):
    ```text
    Weekly Expenses – 10 Nov to 16 Nov
    Mon – ₹230
    Tue – ₹180
    ...
    Week total: ₹1,450
    Balance left: ₹13,550
    ```
  - Reuses the Phase 2 Share Preview Bottom Sheet (WhatsApp, Saved Person, Copy).
  - **Safety**: Sharing is disabled while in Personal Mode with an explanatory warning banner.

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

To run all unit tests:
```bash
./gradlew testDebugUnitTest
```

To build debug APK:
```bash
./gradlew assembleDebug
```
Output APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
