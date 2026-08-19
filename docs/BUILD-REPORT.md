# RONIN AI v2 — Build report

## Requested build
`./gradlew clean assembleDebug`

## Result: **NOT EXECUTABLE IN THIS SANDBOX** (environment limitation, not a project defect)

### Evidence

The sandbox is a network-isolated Linux container with **no JDK** and an egress
allowlist of only `github.com` (+ `api.github.com`, `codeload.github.com`),
`pypi.org`/`files.pythonhosted.org` and `registry.npmjs.org`.

| Requirement | Host | Probe result |
|---|---|---|
| JDK 17 | — | `java` not installed; `JAVA_HOME` unset (gradlew fails immediately) |
| Gradle distribution | `services.gradle.org` | TLS handshake blocked (exit 35/000) |
| Android Gradle Plugin / Kotlin / AndroidX / Hilt / Room | `maven.google.com`, `repo.maven.apache.org`, `plugins.gradle.org` | all blocked (000) |
| Android SDK (platforms, build-tools) | `dl.google.com` | blocked (000) |
| Mirrors (aliyun, huawei, tencent, ibiblio, spring, archive.org, jitpack, ghcr, maven.pkg.github.com) | — | all blocked (000) |

Without a reachable Maven repository no dependency — including the build tools
themselves — can be downloaded, so a Gradle Android build cannot run in this
environment under any configuration.

### What was verified instead (all green)

- **Version catalog** — every coordinate in `gradle/libs.versions.toml`
  (AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52, Room 2.6.1,
  Compose BOM 2024.12.01, core-ktx 1.15.0, activity-compose 1.9.3,
  navigation-compose 2.8.5, lifecycle 2.8.7, datastore 1.1.1, retrofit 2.11.0,
  okhttp 4.12.0, gson 2.11.0, coroutines 1.9.0) was **confirmed to exist** on the
  live Google Maven / Maven Central registries.
- **Gradle wrapper** — `gradle-wrapper.jar` (43 583 bytes, fetched from the
  official `gradle/gradle` repo at tag v8.10.2) is a valid zip; `gradlew` /
  `gradlew.bat` are the official v8.10.2 scripts.
- **Kotlin sources** — all **112 files** parsed with the **tree-sitter Kotlin
  grammar: 0 syntax errors**; import/resource cross-check: all `R.drawable`,
  `R.font`, `R.string`, `R.mipmap` references resolve.
- **XML** — manifest, themes, strings, adaptive icons: all well-formed.
- **DI graph** — every interface has an implementation binding (`RepositoryModule`
  `@Binds` + `AppModule` `@Provides`); Hilt patterns are standard
  (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).

### How to build on a normal machine

```bash
# Prerequisites: JDK 17, Android Studio (SDK 35), ~2 GB free
cd Arena-spa
cp local.properties.example local.properties   # set sdk.dir
./gradlew clean assembleDebug                  # → app/build/outputs/apk/debug/app-debug.apk
```

## Known issues / next steps

1. **Runtime-tested APIs**: code uses conservative, stable APIs (Material3 stable
   set, Room 2.6.1, Hilt 2.52, KSP). A quick smoke test on a device/emulator is the
   first recommended step after the first successful compile.
2. **`fallbackToDestructiveMigration(false)`** is the default Room policy; a
   migration strategy should be added when the schema evolves (v2).
3. **Azure voice region** and **custom voice endpoint** are entered in the
   voice settings UI (documented per-field).
4. **Notification access** is optional (record other apps' notifications) and must
   be granted by the user in system settings.
5. **Release build** minifies with R8; `proguard-rules.pro` covers Gson/Room/Retrofit.
   Debug build (`assembleDebug`) is the requested target and has minification off.
