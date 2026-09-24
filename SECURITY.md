# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 1.0.x   | Yes       |

## Report a vulnerability

Report a vulnerability through GitHub private vulnerability reporting. Open the
**Security** tab of this repository, then select **Report a vulnerability**. Do
not open a public issue for a security problem.

Expect a first reply in 7 days. Do not disclose the problem in public until a
fix is released.

## Security design

TaskChain is a local-only Android application. The design removes most common
risks:

- The application does not declare the `INTERNET` permission. It contains no
  HTTP client and no network code.
- The application has no accounts, no telemetry, and no analytics.
- All data stays in app-private internal storage. The application never writes
  to external or shared storage.
- The application stores no API keys, tokens, or credentials. It needs none.

Keep these properties. Do not add remote services, secrets, or analytics.

## Permissions

The application declares three permissions. Each one supports the reminder
feature:

| Permission              | Purpose                                     |
| ----------------------- | ------------------------------------------- |
| `POST_NOTIFICATIONS`    | Show a reminder notification.               |
| `RECEIVE_BOOT_COMPLETED`| Restore scheduled reminders after a restart.|
| `VIBRATE`               | Give haptic feedback with a reminder.       |

## Secrets

This repository contains no secrets. If work later adds one:

1. Put the value in a file that `.gitignore` excludes. Never commit the value.
2. Commit a `.env.example` file that holds a placeholder value only.
3. Store a value that CI needs as a GitHub Actions secret.
