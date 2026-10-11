# Manual emulator testing

Use the `demo` build for offline testing. Use the configured `prod` build for real Google sign-in and backend testing. Both debug builds have two launcher entries for the same installed app:

- **TailorMyResume** opens the normal app.
- **TailorMyResume dev** opens the developer menu with sample data and forced screen states.

The launcher can shorten both labels to the same text. Release builds have only the normal launcher entry. For real backend testing, open the normal app and sign in with Google.

## Restore automatic story playback

Instrumented tests disable Android animations. If an emulator still has animations disabled, the app uses reduced motion. The sign-in story stays on its current page and its progress bar stays still. Story taps still work. See the motion rules in `AGENTS.md`.

Before a manual motion check, list the connected devices:

```sh
adb devices -l
```

Use the emulator serial from that list. If its animator duration scale is `0` or `0.0`, restore normal animation speed:

```sh
serial=emulator-5554
adb -s "$serial" shell settings get global animator_duration_scale
adb -s "$serial" shell settings put global animator_duration_scale 1.0
adb -s "$serial" shell settings put global transition_animation_scale 1.0
adb -s "$serial" shell settings put global window_animation_scale 1.0
```

These commands change Android settings for the selected emulator. Keep animations disabled when you test reduced motion.

After you change the settings, restart the app. The app reads the reduced-motion setting when it creates the theme:

```sh
adb -s "$serial" shell am force-stop com.tailormyresume.app
adb -s "$serial" shell am start -W -n com.tailormyresume.app/.MainActivity
```

On the sign-in screen, check that the current progress bar fills. Without a tap or hold, check that the story advances every five seconds through pages 1, 2, and 3, then returns to page 1. Hold the story artwork to pause. Release it to resume. Tap the left third to go back. Tap the rest of the artwork to go forward.
