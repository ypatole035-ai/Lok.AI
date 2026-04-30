# Changelog

## [1.0.0] — Phase 6: Onboarding, Polish & v1.0 Release

### Added
- `OnboardingScreen.kt` — animated first-launch flow shown once and never again:
  - Page 0: Hardware scan with animated progress bar and live status labels (`/proc/meminfo`, `/proc/cpuinfo`, GPU detection, zram, tier classification, model filtering)
  - Page 1: Device profile card — chip, RAM, tier badge with colour + explanation, GPU, cores, Android version; amber CTA button
  - Page 2: Top 3 compatible model preview cards with RAM requirement and thinking badge; final CTA to browse models
  - DataStore-persisted — `onboarding_done` key ensures onboarding is never shown again after completion
- `OnboardingViewModel.kt` — drives scan animation with realistic step progression, calls `DeviceDetector` and `ModelCatalog`, exposes `isOnboardingDone` flow
- `EmptyStates.kt` — full set of empty, error, and loading components:
  - `EmptyState` — generic icon + title + body + optional CTA
  - `NoModelsEmpty` — with "Browse models" action
  - `NoAgentsEmpty` — with "Create agent" action
  - `NoSessionsEmpty` — no action needed
  - `HFSearchEmpty` — shows search query in title
  - `ErrorState` — red error icon variant with optional retry/dismiss CTA
  - `DownloadFailedError`, `ModelLoadFailedError`, `PdfNoTextError`, `FileTooLargeError`, `NotEnoughRamError` — all preset error states
  - `ShimmerBlock` — animated shimmer brush for skeleton loading
  - `CatalogLoadingShimmer` — 5 stacked shimmer cards for model browser loading state
- `build-debug.yml` — GitHub Actions debug build on every push to `main`:
  - Checkout with submodules, JDK 17, Android SDK, NDK r27, CMake 3.22
  - Gradle cache, `assembleDebug`, uploads APK artifact with 30-day retention
- `build-release.yml` — GitHub Actions signed release build on `v*.*.*` tags:
  - Same setup as debug + keystore decode from `KEYSTORE_BASE64` secret
  - `assembleRelease` with signing parameters
  - APK renamed to `Lok.AI-v*.*.*.apk`
  - CHANGELOG.md section auto-extracted for GitHub Release body
  - GitHub Release created via `softprops/action-gh-release`
- `MainActivity.kt` updated — onboarding gate via DataStore; routes first-time users to `OnboardingScreen`, returning users to `LokaiNavGraph`
- `README.md` — full v1.0.0 documentation with install guide, full feature list, all 5 agent categories, inference mode table, and license summary
- `CHANGELOG.md` — v1.0.0 entry (this entry)
- `build.gradle` — `versionCode` 6, `versionName` 1.0.0

### Changed
- `build.gradle` versionCode 5 → 6, versionName `0.5.0-phase5` → `1.0.0`

### Phase 6 complete when
New user installs APK, sees animated scan, views their device profile, previews compatible models, dismisses onboarding, downloads a model, starts a chat or creates an agent — complete first experience with zero confusion. Signed v1.0.0 APK built by GitHub Actions on tag push.

---

## [0.5.0-phase5] — Phase 5: Named Agents, TF-IDF & File Processing

### Added
- `AgentProfile.kt` — saved agent profile with category, model, file, system prompt, and inference mode
- `AgentCategory.kt` — 5 categories (Code, Story, Research, Reference, Custom) with strategy names and default prompts
- `FileChunk.kt` — chunked file piece with TF-IDF vector JSON and skeleton flag
- `AgentSession.kt` — agent chat session stored in Room alongside regular sessions
- `AgentDao.kt` / `ChunkDao.kt` / `AgentSessionDao.kt` — Room DAOs for all agent data
- `LokaiDatabase.kt` updated to v5 — adds `agent_profiles`, `file_chunks`, `agent_sessions` tables
- `FileProcessor.kt` — reads files by extension; supports .txt, .md, code files, JSON/XML/YAML; clear PDF/unsupported errors
- `FileChunker.kt` — splits text into word-count chunks per strategy (skeleton for Story/Research, pure retrieval for Reference/Custom, full load for Code)
- `TfIdfEngine.kt` — fully on-device TF-IDF indexing and cosine similarity retrieval; no embedding model required
  - `index()` — computes TF-IDF vectors for all chunks at agent creation time
  - `retrieve()` — returns top-N chunks by cosine similarity for each user query
  - `retrieveBroader()` — lower-threshold fallback search triggered on uncertainty
  - `containsUncertainty()` — detects model uncertainty signals in response text
