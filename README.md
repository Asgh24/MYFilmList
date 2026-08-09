<div align="center">

# 🎬 MYFilmList

**A smart local media manager for your Movies, Series, and Anime — with automatic metadata, AI-powered recommendations, and zero manual tagging.**

Point it at your local video folders. It parses filenames, fetches real metadata from **TMDB** and **AniList**, builds a clean poster-grid library, and launches anything in your favorite external player — all on-device.

[![Platform](https://img.shields.io/badge/platform-Android-3ddc84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/built_with-Kotlin-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![License](https://img.shields.io/badge/license-see_LICENSE-blue?style=for-the-badge)](#-license)
[![Stars](https://img.shields.io/github/stars/Asgh24/MYFilmList?style=for-the-badge&color=yellow)](https://github.com/Asgh24/MYFilmList/stargazers)

</div>

---

## ⭐ Why MYFilmList?

If your Movies/Series/Anime folder is a mess of `S01E04.1080p.WEBRip.x264-GROUP.mkv`-style filenames with no posters, no ratings, and no organization — **MYFilmList fixes that automatically.**

No manual tagging, no cloud server, no subscription. It reads your local files, figures out what they actually are, pulls real posters/synopses/ratings from TMDB and AniList, and gives you a proper library you can browse — then hands playback off to any external player you already use.

If this project saves you time organizing your local media collection, **a ⭐ star helps a lot** — it's the main way other people discover it.

## ✨ Features

- **🔍 Smart filename parsing** — automatically detects title, year, season/episode, and resolution/release-group noise from messy local filenames, for movies, TV series, *and* anime
- **🎞️ TMDB metadata retrieval** — posters, backdrops, synopses, cast, and ratings for movies and series
- **📺 AniList metadata retrieval** — accurate matching and rich metadata for anime, including alternate/romanized titles
- **📂 100% local-first library** — your files never leave your device; MYFilmList only calls out to fetch metadata
- **▶️ External playback via Android intents** — plays through the media player you already have installed (VLC, MX Player, etc.) instead of a bundled player
- **🤖 AI-powered recommendations** — get "what to watch next" suggestions based on what's actually in your library
- **🗂️ Unified library view** — movies, series, and anime organized together with posters, not folders of filenames

## 📱 Screenshots

<!-- Add a few screenshots or a short GIF here — this is the single biggest thing for attracting stars.
<p align="center">
  <img src="docs/screenshots/library.png" width="250" />
  <img src="docs/screenshots/details.png" width="250" />
  <img src="docs/screenshots/recommendations.png" width="250" />
</p>
-->

## 🛠️ Tech Stack

- **Kotlin** + Android Gradle build (`build.gradle.kts`)
- **TMDB API** — movie & series metadata
- **AniList API** (GraphQL) — anime metadata
- **Android Intents** — delegates playback to external player apps instead of reinventing a video player

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable)
- A [TMDB API key](https://www.themoviedb.org/settings/api) (free)
- An [AniList API client](https://anilist.co/settings/developer) (free)

### Setup

```bash
git clone https://github.com/Asgh24/MYFilmList.git
cd MYFilmList
cp .env.example .env
# then fill in your TMDB and AniList API credentials in .env
```

Open the project in Android Studio, let Gradle sync, and run it on a device or emulator.

## 📖 How It Works

1. **Scan** — point MYFilmList at your local movie/series/anime folders on-device.
2. **Parse** — filenames are parsed to extract title, year, and season/episode info, stripping out release tags and junk.
3. **Match & enrich** — parsed titles are matched against TMDB (movies/series) or AniList (anime) to pull posters, synopses, and ratings.
4. **Browse** — your files are shown as a clean, organized library instead of a raw file list.
5. **Play** — tapping a title fires an Android intent to open it in your installed external player.
6. **Discover** — the recommendation engine suggests what to watch next based on your existing library.

## 🤝 Contributing

Issues and pull requests are welcome — whether it's a filename-parsing edge case, a metadata-matching bug, or a new feature idea.

## 📄 License

See [LICENSE](./LICENSE) for details.

## ⭐ Support the Project

If MYFilmList helped you get your local media collection organized, please consider giving the repo a **star** — it's free, takes two seconds, and is the best way to help this project reach more people.

<div align="center">

**[⭐ Star this repo](https://github.com/Asgh24/MYFilmList/stargazers) · [🐛 Report a bug](https://github.com/Asgh24/MYFilmList/issues) · [💡 Request a feature](https://github.com/Asgh24/MYFilmList/issues)**

</div>
