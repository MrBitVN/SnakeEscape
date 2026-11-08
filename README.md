# Snake Escape 🐍

Snake Escape is a multiplatform puzzle game for **Android** (Jetpack Compose) and **iOS** (SwiftUI), featuring organic snake movement, procedural puzzle generation with guaranteed solvability, run-based heart mechanics, online realtime leaderboards via Firebase, and custom sound effects.

---

## 📁 Repository Structure

```
snake-escape/
├── android/                    # Android Native Project (Kotlin + Jetpack Compose)
│   ├── app/                    # Compose UI, Audio, DataStore, Firebase Repositories
│   ├── logic/                  # Pure Kotlin Game Engine & Unit Tests
│   ├── gradle/                 # Gradle Wrapper
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── ios/                        # iOS Native Project (Swift + SwiftUI Canvas)
│   ├── Sources/                # SwiftUI Canvas UI, MVVM, Audio, Repositories
│   ├── Tests/                  # XCTest Unit Tests matching Android
│   ├── SnakeEscape.xcodeproj/  # Xcode Project
│   └── Package.swift           # Swift Package Manager Manifest
│
├── firestore.rules             # Production Firebase Firestore Security Rules
├── GAME_SPEC.md                # Comprehensive Game Specification & Algorithm Documentation
└── README.md
```

---

## 🎮 Core Game Mechanics & Rules

1. **Run & Hearts System**:
   - Each run begins at **Level 1** with **0 score** and **3 hearts**.
   - Each level is independent: entering a new level resets hearts to 3 and resets the countdown timer.
   - Dying or crashing into an obstacle or another snake deducts 1 heart immediately (3 ➔ 2 ➔ 1 ➔ 0).
   - If hearts remain, the board refreshes with the remaining hearts and timer restarts.
   - Losing all 3 hearts in a level ends the run immediately, bringing up the **Run Summary** screen.
   - Anti-cheat: Exiting the app mid-level penalizes 1 heart upon reopening.

2. **Scoring & Leaderboard**:
   - Level Score = `BaseScore(level) × SpeedMultiplier - Penalties`.
   - Real-time online global leaderboard powered by Firebase Firestore.
   - Atomic player name registration with case-insensitive uniqueness check.

3. **Boosters**:
   - **Hint**: Highlights the next correct snake capable of escaping.
   - **Grid Vision**: Reveals exit trajectories for all snakes.

---

## 🛠️ Build & Run

### Android
```bash
cd android
./gradlew :app:assembleDebug
```
Output APK is located in `app/build/outputs/apk/debug/app-debug.apk`.

### iOS
Open `ios/SnakeEscape.xcodeproj` in Xcode or use Swift Package Manager:
```bash
cd ios
swift test
```
