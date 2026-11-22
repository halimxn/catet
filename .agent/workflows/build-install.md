---
description: Build APK rilis catet, pasang ke HP lewat adb, lalu ambil tangkapan layar
---

1. Cek kompilasi cepat:
   `cd /d/Koding/Catet && export JAVA_HOME="D:/AndroidDev/jdk17" && /d/AndroidDev/gradle-8.9/bin/gradle compileReleaseKotlin --no-daemon -q`
2. Build APK rilis (6-10 menit):
   `/d/AndroidDev/gradle-8.9/bin/gradle assembleRelease --no-daemon -q`
3. Pasang:
   `D:/AndroidDev/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/release/app-release.apk`
   Di HP OPPO, ketuk "Lanjutkan pemasangan" (sekitar 360,1368).
4. Buka aplikasi:
   `adb shell am start -n id.kantong.app/.MainActivity`
5. Sebelum setiap `adb shell input tap/swipe`, cek bahwa `adb shell dumpsys window | grep mCurrentFocus` berisi `id.kantong.app`. Kalau tidak, berhenti.
6. Ambil tangkapan layar:
   `adb exec-out screencap -p > layar.png`
   Bandingkan dengan file di `desain/`.
7. Salin APK ke `catet-<versi>.apk` di root proyek.
