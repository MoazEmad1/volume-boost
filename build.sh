#!/usr/bin/env bash
# Builds VolumeBoost.apk without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Needs a JDK and the Android SDK (build-tools 34+, platform android-34); ANDROID_HOME points at the SDK.
# The first run makes a signing key, release.keystore, with a random password in keystore.properties.
# Keep both: an update must be signed with the same key. Neither belongs in git.
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] && [ -d "$SDK/build-tools" ] || { echo "Set ANDROID_HOME to your Android SDK." >&2; exit 1; }
BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
AJ="$SDK/platforms/android-34/android.jar"
[ -f "$AJ" ] || { echo "Missing platform: sdkmanager \"platforms;android-34\"" >&2; exit 1; }

if [ ! -f keystore.properties ]; then
  pass="$(od -An -tx1 -N16 /dev/urandom | tr -d ' \n')"
  keytool -genkeypair -keystore release.keystore -storepass "$pass" -keypass "$pass" -alias release \
    -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Volume Boost" >/dev/null 2>&1
  printf 'storePassword=%s\n' "$pass" > keystore.properties
  chmod 600 keystore.properties release.keystore
  echo "Made release.keystore and keystore.properties: keep both, and keep them out of git."
fi
PASS="$(sed -n 's/^storePassword=//p' keystore.properties)"

rm -rf out && mkdir -p out/classes
"$BT/aapt2" link -o out/base.apk -I "$AJ" --manifest AndroidManifest.xml
javac -nowarn -Xlint:-options -source 8 -target 8 -bootclasspath "$AJ:$BT/core-lambda-stubs.jar" \
  -d out/classes $(find src -name '*.java')
"$BT/d8" --release --min-api 26 --lib "$AJ" --output out $(find out/classes -name '*.class')
(cd out && cp base.apk unsigned.apk && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk
"$BT/apksigner" sign --ks release.keystore --ks-key-alias release --ks-pass "pass:$PASS" \
  --out VolumeBoost.apk out/aligned.apk
rm -f VolumeBoost.apk.idsig
echo "Built VolumeBoost.apk ($(du -h VolumeBoost.apk | cut -f1))"
