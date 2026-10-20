# Contributing to Kosh

Kosh is a native Android app in early development. The implementation currently lives on `feature/phase-1-foundation` in PR #1; check that branch and open pull requests before starting overlapping work.

## Development workflow

1. Read the README and [roadmap](docs/ROADMAP.md).
2. Branch from the current implementation branch until it is merged into main.
3. Make a focused change with a clear user-visible outcome.
4. For behavior changes, add a regression test that fails before the fix.
5. Run the relevant unit tests, Android lint, and debug build.
6. Open a pull request describing the problem, the change, validation, and remaining limitations.

Do not commit device databases, real bank exports, raw personal SMS, credentials, signing keys, APKs, or local SDK settings.

## Validation

```bash
bash gradlew testDebugUnitTest
bash gradlew lintDebug
bash gradlew assembleDebug
```

Record what actually ran and the outcome. If a command is blocked by the environment, state that explicitly. Device behavior must be verified separately; JVM tests alone do not prove SMS permissions, broadcast delivery, document picking, or database migration behavior.

## Finance and data integrity

- Use `BigDecimal` for monetary values and numeric comparison when decimal scale should not affect equality.
- Preserve the distinction between account cash movement and spending.
- Keep source provenance so users can inspect an interpretation.
- Make multi-step database replacements atomic and scope them to a bank and account identity.
- Check idempotency, duplicate handling, cancellation, failure recovery, and user corrections.
- Avoid destructive schema changes without explicit migration planning.

## Parser fixtures

Use synthetic examples. Preserve the format needed to reproduce the bug while replacing names, amounts where appropriate, phone numbers, account numbers, UPI IDs, references, balances, and dates. Do not upload a real statement just because account digits are masked.

Cover valid input, unrelated messages, unsupported formats, malformed input, and repeated import where relevant.

## Reporting bugs

Include the affected screen or parser, steps to reproduce, expected behavior, actual behavior, Android version, and app commit/version. Attach only redacted or synthetic evidence. For security concerns, follow [SECURITY.md](SECURITY.md).
