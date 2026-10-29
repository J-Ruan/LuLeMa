#!/usr/bin/env bash
set -Eeuo pipefail
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sudo apt-get update
sudo apt-get install -y --no-install-recommends libpulse0 libasound2t64 libnss3 libx11-xcb1 libxcb-cursor0 libxcomposite1 libxcursor1 libxi6 libxtst6 libvulkan1
sudo chmod 666 /dev/kvm
export ANDROID_AVD_HOME="$RUNNER_TEMP/lulema-avd"
mkdir -p "$ANDROID_AVD_HOME"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools' 'emulator' 'system-images;android-30;default;x86_64'
echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd --force --name ci --path "$ANDROID_AVD_HOME/ci.avd" --package 'system-images;android-30;default;x86_64'
test -s "$ANDROID_AVD_HOME/ci.avd/config.ini"
printf 'avd.ini.encoding=UTF-8\npath=%s\ntarget=android-30\n' "$ANDROID_AVD_HOME/ci.avd" > "$ANDROID_AVD_HOME/ci.ini"
"$ANDROID_HOME/emulator/emulator" -avd ci -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect > emulator.log 2>&1 &
emulator_pid=$!
sleep 3
kill -0 "$emulator_pid"
timeout 120 adb wait-for-device
for i in $(seq 1 180); do
  if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then break; fi
  kill -0 "$emulator_pid"
  sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
adb shell input keyevent 82
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
