# Gym Buddy 💪

A polished, **offline-first** Android app for browsing **1,324 exercises** and running a
**personalized workout tracker** — built on the open
[`hasaneyldrm/exercises-dataset`](https://github.com/hasaneyldrm/exercises-dataset).

> **Also in this repo:** [Gym Buddy Web](#gym-buddy-web-pwa) — an installable PWA companion
> for a fixed 6-day split, deployed to GitHub Pages from [`web/`](web/).

The full exercise catalogue ships **inside the app**, so it works with no network on first
launch. Animations/thumbnails are cached the first time you view them (kept permanently), and you
can optionally **download everything (~130 MB) for full offline use**. A built-in updater pulls the
**latest dataset from GitHub** on demand.

## Features

### Personalized
- **Onboarding** captures your profile (sex, age, height, weight, goal, experience, days/week,
  available equipment, units).
- **Tailored plan generator** builds a split (full-body / upper-lower / push-pull-legs) with
  exercises filtered to your equipment and rep schemes matched to your goal.

### Plans
- Create plans automatically (*"Generate for me"*) or from scratch.
- Edit days & exercises, tune sets/reps/rest, and **swap any exercise in one tap**.
- Mark a plan active; the home screen rotates to your next workout.

### Tracking
- Log sets with **reps + weight**, tick them off, rest timer auto-starts between sets.
- **Calorie-burn estimation** (ACSM MET formula using your body weight).
- **Adaptive progressive overload** suggests next-session targets from recent performance.
- **Personal records & estimated 1RM** (Epley/Brzycki) with PR celebrations.
- **Streaks, weekly goal ring, achievements/badges**.

### Insight & extras
- **Progress dashboard**: weekly volume bars, body-weight trend, PRs, achievements (custom
  Compose charts — no heavy chart deps).
- **Body-weight logging**, **daily workout reminders**, **Health Connect** sync (workouts,
  calories, weight), and a **home-screen widget** (Jetpack Glance) showing your streak.
- Material 3 with dynamic color, light/dark, English / Türkçe / Italiano instructions.

## Architecture

- **Kotlin + Jetpack Compose (Material 3)**, single-Activity, Compose Navigation.
- **Room** (local store for the dataset *and* all user data) — verified at compile time via KSP.
- **Retrofit + OkHttp + kotlinx.serialization** for the GitHub update check / download.
- **Coil** (with GIF support) backed by a large, non-expiring disk cache for offline media.
- **DataStore** for settings, **WorkManager** for the bulk media download + reminders.
- **Manual DI** via a single `AppContainer` (no Hilt). MVVM with `ViewModel` + `StateFlow`.

```
app/src/main/java/com/gymbuddy/
  data/        local (Room), remote (GitHub), repo, model, seed, health
  domain/      PlanGenerator, CalorieCalculator, OneRepMax, ProgressionEngine, MET, Streak
  media/       Coil loader + bulk-download worker
  ui/          theme, nav, onboarding, home, browse, detail, plan, workout, progress, profile
  widget/      Glance home-screen widget
```

The bundled snapshot lives at `app/src/main/assets/exercises.json`; its source commit is recorded
in `BuildConfig.BUNDLED_DATASET_SHA` and compared against GitHub for updates.

## Build

Requirements: Android SDK 35, JDK 17+.

```bash
# create local.properties with: sdk.dir=/path/to/Android/sdk
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
Minimum Android 8.0 (API 26, required by Health Connect), target API 35.

## Gym Buddy Web (PWA)

The [`web/`](web/) folder contains a **zero-build, installable PWA**, designed to be used
*at the gym*, built around a default **6-day workout split** (Chest+Shoulders · Back+Biceps ·
Legs+Core · Chest+Triceps · Back+Shoulders · Arms+Legs · Rest) that's fully customizable:

- **Today view** auto-selects the day's workout; every exercise has an **animated
  illustration and step-by-step instructions** from the same dataset.
- **Customizable plan**: change the number of training days per week and the start
  weekday, reorder/add/remove days from templates or blank, and **swap or add any
  exercise on any day** — edits persist and drive tracking immediately.
- **Library tab**: all **1,324 exercises**, grouped by muscle, with instant **fuzzy
  search** — tap any result for its steps and illustration. The full catalog is loaded
  into IndexedDB **lazily** (only the first time you open Library or the exercise
  picker), so first load stays fast.
- **Set-by-set tracking** (weight × reps) with prefill from your last session, a
  **rest countdown** (vibration + beep), a screen wake-lock, and a **progression hint**
  (top of rep range on all sets → +2.5%).
- **History & backup**: sessions in `localStorage`, JSON export/import.
- **Install prompt**: proactively offers "Add to Home Screen" so it opens full-screen
  like a native app (with an iOS Safari fallback hint).
- **100% offline** once loaded — the service worker precaches the app shell and the 34
  curated GIFs (~3 MB); the full library's data/media are cached as you browse them.

**Live app:** https://aswinavofficial.github.io/gym-buddy/ — open on your phone and
"Add to Home screen" to run it like a native app.

Deployed automatically on every merge to `main` by
[`.github/workflows/pages.yml`](.github/workflows/pages.yml). To run locally:
`cd web && python3 -m http.server 8080`.

## Attribution

Exercise text, images and animations come from
[`hasaneyldrm/exercises-dataset`](https://github.com/hasaneyldrm/exercises-dataset) and are used for
**educational, non-commercial** purposes. Only the JSON snapshot is bundled; media is downloaded at
runtime. Calorie figures are MET-based **estimates**.
