# Security and financial-data privacy

Kosh is experimental and has not undergone a comprehensive security audit. There is no stable release with a defined support window.

## Report a concern privately

Please do not include exploitable vulnerability details, real SMS, bank statements, database exports, account information, or credentials in a public issue.

If GitHub shows **Security → Report a vulnerability** for this repository, use that private reporting channel. Its availability depends on repository settings. Otherwise, arrange a private channel with the repository owner before sharing sensitive details.

A useful report contains the affected commit, a concise description, impact, and reproduction steps using synthetic data.

## Current boundaries

The application stores transaction data and original source text locally. Android application backup is disabled, but Kosh does not currently encrypt its Room database. Device compromise can expose this data.

Statement import and reconciliation are unfinished. Consult the README's known limitations before using them with valuable data. Do not assume this development app preserves data across reinstallations or schema upgrades.

## Contributor handling

Keep credentials and personal financial data out of source control, logs, test fixtures, screenshots, and build artifacts. Share only the minimum sanitized information needed to reproduce an issue.
