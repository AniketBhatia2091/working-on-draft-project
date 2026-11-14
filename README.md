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
- [x] **Phase 4**: Dedicated Savings Screen, Goals & Trend Chart
- [ ] **Phase 5**: App Lock & Biometrics for Personal Mode *(Upcoming)*

---

## Phase 4 Deliverables & Features

### 1. Dedicated Personal-Only Savings Screen
- **Personal-Only Access**: The "Savings" tab is visible in the bottom navigation bar **only** when the app is in `PERSONAL` mode.
- **Home Invariant & Ejection**: Default mode on every app launch is strictly `HOME`. If the user toggles back to `HOME` mode while on the Savings screen, the app immediately ejects back to the Dashboard and clears the back stack entry so Back cannot return to it.
- **Lock Forward-Compatibility**: Annotated with `// TODO Phase 5: gate Savings behind PIN/biometric`.

### 2. Comprehensive Savings Metrics
- **Current Period Savings**: Net delta (`homeSpent - realSpent`) for the active budget period.
- **All-Time Savings**: Cumulative savings across all logged expenses.
- **Timeframe Breakdown**: This week's savings and this month's savings.
- **Profitable Expense Stats**: Average savings per profitable entry (`home > real`) along with the count of saving entries.
- **Honest Negative Savings Tracking**: Highlights when any expense had `home < real` (actual spending exceeded family report) with count and total deficit, factored honestly into net savings.

### 3. Savings Target & Goal Tracking
- **Settings Integration**: Configure an optional savings target (₹) and target cycle (`WEEKLY`, `MONTHLY`, or `NONE`), validated $\ge 0$ and stored in DataStore.
- **Goal Progress Card**: Shows target amount, amount saved so far in the period, remaining deficit, percentage, and an animated progress bar.
- **Overachievement Support**: Progress bar clamps visually to 100%, and when exceeded, celebrates with `"Goal reached! +₹X extra saved"`.
- **Empty State**: Friendly prompt to set a goal with a direct shortcut button to Settings.

### 4. Savings Trend Canvas Chart
- **Pure Compose Canvas**: Built without heavy external chart libraries.
- **Range Switcher**: Toggle between `Last 7 days`, `Last 4 weeks`, and `Last 6 months`.
- **Zero Baseline & Negative Bar Support**: Positive savings rise upwards in green above the zero baseline; deficit/negative savings drop downwards in red below the baseline.
- **Cumulative Savings Overlay**: Plots a continuous cumulative savings trajectory line across the periods.

### 5. Architectural Privacy & Safety
- **Zero Leaks by Type**: Structural unit tests verify that home message builders (`HomeMessageBuilder`, `HomeWeeklyMessageBuilder`) never accept `SavingsSummary` or any savings models.
- **No Share Action**: No share actions or intents exist on the Savings screen.
- **Zero Logcat Logging**: Real amounts, savings figures, and private labels are never logged to Logcat.

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
