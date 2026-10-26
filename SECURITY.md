# Security and demonstration scope

Chimera is a portfolio project. Use fictitious accounts and non-sensitive content
for demonstrations. Its current scope is documented in the README and demo guide.

- Credentials, signing keys, local preferences, pending messages and private
  configuration must never be committed or attached to an issue.
- Client packages must not contain privileged Supabase or database credentials.
- Message encryption is performed by the backend; it is not end-to-end encryption.
- Local refresh credentials and pending-send files do not use a dedicated
  encrypted credential vault or encrypted local database.
- CI checks changes with Gitleaks. Automated detection cannot guarantee that all
  secrets or security defects have been found.

If you find a security issue, do not include exploitation details, credentials or
personal data in a public issue. Use GitHub's private vulnerability reporting if
available; otherwise request a private contact channel without disclosing details.

Before publishing a build, follow [the release checklist](docs/release-checklist.md)
and review the actual contents of the distribution archive.
