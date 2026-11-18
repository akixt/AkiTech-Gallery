# AkiTech Gallery

**AkiTech Gallery** is a fast, modern Android photo & video gallery, offline studio editor, and **Firebase AI Studio** built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **MediaStore**, **Jetpack Media3 (ExoPlayer & Transformer)**, **Firebase AI Logic**, **Coil**, and **SQLite**.

## Features

- **Unified Media Library**: Smooth, responsive lazy grid displaying device photos and videos sorted newest first via `MediaStore`, with timeline date section headers, a **Latest Capture** spotlight card, configurable **Photo Grid Columns (`2`–`5` columns)** with responsive tablet scaling, **Compact vs. Detailed View** toggle, sorting (`Newest`, `Oldest`, `Name A–Z`, `Largest`), and multi-select batch actions.
- **AkiTech AI Studio** (Powered by Firebase AI Logic & Gemini Interactions API):
  - **Create & Edit Images** (`gemini-nano-banana-2.1`): Generate images from text or attach/pick an existing photo for prompt-based AI editing across `1:1`, `16:9`, `9:16`, `4:3`, and `3:4` aspect ratios, with one-tap save to gallery.
  - **Animate Photo into Video** (`gemini-omni-1.2-flash` via Interactions API): Pick or send any gallery photo and animate it into an MP4 video in **`16:9` (Landscape)** or **`9:16` (Portrait)** format.
  - **Text-to-Video** (`gemini-omni-1.2-flash` via Interactions API): Generate **`16:9`** or **`9:16`** videos with synchronized audio directly from text prompts and play/save directly in-app via Media3.
  - **Music Studio** (`lyria-3-clip-preview` & `lyria-3-pro-preview`): Generate 30-second stereo music clips or full-length songs with optional style tags and custom lyrics, preview via Media3 ExoPlayer, and save to `Music/AkiTech Gallery`.
- **Multi-Format Image & Video Player**:
  - Supports **JPEG, PNG, WebP, HEIC, GIF, BMP** images (including animated GIF/WebP via Coil) and **MP4, MKV, WebM, MOV, 3GP, AVI** videos via **Media3 ExoPlayer**
  - Horizontal swipe paging with synced filmstrip, pinch-to-zoom, pan, double-tap zoom (`1×` ↔ `2.5×`), and **Slideshow** auto-advance
  - Full video player HUD with scrub slider, `-10s` / `+10s` seek, mute, loop, and playback speed (`0.5×` – `2.0×`)
  - **Share**, **Delete** with system confirmation (`MediaStore.createDeleteRequest` / `RecoverableSecurityException` / dialog), **AI Studio** launch shortcut, and **Media Details** sheet
- **Offline Photo Studio Editor**:
  - Real-time `ColorMatrix` filter presets (`Original`, `Vivid`, `Mono`, `Cyan`, `Warm`, `Fade`, `Drama`)
  - Brightness, Contrast, Saturation, and Warmth adjustment sliders
  - Rotate (`±90°`), Flip Horizontal/Vertical, Aspect Ratio Crop (`Original`, `1:1`, `4:3`, `16:9`, `3:4`), and direct export to `Pictures/AkiTech Gallery`
- **Offline Video Studio Editor**:
  - Trim start/end range slider with live looped preview
  - Rotate (`0°`, `90°`, `180°`, `270°`), Remove Audio toggle, and GPU filter effects (`Original`, `B&W Noir`, `Vivid`, `Cyan Cool`, `Warm Glow`)
  - Hardware-accelerated offline export via **Media3 Transformer** to `Movies/AkiTech Gallery`
- **Albums & Favorites**: Folder grouping (`BUCKET_DISPLAY_NAME`) with adaptive two-pane tablet view, local **Favorites** backed by **SQLite**, and instant **All / Photos / Videos / Favorites** filter chips + search.
- **Settings & Developer Info**:
  - Obsidian dark default (`#090A0E`) with Solar Amber-Orange accent (`#FF8C00`) harmonized with the AkiTech Gallery logo, plus System and Light theme switcher
  - Photo Grid Columns (`2`, `3`, `4`, `5`), Compact/Detailed Grid View Mode, default sort order, media badge toggles, video auto-play & loop defaults, and thumbnail disk cache cleaner
  - Integrated Developer & Studio info card (**Aklilu Tibebu / Aki Tech** — BSc Information Systems, Madda Walabu University, Ethiopia)