- `ContextBuilder.kt` — assembles final prompt: system prompt + skeleton/summary + retrieved chunks + trimmed history + user message
- 5 strategy classes in `data/agent/strategies/`:
  - `CodeStrategy.kt` — full load, no retrieval, context overflow warning
  - `StoryStrategy.kt` — skeleton (headings + first/last paragraphs) + 3-chunk retrieval + fallback
  - `ResearchStrategy.kt` — summary layer (intro + headings + conclusion) + 4-chunk retrieval + fallback
  - `ReferenceStrategy.kt` — pure retrieval, 5 chunks per query, no pre-load
  - `CustomStrategy.kt` — user-defined reading strategy, chunk count, temperature, context size
- Automatic fallback retrieval — uncertainty signals trigger broader TF-IDF search + silent re-inference; thinking panel logs all steps
- `AgentRepository.kt` — CRUD for agent profiles, background file indexing pipeline with progress callbacks, session persistence
- `AgentViewModel.kt` — agent list, create, and chat state machines; handles all 5 strategy types; fallback retrieval loop
- `SessionsViewModel.kt` — exposes regular ChatSessions for the unified history screen
- `HuggingFaceSearch.kt` — searches HuggingFace Hub API for GGUF models; estimates size, compatibility, and thinking-trained status
- `ModelViewModel.kt` updated — adds `hfSearchState` StateFlow and `searchHuggingFace()` method
- `AgentCard.kt` / `CategoryBadge.kt` — agent list UI components with category emoji, last-used timestamp, delete confirmation
- `AgentListScreen.kt` — list of saved agents with FAB to create; empty state; delete with confirmation
- `AgentCreateScreen.kt` — full agent creation flow: name → category → model → file picker → system prompt → custom settings → indexing progress
- `AgentChatScreen.kt` — full agent chat with streaming inference, thinking panel always showing retrieval steps, mode toggle, context banner
- `HFSearchScreen.kt` — live HuggingFace search with compatible/incompatible badges and thinking-trained detection
- `SessionsListScreen.kt` — unified history view combining regular + agent sessions, sorted by recency, tagged by type
- `LokaiNavGraph.kt` updated — full Phase 5 navigation with overlay screens (CreateAgent, AgentChat, HFSearch, Sessions); wires all agent flows
- `ModelBrowserScreen.kt` updated — HuggingFace search button in header
- `SettingsScreen.kt` updated — View History button in Sessions section; version bumped to 0.5.0-phase5
- `PlaceholderScreens.kt` cleared — all Phase 5 screens are real implementations

### Changed
- `LokaiDatabase` version 2 → 5 (fallbackToDestructiveMigration for dev builds)
- `build.gradle` versionCode 4 → 5, versionName `0.4.0-phase4` → `0.5.0-phase5`

---

## [0.4.0-phase4] — Phase 4: Chat UI, Inference Modes & Thinking Panel

### Added
- `ChatMessage.kt` + `ChatSession.kt` — domain models for chat history
- `ThinkingLog.kt` — timestamped log entry data class for the thinking panel
- `SessionDao.kt` — Room DAO for chat sessions with full JSON serialization of messages
- `ChatConverters.kt` — Room `TypeConverters` using kotlinx.serialization for message lists
- `LokaiDatabase.kt` updated to v2 — adds `chat_sessions` table
- `SessionRepository.kt` — save/load/delete sessions, export as `.md` via Android share sheet
- `SettingsRepository.kt` — DataStore-backed persistence for all user preferences
- `LokaiSettings` — data class covering threads, context size, max tokens, temperature,
  default mode, custom system prompt, auto-save toggle, battery warning threshold, tooltip tracking
