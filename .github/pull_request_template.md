### Issue

Closes #

### Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor or code improvement
- [ ] Documentation
- [ ] Build or CI

### What does this PR do?

<!-- What changed and why it works. If you are not sure why it works, say so. Long
AI-generated descriptions may be closed. -->

### How did you verify your code works?

<!-- Tests added or run, and the device or emulator you checked it on. -->

### Platforms tested

<!-- Most code is shared, so a change usually reaches both apps. Tick what you ran it on; if
you could only test one platform, say so and a maintainer will check the other. -->

- [ ] Android (device or emulator: )
- [ ] iOS (device or simulator: )
- [ ] Only tested on one platform: the other one still needs checking
- [ ] Does not affect the apps (CI, docs, tooling)

### Screenshots or recordings

<!-- Required for UI changes: light and dark theme. Hide server addresses and private sessions. -->

### Checklist

- [ ] `./gradlew spotlessCheck testDebugUnitTest :app:lintDebug :app:assembleDebug compileKotlinIosSimulatorArm64` passes (in `android/`; `spotlessApply` fixes formatting)
- [ ] New user-visible strings are translated in all supported languages (`bun test tools/locales.test.ts`)
- [ ] No unrelated changes, no edits to the generated `android/core/api`
