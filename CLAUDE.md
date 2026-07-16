# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Woodshed Android is a Kotlin/Compose port of the iOS Woodshed app — a video practice journal for musicians. Feature parity goal: video recording, A/B loop playback with section markers, metadata tagging, library search/filter, video trimming, and export. Targets Android API 29+ (Android 10).

The iOS source lives at `../woodshed` and is the canonical reference for behavior, UI layout, and data model design.

## Build & Run

Open `woodshedAndroid/` in Android Studio (Hedgehog or newer). Standard Gradle commands:

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build and install on connected device/emulator
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (requires device/emulator)
./gradlew lint                   # lint
```

Run a single test class: `./gradlew test --tests "com.russert.woodshed.PlayerViewModelTest"`

## Technology Stack (iOS → Android mapping)

| iOS | Android |
|-----|---------|
| SwiftUI | Jetpack Compose |
| `@ObservableObject` / `@Published` | `ViewModel` + `StateFlow` |
| `@StateObject` | `viewModel()` in Compose |
| `@Environment` injection | Hilt DI |
| async/await | Kotlin coroutines + `viewModelScope` |
| Core Data | Room |
| `NSPersistentCloudKitContainer` | Room (local only for v1; no iCloud equivalent) |
| AVFoundation / AVPlayer | Media3 / ExoPlayer |
| AVCaptureSession / CameraX | CameraX |
| NavigationStack + TabView | `NavHost` + `NavigationBar` (Compose Navigation) |
| `.sheet` / `.fullScreenCover` | `ModalBottomSheet` / separate `NavHost` destination |
| `LazyVGrid` / `LazyVStack` | `LazyVerticalGrid` / `LazyColumn` |
| UserDefaults | DataStore (Preferences) |
| `.woodshed` file export | Same custom format (JSON header + video bytes) |

## Architecture

MVVM, mirroring the iOS structure directly.

```
app/src/main/java/com/russert/woodshed/
├── data/
│   ├── db/           # Room database, DAOs, entities (= iOS CoreData/)
│   └── file/         # File I/O, storage paths (= iOS Services/VideoFileService)
├── models/           # InstrumentType enum, data classes (= iOS Models/)
├── services/         # CameraService, ThumbnailService, WoodshedFileService (= iOS Services/)
├── ui/
│   ├── library/      # LibraryScreen, LibraryViewModel (= iOS Views/Library + ViewModels/LibraryViewModel)
│   ├── player/       # PlayerScreen, PlayerViewModel, scrubber composables (= iOS Views/Player)
│   ├── record/       # RecordScreen, RecordingSessionViewModel (= iOS Views/Record)
│   ├── metadata/     # MetadataFormScreen, EditRecordingScreen (= iOS Views/Metadata)
│   ├── settings/     # SettingsScreen, SettingsViewModel (= iOS Views/Settings)
│   ├── onboarding/   # OnboardingScreen (= iOS Views/Onboarding)
│   └── theme/        # Theme.kt (= iOS Utilities/Theme.swift)
└── MainActivity.kt   # NavHost + bottom NavigationBar
```

### Data layer

- **Room entities**: `RecordingEntity` and `VideoTimestampEntity` mirror the iOS Core Data schema exactly — same fields, same relationships.
- Tags are stored as a comma-separated string in a single column, split on read (same as iOS `tagArray`).
- Videos live in `Context.getExternalFilesDir("Videos")` or app-internal storage. Thumbnails always local in `filesDir/Thumbnails/`. Section loop state persisted as JSON files in `filesDir/SectionStates/`.
- No cloud sync in v1 — local storage only. Export/import via `.woodshed` files is the cross-device story.

### State management

Each screen has one `ViewModel` that exposes state via `StateFlow<ScreenState>` and events via `SharedFlow`. Composables collect with `collectAsStateWithLifecycle()`. No shared mutable state between ViewModels except via the Room database.

`SettingsViewModel` is scoped to the `Activity` (shared across tabs), all other ViewModels are scoped to their `NavBackStackEntry`.

### Player

`PlayerViewModel` wraps a Media3 `ExoPlayer` instance. The section loop logic (A–H sections, quick loop, split points) is a direct port of the iOS `PlayerViewModel` — keep the same field names (`sectionStart`, `splitPoints`, `activeSection`, `loopEnabled`, `isQuickLoopActive`, `quickLoopIn`, `quickLoopOut`). Loop enforcement runs in a `Player.Listener.onEvents` callback checking position every 50ms.

### Camera / Recording

`CameraService` wraps CameraX `VideoCapture<Recorder>`. Recording output goes to a UUID-named `.mp4` temp file, then moves to permanent storage on metadata save (same flow as iOS).

### Theme

`ui/theme/Theme.kt` defines the same color tokens as iOS `Theme.swift`:

```kotlin
val Cream     = Color(0xFFF5EDD8)
val Amber     = Color(0xFFCC8C33)
val WarmBrown = Color(0xFF613D21)
val DarkBrown = Color(0xFF382114)
val MutedGreen= Color(0xFF596D4C)
```

Typography uses `serif` (Georgia equivalent — use `FontFamily.Serif` or embed Georgia TTF). Spacing constants: `Padding = 16.dp`, `SmallPadding = 8.dp`, `CornerRadius = 12.dp`, `SmallCornerRadius = 8.dp`.

### Navigation

`MainActivity` hosts a `Scaffold` with a `NavigationBar` (3 tabs: Library, Record, Settings) and a `NavHost`. The Library tab pushes `PlayerScreen` onto the back stack via `navController.navigate("player/{recordingId}")`.

Modals that are `.fullScreenCover` on iOS become full-screen destinations in the nav graph on Android (`enterTransition = fullSlideUp`): `MetadataFormScreen`, `OnboardingScreen`.

### Custom file format (.woodshed)

`WoodshedFileService` on Android must produce/consume the same binary format as iOS:
- 8-byte magic header
- 4-byte JSON length (big-endian)
- JSON metadata bytes
- raw video bytes

See `../woodshed/woodshed/Services/WoodshedFileService.swift` for the authoritative format definition.

## Key dependencies (gradle)

```kotlin
// Compose BOM + UI
implementation(platform("androidx.compose:compose-bom:..."))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:...")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:...")
implementation("androidx.lifecycle:lifecycle-runtime-compose:...")

