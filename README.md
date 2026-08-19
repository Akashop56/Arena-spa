# RONIN AI v2

**A complete, production-ready personal AI assistant for Android.**

Built from zero with Kotlin + Jetpack Compose, Clean Architecture (UI → ViewModel → UseCase → Repository interface → Repository implementation → Data/Device layer), MVVM, Coroutines/Flow, Room, DataStore, Hilt, Retrofit/OkHttp and Android Keystore encryption.

```
┌────────────────────────────────────────────────────────────────────┐
│ UI (Compose)  →  ViewModel  →  UseCase  →  Repository Interface    │
│                                              ↓                    │
│                              Repository Implementation             │
│                                              ↓                    │
│                              Data layer (Room / DataStore /        │
│                              Keystore)  ·  Device layer (Android)  │
└────────────────────────────────────────────────────────────────────┘
```

---

## Features

### 🧠 AI Brain
- **4 providers**: Gemini · Groq · OpenAI · Custom OpenAI-compatible endpoint
- Full provider management: enable/disable, add/edit/delete API keys, model selection (suggestions + custom), base URL for custom endpoints, temperature, **one-tap connection testing**
- API keys are **never stored in plain text** — encrypted with AES-256-GCM, master key held by the **Android Keystore**
- **Reasoning pipeline** (chain-of-thought stays private — the UI only shows public stage labels):
  `Input → Understanding → Intent → Planning → Tool Selection → Execution → Evaluation → Memory Update`

### 🎙️ Voice system (independent voice layer)
- **Default: Android system TTS + STT — zero API keys, offline-capable**
- Optional cloud voices: **ElevenLabs · Google Cloud TTS · Azure Speech · Custom OpenAI-compatible endpoint**
- Settings: provider, API key, voice selection, language (**English + Hindi**), speed, pitch, **test voice**
- **Automatic offline fallback**: if a cloud voice fails, RONIN switches to the on-device voice

### 🧠 Memory
- Five types: **Short-term · Long-term · Preferences · Conversation · Learned solutions**
- Save / retrieve / search / delete; auto-extraction of preferences from conversation
- Full memory manager screen with filters and search

### 🛠️ Tool framework (skills)
- Modular `RoninTool` interface: **App launcher · Browser · Notifications & reminders · Battery · Device info · Device control (volume/torch/brightness) · Files & notes · Automation · Time · Memory**
- Natural-language triggers: *“open Spotify”*, *“check battery”*, *“remind me to drink water in 10 minutes”*, *“remember that my favourite colour is green”*

### ⚙️ Automation
- Multi-action routines (open app, web search, notification, volume, torch, note)
- Enable/disable, manual run, **trigger phrases** (also usable from chat), full **execution history**

### 📈 Self-improvement
- Experience system records **errors, solutions, successful fixes and user preferences**
- Global crash guard writes failures into the experience log instead of crashing silently
- Lessons are injected into the AI context on later turns; visible in the Skills screen

### 📱 Device layer
- All Android framework code lives behind `DeviceManager` — UI never touches the framework directly
- Battery, device info, storage/RAM, app launcher, volume, torch, brightness, settings shortcuts, notification listener

### 🖥️ Screens
Dashboard · AI Chat · Voice Assistant · Memory · Skills · Automation · Device Control · Settings · AI Providers · Voice Settings — premium futuristic dark UI (Orbitron display type, neon cyan/violet accents, glass cards, glow effects).

---

## Project structure

```
app/src/main/java/com/ronin/ai/
├── RoninApp.kt / MainActivity.kt
├── core/
│   ├── common/            # Outcome wrapper, constants, time/string helpers
│   ├── design/            # Theme (Color/Type/Theme), components, navigation
│   ├── domain/
│   │   ├── model/         # Pure Kotlin models
│   │   ├── repository/    # Repository interfaces (contracts)
│   │   └── usecase/       # SendMessage, Dashboard, Memory, Routine, Provider, Device
│   ├── ai/
│   │   ├── providers/     # Gemini + OpenAI-compatible (OpenAI/Groq/Custom) via Retrofit
│   │   ├── brain/         # AiEngine pipeline, intent classifier, context assembler,
│   │   │                  #   prompt builder, preference extractor
│   │   └── tools/         # 10 modular skills + ToolRegistry
│   ├── data/
│   │   ├── db/            # Room: 6 entities + DAOs
│   │   ├── datastore/     # SettingsDataStore (provider/voice configs)
│   │   ├── security/      # SecureVault (Android Keystore AES-GCM)
│   │   ├── repository/    # Repository implementations
│   │   └── di/            # Hilt modules
│   ├── device/            # DeviceManager, NotificationCenter, listener service,
│   │                      #   VoiceService + system/remote voice engines, CrashGuard
│   └── automation/        # AutomationEngine (routine executor)
└── feature/               # 10 screens + ViewModels (one package per screen)
```

---

## Building

Requirements: **JDK 17**, **Android SDK** (compileSdk 35, minSdk 26), Gradle 8.10.2 (wrapper included).

```bash
# 1. Create local.properties pointing at your SDK (Android Studio does this automatically)
cp local.properties.example local.properties
#    → set sdk.dir=/path/to/Android/Sdk

# 2. Build the debug APK
./gradlew clean assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Or simply open the project in **Android Studio (Ladybug+)** and run ▶.

> ⚠️ **Sandbox note:** this repository was authored in a network-isolated sandbox whose
> egress allowlist blocks `services.gradle.org`, `repo.maven.apache.org`,
> `maven.google.com` and `dl.google.com`, and which has no JDK — so
> `./gradlew clean assembleDebug` **cannot complete here**. Every dependency version
> in `gradle/libs.versions.toml` was verified against the live registries, and all
> 112 Kotlin files were syntax-validated with the tree-sitter Kotlin grammar.
> See [docs/BUILD-REPORT.md](docs/BUILD-REPORT.md) for the full report.

---

## First-run setup

1. **AI provider** — Settings → AI Providers → pick Gemini/Groq/OpenAI/custom →
   paste your key → **Test** → Save → *Set default*.
2. **Voice** — Settings → Voice: the System voice works immediately (offline).
   For a cloud voice, add the provider key, pick a voice, **Test voice**, Save.
3. **Try it** — Chat: *“hello”*, *“check battery”*, *“remember that my name is Arjun”*,
   *“open Spotify”*, *“remind me to stretch in 20 minutes”*, *“what do you know about me?”*.
4. **Automation** — Automation → ＋ → name + actions (e.g. open app + notification) →
   save → run manually or say *“run routine good morning”*.

---

## Security

- API keys encrypted with **AES-256-GCM**; the key never leaves the **Android Keystore**
- No keys, logs or prompts on disk in plain text; `allowBackup=false`
- Runtime permissions requested contextually (microphone, camera/torch, notifications)
- Crash protection: uncaught exceptions are recorded to the experience log
- Chain-of-thought is never exposed to the model context or the UI

## Tech stack

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) · Material 3 · Hilt 2.52 · Room 2.6.1 ·
DataStore 1.1.1 · Navigation Compose 2.8.5 · Retrofit 2.11.0 / OkHttp 4.12.0 · Gson ·
Coroutines 1.9.0 · AGP 8.7.3 · Gradle 8.10.2

## License

Proprietary — RONIN AI. All code in this repository is provided as-is.
