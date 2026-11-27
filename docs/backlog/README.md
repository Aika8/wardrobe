# The backlog run: cloud agents working through a queue

For the week Aika is away (2–8 December 2026) and later. Copied from `Aika8/learning-exchange` (same rules, same
`backlog.py`, config adapted; a shared library later — `DECISIONS.md`). Agents in Claude Code cloud sessions, started by
scheduled tasks (routines), take one small checkable task at a time from `QUEUE.md`, open a PR, an independent
run reviews it, and a GitHub Actions workflow — code, not an agent — merges it when every gate holds.
Nothing here costs money beyond the subscription: no API keys, no purchases, no sign-ups; Actions stay within the
free minutes.

## Parts

| Part | File | Who changes it |
|---|---|---|
| The queue | `docs/backlog/QUEUE.md` | Aika by hand; the intake workflow appends |
| The rules as code | `scripts/backlog/backlog.py` (+ tests, run by `make test`) | Aika |
| Merge gate | `.github/workflows/automerge.yml` | Aika |
| Proposals → queue, every one that passes the filter | `.github/workflows/intake.yml`, format in `PROPOSALS.md` | Aika |
| Off switch | `docs/backlog/PAUSE` (create it to stop everything) | Aika, also from the GitHub phone app |
| Guard against pushes that skip the gate | `.github/workflows/guard.yml`, active while `docs/backlog/UNATTENDED` exists | Aika |
| Run prompts | `docs/backlog/prompts/*.md` | Aika |
| Environment setup | `scripts/cloud/setup-env.sh`, run by the SessionStart hook in `.claude/settings.json` | Aika |
| Answers given before leaving | `docs/backlog/QUESTIONS-BEFORE-LEAVE.md` | Aika |
| Proposals of the day | `docs/backlog/proposals/<date>-<role>.md` | the analyst and user runs (one new file each) |
| Daily summary | `docs/backlog/summaries/<date>.md` | the summary run |
| Reports of tasks | `docs/backlog/reports/*.md` | tasks that say so |
| Tech debt found outside a task | `docs/backlog/TECH-DEBT.md` (format), `docs/backlog/tech-debt/*.md` (one file each) | runs add, never change |
| Experience log, write-only | `docs/backlog/experience/*.md` — agents never read it | runs add; Aika reads |

The merge gate refuses any agent PR that changes a file in the "Aika" rows, `Makefile`, `CLAUDE.md`, an existing
test, an accepted ADR, or the design (`docs/design/`: theme, components, mockups) — see `PROTECTED`, `VISUAL`, `APPEND_ONLY` in `backlog.py`.

## Environment: nothing to configure on claude.ai

