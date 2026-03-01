# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x     | ✅ Yes    |

## Reporting a Vulnerability

If you discover a security vulnerability within SimpleShortcut, please **do not open a public issue**. Instead, report it privately by:

1. Opening a GitHub Security Advisory (under the **Security** tab of this repository → **Report a vulnerability**).
2. Or contacting the maintainer directly via GitHub DM.

Please include:
- A description of the vulnerability
- Steps to reproduce
- Potential impact

You can expect an initial response within **72 hours**. We will work with you to address the issue and credit you in the release notes if desired.

## Scope

This project is a client-side Android utility app. Key security considerations:

- **`QUERY_ALL_PACKAGES` permission** — used solely to resolve intent targets at launch time. No package data is transmitted or stored remotely.
- **No network access** — all data is stored locally in Room (SQLite).
- **Deeplinks are user-supplied** — the app does not validate deeplink content beyond attempting to parse it as a URI. Users are responsible for the deeplinks they add.