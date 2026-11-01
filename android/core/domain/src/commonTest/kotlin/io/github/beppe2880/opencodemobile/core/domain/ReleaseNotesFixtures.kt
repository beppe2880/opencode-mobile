package io.github.beppe2880.opencodemobile.core.domain

/** Release notes as published on GitHub (`gh release view <tag> --json body`). */
internal object ReleaseNotesFixtures {
    val V0_9_0 = """
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
    """.trimIndent()

    val V0_8_0 = """
## [0.8.0](https://github.com/beppe2880/opencode-mobile/compare/v0.7.0...v0.8.0) (2026-10-22)


### Features

* Add a home screen widget with running and recent sessions ([#67](https://github.com/beppe2880/opencode-mobile/issues/67)) ([40210e9](https://github.com/beppe2880/opencode-mobile/commit/40210e984ceaf77c078c6d86ff292572d936aca5))
* Add a read-only file viewer ([#69](https://github.com/beppe2880/opencode-mobile/issues/69)) ([39a54f6](https://github.com/beppe2880/opencode-mobile/commit/39a54f63ecb8f58306a750db6b75d89ce1428eee))
* Add a terminal through the PTY WebSocket ([#66](https://github.com/beppe2880/opencode-mobile/issues/66)) ([a48fc61](https://github.com/beppe2880/opencode-mobile/commit/a48fc611bd1870ac5fbb9fef37f0442ad995031a))


### Bug fixes

* **chat:** Show the compaction summary in the transcript ([#64](https://github.com/beppe2880/opencode-mobile/issues/64)) ([74b4003](https://github.com/beppe2880/opencode-mobile/commit/74b400362eb5bd564f64d10004d2e784c57621d4))
    """.trimIndent()

    val V0_3_0 = """
First public release of OpenCode Mobile, an unofficial native Android client for [OpenCode](https://opencode.ai) V2.

### Highlights

* Pair with `opencode pair` (QR code or link), a password, or find servers on your Wi-Fi.
* Live sessions: streaming text, reasoning and tool calls, permissions and questions.
* Prompts with steer/queue, `/` commands, `@` files, images, undo/redo, compact.
* **New:** fork a session from any prompt, or copy it whole with `/fork`.
* Per-server offline cache, 11 languages, phones, foldables and tablets.

Download `OpenCode-Mobile-v0.3.0.apk` below. Checksums are in `SHA256SUMS`.

    """.trimIndent()
}
