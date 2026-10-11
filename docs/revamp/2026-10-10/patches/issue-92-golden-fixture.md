# Issue 92 golden fixture (analysis key only)

Re-captured from 4dd20527 (the real prompt sent by the "without userStatedSkills" analysis test). The other keys (prep, cover-write, cover-repair, tailoring-generate, tailoring-repair) are byte-identical and equal #102 (27b1cc1b).

Difference vs the #102 analysis prompt: one system-prompt line added (line 8 of the system content). The user message (job text and facts JSON) is unchanged. Added line:

Add a question and a questionWhy only on the first MUST_HAVE requirement of type EXPERIENCE or SOFT_SKILL that you set to GAP or PARTIAL; leave both out, or set them to null, on every other requirement. The question is one yes/no question of at most 80 characters starting with Have, Has, Do, Does, Did, Are, Is, Were, Can or Could and ending in a question mark, asking whether the candidate has done it, and the questionWhy is at most 200 characters saying why the job asks for it. Word the question and the questionWhy only with words from the requirement or the job description; never mention the candidate's employers, job titles or any fact about the candidate. Never use the words match, fit, ATS, likely, chance, guarantee, score, perfect or ideal candidate in a question or a questionWhy.

Verification: npm ci --ignore-scripts and npm run check against a throwaway local Postgres 16: 559 tests, 559 pass, 0 fail.
