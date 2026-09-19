# Architecture

Phase 1 decisions for the native Android rewrite. Each choice records what we picked, why, and
what we rejected, so the reasoning survives after the choice stops feeling obvious. Product
context and earlier decisions (direct API calls, Room, JSON vintage data, fake services, no
remote) are in [app-inventory.md](app-inventory.md).

Library versions are deliberately not pinned here. They are pinned in the Gradle version catalog
when the skeleton app is created.

## Principles

- **Follow Google's recommended app architecture.** Unidirectional data flow, a UI layer that
  never touches data sources directly, and a data layer that owns all I/O.
- **Re-design, don't transliterate.** The Angular app is a reference for behavior, not structure.
- **Make the boring choice unless there's a reason.** This project exists to learn the standard
  way to build Android apps, not to try unusual libraries.
- **Everything testable without a device.** Business logic lives in plain Kotlin classes with
  injected dependencies.

## Stack

| Concern | Choice | Why | Rejected |
|---|---|---|---|
| Language | Kotlin | The goal of the project | none |
| UI | Jetpack Compose + Material 3 | Current standard; declarative and state-driven, closest to the Angular signals mental model | XML Views (legacy) |
| Presentation | MVVM + unidirectional data flow: `ViewModel` exposes a `StateFlow<UiState>`, UI sends events up | Google's recommended pattern; survives rotation; easy to unit test | MVI framework (extra machinery); logic in composables |
| Async | Coroutines + Flow | Native to Kotlin, first-class in Room, Retrofit and Compose | RxJava (Angular habit, but legacy on Android) |
| DI | Hilt | Official recommendation, integrates with `ViewModel` and WorkManager | Koin (runtime errors instead of compile-time); manual DI (fine at this size, but no learning value) |
| Networking | Retrofit + OkHttp + kotlinx.serialization | Standard stack; interceptors give one place for auth headers and logging | Ktor client; hand-rolled `HttpURLConnection` |
| OpenAI access | Decided in the skeleton phase. Candidates: plain Retrofit against the REST API, or an OpenAI SDK for JVM/Kotlin | The old app uses the stateful Responses API (`previousResponseId`), embeddings, and vision, so any SDK must support all three. Otherwise Retrofit wins | Choosing now without trying it |
| Local storage | Room | Decided; see the inventory | Preferences/JSON blob |
| Settings | Jetpack DataStore (Preferences) | Replaces SharedPreferences for the few flags such as language and Burgundy focus | SharedPreferences |
| Navigation | Navigation Compose, type-safe routes | Official; supports back stack and arguments (`edit-note/{id}`) | Hand-rolled screen state |
| Images | Coil | Compose-native image loading for gallery grids | Glide (fine, less Compose-idiomatic) |
| Camera / picking | Photo Picker and `ActivityResultContracts.TakePicture`, with CameraX only if needed | The system contracts avoid a permission for simple capture | Full CameraX from day one |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) | Current standard; one place for versions | Groovy DSL |
| Secrets | `local.properties` (gitignored) exposed as `BuildConfig` fields | Decided; see the inventory | Committing keys |
| i18n | `strings.xml` in `values/` and `values-fr/` | Platform-native; replaces the hand-rolled `ContentService`. Per-app language via the `LocaleManager` / AppCompat per-app language API | Custom content bundles |
| Min SDK | 26 (Android 8.0) | Covers nearly all devices, avoids desugaring workarounds | Lower: extra compat work for no benefit |
| Testing | JUnit, kotlinx-coroutines-test, Turbine (Flow), MockK or fakes, Room in-memory DB, Compose UI tests | Covers each layer; see Testing | Instrumented-only testing |

## Layers

```
UI layer               Composable screens  <-->  ViewModel (StateFlow<UiState>)
                                                     |
Domain layer (optional)                       Use cases, only where logic is shared
                                                     |
Data layer             Repositories (interfaces)  ->  data sources
                          real:  Room DAO, Retrofit (OpenAI, Pinecone), file store
                          fake:  in-memory / canned responses
```