- **Permissions UX**: Runtime permission handling for `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+), and `READ_EXTERNAL_STORAGE` (Android 8–12).

---

## Project Structure

```text
app/src/main/java/com/akitech/gallery/
├── AkiTechGalleryApp.kt         # Application class, AppContainer, Coil ImageLoader (Video + GIF)
├── MainActivity.kt              # Single-activity host + MediaStoreLazyGrid & MediaStoreCoilThumbnail
├── ai/
│   ├── AppCheckInit.kt          # Firebase App Check (Debug / Play ReCAPTCHA Enterprise)
│   └── AiMediaRepository.kt     # Nano Banana 2.1 images, Omni 1.2 Flash Interactions video, Lyria 3 music
├── data/
│   ├── local/                   # SQLiteOpenHelper FavoriteEntity, FavoriteDao, GalleryDatabase
│   ├── mediastore/              # MediaStoreDataSource (photo/video/audio queries, save, delete)
│   └── repository/              # GalleryRepository (MediaStore + SQLite + AppSettings prefs)
├── di/
│   └── AppContainer.kt          # Manual constructor dependency injection
├── domain/
│   └── model/                   # Photo, Album, UiState, AppSettings, AiStudioModels
└── ui/
    ├── components/              # GalleryTopBar, PhotoGridItem, DeveloperInfoCard, AiStudioPreviewCards, StateViews
    ├── navigation/              # GalleryAppRoot (NavHost, FloatingGalleryNavBar, NavigationRail)
    ├── screens/                 # PhotosScreen, AlbumsScreen, AiStudioScreen, FavoritesScreen,
    │                            # SettingsScreen, PhotoViewerScreen, PhotoEditorScreen, VideoEditorScreen
    ├── theme/                   # Color.kt, Type.kt, Theme.kt (Space Grotesk + Plus Jakarta Sans)
    ├── util/                    # Formatters, PermissionHelper
    └── viewmodel/               # GalleryViewModel, AiStudioViewModel (StateFlow + SavedStateHandle)
```

---

## Opening in Android Studio

1. Install **Android Studio** (Ladybug / Meerkat or newer) with **JDK 17** and **Android SDK 36** (`minSdk 26`, `targetSdk 34`).
2. Clone or extract the repository and open the root folder in Android Studio (**File → Open…**).
3. Wait for Gradle sync to complete.
4. Select an emulator or physical Android device (Android 8.0 Oreo / API 26+) and click **Run ▶**.

---

## Building the APK

### 1. Android Studio (Local)
- **Debug APK**: Select **Build → Build Bundle(s) / APK(s) → Build APK(s)** (or run `gradle :app:assembleDebug` from the terminal).
- Output location:
  `app/build/outputs/apk/debug/app-debug.apk`

### 2. GitHub Actions (CI)
A ready-to-run workflow is included at `.github/workflows/android-build.yml`:
1. Push this repository to GitHub (`main` or `master` branch).
2. Open the **Actions** tab in your GitHub repository.
3. Each push/PR (or manual **Run workflow**) runs `:app:testDebugUnitTest` and `:app:assembleDebug`, then uploads **`akitech-gallery-debug-apk`** as a downloadable build artifact.

### 3. Codemagic (CI/CD)
To build on **Codemagic**:
1. Connect your GitHub/GitLab repository in the Codemagic dashboard and choose **Android App** (or add a `codemagic.yaml` file at the repo root):
   ```yaml
   workflows:
     android-debug:
       name: AkiTech Gallery Debug
       instance_type: mac_mini_m2
       environment:
         java: 17
       scripts:
         - name: Build Debug APK
           script: gradle :app:assembleDebug
       artifacts:
         - app/build/outputs/apk/debug/*.apk
   ```
2. Trigger a build in Codemagic and download `app-debug.apk` from the **Artifacts** tab.

---

## Signing a Release APK

1. **Generate an upload keystore** (keep this file and its passwords private — never commit them to Git):
   ```bash
   keytool -genkeypair -v \
     -keystore my-upload-key.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Provide signing credentials via environment variables** (configured in `app/build.gradle.kts`):
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD="your-store-password"
   export KEY_PASSWORD="your-key-password"
   ```
3. **Build the signed Release APK or AAB**:
   ```bash
   gradle :app:assembleRelease   # Outputs app/build/outputs/apk/release/app-release.apk
   gradle :app:bundleRelease     # Outputs app/build/outputs/bundle/release/app-release.aab
   ```
4. Optionally verify the APK signature:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
