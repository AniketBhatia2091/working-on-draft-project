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
- [x] **Phase 5**: App Lock & Biometrics for Personal Mode, Privacy Hardening & Settings Polish

---

## Phase 5 Deliverables: App Lock, Privacy Hardening & Settings Polish

### 1. App Lock for Personal Mode
- **PIN & Biometric Gating**: Switching from Home mode to Personal mode (from any screen's toggle or bottom navigation) requires authentication. Home mode never requires authentication.
- **Biometric Integration**: Uses `androidx.biometric.BiometricPrompt` (fingerprint/face). Device lock screen credentials are never used as a silent bypass unless explicitly configured; the app PIN serves as the fallback.
- **PIN Security Architecture**:
  - Validates 4 to 6 digit numeric PINs.
  - Generates 32-byte cryptographically secure random salts via `SecureRandom`.
  - Hashes PIN using PBKDF2 (`PBKDF2WithHmacSHA256`) with 10,000 iterations.
  - Constant-time verification (`MessageDigest.isEqual`) prevents timing attacks.
  - PIN is never stored or logged in plain text.
- **Lockout Mechanism**:
  - After 5 consecutive failed attempts, locks out further attempts with escalating backoff:
    - 5 failed: 30 seconds
    - 6 failed: 60 seconds (1 minute)
    - 7 failed: 300 seconds (5 minutes)
    - 8+ failed: 900 seconds (15 minutes)
  - Lockout status and remaining time are persisted across app restarts in DataStore.

### 2. Auto Re-Lock & Inactivity Timer
- **Launch Invariant**: Personal mode is held strictly in memory and resets to `HOME` on every app launch.
- **Background Auto-Lock**: Employs `ProcessLifecycleOwner` to detect `ON_STOP` and instantly resets the view mode to `HOME` when the app is backgrounded.
- **Inactivity Timeout**: Configurable auto-relock when foregrounded without user interaction:
  - Immediately
  - 30 seconds *(Default)*
  - 1 minute
  - 5 minutes
- Real-time window touch tracking records user interaction and resets when idle.

### 3. Screen Privacy (`FLAG_SECURE`)
- **Screenshot & Recording Protection**: Window sets `FLAG_SECURE` whenever Personal mode is active or when "Always Block Screenshots" is enabled.
- **Recent Apps Blanking**: In recent-apps switcher, Android renders a blank window instead of exposing real spending amounts or private savings figures.
- **Synchronous Pre-Unlock**: `FLAG_SECURE` is applied to the window *synchronously before* Personal state flows emit and before Compose renders private figures.

### 4. Settings Polish (Grouped Sections)
Settings are organized into 5 clear sections:
1. **Budget Settings**: Period budget, cut-off threshold, and period type (Weekly / Monthly).
2. **Savings Target**: Savings goal amount (₹) and target cycle (Weekly / Monthly / None).
3. **Family Contacts**: WhatsApp recipient management (add, edit, delete contacts with phone validation).
4. **Privacy & Lock**:
   - Status badge ("PIN is active" vs "No PIN set").
   - Change PIN dialog (current PIN $\to$ new PIN $\to$ confirm).
   - Biometric toggle (fingerprint/face recognition).
   - Auto-lock inactivity timeout selector.
   - "Always Block Screenshots" toggle (keeps `FLAG_SECURE` active even in Home mode).
   - "Reset App Lock & Purge Data": Red destructive action with double confirmation warning. Resets app lock and wipes all expense records and settings to prevent unauthorized data extraction.
5. **About DayKhata**:
   - Application version (1.0).
   - Neutral, family-friendly explanation of the dual-accounting concept.

### 5. Privacy Hardening & Audit
- **Disabled ADB Backup**: Set `android:allowBackup="false"` in `AndroidManifest.xml` to prevent extraction of financial records via debug backups.
- **Zero Logcat Output**: Complete code audit verifies zero occurrences of `android.util.Log` or `println` statements with sensitive data across the entire production codebase.
- **Comprehensive Unit Testing**:
  - `PinSecurityManagerTest`: PBKDF2 hashing, salt uniqueness, format validation, constant-time verification, and lockout backoff steps.
  - `AutoLockManagerTest`: Inactivity tracking and timeout verification across all timeout configurations.
  - `PrivacyAuditTest`: Verifies home builders never leak private tags or numbers, verifies zero log statements in production source, and asserts `allowBackup="false"`.

---

## Manual Verification Checklist

1. **First-Time Personal Mode Setup**:
   - Open app $\to$ Tap "Personal Mode" on Dashboard or Daily List.
   - Verify PIN Setup dialog appears (4-6 digits, confirmation, optional biometric opt-in).
   - On completion, verify Personal mode activates and "Savings" tab appears in bottom bar.
2. **Biometric Authentication**:
   - Return to Home mode $\to$ Tap "Personal Mode".
   - Verify biometric prompt appears; scan fingerprint to unlock.
3. **PIN Fallback & Lockout**:
   - Tap "Use PIN" on biometric prompt.
   - Enter 5 consecutive incorrect PINs.
   - Verify lockout message: "Too many failed attempts. Try again in 30 seconds." Input is disabled.
   - Force close and reopen app $\to$ Attempt unlock $\to$ Verify lockout timer persists.
4. **Recent Apps Preview & Screenshot Block (`FLAG_SECURE`)**:
   - Unlock Personal mode $\to$ Take a screenshot $\to$ Verify Android blocks screenshot ("Cannot take screenshot due to security policy").
   - Open recent apps switcher $\to$ Verify DayKhata appears as a blank secure preview.
5. **Background & Inactivity Auto-Lock**:
   - In Personal mode, press Home button $\to$ Re-open app $\to$ Verify app returns to Home mode.
   - In Personal mode, leave app idle for configured timeout $\to$ Verify app auto-locks to Home mode.
6. **Destructive Reset App Lock**:
   - Go to Settings $\to$ Privacy & Lock $\to$ Tap "Reset App Lock".
   - Read warning dialog $\to$ Confirm reset $\to$ Verify all expenses, settings, and lock credentials are fully purged.

---

## Tech Stack & Configuration

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34
- **Local Persistence**: Room 2.6.1 with KSP (`com.google.devtools.ksp`)
- **Key-Value Storage**: AndroidX DataStore Preferences 1.1.1
- **Biometrics**: AndroidX Biometric 1.1.0
- **Process Lifecycle**: AndroidX Lifecycle Process 2.8.6
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
