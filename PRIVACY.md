# Privacy Policy

OpenCode Mobile is an open-source client for an OpenCode server that you run yourself.
This policy covers the Android and iOS apps, including TestFlight builds.

## Data we collect

None. The app has no analytics, advertising, crash reporting or tracking, and the
developers run no server that receives your data.

## Data the app handles

- **Your OpenCode server.** The app connects only to the servers you add. Sessions,
  prompts, files and attachments travel directly between your device and your server.
- **Credentials.** Server addresses, passwords, pairing tokens and optional
  speech-to-text API keys are stored only on your device (Android encrypted storage,
  iOS Keychain).
- **Local cache and settings.** Conversations may be cached on the device to load
  faster; settings and drafts are stored on the device. Uninstalling the app deletes them.
- **Camera.** Used only to scan the pairing QR code; images are not stored or sent.
- **Microphone and speech recognition.** Used only when you dictate a prompt. Speech is
  converted to text by the speech recognition service of your operating system (Apple or
  Google) or, if you configure one, by the speech-to-text provider you choose with your
  own API key. These services process audio under their own privacy policies.
- **Photos and files.** Only the items you pick are read and sent to your server as
  attachments.
- **Local network.** Used to find and connect to OpenCode servers on your network.

## Third parties

The app sells or shares nothing for advertising or tracking. Besides your server, it
contacts only:

- GitHub (`api.github.com`), to check for new app releases and show release notes;
- services you choose to sign in to or configure, such as AI provider logins started
  from the app or a speech-to-text provider.

Your OpenCode server and the AI model providers it is configured to use process your
prompts under their own policies.

## Children

The app is not directed to children under 13.

## Changes and contact

Changes to this policy are published in this repository. For questions, open an issue at
https://github.com/beppe2880/opencode-mobile/issues or write to beppenucara28@gmail.com.

Last updated: November 16, 2026.
