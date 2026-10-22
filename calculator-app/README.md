# Calculator (Android)

A simple native Android calculator app. **`Calculator.apk`** in this folder is a
ready-to-install, debug-signed build.

## Features
- `+ − × ÷`, percent (`%`), and smart parentheses (`( )` opens or closes as needed)
- Correct operator precedence, unary minus and implicit multiplication (`2(3+4)`)
- Live result preview while typing; exact decimal math (`0.1 + 0.2 = 0.3`)
- `⌫` deletes one character (long-press to clear), `AC` clears everything
- Dark theme; runs on Android 5.0 (API 21) and newer

## Install
Copy `Calculator.apk` to your phone and open it (allow "Install unknown apps"
when prompted), or use `adb install Calculator.apk`.

## Build from source
No Gradle needed — `build.sh` calls the SDK build tools directly. On Debian/Ubuntu:

```sh
sudo apt-get install aapt android-sdk-platform-23 dalvik-exchange apksigner zipalign
./build.sh
```

The script generates a debug keystore on first run (not committed).

## Layout
- `src/com/fswebdev/calculator/Evaluator.java` – expression parser/evaluator
- `src/com/fswebdev/calculator/MainActivity.java` – UI and input handling
- `res/` – layout, colours, styles, button and icon drawables
