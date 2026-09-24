# Secrets classification

This file records which configuration values are sensitive and which are safe
to commit. It holds classification only. Do not add filepaths, cloud resource
names, or account detail to this file, because this file is committed.

## Current state

This project holds **no secrets**. It is local-only and account-free. It
declares no `INTERNET` permission and calls no remote service. No API key,
token, or credential is needed to build or run it.

## Classification

| Value | Sensitive | Note |
| ----- | --------- | ---- |
| `local.properties` → `sdk.dir` | No, but do not commit | A machine-local path. `.gitignore` excludes it. Each developer generates their own. |
| `GRADLE_USER_HOME`, `JAVA_HOME` | No | Standard build environment variables. The helper scripts read them. |
| Release signing keystore | Yes | None exists yet. `.gitignore` excludes `*.jks`, `*.keystore`, and `keystore.properties`. |

## Rules

- Never commit a real value. Commit a `.env.example` placeholder instead.
- Store a value that CI needs as a GitHub Actions secret.
- Keep the local-only design. A new remote service adds a secret to manage.
- The untracked `.agents/secrets.md` file holds the real inventory, if one
  exists. That file is never committed.
