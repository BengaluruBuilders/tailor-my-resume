Patch for #278 (apply on revamp/278-tailored head 82df34b5 before pushing): git apply docs/revamp/2026-10-10/patches/issue-278-tailor-tests.patch
- TailorViewModelTest: the three per-bullet tests (onAccept, onKeepOriginal, onUndo) become four single-accept tests
  (onUndoChange, onAcceptChanges, undo-then-accept, export gate via ExportReadiness). Every other I.2 test is untouched.
- TailorEntryKeysTest: the three routed keys (Tailoring, TailorFailed, Tailored) now need Hilt, so only the two placeholder
  keys are rendered; the routed keys are checked to resolve to entries.
- app ShellHarness (test helper): default entries are placeholders for every key. ShellWalkTest and TabBarVisibilityTest
  rendered the real tailorEntry and crashed without Hilt (the plan said they stay correct; they do not). verifyRoborazzi stays identical.
With the patch applied: tools/ci/verify-local.sh passes. Without it the module tests do not compile (D50).
