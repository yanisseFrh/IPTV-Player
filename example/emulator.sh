#!/usr/bin/env bash
# Crée (si besoin) et démarre l'AVD Android TV local, puis attend la fin du boot.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

AVD="${AVD_NAME:-IPTVPlayer}"
IMAGE="system-images;android-28;android-tv;x86"   # API 28 = version de la Mi Box
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
LOG="${TMPDIR:-/tmp}/iptvplayer-emulator.log"

if ! "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: ${AVD}$"; then
    echo "[emulator] Installation de l'image système ${IMAGE}..."
    yes | "$SDKMANAGER" --sdk_root="$SDK" "$IMAGE" >/dev/null || true
    echo "[emulator] Création de l'AVD ${AVD} (Television 1080p)..."
    echo no | "$AVDMANAGER" create avd -n "$AVD" -k "$IMAGE" -d tv_1080p >/dev/null
fi

# 1.5 Go suffisent pour ce launcher et laissent de la RAM à l'hôte
CFG="$HOME/.android/avd/${AVD}.avd/config.ini"
sed -i 's/^hw\.ramSize *=.*/hw.ramSize=1536/' "$CFG"
awk '!seen[$0]++' "$CFG" >"${CFG}.tmp" && mv "${CFG}.tmp" "$CFG"
sed -i 's/^hw.keyboard *=.*/hw.keyboard = yes/' "$CFG"

if ! adb devices | grep -q "^emulator-[0-9]"; then
    echo "[emulator] Démarrage de ${AVD} (log: ${LOG})..."
    nohup "$SDK/emulator/emulator" -avd "$AVD" \
        -no-boot-anim -no-audio \
        -gpu "${EMU_GPU:-swiftshader_indirect}" \
        -netdelay none -netspeed full >"$LOG" 2>&1 &
fi

echo "[emulator] Attente du boot..."
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
done
adb shell input keyevent 3 >/dev/null 2>&1 || true
echo "[emulator] Prêt."
