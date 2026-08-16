# myFILMlist

[![Android CI](https://github.com/Asgh24/MYFilmList/actions/workflows/android-build.yml/badge.svg)](https://github.com/Asgh24/MYFilmList/actions/workflows/android-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-orange)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/minSdk-24-brightgreen)](https://developer.android.com/)

مدیریت هوشمند فیلم‌ها، سریال‌ها و انیمه‌های ذخیره‌شده روی دستگاه شما. اسکن خودکار فایل‌های ویدیویی، تشخیص هوشمند نام انیمه/فیلم، دریافت متادیتا و کاور از AniList و TMDB، گروه‌بندی هوشمند مجموعه‌ها، افزودن مجموعه به صورت دستی و پیشنهادهای هوش مصنوعی.

A smart local movie, series & anime library manager for Android. Automatically scans video files on your device, recognizes anime/movie/series names, fetches posters & metadata from AniList, MyAnimeList, TMDB and OMDb, groups scattered episodes into clean collections, lets you add collections manually, and recommends similar titles with Gemini AI.

---

## ✨ Features

- **Smart filename parsing** — strips download-site watermarks (AioFilm, Soft98, ZarFilm, FarsiSub, 300MB, …), resolutions and codec tags, and normalizes franchise names (Jujutsu Kaisen, Frieren, Solo Leveling, Attack on Titan, …) across many English / Romaji / Persian variants.
- **Accurate anime detection** — a curated database of ~100 anime franchises plus fansub-group heuristics, so anime files are no longer confused with western movies.
- **Metadata enrichment** — automatic posters, banners, synopses, ratings and genres from Gemini AI → AniList → MAL/Jikan → TVMaze/TMDB/OMDb/iTunes.
- **Smart collection grouping** — merges scattered episodes of the same franchise into one collection with fuzzy token-overlap matching.
- **Manual add collection** — add a collection entirely by hand (title, type, synopsis, poster URL and episode list) without scanning any file.
- **Ambiguous match review** — when the app can't decide, it offers AI candidates and lets you pick the right franchise.
- **External playback** — plays files with the system chooser, VLC or MX Player.
- **Watch progress & favorites** — track watched episodes and progress per collection.
- **AI assistant** — direct Gemini Q&A, batch re-categorization, and smart recommendations.
- **Bilingual UI** — Persian (فارسی) and English.

---

## 🧱 Tech stack

| Layer      | Tech                                                     |
| ---------- | -------------------------------------------------------- |
| UI         | Jetpack Compose, Material 3                              |
| DI/State   | AndroidX ViewModel, StateFlow, Room (with KSP)           |
| Networking | Retrofit, OkHttp, Moshi, Coil                            |
| AI         | Google Gemini REST API (dynamic model fallback)          |
| Media APIs | AniList GraphQL, MyAnimeList v2, Jikan v4, TVMaze, TMDB, OMDb, iTunes |
| Testing    | JUnit 4, Robolectric, Roborazzi                          |

- Kotlin 2.2 · AGP 9.1 · Gradle 9.7 (wrapper included) · minSdk 24 · target/compile SDK 36

---

## 🚀 Build locally

```bash
# 1. Clone the repository
git clone <your-repo-url> myFILMlist
cd myFILMlist

# 2. Build the debug APK (Android Studio: File → Sync, then Build → Build APK)
./gradlew assembleDebug

# 3. The APK is written to:
#    app/build/outputs/apk/debug/app-debug.apk
```

> `./gradlew` (the Gradle wrapper) is committed, so no local Gradle installation is required.

### Release APK

The release build is signed with your real keystore when these environment
variables (or GitHub Actions secrets) are present:

| Variable        | Description                                    |
| --------------- | ---------------------------------------------- |
| `KEYSTORE_PATH` | Absolute path to your `.jks` keystore          |
| `STORE_PASSWORD`| Keystore store password                        |
| `KEY_PASSWORD`  | Key password (alias is `upload`)               |

When the secrets are absent (e.g. PR builds), the release APK is automatically
signed with the debug keystore so it remains installable.

---

## 🤖 GitHub Actions (CI)

A ready-to-use workflow is included at `.github/workflows/android-build.yml`:

- Builds **debug** and **release** APKs on every push / PR to `main`.
- Runs the unit test suite (non-blocking) and uploads test reports.
- Uploads the APKs as build artifacts (`myFILMlist-debug`, `myFILMlist-release`).

1. Push the repository to GitHub.
2. Open the **Actions** tab → the `Android CI` workflow runs automatically.
3. Open a completed run → **Artifacts** → download `myFILMlist-debug` / `myFILMlist-release`.
4. (Optional) Add the `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` secrets under **Settings → Secrets and variables → Actions** to get a real release signature.

To install the APK on a device: copy the `.apk` to the phone, enable "Install from unknown sources" and open the file.

---

## 📥 Download & install the APK

- **From CI artifacts (recommended):** open the latest [Android CI run](https://github.com/Asgh24/MYFilmList/actions/workflows/android-build.yml), open it, scroll to **Artifacts**, and download `myFILMlist-debug` / `myFILMlist-release` (a `.zip` containing the `.apk`). Artifacts expire after ~90 days.
- **Build it yourself:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- **Installation tips:**
  - On first install, Android asks for permission to install "unknown apps" — allow it for your file manager / browser.
  - If the phone blocks installation because "Play Protect" is on, tap *More details → Install anyway* (only for sideloaded builds you trust).
  - The release APK produced without the signing secrets is signed with a debug key — fine for sideloading, but a production keystore is required for Play Store distribution.

---

## 🔑 API keys (optional but recommended)

No key is required for the app to work — the public APIs (AniList, MAL, TVMaze, …) cover most titles.

For the **smartest** results (Persian synopses, AI grouping, recommendations) enter a free Gemini API key in **Settings → Google Gemini API Key**. You can also configure TMDB / OMDb keys there.

- `GEMINI_API_KEY` is injected from `.env` by the Gradle Secrets plugin at build time (`MY_GEMINI_API_KEY` is the placeholder and is ignored at runtime).
- User-entered keys are stored locally in `SharedPreferences` and never uploaded.

---

## 📁 Project layout

```
app/src/main/java/com/example/
├── data/
│   ├── parser/         FileNameParser — smart anime detection & name normalization
│   ├── scanner/        MediaScanner — MediaStore + SAF folder scans
│   ├── repository/     MediaRepository — DB access, enrichment, manual collections
│   ├── local/          Room entities / DAO / database
│   ├── model/          MediaItem, MediaCollection, ManualCollection, …
│   └── remote/         AniList, PublicMediaApi (MAL/TMDB/OMDb/TVMaze), Gemini
├── ui/
│   ├── screens/        MainScreen (library, folders, smart AI, settings)
│   ├── components/     CollectionCard, CollectionBottomSheet, ManualAddCollectionSheet, …
│   ├── viewmodel/      MediaViewModel
│   └── theme/          Material 3 dark/light theme
└── MainActivity.kt
```

---

## 🧪 Tests

```bash
./gradlew testDebugUnitTest
```

`FileNameParserTest` covers anime/series/movie parsing and franchise normalization.

---

## 💡 Tips & troubleshooting

- **"google-services.json is missing" warning during build:** expected and harmless — the build passes `googleServices.missing.passthrough=true` so it works without Firebase config. Only relevant if you enable Firebase features.
- **`.env` file:** the Gradle Secrets plugin reads `.env` (falling back to `.env.example`). The placeholder `MY_GEMINI_API_KEY` is ignored at runtime, so builds never fail when no real key is set. Never commit a real `.env`.
- **App not grouping files as expected?** Long-press a collection → *AI tools* → re-run the smart grouping, or use *re-categorize* to let the parser + AI reclassify everything.
- **Anime files classified as movies?** The parser uses a franchise database + fansub heuristics; make sure the file name contains a recognizable franchise keyword or a fansub tag (e.g. `FarsiSub`, `[SubsPlease]`, `Erai-raws`).
- **Slow first scan:** the first scan fetches metadata from the network for every file. Give it a moment or reduce the library size; the Gemini key makes classification faster and adds Persian synopses.
- **No network / privacy:** user-entered API keys live only in the app's `SharedPreferences` on your device and are never uploaded anywhere.

---

## 📄 License

MIT — see [LICENSE](LICENSE) for details.
