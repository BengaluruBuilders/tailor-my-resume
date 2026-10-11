# PR 306 behaviour check (sha 22622eea, demoDebug, emulator-5554, 1011x2856 @480 = 337dp, font scale 2.0)

| Check | Action | Result | File |
|---|---|---|---|
| Build | `./gradlew assembleDemoDebug` in worktree issue-266 | succeeded | none |
| Launch, no crash | install, monkey launch, tapped the three dock tabs | MainActivity resumed; no FATAL EXCEPTION (the one logcat hit is the app-died line from my own `am force-stop` before relaunch) | launch.png, applications.png, profile.png, settings.png, fatal.txt |
| Existing screens | read screenshots | dock renders; tab bodies are black placeholders as in PR 303 evidence; nothing to judge at font 2.0 | same |
| Font-200 images | vision | hero card at 2.0: eyebrow and title whole, no ellipsis, no clipping; primary button "Looks right, continue" wraps to two centred lines inside the pill, whole | hero_card_blue_font200_prototype-font-200_dark.png, input_button_primary_prototype-font-200_dark.png |
| Dark images | vision | amber hero card, amber and default fields, toggle, upload card, selected choice: text whole, nothing clipped, outlines visible. Default-field placeholder and unselected-grey text are the dimmest but legible (D32 boundary token) | hero_card_amber_prototype_dark.png, input_field_amber_prototype_dark.png, input_field_default_prototype_dark.png, input_toggle_off_prototype_dark.png, input_upload_card_prototype_dark.png, input_choice_selected_prototype_dark.png, hero_paige_home_prototype_dark.png, hero_paige_paywall_prototype_dark.png |

Vision remark that hero_paige_home and hero_paige_paywall are identical is wrong: the files differ (cmp). No findings.
