# revamp/be-all (apps-backend integration branch)

Purpose: one branch holding all revamp backend work (#88-#97) so backend docs (#100) and end-to-end checks can run while merges to main are blocked (tailor-my-resume #256). Worktree: /Users/aibuilders/Documents/dev/products/apps-backend-wt/be-all, based on origin/main 59a8abb. Pushed to origin/revamp/be-all.

## Merged (--no-ff, in order; tip SHA merged -> merge commit)
- 88-credit-packs (#101) 7096cf5 -> 27bd934
- 89-credit-ledger (#104) 42ebac3 -> ceaaf88
- 90-spend-per-tailoring (#108) 59adb70 -> 966f861
- 91-analysis-reasons (#102) 27b1cc1 -> 3c92455
- 92-quick-question (#105) 2e70050 -> fd8df7f
- 93-parse-fields (#103) fafb425 -> c6f6cdd
- 94-verified-summary (#109) f646e06 -> 6673afa
- 95-answer-facts (#110) d3bb6d2 -> 84a9970 (head)

- 96-job-link-import (#111) edb26639: arrived through #112 (it carries #111 on top of the 95 stack; migration regenerated as 0011_strong_blizzard per D49). Not merged separately.
- 97-drop-prep-cover (#112) 9e402ba3 -> fd43aeb (head, pushed to origin/revamp/be-all)

## Conflicts resolved (docs only, docs/architecture.md)
- 97 (#112): one hunk. Skill provenance paragraph: took #112's text (no prep-question or cover-letter wording). /resume/parse paragraph: kept the stack's longer 93 text. No drizzle conflicts; 0008_outgoing_post does not exist.
- 93: kept 93's longer /resume/parse paragraph and the /analyses paragraph from the base (reason and question text).
- 94: kept 94's skill provenance paragraph; /resume/parse (93 text) and /analyses (base) paragraphs kept.
- D31 (analysesLeftToday 2 vs 20): no conflict arose; not yet checked by tests.

## Check
- npm run check against a throwaway local Postgres: typecheck, lint, format, boundaries and 647 tests pass, 0 fail.
- Migrations from scratch on a fresh DB (drizzle migrate, folder drizzle): 12 applied (0000-0011), no errors. scripts/migrate-all.ts was not used because it requires Neon sslmode=require URLs.

## Refresh rule
Merge new branch tips only (git merge --no-ff origin/revamp/<n>); never rebase, force-push or rewrite migrations. Final tips: 88-95 stack at 84a9970, then fd43aeb. If #111 or #112 changes, merge the new tip of origin/revamp/97-drop-prep-cover (it contains #111) and rerun check and the migration run.
