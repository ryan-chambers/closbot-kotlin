# App Inventory: closbot (Ionic/Angular) → closbot-kotlin

Reference inventory of the existing app at `/Users/ryanchambers/Documents/GitHub/closbot`,
captured before writing any native code. Source repo is read-only reference — nothing here
implies a line-by-line port; each area gets re-designed the idiomatic Android way.

## What the app does

An AI sommelier / wine-tracking companion, backed by OpenAI (chat, vision, embeddings) and a
Pinecone RAG index of the author's own wine notes plus other compiled sources. Bilingual
(English/French).

## Screens (currently Ionic tabs)

| Screen | Route | Purpose |
|---|---|---|
| Chat | `/tabs/chat` | Chat with an AI sommelier. Initial greeting message. RAG context pulled in automatically when "Burgundy focused" mode is on and message is substantial (>10 chars). Flag button to mark a response as hallucinated/bad. |
| Add Note | `/tabs/add-note` | Add a wine note, optionally with a photo. If a photo of a bottle label is taken, AI vision reads producer/appellation/vintage to pre-fill fields. Note text is upserted into the RAG index. |
| Gallery | `/tabs/gallery` | Grid of past wine photos/notes. Searchable and filterable by label/tag. |
| Edit Note | `/tabs/edit-note/:id` | Detail/edit view for a single gallery entry, resolved via `photoIdResolver` before navigation. |
| Vintage Report | `/tabs/vintage` | Static-ish reference data: vintage quality ratings (good/great/challenging) and drink-or-hold guidance by year and region (Burgundy-specific: Côte de Beaune, Côte de Nuits, village/premier/grand cru tiers, red vs white). |
| (menu recommendation) | via chat flow | Take a photo of a wine menu; AI reads it and gives a recommendation. Handled as part of the chat/wine service flow, not a separate route. |

Language toggle (EN/FR) is global, not a separate screen — affects all `ContentService`-registered
strings at once.

## Data models (current TS shapes)

- `ChatMessage { content, sender: 'user'|'system', timestamp }`
- `WinePhoto { id, filepath, webviewPath?, wineDetails?, labels?, date }` — a gallery entry
- `WineBottleInfo { producer, appellation?, vintage? }` — AI-extracted label data
- `WineContext { personalNotes: RagQueryResult[], otherContext: RagQueryResult[] }`
- `RagQueryResult { score, content }`
- `VintageReport { redDrinkHold, whiteDrinkHold, rating: VintageRating, ...notes }`
- `VintageRating` enum: `GOOD | GREAT | CHALLENGING`
- `WhenToDrink { type: 'red'|'white', location, holdFor, thenDrinkOrHoldFor }`

## Services / architecture (current)

- **WineService** — facade that switches between `AiWineService` (real) and `FakeWineService`
  (canned responses) based on a `ConfigService` flag. Defines the core contract:
  `invokeChat`, `addWineNote`, `readWineMenu`, `describeWine`, `readWineBottlePhoto`,
  `flagResponse`.
- **AiWineService** — implements the real flow: combines Pinecone RAG lookups with OpenAI calls,
  builds prompts via `PromptService`, tracks responses via a `@TrackResponse` method decorator.
- **OpenAiService** — thin wrapper around the `openai` JS SDK. Called **directly from the client**
  with an API key embedded in `environment.ts` (excluded from git). Chat uses a stateful
  `previousResponseId` (OpenAI Responses API). Also does embeddings
  (`text-embedding-3-large`) and vision calls (bottle photo, menu photo).
- **PineconeService** — talks to Pinecone's REST API directly via `HttpClient` (also with an
  embedded API key) for RAG query + upsert of notes.
- **VintagesService** — hardcoded Burgundy vintage/drink-window reference data (no backend).
- **ContentService** — hand-rolled i18n: EN/FR string bundles registered per-component via
  Angular signals, plus a separate error-message table and `{{ arg }}` interpolation.
- **CameraService** — wraps Capacitor Camera plugin (`getPhoto`), used both for base64 (AI vision
  calls) and file URI (gallery storage) capture flows.
- **GalleryService** — owns the `WinePhoto[]` list (Angular signal), persists via Capacitor
  `Preferences` (key `winePhotos`, JSON blob) + photo files via Capacitor `Filesystem`. Supports
  search-term and tag filtering via computed signals.
