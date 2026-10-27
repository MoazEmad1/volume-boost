# Volume Boost

A tiny Android TV app that makes the built-in speaker louder than the system's maximum. Made for cheap
projectors and TVs whose speaker is still quiet at volume 100.

- **Off, +3, +6, +9 or +12 dB**, picked with the remote.
- Boosts **everything**: YouTube, streaming apps, HDMI.
- **No internet permission, no ads, no tracking.** About 16 KB, four Java files, no libraries.
- Remembers the level and turns itself back on after a restart.

## How it works

It puts Android's own [`LoudnessEnhancer`](https://developer.android.com/reference/android/media/audiofx/LoudnessEnhancer)
effect on audio session 0, the global output mix, at the gain you picked, and keeps it there with a small
foreground service. Android logs that attaching it to the global mix is deprecated, but it still works on
Android 14.

The effect is a limiter, not a bigger amplifier: once the speaker is at full volume, extra gain lifts quiet
sounds (dialogue, soft music) and squashes loud peaks so they stay in range. That's why it's capped at +12 dB.
Past that, the sound mostly gets flatter, not louder, and a small speaker works harder all the time.

**Start low.** Try +3, then +6, +9 and +12 with something that has voices and bass. Keep the highest level that
still sounds clean; if anything crackles or buzzes, or the speaker gets unusually hot, go a step down.

## Install

You need adb access to the TV (Developer options, USB or network debugging).

```bash
adb connect <tv ip>:5555
adb install VolumeBoost.apk
```

Then open **Volume Boost** on the TV and pick a level. The APK is on the
[releases page](https://github.com/MoazEmad1/volume-boost/releases).

## Starting at boot

The app listens for `BOOT_COMPLETED` and restarts the boost at the saved level. Some firmware only delivers that
broadcast to its own system apps (the HY300 Pro+ below is one). There, have your launcher open the app once after
each boot: the app's TV entry point is an invisible starter that turns the boost on at the saved level and closes
with nothing on screen. With [cleanyourtv](https://github.com/Gathin23/cleanyourtv)'s launcher, set
`"open_on_start_choice": true` in its config and pick Volume Boost under *Launcher settings* → *Open on start*.

Opened while the boost is already running, the same entry point shows the level screen.

## From adb

Set the level without opening the app over what's playing. Only callers with the `DUMP` permission (the adb shell,
the system) can start the service this way:

```bash
adb shell am start-foreground-service -n io.github.moazemad1.volumeboost/.BoostService --ei db 9
adb shell am start-foreground-service -n io.github.moazemad1.volumeboost/.BoostService --ei db 0   # off
```

Right after a fresh install Android may refuse this (the log says `startForeground() not allowed`): open the app
once on the TV, or start it with `adb shell monkey -p io.github.moazemad1.volumeboost -c
android.intent.category.LEANBACK_LAUNCHER 1`, and it works from then on. The level is saved either way.

## Build

No Gradle. You need a JDK and the Android SDK (build-tools 34 or newer, platform android-34):

```bash
ANDROID_HOME=/path/to/android-sdk ./build.sh
```

The first run makes a signing key, `release.keystore`, with a random password in `keystore.properties`. Keep both
to sign later updates, and keep them out of git (`.gitignore` already does).

## Tested on

Magcubic HY300 Pro+ projector (Rockchip rk3566), Android 14, firmware Projector.20260817.160250: the speaker was
already at 100 of 100 with master volume at full, and +12 dB was clearly louder and still clean.

## Licence

[MIT](LICENSE)
