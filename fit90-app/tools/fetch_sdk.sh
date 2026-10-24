#!/usr/bin/env bash
# Downloads the Android framework jars the build compiles and links against.
#
# Google's SDK servers are not needed: Robolectric publishes complete Android
# framework jars on Maven Central. We use
#   - android-all 13 (API 33): its framework resource table links the app's
#     resources and manifest (aapt cannot read the API 34+ table format), and
#   - android-all 15 (API 35): its classes are the compile classpath.
# Only public SDK APIs may be used; these jars also contain hidden APIs.
set -euo pipefail

SDK_CACHE="${SDK_CACHE:-$HOME/.cache/fit90-sdk}"
MAVEN="${MAVEN_URL:-https://repo.maven.apache.org/maven2}"
RES_VERSION="13-robolectric-9030017"
CLASSES_VERSION="15-robolectric-12650502"

mkdir -p "$SDK_CACHE"

fetch() {
    local version="$1" out="$2"
    if [ ! -s "$out" ]; then
        echo "==> Downloading android-all $version"
        curl -fSL --retry 4 -o "$out.part" \
            "$MAVEN/org/robolectric/android-all/$version/android-all-$version.jar"
        mv "$out.part" "$out"
    fi
}

if [ ! -s "$SDK_CACHE/android-33-res.jar" ]; then
    fetch "$RES_VERSION" "$SDK_CACHE/android-all-13.jar"
    tmp="$(mktemp -d)"
    (cd "$tmp" && unzip -q "$SDK_CACHE/android-all-13.jar" AndroidManifest.xml resources.arsc \
        && zip -q "$SDK_CACHE/android-33-res.jar" AndroidManifest.xml resources.arsc)
    rm -rf "$tmp" "$SDK_CACHE/android-all-13.jar"
fi
fetch "$CLASSES_VERSION" "$SDK_CACHE/android-all-15.jar"
echo "$SDK_CACHE"