- **OcrService** — despite the name, not a traditional OCR SDK; delegates to
  `WineService.readWineBottlePhoto`, i.e. AI-vision-based label reading.
- **ResponseLogService** — buffers the most recent AI request/response context per navigation
  cycle (cleared on route change), writes to Capacitor `Filesystem`, used for the "flag response"
  feature to capture hallucinations for later review.
- **ConfigService** — two feature-flag signals: `useRealServices` (real AI vs fake/canned) and
  `burgundyFocused` (whether to auto-attach RAG wine context to chat).
- **ToastService** — thin wrapper over Ionic toast UI.
- **track-response.decorator** — method decorator (`@TrackResponse(context)`) that wraps AI-calling
  methods to record their result into `ResponseLogService`.

## Third-party integrations

- **OpenAI** — chat (Responses API, stateful via `previousResponseId`), vision (bottle label read,
  menu read), embeddings (`text-embedding-3-large`). Called directly from the client today; API
  key currently lives in an untracked `environment.ts` (acceptable for this POC per the author's
  own notes since it's never shipped to an app store — **worth a deliberate decision** for the
  Android app: keep client-side calls, or introduce a thin backend proxy to avoid shipping a
  secret in an installable APK).
- **Pinecone** — RAG vector store, REST calls directly from client, same API-key-exposure
  consideration as above.
- **Capacitor plugins in use**: Camera, Filesystem, Preferences, Haptics, Keyboard, StatusBar, App.

## Local persistence (current)

- Capacitor `Preferences` (simple key/value, backed by SharedPreferences on Android under the
  hood) stores the entire `winePhotos` array as one JSON blob.
- Capacitor `Filesystem` stores photo files and response-log text files on device.
- No structured local database (no SQLite) in the current app — everything is a flat JSON blob or
  files. This is a natural place for the Android rewrite to upgrade to Room instead of carrying
  the "one big JSON blob" pattern forward.

## i18n

EN/FR only, no framework — custom signal-based content bundles. Android equivalent: standard
`strings.xml` (`values/` + `values-fr/`) resource qualifiers instead of a bespoke content service.

## Known rough edges (from `BACKLOG.txt`, for awareness — not necessarily to replicate)

- RAG sometimes confuses similar bottles (e.g. village white vs. town red).
- `GalleryService.deletePhoto` doesn't currently guard against `Filesystem.deleteFile` failing.
- No streamed/incremental chat responses yet (full response arrives at once).
- Model choice varies by use case (regular vs. mini model) for cost/speed tradeoffs — worth
  carrying the *idea* forward (different model per use case) even though the specific Android
  networking layer will look different.

## Decisions made

1. **API access: keep calling OpenAI/Pinecone directly from the app; no backend proxy.** Keys live
   in an untracked file (`local.properties`, already gitignored), read by Gradle and exposed as
   `BuildConfig` fields (e.g. via the Secrets Gradle Plugin). Same tradeoff as the original app:
   keys are extractable from the built APK, so this is only acceptable for a personal,
   non-distributed app. Revisit before any Play Store release.

2. **Local storage: Room for gallery/notes**, replacing the flat JSON blob. Photos stay as files in
   the app's private `filesDir`; the table stores only the file name. Search/tag filtering becomes
   SQL queries exposed as `Flow`s.
3. **Migrating existing photos**: the old app was installed via Android Studio, so it is
   debuggable. Photos and notes can be pulled with `adb run-as com.ryanthink.closbot` (photo files
   from `files/`, notes from `shared_prefs/CapacitorStorage.xml`) with no change to the old app,
   then imported once into Room. Do this later, when the phone is attached.
4. **Vintage reference data: bundled JSON asset**, parsed at startup. It is read-only, so it needs
   no Room schema or migrations, and it can be edited without touching code.
5. **Fake services: kept, as a fake repository swapped in via DI (Hilt).** Repository interfaces
   get a real and a fake implementation, so UI work and tests don't call OpenAI or Pinecone.

6. **No GitHub remote for now.** The repo stays local-only until the app is much further along.

## Open decisions for the Android rewrite

None outstanding from the inventory.
