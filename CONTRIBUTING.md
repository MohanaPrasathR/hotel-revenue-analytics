# Contributing to Stay Pulse

Thanks for helping improve Stay Pulse! This guide keeps contributions quick to review.

## Getting started

1. Fork the repository and create a topic branch from `main`.
2. Backend: `cd stay-pulse-api && mvn verify` (JDK 17+).
3. Frontend: `cd frontend && npm ci && npm run build`.
4. Open a pull request using the provided template.

## Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` a new user-facing capability
- `fix:` a bug fix
- `test:` adding or improving tests
- `docs:` documentation only
- `chore:` / `ci:` / `refactor:` / `perf:` tooling and internal changes

## Code style

- Java: 4-space indentation, Lombok for boilerplate, constructor injection only.
- TypeScript: standalone Angular components, strict typing, no `any`.
- Every behaviour change needs a test.

## Reporting security issues

Please do not open a public issue for vulnerabilities. Contact the maintainer privately instead.
