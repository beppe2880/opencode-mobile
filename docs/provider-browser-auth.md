# Mobile browser provider authentication

Android supports OpenAI's **Sign in with ChatGPT** (`openai/chatgpt-token-sharing`).
This is not the legacy Codex device flow. Other browser methods remain disabled.
The shared engine has an explicit provider adapter; server discovery alone does not
supply client registration, endpoints, scopes, redirect policy or credential metadata.
iOS browser authentication is disabled pending a native loopback listener and verifier.

## Ownership and security

- The phone opens the system browser and listens only on `127.0.0.1`, at an ephemeral
  port with `/auth/callback`. PKCE S256, random state and nonce are per attempt.
- Invalid paths, duplicate parameters, mismatched state, unexpected Host, POSTs and
  cross-origin requests are rejected. Reads are bounded; an attempt expires after five minutes.
- The phone exchanges the code using the exact redirect and the callback's issued client ID.
  Required token-sharing scope and ID-token RS256 signature, issuer, audience,
  subject, nonce and validity are checked against OpenAI's HTTPS JWKS.
- OAuth requests use an isolated, unauthenticated HTTP client with redirects disabled.
  Credentials are imported through the generated `/api/credential` client, with
  authenticated TLS and redirects disabled. Literal local loopback is also permitted
  for local development (for example `adb reverse`); LAN/VPN HTTP is not presumed protected.
- The import contains method ID, access/refresh token, expiry, issued client ID and scopes.
  It neither reads all server credentials nor modifies the server plugin's host-ID store.
  The phone's stable **local authorizing host ID** is separate; it is never imported.
  Before local authorization, an unopened server OAuth attempt is prepared and immediately
  cancelled: the existing plugin initializes/preserves its own host ID before import.
- Tokens and codes are temporary in-memory values, never saved to settings, drafts,
  logs or clipboard; tokens are never put in URLs. Closing the panel, changing server or cancelling its scope
  releases the listener. Process death requires starting again. Only the server refreshes tokens.
- A cancelled import may already have reached the server; no automatic delete or
  refresh/retry is attempted. Model caches are invalidated before and after connection.
- The browser callback page says to return to the app; only the app reports whether
  validation and import actually succeeded. OAuth/server error bodies are not displayed.

## Verification limits

Tests use generated RSA keys and fake provider responses for successful authorization
and credential import; actual sockets test callback handling and cancellation.
Live emulator checks do not sign into personal accounts. Completing consent, using a
real ChatGPT-plan token, and subsequent server refresh require user-assisted verification.

Failures include a safe diagnostic such as `OAUTH_TokenExchange_HTTP_400` or
`OAUTH_Claims`. Stages distinguish server preparation, callback, token exchange,
scope, response shape, JWKS download, signature, claims, target-server changes,
credential import and expiry. Only fixed identifiers and an available HTTP status
reach the UI; original exception messages, response bodies and causes are discarded.
An import error cannot prove the server did not save: check provider status before retrying.
An absent `expires_in` defaults to one hour, matching the server plugin. Explicit
malformed, non-positive or overflowing expiry values still fail validation; a valid
expiry is not artificially restricted to one day. All OIDC checks remain required.

References: [OpenAI registration](https://developers.openai.com/siwc/token-sharing-open-source/sign-in),
[remote hosts](https://developers.openai.com/siwc/token-sharing-open-source/self-hosted-vms),
[RFC 8252](https://www.rfc-editor.org/rfc/rfc8252#section-7.3).
