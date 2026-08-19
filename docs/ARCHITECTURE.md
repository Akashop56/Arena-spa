# RONIN AI — Architecture

## Layering (strict, one-directional)

```
feature/* (Compose UI + ViewModel)
        │  collects StateFlow / calls
        ▼
core/domain/usecase (orchestration, no Android imports)
        │  depends on interfaces only
        ▼
core/domain/repository (interfaces) ──── core/device (DeviceManager contract)
        │  implemented by
        ▼
core/data/repository (Room / DataStore / Keystore)
core/device (AndroidDeviceManager, VoiceService, NotificationCenter)
```

Rules enforced by design:
- UI never touches `Context`, `SharedPreferences`, Room, or the framework for device control.
- Domain models are plain Kotlin (`core/domain/model`) — no Android types.
- All Android framework code lives in `core/data` and `core/device`.

## The reasoning pipeline (`core/ai/brain/AiEngine`)

```
Input
 ├─ 1. Understanding      intent classifier (regex + keyword rules, offline)
 ├─ 2. Intent             OPEN_APP | BROWSER_SEARCH | BATTERY_STATUS | DEVICE_INFO
 │                        CREATE_NOTE | SEND_NOTIFICATION | RUN_ROUTINE | SAVE_MEMORY
 │                        RECALL_MEMORY | DEVICE_CONTROL | TIME_INFO | GENERAL
 ├─ 3. Planning           choose tool + assemble context (memory recall, prefs, lessons)
 ├─ 4. Tool selection     ToolRegistry → RoninTool
 ├─ 5. Execution          tool runs (or AI provider chat completion with tool result)
 ├─ 6. Evaluation         success/failure → experience log (learned solutions)
 └─ 7. Memory update      conversation saved, preferences extracted, short-term trimmed
```

Only the stage *labels* are published (`PipelineStage` StateFlow); the internal chain
of thought is never rendered or sent to the model.

## AI providers (`core/ai/providers`)

| Provider | Transport | Endpoint |
|---|---|---|
| Gemini | Retrofit + Gson | `POST /v1beta/models/{model}:generateContent?key=…` |
| OpenAI | Retrofit + Gson | `POST {base}/chat/completions` (Bearer) |
| Groq | same client | base URL `https://api.groq.com/openai/v1` |
| Custom | same client | user-provided base URL |

Configs (key, model, temperature, base URL, enabled) live in DataStore; the key field
is an encrypted blob (`SecureVault` = Android Keystore AES-GCM). Error mapping turns
HTTP codes into actionable hints (401 → check key, 404 → check model name, 429 → wait).

## Voice layer (`core/device`)

`VoiceService` is the single facade. Providers: SYSTEM (TextToSpeech +
SpeechRecognizer, offline), ELEVENLABS, GOOGLE_CLOUD, AZURE, CUSTOM
(OpenAI-compatible `/audio/speech`). Any cloud failure falls back to the system voice.
STT always uses the on-device recognizer with `en-US` / `hi-IN` language selection.

## Memory & persistence

- `conversations` — chat history (trimmed to 80 messages)
- `memories` — SHORT_TERM / LONG_TERM / PREFERENCE / CONVERSATION / LEARNED_SOLUTION
- `experiences` — errors, fixes, preferences (self-improvement log)
- `routines` + `routine_history` — automation
- `notification_events` — notification memory (own + other apps via listener service)
- `ronin_settings` (DataStore) — provider + voice configs, assistant name, flags

## Adding a new tool

1. Implement `RoninTool` in `core/ai/tools` (definition + `matches` + `execute`).
2. Add an `IntentType` if a new intent is needed; extend `IntentClassifier` rules.
3. Register the tool in `ToolRegistry` — it instantly appears in Skills, chat and
   routine actions (add a `RoutineActionType` for automation support).

## Adding a new AI provider

Implement `AiProvider` (or reuse `OpenAiCompatibleProvider` with a new
`AiProviderType` entry + defaults). Register nothing else — the factory,
settings screen and connection tester are generic.
