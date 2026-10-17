Stage 12 behaviour check, head 20589a7, Pixel_9_Pro emulator (Android 12+), demo debug APK. Evidence in qa-evidence/ (not committed).

| Criterion | Result | Evidence |
|---|---|---|
| Launcher icon: black arrow on lime, no "H" | PASS | drawer-clean.png (also drawer2.png) |
| Not clipped by the mask | PASS | drawer-clean.png, home-themed-try.png |
| "TailorMyResume dev" entry shows the same icon | PASS | drawer-clean.png (two arrow icons, labels truncated by the launcher; both activities resolve to com.tailormyresume.app) |
| Themed monochrome icon | PARTIAL | APK ic_launcher.xml declares a monochrome layer (aapt2). The emulator launcher did not render a themed icon after setting themed_icon_enabled=1, so the themed render is not verified on device. |
| Cold-start splash, dark mode | PASS | splash-dark-1.png, splash-dark-2.png: arrow on lime disc |
| Cold-start splash, light mode | FAIL | splash-light-r1..r3.png, splash-1..3.png: black arrow on white, no lime background. The splash theme sets no windowSplashScreenIconBackgroundColor, so the lime is missing in light mode. |

Note: an older com.hirehop.app install on the AVD showed "H" icons; it is a stale package, not this build, and was removed from the emulator.
