# Changelog

## [0.6.0](https://github.com/beppe2880/opencode-mobile/compare/v0.5.0...v0.6.0) (2026-10-18)


### Features

* **settings:** Show usage statistics ([#35](https://github.com/beppe2880/opencode-mobile/issues/35)) ([a1eadd7](https://github.com/beppe2880/opencode-mobile/commit/a1eadd7df526e0ac35d3ef12726ae86c04c09a5d))


### Documentation

* Remove the demo video from the README ([#25](https://github.com/beppe2880/opencode-mobile/issues/25)) ([eceaea9](https://github.com/beppe2880/opencode-mobile/commit/eceaea9b1dfb2d1ad7118f0346b95508096f90f5))

## [0.5.0](https://github.com/beppe2880/opencode-mobile/compare/v0.4.0...v0.5.0) (2026-10-17)


### Features

* **settings:** Choose which kinds of notifications to get ([#22](https://github.com/beppe2880/opencode-mobile/issues/22)) ([fa2a825](https://github.com/beppe2880/opencode-mobile/commit/fa2a825f100a0f79e0e5c0432e22012734af4f88))


### Bug fixes

* **chat:** Undo prompts older than the loaded history and prompts with only images ([#24](https://github.com/beppe2880/opencode-mobile/issues/24)) ([823ae3a](https://github.com/beppe2880/opencode-mobile/commit/823ae3a45401e0b6893e6c4873fef4f1997d22f4))

## [0.4.0](https://github.com/beppe2880/opencode-mobile/compare/v0.3.0...v0.4.0) (2026-10-17)


### Features

* **chat:** Run ! shell commands from the composer ([#19](https://github.com/beppe2880/opencode-mobile/issues/19)) ([35e60e6](https://github.com/beppe2880/opencode-mobile/commit/35e60e628147308501f4a7cddaed3b469539cb93))
* **chat:** Show queued prompts and let them be sent now, edited or removed ([#20](https://github.com/beppe2880/opencode-mobile/issues/20)) ([38e0d01](https://github.com/beppe2880/opencode-mobile/commit/38e0d01ae7b88147ad9f5e1b77078fb6b7ada319))
* **chat:** Show the files a session changed with their diffs ([#18](https://github.com/beppe2880/opencode-mobile/issues/18)) ([4aaf04c](https://github.com/beppe2880/opencode-mobile/commit/4aaf04cd5bfc6d8e4c39132c122dea45d3984839))
* Notify pending permissions and finished sessions ([#17](https://github.com/beppe2880/opencode-mobile/issues/17)) ([e0336d2](https://github.com/beppe2880/opencode-mobile/commit/e0336d26c02bb1d63983abb3c110e0ad933c92d7))


### Documentation

* Add a demo video and GIF to the README ([#15](https://github.com/beppe2880/opencode-mobile/issues/15)) ([765ee28](https://github.com/beppe2880/opencode-mobile/commit/765ee2824a948092823e23074ac1b9deab5bddf7))

## 0.3.0 (2026-10-16)

First public release. Earlier versions were developed privately; their notes are kept below.

### Features

* **chat:** Fork a session from a message, like the OpenCode TUI.
* **chat:** Add the `/fork` command to copy the whole session.

## 0.2.0 (2026-10-15)

### Features

* **connect:** Find OpenCode servers on the local network.
* **connect:** Warn before sending credentials over plain HTTP to a public address.
* **data:** Cache session lists per server and recover dead connections.
* **ui:** Add nine interface languages and Arabic RTL support.

### Bug fixes

* **connect:** Show the pairing command inline and highlighted.
* **release:** Refuse debug-signed release builds and verify the certificate.

## 0.1.0 (2026-10-15)

* First release: pairing by QR code, link or password, live sessions and chat, agent and
  model switching, permissions and questions, local transcript cache, signed APKs.
