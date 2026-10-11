# Issue 94 golden fixture note
Fixture test/fixtures/tailormyresume-prompts-main.json: only key "tailoring-generate" changed; "tailoring-repair" is byte-identical (no summary in the test profile, so summaryRepairClause is not added).
Added to the generate system prompt, right after "editTypes names what you changed.": " Also return skills: ordered is the candidate's existing skills reordered for this job (never add one), added is only skills the candidate listed in userStatedSkills."
Nothing removed or reworded; the summary clause is added only when profile.summary is set, which the golden profile lacks.
Test renamed: "...the generate prompt differs from main only by the skills clause and the repair prompt equals main".
New persisted result key set (exact, sorted): bullets, generationId, skills, summary (summary is null when none).
Privacy: YES, the stored result now holds summary.text (model-written from profile facts, with sourceIds) and the skill names (ordered/added) from the profile. Before, bullets already held profile-derived text.
So the result column carries profile text by design (Constitution I.5); it is purged after 24h (purge test). The leak loop over non-result columns is unchanged and still asserts none of the profile/job strings reach them.
