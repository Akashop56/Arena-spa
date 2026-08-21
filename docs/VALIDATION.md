# RONIN — Phase 2 production validation

## 1. CI build: **SUCCESS**

| | |
|---|---|
| Run | [32475823724](https://github.com/Akashop56/Arena-spa/actions/runs/32475823724) |
| Commit | `9e896ed` |
| Result | **success** — passed on the first attempt, no CI fixes were required |
| Artifact | `Arena-debug-apk`, **18,615,093 bytes** |
| Steps | Setup JDK 17 · Setup Gradle · Clean · **assembleDebug** · Find APK · Upload — all green |

`assembleDebug` running green means KSP executed for real: the **Hilt** component
graph and all **Room** DAOs were generated and compiled. Those are the two things
the offline gates can only approximate.

Only warnings were GitHub's own Node 20 / `setup-java@v4` deprecations — no
Kotlin or Gradle warnings.

> Note: the workflow triggers on `main` only, and this sandbox's token lacks the
> `workflows` scope, so the trigger list cannot be edited from here and
> `workflow_dispatch` is rejected. Builds are therefore produced by merging to
> `main`.

## 2. Android 13 (API 33) install & runtime compatibility

`minSdk 26 · targetSdk 35 · compileSdk 35` — Android 13 is in range.

| Check | Status |
|---|---|
| `android:exported` on every component with an intent-filter (install blocker since API 31) | MainActivity `true`, NotificationListenerService `false` — both present |
| `NotificationListenerService` exported value | `false` + `BIND_...` permission — matches the official Android reference exactly |
| `POST_NOTIFICATIONS` (runtime permission from API 33) | declared, requested at runtime, and guarded by `SDK_INT < 33` before use |
| `PendingIntent` mutability flag (mandatory API 31+) | `FLAG_IMMUTABLE` set |
| `registerReceiver` export flag (API 33+) | only a **null-receiver sticky query** of `ACTION_BATTERY_CHANGED`, which is exempt (verified against Android docs) — now guarded anyway |
| Foreground services | none declared, so no API 34 service-type requirement |
| Cleartext traffic | `usesCleartextTraffic=false` + OkHttp interceptor |

**The APK itself could not be installed/instrumented here.** Artifact download
redirects to `productionresultssa*.blob.core.windows.net`, which the sandbox
egress policy blocks, and there is no emulator. Manifest-level install
compatibility is verified statically; on-device launch remains unexecuted.

## 3. Runtime crash audit → fixes applied

Crash surfaces reachable on a real device, found by auditing constructor-time
and permission-dependent code:

| Defect | Impact | Fix |
|---|---|---|
| `NotificationCenter` hard-cast `NOTIFICATION_SERVICE` and called `createNotificationChannel()` in its `init` block | It is a `@Singleton`; any throw happens during Hilt graph construction → **app fails to start** | lazy nullable + channel created on first post |
| `notify()` unguarded | `POST_NOTIFICATIONS` can be revoked between check and call → crash | wrapped |
| `AndroidDeviceManager` hard-cast `AUDIO_SERVICE` / `CAMERA_SERVICE` in constructor | crash at injection on devices without a camera / restricted profiles | lazy nullables + safe fallbacks |
| `getBatteryState()` `registerReceiver` unguarded | throws on some OEM builds | guarded |
| `getDeviceInfo()` `StatFs` / `ActivityManager` unguarded | throws on restricted profiles | degrades to zeros |
| `Settings.System.canWrite()` unguarded | throws on some ROMs | guarded |

## 4. Subsystem verification (executed, not inspected)

Logic was extracted and **run** against real engines — Gson 2.13.2 (from the
PySpark distribution), the JDK's JCE provider, SQLite3, and kotlinx-coroutines.
Sources in `tools/verify/`.

**Secure key storage** (`vault.kt`) — blob format `b64(iv):b64(ct)`; plaintext
absent from the blob; round-trip incl. Unicode; wrong key → `null`; tampered
ciphertext → `null`; garbage/empty/no-separator → `null`; unique IV per call.
**No case throws** — a corrupted blob degrades to "no key", never a crash.

**AI provider system** (`provider.kt`) — OpenAI/Groq buffered parse (normal,
`content: null`, missing `message`, error body, empty `choices`); **SSE stream
assembles to "Hello world" and silently skips a malformed chunk**; Gemini parse
(normal, multi-part, `SAFETY` block → readable message, no candidates); Gemini
SSE preserves Hindi text (`स्ट्रीमिंग`) exactly.

**Chat streaming** — verified end to end by the SSE cases above plus the
markdown parser handling **unterminated code fences**, which streaming
necessarily produces mid-reply.

**Memory engine** (`memory.py`) — real DAO SQL against SQLite: recall excludes
`CONVERSATION`; exact-match dedup returns 1/0 correctly; tag search; importance
ordering; **parameterisation proven injection-safe**; Unicode recall.

**Settings / provider config** (`settings.kt`) — persisted JSON provably
contains **no** plaintext key; model/baseUrl defaults; `http://` custom endpoint
**rejected**; missing model/URL rejected; trailing slash normalised.

**Retry policy** — transient recovers; `401` and DNS never retry; `429`/`503`
retry then exhaust; cancellation propagates.

**Dashboard** — 5-arity `combine` + `flatMapLatest` emits the expected
aggregate; `takeLast(3)` returns newest messages.

## 5. Static gates (all green)

```bash
python3 tools/xref.py       # 0 broken internal imports
python3 tools/vmcheck.py    # 0 bad screen -> ViewModel references
python3 tools/sqlcheck.py   # 38/38 Room queries execute on real SQLite
python3 tools/hiltcheck.py  # 48/48 injected constructors satisfiable
```

## Not verified (honest gaps)

1. **No on-device / emulator run.** No emulator here and the APK cannot be
   downloaded. Voice is the biggest gap: `SpeechRecognizer` and `TextToSpeech`
   behaviour is OEM-specific and cannot be exercised off-device. The voice fixes
   are lifecycle-correctness fixes verified by reading, not by listening.
2. **Dashboard/Chat/Memory UI** verified at the state-and-logic layer, not
   pixels — no screenshot testing.
3. **R8/minified release build** not exercised; CI builds `assembleDebug`.
4. Room is still schema **v1**; the next schema change needs a real `Migration`.
