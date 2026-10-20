# Kosh engineering roadmap

This roadmap is based on the implementation at `1d4fab6` on `feature/phase-1-foundation`. It distinguishes existing code from work still required. Items below are priorities, not completed milestones.

## Existing implementation

- [x] Native Kotlin and Jetpack Compose app structure.
- [x] Room storage, Hilt injection, and Flow-based UI data.
- [x] Historical SMS scanning and live SMS receiver.
- [x] HDFC, Kotak, Slice, and generic UPI SMS parsers.
- [x] Keyword categories and remembered merchant-category mappings.
- [x] Overview, Trail, Insights, Review, and transaction detail screens.
- [x] Experimental Kotak CSV, HDFC XLS, and Slice XLSX statement parsers.
- [x] Unit test source for parsers, financial calculations, and selected mapping behavior.

These checkboxes mean code exists; they do not imply current build validation or production readiness.

## 1. Establish a trustworthy baseline

- [ ] Add CI for unit tests, lint, and the Android debug build.
- [ ] Validate the exact PR head before merging.
- [ ] Add database integration tests for statement replacement, rollback, and cancellation.
- [ ] Make statement replacement atomic.
- [ ] Scope account matching to bank plus account identity; handle suffix collisions explicitly.
- [ ] Prevent statement rows with missing or repeated references from overwriting one another.
- [ ] Harden malformed-file handling, XML parsing, decompression limits, and UI error recovery.

## 2. Make financial totals dependable

- [ ] Reconcile overlapping SMS and statement imports across repeated scans.
- [ ] Add an explicit owned-account model before treating heuristically paired entries as self-transfers.
- [ ] Distinguish spending, income, EMI, investments, refunds, and credit-card settlements.
- [ ] Improve merchant normalization without overwriting user corrections.
- [ ] Verify date ranges, time zones, rounding, and statement balances with synthetic fixtures.

## 3. Protect local data

- [ ] Replace destructive migration fallback with tested schema migrations.
- [ ] Define export, backup, restore, and deletion behavior.
- [ ] Evaluate database encryption and key management.
- [ ] Audit logs, dependencies, permissions, and source-text retention.

## 4. Polish and release

- [ ] Verify fresh installation, permission denial/revocation, and live capture on real devices.
- [ ] Check accessibility, large fonts, empty states, and light/dark themes.
- [ ] Add screenshots captured from the actual app.
- [ ] Select a project license.
- [ ] Define release signing, distribution, and versioning.
- [ ] Publish a release only after its build and validation evidence are available.
