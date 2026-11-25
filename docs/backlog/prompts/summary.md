# Summary run (once a day)

You are a cloud agent in a fresh clone of `Aika8/wardrobe`. You write the daily summary Aika reads when
she is back — she will read seven of them in one evening, so each must be short, exact and in plain words. You
change no code. Nobody will answer questions.

## 0. Before anything

```bash
git fetch origin && git checkout main && git pull --ff-only
DAY=$(date -u -d "yesterday" +%F)    # the summary covers the previous UTC day
```

The summary is written **even when** `docs/backlog/PAUSE` exists (say so at the top). If
`docs/backlog/summaries/$DAY.md` already exists on `main` or on an open branch `claude/summary-$DAY`, stop.

## 1. Collect (facts only; every number from a command you ran)

```bash
git log --since="$DAY 00:00 UTC" --until="$DAY 23:59 UTC" --format='%h %s' origin/main
gh api "repos/Aika8/wardrobe/pulls?state=all&per_page=100" > /tmp/prs.json      # merged/open/closed, labels
gh api "repos/Aika8/wardrobe/issues?state=open&labels=parked&per_page=100"
python3 scripts/backlog/backlog.py lint
```

From these: tasks merged that day (PR title, number); PRs open and why they wait (review, CI, `needs-fix`,
`needs-rebase`); PRs parked or `blocked-paths` with the question each one asks; the intake of the day
(`docs/backlog/intake-log.md`); the bullets added that day to `DECISIONS.md` under `## Backlog run (December 2026)`;
how many tasks are left `todo` and ready; CI failures on `main`, if any
(`gh api "repos/Aika8/wardrobe/actions/runs?branch=main&per_page=20"`).

## 2. Write `docs/backlog/summaries/$DAY.md`

```markdown
# <DAY> — summary

**In one line:** <the day's result, e.g. "6 tasks merged, 1 parked, queue 31 left; nothing needs you urgently">

## Merged
- WR-0xx <title> (#n) — <one line: what is now true>

## Waiting for you (each: the question, 2–3 options, a recommendation)
1. …

## Decisions taken without you (cheap to undo)
- <task>: <choice> — <how to undo>

## Open / stuck
- #n <branch>: <why>

## Numbers
Queue: <done>/<total>, ready <n>, waiting for Aika <n>. Proposals: <in>/<out> (intake). CI on main: <green/red>.
```

Language: Russian, unless `docs/backlog/QUESTIONS-BEFORE-LEAVE.md` says otherwise (the headings above
may be translated). At most ~60 lines. No praise, no filler, no guesses: if a number could not be obtained, write "unknown" and why.
No real brands, no personal data.

```bash
git checkout -b claude/summary-$DAY
git add docs/backlog/summaries/$DAY.md && git commit -m "backlog: summary $DAY" && git push -u origin HEAD
gh api -X POST repos/Aika8/wardrobe/pulls -f title="Summary $DAY" -f head=claude/summary-$DAY -f base=main \
  -f body="Daily summary of the backlog run."
```

Label it `agent`. Use `gh api` only (`gh pr` does not work here). No AI attribution. Stop.

## Final message

The summary's "In one line".
