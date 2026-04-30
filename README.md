# Lok.AI

> **Run local AI on your Android phone. No cloud. No subscription. No struggle.**

Lok.AI is a free Android app that runs AI language models directly on your phone — fully offline,
no account required, no data sent anywhere. It reads your hardware, filters the model catalog
to only show what will actually run on your device, and lets you chat immediately.

---

## Install

1. Go to [Releases](../../releases) and download the latest `Lok.AI-v*.*.*.apk`
2. On Android: **Settings → Install unknown apps** → allow your file manager or browser
3. Tap the APK to install
4. Open Lok.AI — the hardware scan runs automatically on first launch

> **Min Android:** 8.0 (API 26) · **Target:** Android 15 (API 35)

---

## How it works

### First launch
The app scans your hardware — RAM, CPU, GPU, swap — classifies your device into one of 7 tiers
(Micro → Workstation), then shows you only AI models that will actually fit and run.

### Download a model
Browse the built-in catalog of 41 verified models. Tap any compatible model, confirm the
download, and the app handles it in the background. Downloads resume if you switch apps.
SHA-256 checksum verified on completion.

### Chat
Tap **Chat**, select a downloaded model, and start talking. Responses stream token by token.
Three inference modes:

| Mode | When | What it does |
|---|---|---|
| **Normal** | Always | Standard inference, temperature 0.7 |
| **⚡ Precise** | Thinking-trained models only | Activates model's built-in `<think>` tokens, temp 0.4 |
| **🎯 Focused** | Regular models | Injects step-by-step system prompt prefix, temp 0.4 |

Every response shows a **Thinking Panel** — tap ∨ to see retrieval steps, tokens used, inference time.

### Named Agents
An agent is a saved AI profile tied to a specific file with a context strategy matched to how
that file should be read.

| Category | Strategy | Best for |
|---|---|---|
| 💻 Code | Full file load every message | Source code |
| 📖 Story | Skeleton + on-demand retrieval | Fiction, writing |
| 📄 Research | Summary layer + retrieval | Papers, dense docs |
| 📋 Reference | Pure retrieval (5 chunks) | API docs, specs |
| 🎛️ Custom | User-defined | Power users |

Retrieval uses TF-IDF on-device — no embedding model, no server, fully offline. If the model
says "I'm not sure", the app silently searches deeper and re-runs inference.

---

## Features

- Hardware detection — RAM, CPU, GPU, swap via `/proc` + Android APIs
- 7-tier device classification (Micro → Workstation)
- 41 verified models filtered by device tier
- Thinking-trained model detection (catalog flag + name patterns)
- HuggingFace live search with RAM estimate and compatibility badge
- Background downloads with HTTP resume and SHA-256 verification
- All sessions saved in Room DB — searchable, resumable, exportable as `.md`
- Benchmark recording — tokens/sec rolling average
- No accounts. No telemetry. No ads. Ever.

---

## Tech Stack

Kotlin · Jetpack Compose + Material 3 · llama.cpp via JNI · MVVM + Repository · Room ·
DataStore · WorkManager · TF-IDF retrieval · Gradle + CMake · GitHub Actions CI/CD

ABI targets: `arm64-v8a` (primary), `armeabi-v7a` (secondary)

---

## Build from source

```bash
git clone --recursive https://github.com/ypatole035-ai/Lok.AI
cd Lok.AI
./gradlew assembleDebug
```

Requires: Android Studio, NDK r27, CMake 3.22+, JDK 17.
CI builds on every push to `main`. Signed release builds on `v*.*.*` tags.

---

## Status

| Phase | Description | Status |
|---|---|---|
| 1 | Native build + JNI inference | ✅ Complete |
| 2 | Device detection + model catalog | ✅ Complete |
| 3 | Downloader + model management | ✅ Complete |
| 4 | Chat UI + inference modes + thinking panel | ✅ Complete |
| 5 | Named agents + TF-IDF + sessions | ✅ Complete |
| 6 | Onboarding + polish + v1.0 release | ✅ Complete |

---

## License

Source Available — All Rights Reserved. See [LICENSE](LICENSE).
The inference engine ([llama.cpp](https://github.com/ggerganov/llama.cpp)) is MIT licensed.
