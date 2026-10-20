<div align="center">

# Kosh

**Your money, understood. On your device.**

A native Android personal finance tracker for Indian bank SMS and statements.

![Platform: Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Language: Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4)
![Status: Early development](https://img.shields.io/badge/status-early_development-D4AF37)

</div>

Kosh turns financial messages into a searchable transaction history, monthly spending summaries, and a queue for reviewing categories. Parsing, categorization, and storage happen on the device, without an account or cloud finance backend.

> **Development status:** The Android implementation is currently on [`feature/phase-1-foundation`](https://github.com/itzzzzzzak/Kosh/tree/feature/phase-1-foundation) in [PR #1](https://github.com/itzzzzzzak/Kosh/pull/1). It has not been merged into `main`. This README describes that implementation. It is an experimental app; classification and reconciliation remain incomplete.

## What Kosh does

- **Import financial SMS:** scan existing messages and receive new messages through Android's SMS broadcast.
- **Parse Indian bank formats:** dedicated HDFC, Kotak, and Slice parsers, plus a generic UPI fallback.
- **Explore monthly activity:** income and expense summaries, category breakdowns, daily spending trends, and per-account debit/credit activity.
- **Search your transaction trail:** browse transactions by date and filter the history.
- **Review categories:** classify unresolved transactions and remember category choices for matching merchants.
- **Understand the source:** inspect the original message and how a transaction was interpreted.
- **Import bank statements:** experimental parsers for Kotak CSV, HDFC XLS, and Slice XLSX, with an import confirmation step.

Parser coverage depends on the exact bank message or export format. This is not universal support for every SMS template or statement variant.

## A clearer view of your money

| Screen | Purpose |
| --- | --- |
| Overview | Monthly summaries, category spending, recent activity, and account activity |
| Trail | Searchable, filterable transaction history |
| Insights | Spending trends and comparisons |
| Review | Resolve uncategorized transactions |
| Transaction detail | Inspect source data and edit the category |

## Privacy and permissions

- Transactions, source SMS text, and learned merchant categories are stored in a local Room database.
- The app manifest requests **READ_SMS** for historical import and **RECEIVE_SMS** for live capture.
- The app manifest declares no **INTERNET** permission. There is no finance-data upload or analytics integration in the current app code.
- Android application backup is disabled.
- The UI uses Google's downloadable Manrope font through Google Play services, with a system-font fallback. Font retrieval is separate from financial-data processing.
- Local storage is not currently encrypted by Kosh. Device security still matters, and uninstalling the app normally removes its local data.

Keep real SMS, statements, account details, database exports, and signing credentials out of issues, screenshots, and commits.

## Build and run

### Requirements

- Android Studio with JDK **17**.
- Android SDK Platform **34** and the corresponding build tools.
- An Android **8.0 / API 26** or newer device or emulator.
- Network access for the initial Gradle and Maven dependency downloads.

### Get the implementation

Until PR #1 is merged, check out the implementation branch explicitly:

```bash
git clone https://github.com/itzzzzzzak/Kosh.git
cd Kosh
git switch feature/phase-1-foundation
```

Open the project in Android Studio, let Gradle sync, and run the `app` configuration. Android Studio can configure the SDK path in the untracked `local.properties` file.

For a command-line build, configure `ANDROID_HOME` or `sdk.dir` in `local.properties`, then run:

```bash
bash gradlew testDebugUnitTest
bash gradlew lintDebug
bash gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `bash gradlew`.

To install on a connected device with Android platform tools:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant SMS permissions when prompted to use message import and live capture. Statement imports use Android's document picker.

## Architecture

Kosh uses Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, coroutines/Flow, and Room. HDFC XLS import uses Apache POI; Slice XLSX import reads worksheet XML from the ZIP container.

The main data path is:

**SMS or statement → bank parser → transaction mapping → local repository → categorization and transfer reconciliation → Compose UI**

```text
app/src/main/kotlin/dev/kosh/financetracker/
  core/          Models, SMS parsers, statement parsers, classifiers, finance calculations
  data/          Room entities and DAOs, repository, SMS reader and receiver
  di/            Dependency injection
  feature/       Overview, Trail, Insights, Review, detail, sync, statement import
  navigation/    Destinations and shared navigation state
  ui/            Components, theme, and rupee formatting
```

Money is represented using `BigDecimal`. SMS source identifiers support idempotent rescans of the same message; this does not provide full cross-source deduplication.

## Known limitations

- **Statement import needs hardening:** replacement is not atomic and account matching uses suffixes without bank identity. Avoid importing valuable data until these issues are resolved.
- **Transfer detection is heuristic:** matching amount, direction, timing, and account suffix does not prove both accounts belong to the user.
- **Classification is incomplete:** debits generally become expenses; salary heuristics identify some income. EMI, investment, refund, and credit-card-payment semantics need further work.
- **Cross-source reconciliation is incomplete:** statement import followed by an SMS rescan can bring back overlapping transactions.
- **Statement identity needs improvement:** rows without reference numbers can share a source identifier.
- **Import robustness needs work:** malformed or oversized files need stricter handling and parser limits.
- **Database upgrades need migrations:** the current configuration allows destructive migration.
- **Validation is not automated yet:** the implementation branch contains unit tests, but there are no GitHub Actions checks configured. Prior PR notes report device testing; this is not a current CI result.

See the [roadmap](docs/ROADMAP.md) for the next engineering priorities. Treat app totals as experimental and verify them against bank records.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) for setup, test expectations, and privacy-safe fixtures. Focused fixes and reproducible reports are welcome.

For vulnerabilities or financial-data exposure, follow [SECURITY.md](SECURITY.md).

## License

A project license has not been selected or added. Public source availability does not itself grant an open-source license.
