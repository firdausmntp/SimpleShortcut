# Contributing to SimpleShortcut

First off — thanks for taking the time to contribute! 🎉

All types of contributions are welcome: bug reports, feature requests, code, documentation, and more.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Request a Feature](#how-to-request-a-feature)
- [How to Contribute Code](#how-to-contribute-code)
- [Code Style](#code-style)
- [Commit Message Convention](#commit-message-convention)

---

## Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold it.

---

## How to Report a Bug

1. **Search existing issues** first — the bug may already be reported.
2. If not found, [open a new issue](../../issues/new?template=bug_report.md) and fill in the template.
3. Include:
   - Android version & device model
   - Steps to reproduce
   - Expected vs actual behaviour
   - Logcat output if available

---

## How to Request a Feature

1. [Open a new issue](../../issues/new?template=feature_request.md) using the feature request template.
2. Describe the use case clearly — _why_ is this feature needed?
3. If this is a deeplink / app integration, include example deeplink strings.

---

## How to Contribute Code

### Setup

```bash
# 1. Fork this repository on GitHub
# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/SimpleShortcut.git
cd SimpleShortcut

# 3. Create a feature branch
git checkout -b feature/your-feature-name

# 4. Build to verify everything works
./gradlew assembleDebug
```

### Workflow

```bash
# Make your changes...

# Build & verify
./gradlew assembleDebug

# Commit (follow convention below)
git commit -m "feat: add edit shortcut functionality"

# Push and open a PR
git push origin feature/your-feature-name
```

### Pull Request Guidelines

- **One PR per feature or fix** — keep PRs focused.
- Include a clear description of _what_ you changed and _why_.
- Reference any related issues (e.g., `Closes #12`).
- Make sure the project builds successfully before submitting.

---

## Code Style

- Follow standard **Kotlin coding conventions**.
- Use `camelCase` for variables/functions, `PascalCase` for classes.
- Keep functions short and focused — prefer multiple small functions over one large one.
- Add comments only when the _why_ isn't obvious from the code.
- Do not commit unused imports or commented-out code.

---

## Commit Message Convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>

[optional body]
[optional footer: Closes #issue]
```

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `docs` | Documentation only |
| `style` | Formatting, whitespace (no logic change) |
| `perf` | Performance improvement |
| `chore` | Build system, dependencies, tooling |

**Examples:**
```
feat: add edit shortcut dialog
fix: widget "+" button not opening add dialog on some launchers
docs: add deeplink extraction guide in README
perf: replace ConstraintLayout with LinearLayout in item layout
```

---

Thank you for helping make SimpleShortcut better! ❤️