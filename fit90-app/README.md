# Fit90 — 90-day home calisthenics, diet & water tracker (Android)

**`Fit90.apk`** in this folder is a ready-to-install build (Android 5.0+, targets Android 15).

## What's inside
**Workout** — 90-day plan using only bodyweight, a pull-up bar and two dumbbells.
- Level 1 Beginner (days 1–30), Level 2 Intermediate (31–60), Level 3 Advanced (61–90)
- Weekly split: Push · Pull · Legs & Core · Push · Pull · Legs & Core · Rest
- Every exercise has an animated stick-figure demo, sets × reps, the equipment it needs and a form cue;
  tap to mark it done
- Targets rise automatically: reps or seconds go up each session (shown as "↑ +1 rep vs last time"),
  then reset to the harder exercises of the next level
- Warm-up, cool-down and a weekly progression tip

**Stamina** — daily pelvic-floor and stamina routine (10–15 min + cardio), 3 levels that follow the plan.
- Slow, quick, long-hold and "elevator" Kegels, glute bridges and squats with a pelvic squeeze,
  reverse-Kegel breathing, hip mobility and cardio
- Tap to mark each item; shows a daily streak and evidence-based notes (no size myths)

**Diet** — budget Indian meals for a 70 kg, 24-year-old maintaining weight while building muscle.
- About 120 g protein, about 2,400–2,570 kcal, at most ₹100 a day (all 6 menus: 121–127 g, ₹88–99)
- Veg / Non-veg switch you can flip any time; 3 rotating menus each
- Every item shows its quantity and protein; every meal its protein, kcal and cost; tap to mark eaten
- Prices used for the costs are listed in the app

**Water** — 2 L daily goal with +100 / +200 / +500 ml buttons, a progress ring, today's log and undo.
Resets automatically each day.

**Progress** — day X of 90, workouts done, streak, per-level progress and a 90-day grid
(done / partly / missed). Tap any day to open it. "Restart plan" sets today as day 1.

All data stays on the phone (no internet permission).

## Build from source
```sh
sudo apt-get install aapt android-sdk-platform-23 dalvik-exchange apksigner zipalign
./build.sh
```

## Code
- `src/com/fswebdev/fit90/Plan.java` – the 90-day workout program
- `src/com/fswebdev/fit90/Pelvic.java` – the daily pelvic-floor & stamina routine
- `src/com/fswebdev/fit90/Diet.java` – foods, prices and menus (totals are computed from these)
- `src/com/fswebdev/fit90/Store.java` – saved progress, diet choice and water log
- `src/com/fswebdev/fit90/MainActivity.java` – the four screens
- `src/com/fswebdev/fit90/Anim.java`, `AnimView.java` – exercise animations (poses + two-bone IK)
- `src/com/fswebdev/fit90/RingView.java` – water progress ring