// Room
implementation("androidx.room:room-runtime:...")
implementation("androidx.room:room-ktx:...")
ksp("androidx.room:room-compiler:...")

// Hilt
implementation("com.google.dagger:hilt-android:...")
ksp("com.google.dagger:hilt-android-compiler:...")
implementation("androidx.hilt:hilt-navigation-compose:...")

// Media3 / ExoPlayer
implementation("androidx.media3:media3-exoplayer:...")
implementation("androidx.media3:media3-ui:...")

// CameraX
implementation("androidx.camera:camera-camera2:...")
implementation("androidx.camera:camera-lifecycle:...")
implementation("androidx.camera:camera-video:...")
implementation("androidx.camera:camera-view:...")

// DataStore
implementation("androidx.datastore:datastore-preferences:...")

// Coil (thumbnail loading)
implementation("io.coil-kt:coil-compose:...")
```

## iOS parity notes

- The scrubber is a custom `Canvas`-drawn composable — not a standard `Slider`. It must show loop region highlight, timestamp tick marks, quick loop handles, and section boundary drag handles, matching the iOS `ScrubberView`.
- The A/B loop section system (up to 8 sections labeled A–H) with drag-to-move boundaries is the most complex UI piece. Port the iOS `ABLoopControlView` logic directly; the business logic in `PlayerViewModel` can be translated nearly line-for-line.
- `MetadataFormView` has a tag input field that splits/joins on commas, an instrument picker (bottom sheet), and an `AsyncImage` thumbnail preview.
- The library card grid uses `LibraryCardView` with thumbnail + overlay — use `LibraryCardComposable` backed by Coil for async thumbnail loading.
- The onboarding tour overlay (iOS `PlayerTourOverlay`) highlights anchored composables in sequence. Use `onGloballyPositioned` to capture Compose layout coordinates for the same effect.
