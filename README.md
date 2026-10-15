# OwnFit

A fast, minimal Material 3 home-workout app for Android — with or without dumbbells.

## Features

- **Coach-style onboarding** — asks gender, age, height, weight, goal, experience level,
  equipment, injuries/operations, yoga & breathing preferences and schedule, then builds a weekly plan.
- **Safe by design** — every exercise is tagged with equipment, level, impact and the injuries it's
  unsuitable for. Plans never include moves that load an injured area; high-impact moves are dropped
  for knee/ankle/heart/pregnancy/post-surgery, age 60+ or high BMI.
- **Freehand or equipment** — pick exactly what you own (pair/single/adjustable dumbbells, kettlebell,
  band, pull-up bar, bench/chair, mat).
- **Train any body part** — Full body, Upper, Lower, Chest, Back, Shoulders, Arms, Core, Legs & glutes,
  Cardio, Yoga or Breathe, for 5–60 minutes, with a shuffle button.
- **Yoga & breathing** — dedicated yoga flows and guided breathing (box, 4-7-8, coherent…) with an
  animated breathing circle. Toggle them in Settings and the plan is rebuilt.
- **Guided player** — timers, rest countdowns, voice coach (TTS), 3-2-1 beeps, haptics, screen stays on.
- **Adapts to you** — "Too easy / Just right / Too hard" after each workout nudges future volume.
- **Your own workouts** — build routines from 120+ exercises, or create custom exercises.
- **Your own demos** — attach a YouTube link (plays inline) and/or a GIF/image (URL or from your phone)
  to any exercise; it's shown during the workout.
- **Real exercise demos** — 80 exercises show real-person start/end photos looping as a demo; the rest
  (mostly yoga, breathing and a few cardio moves) use a built-in animated figure. A small muscle map
  shows what each move targets. You can still attach your own GIF/image or YouTube link to any exercise.
- **Voice coach** — choose **Alex** (male voice) or **Maya** (female voice) from the home screen. Natural
  pre-recorded voices announce each move, reps or time, sides, countdowns, halfway cheers and breathing
  cues; music ducks while the coach talks.
- **Background music** — four bundled royalty-free tracks or your own songs; play/pause and skip during workouts.
- **Progress** — streak, weekly goal, minutes, calories estimate, 16-week heatmap, history.
- **Git sync** — back up and sync everything to any HTTPS git repo (see below).
- **Optional AI coach** — with your Anthropic API key, Claude designs the plan (choosing only from
  exercises that passed the on-device safety filter). Without a key, a fast on-device coach does it.
- Gruvbox dark / light theme (quick toggle on the home screen, or follow the system), daily motivational quote.
- Choose the app name shown in your launcher (presets, or any name as a home-screen shortcut).

## Git sync

Settings → *Sync with git*:

1. Create a **private** repo (e.g. `fitness-data`) on GitHub/GitLab/Gitea.
2. Create a personal access token with write access to that repo
   (GitHub: fine-grained token → *Contents: Read and write*).
3. Enter the HTTPS URL, branch, username and token, then **Sync now**. Enable *Auto-sync* to sync after
   each workout.

Data is stored as plain JSON under `ownfit/` in the repo (`profile.json`, `plan.json`, `workouts.json`,
`custom_exercises.json`, `media.json`, `history.json`, `media/`). Syncs merge per item (newest edit wins,
history is a union), so several phones can share one repo without merge conflicts. Tokens and API keys
are stored only on the device and never committed.

## Build

Requirements: JDK 17 and the Android SDK (or just open the project in Android Studio).

```bash
./gradlew :app:assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk` (signed with the debug key so it
installs directly — set up your own signing config before publishing). CI builds it on every push
(see *Actions → Build APK → artifacts*).

```bash
./gradlew :app:testDebugUnitTest
```

## Tech

Kotlin · Jetpack Compose · Material 3 · kotlinx.serialization (JSON files, no database) · Coil (GIFs) ·
JGit (sync) · Anthropic Java SDK (optional AI). minSdk 26.

## Regenerating assets

- Exercise photos: `python scripts/fetch_exercise_photos.py` (mapping lives in that script).
- Voices: `python scripts/gen_voices.py <piper dir>` ([Piper](https://github.com/rhasspy/piper)).
- Animated-figure keyframes: `app/src/main/java/com/ownfit/app/anim/Motions.kt`; review with
  `./gradlew :app:testDebugUnitTest --tests "com.ownfit.app.anim.*"` then `python scripts/render_motions.py`.

## Credits

- Exercise photos: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense / public
  domain). Note: the dataset does not document the photos' original source — review before publishing commercially.
- Voices: Piper TTS — `en_US-joe-medium` (CC0) and `en_US-kristin-medium` (public domain).
- Music: Kevin MacLeod ([incompetech.com](https://incompetech.com)) — *Werq*, *Volatile Reaction*,
  *Movement Proposition*, *Meditation Impromptu 01* — licensed under
  [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

## Disclaimer

Exercise suggestions are not medical advice. If you have an injury, heart condition, are pregnant or
recovering from surgery, get clearance from a professional and stop if anything hurts.