The toolchain comes from the repository itself. `.claude/settings.json` runs `scripts/cloud/setup-env.sh` as a
SessionStart hook in every cloud session (it does nothing locally): JDK 25 from download.oracle.com (fallback:
Ubuntu's package), Node 24 from the npm registry (fallback: nodejs.org) — all on the
default allowlist of environment `Default` (Limited, package managers allowed). Each prompt calls the script again as
its first step; on a VM where it already ran it takes about a second. Log: `/tmp/setup-env.log`.

The only cost is time: without a cached setup script each new session downloads the JDK again (about a minute).
If the trial run shows that this is too slow or fails, the one field to change is claude.ai → Code → environment
`Default` → **Setup script**: paste `scripts/cloud/setup-env.sh` there; the result is then cached between sessions.

## Runs

All runs: environment `Default`, model Opus 5.5 (never Sonnet), repository `Aika8/wardrobe`. The prompt
of each scheduled task is one line — the file holds the rest, so changing a prompt is a commit, not a click:

> Read `docs/backlog/prompts/<role>.md` in this repository and follow it exactly. You start from nothing: that
> file and the files it names are all your instructions.

| Run | Prompt file | When (UTC; Almaty = UTC+5) | Writes |
|---|---|---|---|
| Executor | `prompts/executor.md` | every 2 hours at :17 (`17 */2 * * *`) | branch `claude/<task id>`, a PR |
| Reviewer | `prompts/reviewer.md` | every 2 hours at :47 (`47 */2 * * *`) | a review comment and a label on one PR |
| Analyst | `prompts/analyst.md` | daily 16:40 | PR adding `proposals/<date>-analyst.md` |
| User | `prompts/user.md` | daily 17:40 | PR adding `proposals/<date>-user.md` |
| Intake | `.github/workflows/intake.yml` (code) | daily 21:55 | commit to `main`: QUEUE.md, intake-log.md |
| Summary | `prompts/summary.md` | daily 01:35 | PR adding `summaries/<date>.md` (+ `proposals/<date>-summary.md`); un-parks what it decided |
| Automerge | `.github/workflows/automerge.yml` (code) | after every CI run and every 3 hours at :43 | merges |

Every two hours, not hourly: three projects (learning-exchange, signal-bot, wardrobe) share one subscription limit
and one pool of free Actions minutes; times are shifted from learning-exchange's so runs do not start together.

A run that has nothing to do stops in its first minute (`backlog.py plan` / `review-queue` say so), so an idle
hour costs little.

## Life of a task

1. Executor: `python3 scripts/backlog/backlog.py plan` names the move. Open PRs come first (fix, rebase, park);
   at most 3 agent PRs are open at once; otherwise the next ready task in file order (`NEW WR-0xx`).
2. Executor works on branch `claude/wr-0xx`, only inside the task's `paths` (+ `DECISIONS.md`), runs the task's
   `verify` and `make test`, checks paths with `backlog.py check-paths`, pushes, opens the PR with `gh api` (REST;
   `gh pr` uses GraphQL, which the cloud proxy refuses) and labels it `agent`.
3. CI runs `make test`.
4. Reviewer (another run, no shared context): when CI is green, answers yes/no questions about the diff against
   the task. Pass → label `review:pass`. Fail → label `needs-fix` and the questions.
5. Executor of a later hour answers `needs-fix` or a red CI: at most 2 fix rounds, then the PR gets `parked` and
   waits for Aika. A pushed fix removes `review:pass` (automerge.yml), so the reviewer looks again.
6. Automerge: PAUSE absent, branch and paths allowed, CI green on the head, branch up to date (if behind, it
   updates the branch; with no CI on the head it dispatches CI; a conflict → `needs-rebase`), and for a task:
   `review:pass` plus a reviewer marker `verdict=pass sha=<that very head>` (only GitHub's own update-branch merges
   may follow the reviewed commit) → squash merge, then CI on `main`. A task is *done* when its PR is merged.

Labels: `agent`, `review:pass`, `needs-fix`, `needs-rebase`, `parked` (the summary run decides and un-parks it), `blocked-paths` (touched
a forbidden path; waits for Aika), `hold` (Aika's: do not touch). GitHub creates a label the first time it is used.

## Rule for every run: nothing waits for Aika

Aika is away and answers nothing. When a run meets a question the task and the files do not answer:

1. **Look for the answer first**: the task, `docs/backlog/QUESTIONS-BEFORE-LEAVE.md` (Aika's answers), the intents,
   ADRs, `DECISIONS.md`.
2. **Cheap to undo → decide and go on.** Take the option that is easiest to undo later (smaller change, behind a
   name that is easy to find, no new dependency, no new public contract). Write it in `DECISIONS.md` under the
   heading `## Backlog run (December 2026)` as one bullet: the task id, the question, the choice, the options not
   taken and why — so Aika can reverse it in a minute.
3. **Costly to undo → park.** Costly means any of: a new or changed public contract (`api/openapi.yaml`) not asked
   for by the task; a new runtime dependency; a storage format; anything an ADR would be needed for (CLAUDE.md:
   costly-to-reverse decisions get an ADR first); deleting or renaming public things; an approved design; a model
   call or anything that would need a key. Then do not do it: comment the question with 2–3 options and a recommendation on
   the PR (or open an issue titled `[parked] <task id>: <question>` with the label `parked` when there is no PR),
   and stop. The next run takes the next task. A parked question does not wait either: the next analyst or summary
   run decides it (step 4).
4. **Decide, do not wait** (Aika, 26 November: «меня ничего не должно ждать»). The analyst, user and summary runs
   decide every open question — a parked PR, a tech-debt note, a contradiction — by its recommendation, or without
   one by the option easiest to undo, and turn the decision into a proposal (`docs/backlog/PROPOSALS.md`); intake
   makes it a task, and that task writes the `DECISIONS.md` bullet. The summary lists these under «Решено без тебя —
   посмотри после отпуска». Only what needs money, keys, sign-ups or external accounts is left for after the holiday.

Never: ask in the chat and wait (nobody answers), weaken or delete a test to get green, touch a protected path,
spend money, sign up anywhere, use a real brand or real credentials, commit a photo or personal data.

**Look is Aika's.** Tasks marked «вид проверяет Aika» build a draft of a screen or of the avatar. Their logic is
under tests; their look is not claimed to be right. The PR says «draft — вид проверяет Aika» and describes in words
what it should look like, so Aika can check it in a minute after the week.

## Off switch

Create `docs/backlog/PAUSE` on `main` (GitHub app → repository → Add file; any content). Every run stops at its
first step, automerge merges nothing, intake moves nothing. Delete the file to resume. To stop the runs
themselves (and their use of the limit), pause the scheduled tasks at claude.ai/code/routines.

## Guard (no branch protection on this plan)

`main` cannot be protected on a private repo of the free plan, and the cloud token can push and merge. So:

- While `docs/backlog/UNATTENDED` exists on `main`, `guard.yml` pauses the whole run (creates `PAUSE`, opens an
  issue labelled `hold`) on **any** push to `main` that did not come through automerge or intake (those use
  `GITHUB_TOKEN`, whose pushes start no workflow). Let through: creating `PAUSE`, creating `UNATTENDED`, and
  deleting `PAUSE` while no `[guard]` issue is open.
- Aika creates `UNATTENDED` on 1 December, after merging this setup (merging it while the file exists would pause
  the run at once). When back she deletes it — that pauses too (so an agent cannot switch the guard off quietly);
  then she closes the guard issue and deletes `PAUSE`.
- GitHub → Settings → Actions → General → Workflow permissions must be **Read repository contents** (the default
  for new repositories): then a workflow an agent adds on its branch gets no write token; the workflows here ask
  for write explicitly.
- What is left: an agent could label its own PR `review:pass` and write a fake reviewer marker. The prompts
  forbid it, the merge gate still needs green CI and allowed paths, and the summary lists every merge with its
  review — read the reviews of merged PRs first when back.

## Money and limits

- Claude: subscription only, extra usage off; when the weekly limit is used up, runs are refused until the reset.
  No API keys exist in the environment.
- GitHub Actions: CI ≈ 3 min per run (Gradle + `npm ci`), automerge and intake ≈ 1 min each. Estimate for the
  week here: about 350 minutes (≈ 25 PRs × 10 min + 56 automerge sweeps + intake and guard). The 2000 free minutes a
  month are shared with the other private repositories (learning-exchange estimates about 900). The Actions spending limit must be $0 (GitHub → Settings → Billing).
- Network: environment `Default`, Limited (package registries and GitHub only).

## Trial before leaving

The facts unchecked for learning-exchange (cloud `make test`, `gh api` PRs, reviewer trigger, usage per run) are
the same here; its trial closes them for both. Specific to this repository: one cloud session runs `make test`
here once (Gradle resolves Spring Boot 4 from Maven Central; `npm ci` of the Expo client within the allowlist).