- `ChatViewModel.kt` — full inference state machine:
  - Model loading/unloading with error handling
  - Streaming token collection via `Flow<String>` from `LlamaEngine`
  - Three inference modes applied at prompt-build time (Normal / Precise / Focused)
  - Context trimming at 80% usage — trims oldest turns, preserves first exchange
  - Live thinking log entries emitted during inference
  - RAM monitor — reads `ActivityManager.MemoryInfo` every 2 seconds during inference
  - Battery level tracking via `BatteryManager` broadcast receiver
  - Mode tooltip shown once per mode type (tracked in DataStore)
  - Auto-save every 5 exchanges and on first message
  - Benchmark recording — tokens/sec written to `DownloadedModelDao`
  - Stop generation — cancels coroutine, commits partial response
- `SettingsViewModel.kt` — exposes `StateFlow<LokaiSettings>` and update actions to UI
- `ChatScreen.kt` — complete chat screen:
  - `ChatTopBar` with model name, RAM indicator, model switcher, export button
  - `ChatInputBar` with mode toggle button, `OutlinedTextField`, send/stop icons
  - Context bar — linear progress + token count, color-coded at 70%/90%
  - `ModeSwitcherButton` — one tap cycles Normal ↔ Precise/Focused, colour-coded per mode
  - `UserBubble` / `AssistantBubble` — distinct rounded corner styles
  - `AssistantStreamingBubble` — live streaming with blinking cursor `▌`
  - `WelcomePrompt` — shown on empty session
  - `NoChatModelEmpty` — shown when no model loaded
  - `LoadingModelOverlay` — spinner during model load
  - `ModeTooltipOverlay` — modal card explaining active mode on first use
  - `ModelPickerSheet` — `ModalBottomSheet` listing all downloaded models with Load buttons
  - Error snackbar for load failures
- `ThinkingPanel.kt` — collapsible panel component:
  - Left accent border, monospace bullet log
  - Auto-expands during generation, auto-collapses after
  - Tap ∨/∧ to toggle manually when collapsed/expanded
  - `ThinkingPanelLive` variant for in-flight streaming state
- `RamIndicator.kt` — small pill widget: available RAM + optional battery warning pill
- `SettingsScreen.kt` — full settings screen:
  - Threads, context size, max tokens, temperature sliders
  - Default inference mode selector (FilterChip row)
  - Custom system prompt text field with Save button
  - Auto-save sessions toggle
  - Battery warning threshold slider
  - About section (version, inference engine, license)
- `LokaiNavGraph.kt` updated:
  - Shared `ChatViewModel` across tab switches (model stays loaded)
  - `pendingChatModel` state — Chat button on MyModelCard navigates to Chat and loads model
  - `SettingsScreen` now wired to real implementation (was placeholder)
- `file_paths.xml` — FileProvider paths for exported `.md` files
- `AndroidManifest.xml` — FileProvider declaration for `${applicationId}.fileprovider`
- `build.gradle` — added `kotlinx-serialization-json:1.6.3`, serialization plugin; versionCode 4
- `build.gradle` (root) — added `kotlin.plugin.serialization` plugin declaration

### Changed
- `PlaceholderScreens.kt` — removed `ChatScreen` and `SettingsScreen` stubs (replaced by real screens)
- `LokaiDatabase` bumped to version 2 with `fallbackToDestructiveMigration` for dev builds
- Nav default tab remains "My Models"; Chat tab now functional

### Phase 4 complete when
Full streaming chat works in all three modes. ThinkingPanel collapses after response and expands
correctly. Mode labels match model type. RAM indicator updates live during inference.

---


### Added
- `DownloadManager.kt` — WorkManager-based download controller
  - Survives app backgrounding and device sleep
  - HTTP `Range` header resume support (picks up partial downloads)
  - SHA-256 post-download verification via `ChecksumVerifier.kt`
  - Partial file cleanup on cancel or failure
  - Per-model `StateFlow<DownloadState>` for live UI updates
