## AI DISCLOSURE

This project was created entirely with AI. The reverse-engineering of BYD's media system, the car link helper app, the GitHub Actions build workflows and this README were all produced by **Claude Opus 5.5** (via Microsoft Copilot), with testing on the vehicle done by me (repo owner).

# Media Bridge for BYD

Use open-source music players as **built-in media sources** on BYD head units, with no root needed.

When set up, music from a player built in this repo shows up the same way as BYD's supported streaming apps:

- Song title, artist and album art on the **home-screen media widget**
- Now-playing info on the **driver's display (instrument cluster)**
- **Steering-wheel controls** and the widget's play/pause/next/previous buttons
- **Resume playback** when the car starts, if music was playing when it was switched off

This repo builds everything from one place:

| Workflow | Builds | Release |
|---|---|---|
| `auxio.yml` | [Auxio](https://github.com/OxygenCobalt/Auxio) as `com.aspiro.tidal` | `auxio-byd-<version>-<build>` |
| `retro.yml` | [Retro Music](https://github.com/RetroMusicPlayer/RetroMusicPlayer) as `deezer.android.app` | `retro-byd-<version>-<build>` |
| `helpers.yml` | Both car link helpers, from `app/` in this repo | `helper` (one release, always updated) |

The player workflows build from each project's **latest official stable release**, check for a new release **every week**, and also run when their workflow file changes. The helpers rebuild whenever the helper code changes. Personally, I have found that Retro Music Player works best overall, although the currently playing UI is inferior to Auxio.

## How it works

BYD's media app (`com.byd.localmusic`) only shows now-playing info for a fixed set of partner apps. One group of partner apps, which BYD's code calls the **FCE** group, is connected through a standard Android media service. Each slot in that group is defined by two package names:

- **The player package**: the app that plays the audio. When an app with this package name takes audio focus, the car switches to that slot.
- **The helper package** (`com.fce.onlinemedia.*`): the slot is only switched on if this app is installed and has a launchable entry. BYD's media app connects to a service inside it called `com.fce.onlinemedia.voice.CarMediaTransportService`.

This repo fills both halves:

- **The player** is an open-source music player rebuilt from source with a borrowed player package name. Its music code isn't modified. Only the package name, app name and signing key change.
- **The car link helper** provides the service BYD connects to. It connects to the player, passes song info and album art to the car, and passes the car's controls back to the player.

## Available slots (FCE whitelist)

These are the generic FCE slots found in `BydLocalMedia.apk` on the tested firmware. Any of them can be used by building a player under the **player package** and a helper under the matching **helper package**.

| Slot | Shows in the car as | Player package (use as `applicationId`) | Helper package | Cluster ID | Used here |
|---|---|---|---|---|---|
| 105 | FLO | `skplanet.musicmate` | `com.fce.onlinemedia.musicmate` | 33 | |
| 106 | Audials | `com.audials` | `com.fce.onlinemedia.audials` | 41 | |
| 107 | Audible | `com.audible.application` | `com.fce.onlinemedia.audible` | 44 | |
| 108 | Bloomberg | `com.bloomberg.android.plus` | `com.fce.onlinemedia.bloomberg` | 49 | |
| 109 | Deezer | `deezer.android.app` | `com.fce.onlinemedia.deezer` | 34 | ✅ Retro Music |
| 110 | iHeartRadio | `com.clearchannel.iheartradio.controller` | `com.fce.onlinemedia.iheartradio` | 40 | |
| 111 | Libby | `com.overdrive.mobile.android.automotive.libby` | `com.fce.onlinemedia.libby` | 43 | |
| 112 | Melon | `com.iloen.melon` | `com.fce.onlinemedia.melon` | 36 | |
| 113 | Pocket Casts | `au.com.shiftyjelly.pocketcasts` | `com.fce.onlinemedia.pocketcasts` | 39 | |
| 114 | Radioline | `com.radioline.android.radioline.auto` | `com.fce.onlinemedia.radioline` | 42 | |
| 115 | Tidal | `com.aspiro.tidal` | `com.fce.onlinemedia.tidal` | 35 | ✅ Auxio |
| 116 | TuneIn | `tunein.player` | `com.fce.onlinemedia.tunein` | 37 | ⚠️ Avoid |

Notes:

- **Avoid slot 116 (TuneIn).** BYD's focus handling checks `tunein.player` first and sends it to a separate TuneIn path (slot 117). It likely never reaches the generic slot.
- **The car shows the slot's own name and logo.** A player in the Tidal slot appears as "Tidal", and this can't be changed.
- **Never install the real app for a slot you're using.** It has the same package name and will conflict.
- **Slot 111 "Libby" is OverDrive Inc.'s library audiobook app**, unrelated to the OverDrive BYD tool.
- **This list comes from one firmware version.** A BYD update could add, remove or change slots.

### Slots that can't be borrowed

These sources use their own special connections, not the FCE service, so a helper app can't fill them:

| Slot | Source | Connection |
|---|---|---|
| 101 | Android Auto | BYD's projection service |
| 102 | CarPlay | BYD's projection service |
| 103 | Spotify | Vendor SDK (`com.telenav.scoutivi.spotify.byd`) |
| 104 | Amazon Music | Broadcasts to `com.android.car.media` |
| 117 | TuneIn | Separate `com.byd.tunein` path |

### Adding another player

1. Pick an unused slot from the table above.
2. Copy `auxio.yml` or `retro.yml`, then change `UPSTREAM` and `APP_NAME`, and set `NEW_ID` to the slot's **player package**.
3. Add an entry to the `matrix` in `helpers.yml` with the slot's **helper package**, the same **player package**, a label and an output file name.

The player must expose a standard Android `MediaBrowserService`, because that's what the helper connects to.

## Tested vehicles

| Vehicle | DiLink Version | System Version | Auxio (Tidal slot) | Retro Music (Deezer slot) |
|---|---|---|---|---|
| BYD Sealion 7 |DiLink 5|Yet to find| ✅ Working | ✅ Working |

If you try it on another BYD model, please open an issue with the result.

## Installation

You need ADB access to the head unit. Each player needs **its own helper**.

1. Download the APKs from this repo's **Releases** page:
   - **Auxio:** `Auxio-for-BYD.apk` and `Auxio-car-link.apk` (from the **Car link helpers** release)
   - **Retro Music:** `Retro-for-BYD.apk` and `Retro-car-link.apk` (from the **Car link helpers** release)
2. Install them, for example:
   ```bash
   adb install -r Auxio-for-BYD.apk
   adb install -r Auxio-car-link.apk
   ```
3. Restart BYD's media app so it detects the helper:
   ```bash
   adb shell am force-stop com.byd.localmusic
   ```
4. Open the player, let it scan your music, and play a song.

Both players can be installed at once. Play whichever one you want and the car switches to its slot.

To update, install the newer APK with `adb install -r`. Your library and settings are kept, because every build is signed with the same key.

### Checking it works

```bash
adb logcat -c
adb logcat | grep -iE "isPKGInstall|setMusicEngine|FceBridge"
```

While a song plays, look for `isInstall = true` for the helper, `setMusicEngine succeed! type = <slot>`, and `FceBridge: Connected to player`.

## Known issues

- **No folder selection.** The head unit has no system folder picker, so in Auxio, this setting can't be used. Auxio uses Android's full system scan instead. Retro can selectively only scan the music folder.
- **Library reload on start (Auxio).** Auxio rebuilds its library each time it launches, so there's a short delay after the car starts. Playback position is remembered.
- **Playback Position absent on cluster**. It doesn't show how far through a song you are on the cluster. This has yet to be resolved.
- **Separate apps.** The renamed players are separate from the regular versions, so they start with an empty library and no playlists.
- **Renamed-package side effects.** A feature that relies on the player's original package name, such as a home-screen widget, could break. Playback shouldn't be affected.
- **The helper may appear in the app launcher.** BYD needs the helper to have a launchable entry.
- **Firmware updates may break it.** A BYD software update could change how the media app handles partner apps.
- **Updates are manual.** The car can't download updates from GitHub. Each new build has to be installed over ADB.

## Building it yourself

1. Fork or copy this repo.
2. Create a signing key and add it as the `KEYSTORE_B64` secret (**Settings → Secrets and variables → Actions**):
   ```bash
   keytool -genkeypair -keystore byd.keystore -storepass android -alias androiddebugkey \
     -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=BYD"
   base64 -w0 byd.keystore
   ```
3. Set **Settings → Actions → General → Workflow permissions** to **Read and write permissions**.
4. Run each workflow from the **Actions** tab.

Keep the keystore safe. Updates must be signed with the same key, or they can't install over the previous version.

## Disclaimer

This is an unofficial personal project. It is not affiliated with or endorsed by BYD, any streaming service named above, or the developers of Auxio or Retro Music. Use it at your own risk, and don't operate the head unit in ways that distract you while driving.

## Licence

Auxio and Retro Music are licensed under the **GNU GPL v3.0**, and the builds of them published here are distributed under the same licence. All credit for the music players goes to their developers and contributors.

## AI disclosure

This project was created entirely with AI. The reverse-engineering of BYD's media system, the car link helper app, the GitHub Actions build workflows and this README were all produced by **Claude Opus 5.5** (via Microsoft Copilot), with testing on the vehicle done by the repo owner.
