# google-activation-v1 acceptance map

This is a contract map, not executed feature evidence. Fixture values never activate real admission.
Both writers must implement the named scenarios with deterministic clocks/randomness and real local
Postgres for persistence/locking; a distinct validator assesses the exact final candidate pair.
Inherited real Integrity/admitted-trial/spend acceptance remains BLOCKED.

| ID | Setup/action | Required result |
|---|---|---|
| GA01 | Guest has unused 2; prepare, Firebase same-UID link, observe, activate | Wallet 7; one GOOGLE_SIGNUP +5; no new base; local work/IDs/files intact |
| GA02 | Guest has spent 1 from original 2 | Wallet 6; original trial/ledger/debit preserved |
| GA03 | Cancel credential prompt before Firebase link | Guest auth/work/balance unchanged; no activation/grant |
| GA04 | Kill process before/after intent create response | Private proof persisted first; same create key/proof recovers same handle/expiry |
| GA05 | Firebase link succeeds, process dies before observation/activate | Same UID + authoritative incarnation/provider + durable prelink fact recovers once |
| GA06 | Secret missing/expired; durable prelink valid | Explicit same-UID recovery, new stable recovery key if payload differs, same +5/anchor |
| GA07 | Durable prelink also expired/erased | No fabricated direct-new classification; RECOVERY_EXPIRED/ambiguous, wallet/work retained |
| GA08 | Replace expired Integrity challenge, same business key | New evidence accepted; stable digest/entitlement unchanged |
| GA09 | Commit succeeds, reply lost; pause policy/outage/change amount | Replay original receipt + current post-spend wallet; no provider call/top-up |
| GA10 | Concurrent same/different activation keys/devices | One UID/subject signup grant and unique ledger link/capacity debit |
| GA11 | New direct Google, unused valid continuity | Separate GUEST_TRIAL +2 / GOOGLE_SIGNUP +5, wallet 7 |
| GA12 | New direct Google, signal allocated elsewhere | Base NOT_ELIGIBLE/SIGN_IN_REQUIRED, eligible +5 still granted; no old UID/data |
| GA13 | Base absent/paused/exhausted, signup available | Base unavailable; signup +5, no guessed base/default |
| GA14 | Signup campaign absent/paused/exhausted, protected base available | 200 base +2, signup UNAVAILABLE; pending signup facts intact; missing shared classification/security still closes both |
| GA15 | Either uncommitted requested revision stale, or null campaign revision now usable | 409 POLICY_CHANGED before all new writes; refresh/reconfirm amounts |
| GA16 | New fixture policy changes signup amount 5→6 | New eligible gets configured 6; old grant retains 5; no lifetime reset |
| GA17 | Last capacity race / injected SQL failure after every write | Full one winner per entitlement; all grant/wallet/ledger/capacity/operation writes roll back coherently |
| GA18 | Existing/prelaunch/ambiguous Google signs in/refreshes/new key | No fresh bonus or invitee window; old balance/history retained |
| GA19 | Missing classification/cutoff policy at first Google observation | Record immutable first observed facts/provenance; no award; later policy never rewrites existing into new |
| GA20 | Canceled prelink, weeks of guest use, new prepare | Renew only with fresh authoritative same-incarnation Google-absent check; new unused attempt anchor |
| GA21 | Stale anonymous token after Google link tries prepare | Admin sees Google; freeze/reconcile prior attempt, no new anchor/secret |
| GA22 | Concurrent renewal/finalization with stale metadata snapshot | Sorted user/scope locks and state recheck prevent replacing observed activation |
| GA23 | Link-then-crash; finalize after referral deadline | Original anchor/deadline fixed; expired referral does not erase otherwise eligible signup |
| GA24 | Same UID but changed authoritative Firebase creation | Reject old prelink/incarnation proof for new operations; no stale adoption |
| GA25 | Google provider missing/malformed/multiple subjects/Admin mismatch | GOOGLE_IDENTITY_REJECTED; no new fact/grant inferred from email/client flag |
| GA26 | Cross-app/environment token/intent/challenge/signal/alias | Auth/scope rejection; other app and predecessor guest protocol unchanged |
| GA27 | Invalid JSON, unknown key, bad UUID/proof/type/null union/oversize | Stable 400/413; no state mutation; every endpoint tests documented 200/201 |
| GA28 | Wrong hash/action/revision pair/package/cert/license/device/time | Strict Integrity refusal; no HTTP-selected fake verifier |
| GA29 | Admin/Integrity timeout/503; source in flight when account changes | Retry original business key later; no fallback credits or stale wallet overwrite |
| GA30 | Credential already in use | Explicit choice before switch; cancel leaves guest/snapshot intact |
| GA31 | Complete conflict under fresh destination Google token + guest proof | Destination own wallet unchanged; no bonus/base/invitee creation or economic/server-job transfer |
| GA32 | Token refresh instead of recent Google auth | RECENT_GOOGLE_AUTH_REQUIRED for uncommitted completion |
| GA33 | Random/wrong proof, another UID/app/env, expired secret | Non-enumerating 404 or owned UPGRADE_INTENT_EXPIRED; no side effects |
| GA34 | Simultaneous conflict completions under B and C | One bound destination only; sorted locks; other 404; no destination UID from body |
| GA35 | Lost conflict reply, proof expires | Bound destination with matching proof recovers same completion/current wallet; no new fresh-auth/provider requirement |
| GA36 | Same-UID link then attempt cross-UID complete | ACCOUNT_LINK_CONFLICT; cannot repurpose a successful link capability |
| GA37 | Source or destination deletion concurrent with complete/grant | ACCOUNT_DELETED/deletion wins; no resurrection/partial cross-account edge |
| GA38 | Kill at snapshot/switch/complete/import/cleanup boundaries | Durable selected profile/JD/application/edit/export work recovered once, no loss/duplication |
| GA39 | Destination has colliding IDs/profile/work | Explicit profile choice; collision remap, no silent overwrite; only selected source content visible |
| GA40 | Source has running job/pending ids/generation references | Same UID preserves; cross UID detaches all remote capabilities while preserving local content/input |
| GA41 | Unsaved job/import/manual/editor text + late source writer | Durable save barrier and writer fence preserve snapshot before identity change |
| GA42 | A→B→C plus stale requests/recovery journal | C cannot see A/B snapshot/files/wallet; all results scoped UID/env/generation |
| GA43 | Alias expiry/rotation/missing retired key | Live UID cannot regrant; finite cross-UID limitation explicit; no silent expiry renewal/key reset |
| GA44 | Account erasure then recreation | All attributable hashes/proofs/facts erased; no deletion-resistant identity claim; aggregate capacity unchanged |
| GA45 | Export/read/repeated GET before any mutation | Own activation/dates/receipts only; no new grant/fact; no hashes/proofs/counterparty UID/local upload |
| GA46 | Erasure fails at each DB/storage/Firebase phase, then retries | Existing recovery completes, other account intact; restoration uses existing deletion reconciliation |
| GA47 | Legacy wallets, purchased balance, reservation/failure/completed tailoring | No reset/backfill; free-first spend and one-success-one-debit still hold |
| GA48 | Old client/other app/anonymous GUEST_TRIAL vectors | Additive compatibility; existing section8 bytes/scope/hashes/amount semantics unchanged |
| GA49 | Base-first +2 with signup unavailable; explicit new-key signup retry | Existing base receipt revision binds even while trial paused; +5 once, no second base; same old key replays original unavailable signup snapshot |
| GA50 | Signup-first +5 with base unavailable/omitted; explicit new-key base retry | Original signup revision binds despite signup pause; same chosen continuity or first supplied continuity; +2 once; old key unchanged |
| GA51 | Eligible direct signup/base; only base source actor/continuity/network/campaign attempt limit reached | 200 signup +5, base UNAVAILABLE/RATE_LIMITED, positive Retry-After; only signup economic writes/capacity; no signal allocation; base counters not bypassed |
| GA52 | Eligible direct signup/base; only signup source actor/network/campaign attempt limit reached | 200 protected base +2, signup UNAVAILABLE/RATE_LIMITED, positive Retry-After; only base economic writes/capacity; no signup device cap |
| GA53 | No new grant can commit; otherwise eligible source rate-blocked, counterpart rate-blocked/unavailable/ineligible/already granted | 429 RATE_LIMITED + positive Retry-After; no new economic writes/success snapshot/challenge consumption; bounded attempts and existing observation/continuity facts retained; same stable key/fresh evidence retries after delay |
| GA54 | Partial RATE_LIMITED response lost/replayed after spend/policy pause; delay elapses then explicit missing-source retry | Old key returns immutable statuses/original receipts/current wallet without new counters/provider calls; positive Retry-After; new key/fresh challenge grants only missing source once, original counterpart revision/continuity/dates unchanged; Android persists and respects delay |
| GA55 | One source eligible, common action/challenge/ingress limit or shared identity/Integrity/binding/revision/SQL failure | Whole documented error, no partial new economic writes; independent source handling never bypasses common security; complete receipt recovery remains lookup/counter-free |

Canonical fixture checks: `python3 fixtures/generate-fixtures.py --check` reproduces all four JSON
files and self-checks hashes/negative mutations/arithmetic. This check does not run Android/backend
features, Firebase, Integrity, storage, a device or live policy. Implementations add meaningful route,
transaction, ownership, crash recovery and view-model tests from this table, then required repo gates.
