# PR 303 behaviour check (sha eed859e9, demoDebug, emulator-5554, 1280x2856 @480)

| Check | Action | Result | File |
|---|---|---|---|
| No white flash, light system mode | pm clear, cold start while screenrecording, 8 fps strip | launcher, then black splash with lime icon, then black window; no white frame | night-no-strip.png, night-no-cold.mp4, night-no-launch.png |
| No white flash, dark system mode | same | same | night-yes-strip.png, night-yes-cold.mp4 |
| System bar icons | read screenshots in both modes | clock, notification, wifi, signal, battery icons and nav pill are white on black | night-no-launch.png, night-yes-after.png |
| Dev tools screen dark, default font | opened DebugScenarioActivity, loaded sample data | dark cards, white/grey text readable, lime buttons, default sans (no Manrope/Archivo) | night-yes-devtools.png |
| Applications, Profile, Settings | Open TailorMyResume, tapped each dock tab | dock renders dark with lime active pill and readable labels; tab content is pure black (pixel extrema 0,0,0) because the entries are BasicText placeholders (ApplicationsEntryProvider etc., #259) with default black text on the dark window: invisible | night-yes-home.png, night-yes-profile.png, night-yes-settings.png |
| Crash | logcat -b crash after all steps | no com.tailormyresume entry (only a uiautomator dump self-collision) | crash-no.txt, crash-yes.txt |
