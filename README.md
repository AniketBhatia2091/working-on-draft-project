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
- [x] **Phase 6**: Phase 5 Cleanup, Encrypted Backup/Restore & Signed Release Build

---

## Phase 6 Deliverables: Hardening, Encrypted Backup/Restore & Release Build

### 1. Hardening & Keystore Protection Carry-Over
- **120,000 PBKDF2 Iterations**: Raised PIN hashing iterations from 10,000 to 120,000 using `PBKDF2WithHmacSHA256`.
- **Iteration Storage & Silent Migration**: Stored iteration count with the hash; legacy 10,000-iteration hashes continue to verify and are silently re-hashed to 120,000 iterations upon the first successful PIN entry.
- **Android Keystore Encryption**:
  - *Phase 5 location*: In Phase 5, PIN salt and hash were stored in `SettingsDataStore.kt` lines 29-37 as plain hex/integer preferences in DataStore.
  - *Phase 6 upgrade*: In Phase 6, `pin_hash`, `pin_salt`, `pin_iterations`, `failed_attempts_enc`, and `lockout_until_epoch_enc` are encrypted with hardware-backed AES-256-GCM via `KeystoreCryptoManager` before writing to DataStore.
- **Small UI Fixes Made Across Phases 5 & 6**:
  - Removed obsolete `TODO Phase 5` in `SavingsScreen`.
  - Added visual Lock/Unlock badges and mode status indicators on Dashboard, Daily List, and Weekly Log.
  - Added live countdown timer in `PinAuthDialog` during lockout delays.
  - Added red destructive warning dialog for "Reset App Lock & Purge Data".
  - Structured Settings into 5 categorized sections with Material 3 cards and headers.
  - Applied `FLAG_SECURE` synchronously before Personal mode numbers render.
  - Added neutral fallback banner in Settings when Personal mode is locked for Backup & Restore.

---

### 2. Encrypted Backup & Restore (Settings > Privacy & Lock)
- **Security-First Architecture**:
  - Requires Personal-mode authentication before backup or restore options become accessible.
  - User defines a custom passphrase (minimum 8 characters, confirmed twice) with explicit recovery warning.
  - Key derived via PBKDF2-HMAC-SHA256 (210,000 iterations, 16-byte random salt).
  - Ciphertext encrypted using AES-256-GCM (12-byte random nonce per export, 128-bit authentication tag).
  - Sensitive arrays (`CharArray`, `ByteArray`) zeroed out after use.
- **Versioned Binary Format (`.dkbk`)**:
  - Magic Header: `DKBK` (4 bytes: `0x44, 0x4B, 0x42, 0x4B`)
  - Version: `1` (4-byte Int)
  - KDF Iterations: `210000` (4-byte Int)
  - Salt: 16 bytes
  - Nonce (IV): 12 bytes
  - Payload: Ciphertext + 16-byte GCM authentication tag
- **Payload Schema (Plaintext JSON)**:
  - Contains: `expenses` (date, realAmount, homeAmount, homeLabel, personalLabel), `settings` (budget, cutOffLimit, periodType, periodStartDate), `familyContacts` (name, phone), `savingsGoal` (savingsGoal, goalPeriod).
  - **Zero Security Leaks**: Explicitly verified that PIN hash, salt, iterations, lockout state, and biometrics are never serialized.
- **Storage Access Framework (SAF)**:
  - Export: `ActivityResultContracts.CreateDocument` generates default filename `daykhata-backup-YYYYMMDD.dkbk`.
  - Restore: `ActivityResultContracts.OpenDocument` imports file safely without storage permissions.
  - **No share actions or WhatsApp paths** exist for backup files; saving and opening occurs strictly via local SAF.
- **Atomic Restore Modes**:
  - **Merge**: Adds non-duplicate expenses (matched by date, realAmount, homeAmount, homeLabel, personalLabel) and merges contacts; preserves current budget settings.
  - **Replace**: Wipes current database and replaces all expenses, settings, contacts, and savings goals.
  - Executed inside a single Room database transaction (`database.withTransaction`).

---

