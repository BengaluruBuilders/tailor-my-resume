# PDF import regression corpus

Issue: [#325](https://github.com/BengaluruBuilders/tailor-my-resume/issues/325).

The corpus contains synthetic candidate data. It contains no real personal data.
The fixtures live in `feature/onboarding/impl/src/test/resources/pdf`.
The provider input files live in [`pdf-export-sources`](pdf-export-sources/README.md).
The device test uses copies in `src/androidTest/assets/pdf`.

PyMuPDF generated these files on 2026-10-11. Each text fixture contains the same eight lines:

```text
Sample Candidate
sample@example.test
Experience
Data Analyst | Example Company
Jan 2020 - Dec 2022
Built SQL reports for the finance team.
Skills
SQL, Excel, Python
```

| Fixture | Encoding | SHA-256 |
|---|---|---|
| `uncompressed.pdf` | Helvetica, uncompressed content streams | `9c6c654176ac98de9e71cd60e6de8b3af244b77aa54cebf487ec6dd4f0f34205` |
| `compressed.pdf` | Helvetica, Flate content streams | `68ff706cbeb6e1a345c38a3df59aa8fc652ae3a55c5e84c8ea8ee0c3b96ff21d` |
| `font-subset.pdf` | Bundled Space Mono, subset font, Flate streams | `408da074975dd895c46415f9b0f4d45dcbdf1c321d24f2c63536e18bbf7e4764` |
| `scanned.pdf` | Rasterized negative control, no text layer | `fc2567eb3a085f5fd5adfdb618a0c3cb72d22207557e94ac2016c6fcc7f16ded` |

The baseline reader extracted usable text from 0 of 3 files in the API-28 regression run.
It returned `NoTextLayer` for all three files.
The revised API-28 reader extracts usable text from 2 of 3 text fixtures.
It reads the uncompressed and Flate-compressed fixtures.
It uses `Unreadable` for the subset-font fixture and scanned negative control.
This result does not measure Google Docs, Word, Canva, LaTeX, or LinkedIn exports.
The Google Docs, Word, Canva, and LinkedIn exports are absent from this checkout.
The LaTeX export is measured separately below.

Android API 35 added [PdfRenderer.Page.getTextContents](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page#getTextContents()).
The app uses this native reader when it is available.
The API-35 device check extracts usable text from all 3 text fixtures.
It also identifies the scanned negative control as `NoTextLayer`.
The downstream parser recovers the skills and experience entry.
It runs on the injected IO dispatcher and deletes its temporary file after use.
It normalizes Unicode compatibility characters, including ligatures.

On older Android versions, the fallback supports direct stream lengths, uncompressed streams,
Flate streams, literal strings, hex strings, and basic text positioning.
It preserves token order and line boundaries.
It bounds decompressed output.
It does not implement font-specific CMap decoding, indirect stream lengths, or all PDF filters.
If extraction fails, it uses the generic unreadable-file state.
It does not claim that the file is image-only.

Run the fallback checks with:

```sh
./gradlew :feature:onboarding:impl:testDebugUnitTest --tests 'com.tailormyresume.feature.onboarding.impl.importresume.ContentResolverResumeTextSourceTest'
```

On an API-35-or-newer device, run the native extraction and downstream parser check with:

```sh
./gradlew :feature:onboarding:impl:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tailormyresume.feature.onboarding.impl.importresume.PdfResumeTextDeviceTest
```

The test asserts recovery of the section lines, skills, and a work-experience entry.
It also checks that the reader leaves no temporary resume file behind.
The broader provider-export measurement remains a separate release check.

## Provider exports measured on 2026-10-11

Tectonic 0.17.0 compiled `pdf-export-sources/sample-resume.tex` without rewriting the output.
The original PDF identifies `tectonic` as its creator and `xdvipdfmx (0.1)` as its producer.
It contains one page with a subset Latin Modern font.
The SHA-256 of `latex.pdf` is `1814b474f1b9ffcd66c959e462597fdf68f5053f2064f48f588acd7b2a0ea9de`.
The test resources and device assets contain identical bytes.
The temporary export tool lives under `.context` and adds no app dependency.

A usable fixture must preserve all eight source lines in order.
The downstream parser must also recover the three skills and an experience entry.
The API-28 result comes from the app's fallback reader under Robolectric.
The API-35 result comes from the app's native reader on the Pixel 9 Pro API-35 emulator.

| Export source | Genuine fixtures | API-28 usable | API-35 usable | Remaining dependency |
|---|---:|---:|---:|---|
| Google Docs | 0 | Unmeasured | Unmeasured | Authenticated editor export |
| Word | 0 | Unmeasured | Unmeasured | Word desktop or authenticated Word web export |
| Canva | 0 | Unmeasured | Unmeasured | Authenticated editor export; test Standard and Print separately |
| LaTeX | 1 | 0/1 (0%) | 1/1 (100%) | More templates for broader coverage |
| LinkedIn | 0 | Unmeasured | Unmeasured | Approved synthetic test profile and authenticated desktop export |

The fallback returns `Unreadable` for this LaTeX fixture.
It does not mislabel it as image-only.
The native reader recovers all eight source lines and the expected parser fields.
This is one LaTeX smoke test. It does not establish a general LaTeX success rate.
The workspace has no connected export surface for the other four providers.
Use the [export kit](pdf-export-sources/README.md) to create those fixtures when access is available.
Do not include unmeasured providers in a denominator or count them as extraction failures.

## Options and cost

| Option | Dependency and APK cost | Support limit | Decision |
|---|---|---|---|
| Android native reader plus bounded existing fallback | No new dependency. APK size change is not measured. | Full native extraction requires API 35. The fallback has the limits above. | Implemented for the measured fixtures. |
| General on-device PDF library | Requires a logged dependency and licence decision. APK size and maintenance cost require measurement. | Could support older Android versions and more encodings. | Defer selection until provider exports establish the remaining failure rate. |
| Server parsing | Adds upload, backend, and service costs. It requires a privacy review under Constitution I.5. | Cannot serve the offline demo flavour. | Excluded from this offline fix. |

The provider-export investigation is incomplete. These generated fixtures establish encoding regressions.
The one LaTeX fixture establishes a limited provider smoke test.
The other four provider rates remain unmeasured.
Do not treat this change as completion of that release measurement.
