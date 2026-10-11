# PR 304 behaviour check (sha 72f84dcd, demoDebug, emulator-5554, 1011x2856 @480 = 337dp, font scale 2.0)

| Check | Action | Result | File |
|---|---|---|---|
| Build | `./gradlew assembleDemoDebug` in worktree issue-264 | succeeded | none |
| Launch, no crash | install, monkey launch, tapped Profile, Settings, Applications dock tabs | MainActivity resumed; no FATAL EXCEPTION; crash buffer empty (AndroidRuntime lines are monkey and uiautomator only) | launch.png, applications.png, profile.png, settings.png, crash.txt, fatal.txt |
| Existing screens | read the four screenshots | dock renders (lime active pill, grey inactive icons, visible outline); tab bodies are pure black placeholders, same as PR 303 evidence (#259 entries); nothing to judge at font 2.0 | same |
| Roborazzi dark images | delegate vision plus my own read | colour swatches: all labels whole, disabled grey readable (D32 textDisabled #7D7D7D); type specimen: contrast strong | theme_colour_swatches_prototype_dark.png, theme_type_specimen_prototype_dark.png |

Finding (low): in theme_type_specimen_prototype_dark.png the displayLarge sample line ("displayLarge 0123 Hamburgefons") breaks mid-word ("displayLar/ge", "Hamburgefo/ns"). It is a specimen test string wider than the canvas, not a component; no real screen uses it yet. D40 (labels never break mid-word) applies when a screen uses displayLarge; check it in the first W4 screen PR that does.
No font-200 image exists in this PR (tokens only).
