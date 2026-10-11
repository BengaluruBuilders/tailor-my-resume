# Issue 91 golden fixture: analysis prompt, old (main) vs new (a7f501f)

Only the system message changed. The user message (job text and candidate facts JSON) is byte-identical.
1. Kept: the first three sentences (role, data-fence rule, requirements list, MET/PARTIAL/GAP rule, "A GAP cites no id.").
2. Added: "When the text is not a job posting, for example a chat message, a referral or other text, set isJobPost to false and return no requirements." (NOT_A_JOB_POST)
3. Added: "Return location exactly as the posting writes it, the city plus the work mode when stated, for example Bengaluru · Hybrid, or null when neither is stated."
4. Added: "For each MET or PARTIAL requirement add a reason of one sentence at most 100 characters, using only words and numbers that appear in the cited facts."
5. Added: "Never use the words match, fit, ATS, likely, chance, guarantee, score, perfect or ideal candidate in a reason."
6. Kept, moved to the end on its own line: "Never invent a fact." (was appended to the GAP sentence on the same line)
7. Separators: the new sentences are joined by literal \n line breaks, as on main between the first two paragraphs.
8. Net: system content grows from 1 added-free paragraph set to +4 instructions; no instruction was removed or reworded.
9. Fixture: only the "analysis" key value changed; prep, cover-write, cover-repair, tailoring-generate, tailoring-repair are byte-identical.
10. Captured from the prompt the real test sends (extraction model, last call) at a7f501f; JSON formatting (2-space, trailing newline) preserved.
