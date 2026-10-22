#!/usr/bin/env bash
# Builds Calculator.apk without Gradle, using the Android SDK build tools.
#
# On Debian/Ubuntu the required tools can be installed with:
#   sudo apt-get install aapt android-sdk-platform-23 dalvik-exchange apksigner zipalign
#
# Override ANDROID_JAR / BUILD_TOOLS to use a different SDK installation.
set -euo pipefail

cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-/usr/lib/android-sdk}"
ANDROID_JAR="${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}"
BUILD_TOOLS="${BUILD_TOOLS:-$SDK/build-tools/debian}"
KEYSTORE="${KEYSTORE:-debug.keystore}"
OUT=build
APK=Calculator.apk

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "==> Generating R.java and packaging resources"
"$BUILD_TOOLS/aapt" package -f -m \
    -J "$OUT/gen" \
    -M AndroidManifest.xml \
    -S res \
    -I "$ANDROID_JAR" \
    -F "$OUT/unsigned.apk"

echo "==> Compiling Java"
javac --release 8 -nowarn -encoding UTF-8 \
    -classpath "$ANDROID_JAR" \
    -d "$OUT/classes" \
    $(find src "$OUT/gen" -name '*.java') 2>&1 | grep -v 'warning: \[options\]' || true
[ -n "$(find "$OUT/classes" -name '*.class')" ] || { echo "javac failed" >&2; exit 1; }

echo "==> Converting to dex"
"$BUILD_TOOLS/dx" --dex --output="$OUT/dex/classes.dex" "$OUT/classes"
(cd "$OUT/dex" && zip -q -u ../unsigned.apk classes.dex)

echo "==> Aligning"
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    echo "==> Creating debug keystore"
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi

echo "==> Signing"
apksigner sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
    --out "$APK" "$OUT/aligned.apk"
apksigner verify "$APK"

echo "Built $(pwd)/$APK"