- `ChecksumVerifier.kt` — streaming SHA-256 verification with progress callback
- `ModelDownloadWorker` — `CoroutineWorker` that handles HTTP streaming to file
- `DownloadRepository.kt` — coordinates downloads, DB persistence, and storage scanning
- `DownloadViewModel.kt` — exposes download state and actions to UI
- `DownloadState.kt` — sealed class: Idle / Queued / Downloading / Verifying / Completed / Failed / Cancelled
- `DownloadedModel.kt` — domain model for a locally available GGUF
- `DownloadedModelDao.kt` + `DownloadedModelEntity` — Room DB entity and DAO
- `LokaiDatabase.kt` — Room database (v1)
- Smart variant picker — re-reads live RAM at download time, follows Q5_K_M → Q2_K preference order
- Storage path scanner — detects pre-existing GGUFs and imports them into the DB
- `DownloadProgressCard` — in-app progress card with determinate/indeterminate bar, cancel button, resume label
- `DownloadConfirmSheet` — Material 3 bottom sheet confirming variant, size, RAM requirement before download
- `MyModelCard` — card for downloaded models with Chat and Agent buttons, long-press to delete
- `DeleteModelDialog` — confirmation dialog before permanently deleting a model file
- `MyModelsScreen` — "My Models" tab listing all downloaded models, empty state, loading state
- `ModelCard` updated — Download button + Downloaded ✓ badge; accepts `DownloadState` parameter
- `ModelBrowserScreen` updated — wires download confirm sheet and live progress per model card
- `LokaiNavGraph` updated — added "My Models" tab between Agents and Browse
- `build.gradle` updated — WorkManager 2.9.0, Room 2.6.1, KSP, DataStore dependencies
- `AndroidManifest.xml` updated — INTERNET, FOREGROUND_SERVICE, POST_NOTIFICATIONS permissions; WorkManager provider

### Changed
- `ModelCard` now accepts optional `downloadState` and `onDownloadClick` parameters
- Nav default tab changed to "My Models"
- `versionCode` bumped to 3, `versionName` set to `0.3.0-phase3`

### Phase 3 complete when
Tap model → confirm sheet → background app → return → model downloaded, verified, visible in My Models.

---

## [0.2.0-phase2] — Phase 2: Device Detection & Model Catalog

### Added
- `DeviceDetector.kt` — reads `/proc/meminfo`, `/proc/cpuinfo`, Android Build APIs, GPU vendor paths
- `DeviceProfile.kt` + `DeviceTier.kt` — 7-tier classification (Micro → Workstation)
- Full SoC name map — Snapdragon SM codes, MediaTek MT codes, Exynos, Kirin
- zram/swap detection with 0.6× weight applied to effective RAM
- `ModelCatalog.kt` — parses bundled `models.json`, reads `thinking_trained` flag
- `ModelEntry.kt` / `ModelVariant.kt` data classes
- Model filtering logic — compatible list + incompatible list with reasons
- `DeviceScreen` — chip, RAM, tier badge, GPU, cores, Android version
- `ModelBrowserScreen` — compatible / too large sections, `⚡ Thinking-trained` badges
- `ModelCard` + `TierBadge` components
- `DeviceViewModel` + `ModelViewModel`
- Bottom navigation: Chat / Agents / Models / Device / Settings (placeholders for unbuilt)
- `models.json` updated with `thinking_trained` flag for all 41 models

---

## [0.1.0-phase1] — Phase 1: Foundation & Native Build

### Added
- Full repo skeleton with Gradle + NDK + CMake structure
- llama.cpp as git submodule (pinned to stable tag)
- `CMakeLists.txt` — compiles llama.cpp to `libllama.so` for `arm64-v8a` and `armeabi-v7a`
- `lokai_jni.cpp` — JNI bridge: `loadModel`, `runInference`, `stopInference`, `unloadModel`, `getContextUsed`, `getContextMax`
- `LlamaEngine.kt` — Kotlin wrapper, `Flow<String>` token streaming
- `InferenceMode.kt` — Normal / Precise / Focused enum
- Minimal test Activity — model path input, mode toggle, streamed output
- `build-debug.yml` — GitHub Actions compiles + builds APK on every push to main
- `LICENSE` — custom Source Available, All Rights Reserved
- `README.md` — basic project description
