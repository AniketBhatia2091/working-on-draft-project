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
- [ ] **Phase 3**: Weekly Log, Charts & Analytics *(Upcoming)*
- [ ] **Phase 4**: Savings Screen & Targets *(Upcoming)*
- [ ] **Phase 5**: App Lock & Biometrics for Personal Mode *(Upcoming)*

---

## Phase 2 Deliverables & Features

### 1. Home | Personal Day View
- **Unified Segmented Mode Toggle**: Prominently displayed at the top of the Daily list view (`🏠 Home Mode` | `👤 Personal Mode`), backed by a shared `AppModeState` across ViewModels.
- **Strict Default**: Always defaults to **HOME** mode on every fresh app launch.
- **Home View**: Shows only `homeLabel`, `homeAmount`, day total (home), and remaining period home balance.
- **Personal View**: Reveals `realAmount`, `personalLabel`, day total (real), and the day's savings delta (`home total - real total`).

### 2. Family WhatsApp Share Flow
- **Share Preview Bottom Sheet**: Tapping the Share icon opens a read-only bottom sheet displaying the formatted daily message.
- **Type-Safe Domain Builder (`HomeMessageBuilder`)**:
  - Accepts strictly `HomeExpenseItem` objects (`homeLabel`, `homeAmount`) and `homeBalance`. It is structurally impossible to leak `realAmount` or `personalLabel`.
  - Formatted with Indian currency grouping (`₹14,770`, `₹1,05,000`).
  - Output Example:
    ```text
    Daily Expenses – 14 Nov
    Lunch – ₹150
    Auto – ₹80
    Total today: ₹230
    Balance left: ₹14,770
    ```
  - Handles empty day gracefully: `"No expenses logged today."`
- **Three Share Actions**:
  1. **WhatsApp**: `ACTION_SEND` targeting `com.whatsapp`, fallback to `com.whatsapp.w4b` (WhatsApp Business), with system chooser fallback.
  2. **Saved Person**: Direct WhatsApp link (`https://wa.me/<phone>?text=...`) targeting saved family numbers.
  3. **Copy**: Copies the formatted text to clipboard and displays an in-app confirmation snackbar.

### 3. Safety Controls
- **Share Disabled in Personal Mode**: The share action in the top bar is completely disabled while in Personal Mode with an in-app safety banner warning.
- **Zero Logcat Leaks**: Real amounts and personal labels are never sent to Logcat or any external intent.

### 4. Family Contacts Management
- **Settings Screen Integration**: Add, edit, and delete family contacts (`name`, `phone`).
- **Phone Validation**: Validates 10–15 digit phone numbers and automatically prepends country code `91` for 10-digit Indian mobile numbers.

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
