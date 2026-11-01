---
name: android-security
description: Use for opencode-mobile Android manifests, exported components, intents, cleartext network policy, camera permission, clipboard, credential storage, or sensitive logging.
---

# Android Security

Treat every scanned QR, pasted link, intent and server payload as untrusted.

## Components And Intents

- Only the launcher activity, the widget receiver and the share menu entry
  (`ShareActivity`, which can be disabled in Settings) are exported. New activities,
  services, receivers and providers default to `android:exported="false"`.
- Shared content is untrusted: `ShareIntents` accepts only plain text and images,
  bounds the text, refuses `file:` URIs and this app's own providers, and the images
  are copied at once into app-private no-backup storage. Never log shared content.
- Validate `onCreate` and `onNewIntent` paths for the `singleTop` activity.
- Do not add deep links that accept pairing codes from other apps without an
  explicit decision; codes grant full server access.

## Network

- OpenCode servers are self-hosted on a LAN or VPN and usually plain HTTP, so
  cleartext is allowed by `network_security_config.xml`. Show the scheme in the
  UI and prefer HTTPS URLs when the user provides them.
- Never disable certificate validation or add trust-all managers.

## Permissions

- Camera is requested only when the user opens the QR scanner, with rationale and
  a manual fallback when denied.
- `POST_NOTIFICATIONS` is requested only after explaining why: once, in the dialog shown
  after the first connection, or from the Settings switch. Declining turns notifications
  off. The `dataSync` foreground service runs only while sessions run, notifications are on
  and the permission is granted. Attention notifications are `VISIBILITY_PRIVATE` with a generic public version,
  and their Allow/Reject actions require an unlocked device.
- Do not add a permission without explicit need.

## Credentials And Sensitive Data

- Server passwords and pairing tokens live only in `ServerRepository`'s encrypted
  storage (`androidx.security:security-crypto` on Android, the Keychain on iOS via
  `KeychainServerStore`). Do not migrate it during
  unrelated work; a migration must define behavior for existing ciphertext,
  invalid keys and backup.
- `allowBackup` is false; keep secrets out of backups.
- Never log secrets, Authorization headers, pairing codes/links, prompts,
  transcripts, tool output or file contents. HTTP logging is debug-only with
  headers redacted and no bodies.
- Clipboard: read only on explicit user action ("Paste link"); never copy secrets.
  iOS asks the user before every programmatic paste.
- iOS mirrors the Android policy in `iosApp/iosApp/Info.plist`: ATS allows plain
  HTTP (self-hosted servers), and every permission has a usage description. Add a
  new iOS permission only with its description string (`kmp-ios`).
- Never put credentials in repository files, Maestro flows, screenshots,
  process-visible commands or final responses.

## Verification

Inspect the merged manifest after manifest changes and verify that malformed
links, wrong credentials and denied permissions fail closed without exposing
internal exception details.
