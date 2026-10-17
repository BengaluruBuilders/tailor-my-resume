# New-client sales retirement and release gates

Slice05 removes buying, product/price lookup, payment/restore screens, sale navigation and fake catalogues. Credits opens the existing provider-aware guest/Google earning actions. Both zero-credit entry points preserve the application key; dismiss/back never starts tailoring or spends newly earned credits. Recovery of an unfinished tailoring still precedes zero-balance rejection. A decode-only legacy saved key redirects to Credits.

Wallet balance, free/purchased breakdown, server ledger IDs, historical purchase reads and account exports survive. New-client wallet/history/cleanup uses AccountWallet independently of settlement. Account work and wallet generations reject stale replies, including same-UID sessions after a switch. The server balance remains authoritative when history is missing, unknown or truncated.

Google Play Billing 9.1 and its existing dependency-guard entries remain necessary for app-internal owned-purchase queries and completion notifications. Automatic historical settlement can discover paid-but-unposted tokens that the backend consume worker cannot discover. The client never launches a Billing flow or consumes a purchase. Anonymous and unsupported sessions do not query or post. Failed settlement does not block wallet reads, Google linking or normal navigation. Transient failures have bounded retries, with startup discovery available after process death. Sign-out/account change invalidates outstanding retries and clears caches.

The backend retains old /packs and GET/POST /purchases shapes, product IDs, owner/token verification, one-grant replay and consume retry. No store change, live cutoff, grace period or sales-disabled HTTP rejection is introduced. Installed old clients retain their API, and pre-acquisition clients omit unknown reward history while preserving the authoritative balance. Reward activation therefore requires a compatible Android release. No reward is relabeled as a purchase or welcome grant.

| Gate | Required evidence | Status before independent validation |
| --- | --- | --- |
| Changed-source checks | Focused wallet/settlement/navigation/history tests and unchanged verify-local.sh | Pending final candidate |
| Candidate integration | Exact Android/backend pair, APK signature/manifest/source mapping, actual contacted API and changed UI | Independent validator owns |
| Historical Play obligations | Actual supported-version/product inventory, paid-unposted/pending completion and reviewed eventual store/cutoff/grace decision | External BLOCKED |
| Google/Integrity/rewards | Genuine eligible Google link/conflict/recovery and authoritative Integrity setup, safe disposable accounts | Prior external BLOCKED |
| Links/install attribution | Owned HTTPS origin, association/default resolver, Play referrer, original-origin landing publication | Prior external BLOCKED |
| Policy/lifecycle | Accepted live cutoff/window/capacity/thresholds/retention/disclosure, authoritative historical inventory | External BLOCKED |
| AI budget sizing | Separately authorized real provider evaluation and abuse measurements; optional user-cap mechanism has no live amount | External BLOCKED |

Synthetic fixtures and fakes are coding evidence only. Preserve prior01–04 external limitations. Draft PRs remain unmerged; no production deployment, reward activation, recall enforcement, purchase settlement completion or release readiness is claimed. Compatible rollback pauses new admissions and keeps committed grants/current wallet/spend/recovery/settlement/export/deletion. Do not restore an old implicit-welcome binary or down-migrate data.