### 3. Release Build & Signing Configuration
- **Signing Keystore**:
  - Configured in `app/build.gradle.kts` to read from root `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
  - Added `keystore.properties`, `*.jks`, and `*.keystore` to `.gitignore`. None are tracked in git.
  - Provided `keystore.properties.example` with placeholder credentials.
  - If `keystore.properties` is absent, build automatically falls back to debug signing with a console note so `./gradlew assembleRelease` and `./gradlew bundleRelease` always succeed cleanly.
- **How to Generate Your Real Release Keystore**:
  ```bash
  keytool -genkey -v -keystore daykhata-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias daykhata
  ```
  Create `keystore.properties` at project root:
  ```properties
  storeFile=daykhata-release.jks
  storePassword=your_keystore_password
  keyAlias=daykhata
  keyPassword=your_key_password
  ```
- **R8 Minification & Code Optimization**:
  - `isMinifyEnabled = true`, `isShrinkResources = true` enabled for release.
  - Proguard rules configured for Room, DataStore, Compose, Biometric, and DTOs.
  - Strips all `android.util.Log` calls in release builds via `-assumenosideeffects`.
  - Neutral adaptive launcher icon: `#1E88E5` primary blue background with clean white ledger book glyph (`@mipmap/ic_launcher`).
- **Release Artifacts & Metrics**:
  - Version: `versionName = "1.0.0"`, `versionCode = 1`
  - Release APK: `app/build/outputs/apk/release/app-release.apk` (**2.2 MB**, reduced from 18 MB debug APK)
  - Release AAB Bundle: `app/build/outputs/bundle/release/app-release.aab` (**4.5 MB**)

---

## Release Smoke Checklist

1. **Clean Launch**:
   - Install release APK on an Android device or emulator.
   - Verify app launches cleanly into **Home Mode** (never into Personal Mode).
2. **Personal Mode PIN & Biometric Setup**:
   - Toggle to Personal Mode $\to$ Complete PIN setup (4-6 digits) $\to$ Optionally enable Biometric.
   - Verify Personal Mode unlocks and "Savings" tab appears.
3. **Screen Privacy & Multitasking (`FLAG_SECURE`)**:
   - In Personal Mode, attempt screenshot $\to$ verify system blocks capture.
   - Open recent apps switcher $\to$ verify DayKhata displays a blank/black secure thumbnail.
4. **Auto-Relock Verification**:
   - In Personal Mode, press Home button (background app) $\to$ reopen app $\to$ verify app returns to Home Mode.
   - Leave app idle for configured timeout (30s) $\to$ verify app auto-relocks to Home Mode.
5. **Family WhatsApp Share**:
   - In Home Mode, click Share $\to$ verify message contains only homeLabel and homeAmount.
   - Toggle to Personal Mode $\to$ verify Share button is disabled.
6. **Encrypted Backup Round-Trip**:
   - In Personal Mode $\to$ Settings $\to$ Privacy & Lock $\to$ Export Encrypted Backup.
   - Enter passphrase $\to$ Save file (`daykhata-backup-YYYYMMDD.dkbk`).
   - Add/delete an expense.
   - Restore From Backup $\to$ Select file $\to$ Enter passphrase $\to$ Test Merge and Replace modes.
   - Verify restored expenses and settings match perfectly.
7. **R8 Runtime Verification**:
   - Verify zero crashes when accessing Room, DataStore, Biometrics, and Canvas charts in the release build.

---

## Tech Stack & Configuration

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34
- **Local Persistence**: Room 2.6.1 with KSP (`com.google.devtools.ksp`)
- **Key-Value Storage**: AndroidX DataStore Preferences 1.1.1
- **Security & Keystore**: Android Keystore AES-256-GCM, PBKDF2 (120,000 iterations for PIN, 210,000 for Backup)
- **Biometrics**: AndroidX Biometric 1.1.0
- **Process Lifecycle**: AndroidX Lifecycle Process 2.8.6
- **Navigation**: Jetpack Navigation Compose 2.8.3
- **Architecture**: MVVM + Clean Architecture with Coroutines & StateFlow

---

## Building & Testing

To run all 11 unit test suites:
```bash
./gradlew testDebugUnitTest
```

To build debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk` (18 MB)

To build release APK (with R8 code & resource shrinking):
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk` (2.2 MB)

To build release Android App Bundle (AAB):
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab` (4.5 MB)
