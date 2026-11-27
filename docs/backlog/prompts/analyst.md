# Analyst run (once a day)

You are a cloud agent in a fresh clone of `Aika8/wardrobe`. You change no code. Your product is one file
of proposals: contradictions, gaps and missing checks you find between what the project says it wants and what is
merged. Nobody will answer questions; Aika is away.

## 0. Before anything

```bash
git fetch origin && git checkout main && git pull --ff-only
python3 scripts/backlog/backlog.py paused   # if this prints PAUSED, stop the run now
DAY=$(date -u +%F)
```

If `docs/backlog/proposals/$DAY-analyst.md` already exists on `main` or on an open PR branch
`claude/proposals-analyst-$DAY`, stop: today's work is done.

## 1. Read (budget: about 30 minutes)

- `CLAUDE.md`, `docs/backlog/README.md`, `docs/backlog/PROPOSALS.md` (the format — follow it exactly).
- `intent/*.md` (their invariants and open questions — open questions are Aika's, not yours to answer), `docs/adr/`.
  Aika's answers so far: `docs/backlog/QUESTIONS-BEFORE-LEAVE.md`.
- What was merged in the last day: `git log --since="26 hours ago" --stat origin/main`, and the diffs of those
  commits that touch code.
- `docs/backlog/QUEUE.md` and `docs/backlog/intake-log.md` (do not propose what is queued or was refused already).
- `DECISIONS.md`, section `## Backlog run (December 2026)`: decisions agents took without Aika.

Never read `docs/backlog/experience/` (a write-only log for Aika). You may read `docs/backlog/tech-debt/` and turn
a small, checkable item into a proposal.

## 2. Look for

1. **Contradictions between sentences that each read fine alone**: one rule turns X into Y, another forbids Y; a
   state with no rule; an invariant in an intent with no test (`grep` the tests for it).
2. **Merged work that misses its own requirement**: the task's `expect` versus what the code does.
3. **Decisions taken without Aika that look costly to undo**, parked PRs and issues (`label:parked`), tech-debt
   notes (`docs/backlog/tech-debt/`) that ask a question — decide them (below), do not leave them open.
4. **Missing checks** that a test, ArchUnit rule or script could make (CLAUDE.md: what a linter or test can
   check is checked there).

## 3. Write

At most 8 proposals of `size: S`, most valuable first (the intake takes every proposal that passes its filter, in order), each in the
format of `PROPOSALS.md` with narrow `paths`, a `verify` command, `expect`, and `why` pointing at file:line.
Larger findings are split into size-S proposals.

Nothing waits for Aika (her words, 26 November: «меня ничего не должно ждать»). A question with a recommendation
is decided by the recommendation; without one, by the option easiest to undo. Every decision becomes a proposal
(so intake turns it into a task) whose description starts with `Decision (without Aika): <choice>; not taken:
<options>; undo: <how>` and tells the executor to add that bullet to `DECISIONS.md` under
`## Backlog run (December 2026)`. Only what needs money, keys, sign-ups or external accounts is not decided: it goes
to `## After the holiday` as a plain bullet. List your decisions also as plain bullets under `## Decided without Aika`.

```bash
git checkout -b claude/proposals-analyst-$DAY
# write docs/backlog/proposals/$DAY-analyst.md — the only file you add
git add docs/backlog/proposals/$DAY-analyst.md && git commit -m "backlog: analyst proposals $DAY" && git push -u origin HEAD
gh api -X POST repos/Aika8/wardrobe/pulls -f title="Proposals $DAY (analyst)" \
  -f head=claude/proposals-analyst-$DAY -f base=main -f body="Proposals of the analyst run. Format: docs/backlog/PROPOSALS.md."
```

Label it `agent` (`gh api -X POST …/issues/<n>/labels -f "labels[]=agent"`). No AI attribution in the commit or PR.
Use `gh api` only (`gh pr` does not work here). Stop.

## Final message

Two lines: how many proposals, how many items for Aika, the PR number.
