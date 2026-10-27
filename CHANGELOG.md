# Changelog

## [0.9.0](https://github.com/beppe2880/opencode-mobile/compare/v0.8.0...v0.9.0) (2026-10-27)


### Features

* **chat:** Add MCP status and connection controls ([#91](https://github.com/beppe2880/opencode-mobile/issues/91)) ([fb5de87](https://github.com/beppe2880/opencode-mobile/commit/fb5de8762874497ee1a5c1d9a1b94bf1e55bae09))
* **chat:** Follow the agent's work live ([#72](https://github.com/beppe2880/opencode-mobile/issues/72)) ([9d25372](https://github.com/beppe2880/opencode-mobile/commit/9d2537289366fe62fe2f768983d95386c07350c4))
* **settings:** Add output, refresh, text size and reset options ([#75](https://github.com/beppe2880/opencode-mobile/issues/75)) ([50fae75](https://github.com/beppe2880/opencode-mobile/commit/50fae75ed2edb535efada1097e49149c5657e936))
* **terminal:** Show the keyboard when opening the terminal ([#79](https://github.com/beppe2880/opencode-mobile/issues/79)) ([5b334d8](https://github.com/beppe2880/opencode-mobile/commit/5b334d8f0fbb31b33c1b16c0dae9933070354920))


### Bug fixes

* **chat:** Keep reading position stable while streaming ([#93](https://github.com/beppe2880/opencode-mobile/issues/93)) ([4bf3fad](https://github.com/beppe2880/opencode-mobile/commit/4bf3fada4783a62004ea401fa27678fafe0e0977))
* **connect:** Drop pasted line breaks from passwords ([#85](https://github.com/beppe2880/opencode-mobile/issues/85)) ([246c26d](https://github.com/beppe2880/opencode-mobile/commit/246c26de04fbdc11507386592d2d4651a6ff63b3))
* **connect:** Keep whitespace in password fields ([#53](https://github.com/beppe2880/opencode-mobile/issues/53)) ([04dd59d](https://github.com/beppe2880/opencode-mobile/commit/04dd59dbea6ec3dacde01ccd986b2a37fca47cb7))
* **sessions:** Forget sessions deleted on another device ([#87](https://github.com/beppe2880/opencode-mobile/issues/87)) ([f0eba70](https://github.com/beppe2880/opencode-mobile/commit/f0eba70b1a2722df1638b0e0ff396f6fc4b0d1cb))
* **sessions:** Remove deleted sessions from cached lists ([#81](https://github.com/beppe2880/opencode-mobile/issues/81)) ([5abb58d](https://github.com/beppe2880/opencode-mobile/commit/5abb58de13aa82ea8fe60dd4d87c2100d0f4ddd2))
* **sessions:** Settle the row back when a swipe delete is cancelled ([#83](https://github.com/beppe2880/opencode-mobile/issues/83)) ([49a415e](https://github.com/beppe2880/opencode-mobile/commit/49a415e4bcdd6e07fb474f14b940d32cca0ffadb))
* **terminal:** Restore touch scrolling in the terminal ([#80](https://github.com/beppe2880/opencode-mobile/issues/80)) ([84d9663](https://github.com/beppe2880/opencode-mobile/commit/84d96635676e75f071117a686f8684d479cc7c78))

## [0.8.0](https://github.com/beppe2880/opencode-mobile/compare/v0.7.0...v0.8.0) (2026-10-22)


### Features

* Add a home screen widget with running and recent sessions ([#67](https://github.com/beppe2880/opencode-mobile/issues/67)) ([40210e9](https://github.com/beppe2880/opencode-mobile/commit/40210e984ceaf77c078c6d86ff292572d936aca5))
* Add a read-only file viewer ([#69](https://github.com/beppe2880/opencode-mobile/issues/69)) ([39a54f6](https://github.com/beppe2880/opencode-mobile/commit/39a54f63ecb8f58306a750db6b75d89ce1428eee))
* Add a terminal through the PTY WebSocket ([#66](https://github.com/beppe2880/opencode-mobile/issues/66)) ([a48fc61](https://github.com/beppe2880/opencode-mobile/commit/a48fc611bd1870ac5fbb9fef37f0442ad995031a))


### Bug fixes

* **chat:** Show the compaction summary in the transcript ([#64](https://github.com/beppe2880/opencode-mobile/issues/64)) ([74b4003](https://github.com/beppe2880/opencode-mobile/commit/74b400362eb5bd564f64d10004d2e784c57621d4))

## [0.7.0](https://github.com/beppe2880/opencode-mobile/compare/v0.6.0...v0.7.0) (2026-10-21)


### Features

* **chat:** Dictate prompts with the microphone ([#39](https://github.com/beppe2880/opencode-mobile/issues/39)) ([aeb2ead](https://github.com/beppe2880/opencode-mobile/commit/aeb2ead6a7c8cfea6f07477caa3fd4742d293144))


### Bug fixes

* **chat:** Keep prompt image positions when a payload is missing ([#57](https://github.com/beppe2880/opencode-mobile/issues/57)) ([4684d8d](https://github.com/beppe2880/opencode-mobile/commit/4684d8dbbee8b2e8e9717693404fb3a52f7b7797))
* **chat:** Keep reverted messages gone when the revert is committed ([#44](https://github.com/beppe2880/opencode-mobile/issues/44)) ([4679e22](https://github.com/beppe2880/opencode-mobile/commit/4679e222e5bec73774f78057e8aaaede2ac1b338))
* **chat:** Keep tool row keys unique when call ids repeat ([#62](https://github.com/beppe2880/opencode-mobile/issues/62)) ([4312222](https://github.com/beppe2880/opencode-mobile/commit/43122220baf90e2b2aca749d18bd7440a92d581a))
* **chat:** Re-enable stop after a reconnect loses the execution end ([#51](https://github.com/beppe2880/opencode-mobile/issues/51)) ([9efd2d0](https://github.com/beppe2880/opencode-mobile/commit/9efd2d0c963ce47ec6918aae676fcffa7f80e023))
* **connect:** Re-check the camera permission when the scanner resumes ([#45](https://github.com/beppe2880/opencode-mobile/issues/45)) ([f8c1487](https://github.com/beppe2880/opencode-mobile/commit/f8c14870d9a72014f8de73c263cc4d409c7d351a))
* **data:** Keep the cached session in sync after rename and delete ([#55](https://github.com/beppe2880/opencode-mobile/issues/55)) ([4adf274](https://github.com/beppe2880/opencode-mobile/commit/4adf274e441871826e5fbf98b1672a92e209cb67))
* **data:** Make the sample data mode start and cover every screen ([#36](https://github.com/beppe2880/opencode-mobile/issues/36)) ([f18baa6](https://github.com/beppe2880/opencode-mobile/commit/f18baa6b7889406118da44a699a127f845bbd926))
* **data:** Start empty when the server store cannot be read ([#47](https://github.com/beppe2880/opencode-mobile/issues/47)) ([20c517a](https://github.com/beppe2880/opencode-mobile/commit/20c517a6c8a19212abd77bef0ed831cfba4a1cb4))
* Drop finished refresh jobs from the monitor ([#49](https://github.com/beppe2880/opencode-mobile/issues/49)) ([fcd02f5](https://github.com/beppe2880/opencode-mobile/commit/fcd02f5970b7bacb103cfcb8d912adfc4fcef406))


### Documentation

* Add demo GIFs to the README ([#37](https://github.com/beppe2880/opencode-mobile/issues/37)) ([8ae46f7](https://github.com/beppe2880/opencode-mobile/commit/8ae46f77ae2d4db30b58fa9f117d1b7abde8215c))

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