- **UI never calls data sources.** It only talks to a `ViewModel`.
- **Repositories are interfaces** in the data layer, with a real and a fake implementation
  bound by Hilt. This is how the fake-services decision is realized. Fakes are selected by
  build variant (a `demo` flavor) and are always available to tests.
- **The domain layer is optional.** Add a use case only when logic is reused by more than one
  `ViewModel` or is too big for one. Don't create pass-through use cases.
- **`UiState` is immutable and single-source.** One `data class` (or sealed interface for
  loading/error/content) per screen. One-off events (snackbar, navigation) are modeled
  explicitly, not as state that replays on rotation.

## Repository mapping (old services to new)

| Old service | New home |
|---|---|
| `WineService` / `AiWineService` / `FakeWineService` | `WineAssistantRepository` interface; real and fake implementations |
| `OpenAiService`, `PromptService` | OpenAI data source + prompt builders inside the real repository |
| `PineconeService` | Pinecone data source, used by the real repository |
| `GalleryService`, `CameraService` | `WineNoteRepository` (Room + photo files); camera handled by the UI layer via activity-result contracts |
| `OcrService` | Folded into `WineAssistantRepository.readBottlePhoto` (it was already an AI-vision call) |
| `VintagesService` | `VintageRepository` reading the bundled JSON asset |
| `ContentService` | `strings.xml` resources |
| `ConfigService` | DataStore-backed `SettingsRepository` |
| `ResponseLogService`, `@TrackResponse` | A logging/flagging component around the assistant repository; design in its own feature branch |
| `ToastService` | Snackbar via Material 3 `SnackbarHost` |

## Package structure

Single Gradle module (`:app`) with package-by-feature, not package-by-layer:

```
com.<applicationId>/
  app/            Application class, MainActivity, root nav graph, theme
  core/           shared utilities, DI modules, result/error types
  feature/chat/       ChatScreen, ChatViewModel, ChatUiState
  feature/notes/      add, edit, gallery
  feature/vintage/    vintage report
  data/               repositories, Room, network, assets
```

Splitting into Gradle modules is a later refactor once boundaries are proven. It's overkill for
five screens.

**Application ID:** it must differ from the old app's `com.ryanthink.closbot` so both can be
installed side by side (needed for exporting the old photos). Chosen at skeleton time.

## Error handling

- Data-layer calls return a `Result`-style sealed type or throw typed exceptions that the
  repository maps. `ViewModel`s translate them into `UiState` with a user-facing string
  resource, replacing the old `ErrorCode` plus lookup table.
- Never swallow `CancellationException` in coroutines.
- Network calls have explicit timeouts, and a failed OpenAI or Pinecone call is a normal state
  the UI handles, not an exception path.

## Testing strategy

- **Unit tests (fast, JVM):** `ViewModel`s against fake repositories (state transitions via
  Turbine), prompt builders, mappers, vintage JSON parsing.
- **Data tests:** Room DAOs with an in-memory database; Retrofit layers against `MockWebServer`.
- **UI tests:** Compose test APIs on key screens using the fake repositories.
- A change is not done until tests for its behavior exist. Tests come in the same branch as the
  feature.

## Branching and delivery

- `main` only receives merges from branches. One branch per feature or phase, named
  `feature/...`, `docs/...`, `chore/...`.
- Commits are small and describe why. Each branch leaves the app building and tests green.

## Build order

1. `feature/skeleton-app`: Gradle project, Hilt, theme, navigation shell with tabs, and a first
   end-to-end slice (chat with the fake repository, then a real OpenAI call), with tests.
2. Notes and gallery with Room, photo capture, and the old-photo import.
3. Label reading, menu reading, and Pinecone RAG.
4. Vintage report, settings, EN/FR.
5. Cross-cutting: logging and flagging, error states, accessibility, performance pass.

## Deferred / revisit later

- Streaming chat responses (the old backlog item). Design the chat repository to return a
  `Flow` of partial text so streaming is an implementation detail, not a rewrite.
- Backend proxy for API keys, only before any Play Store release.
- Multi-module split.
