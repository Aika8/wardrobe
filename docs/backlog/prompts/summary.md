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

Nothing waits for Aika («меня ничего не должно ждать»). For every open question — a parked PR or issue (any
day, not only yesterday), a `blocked-paths` PR, a tech-debt note that asks something, a question in a PR comment, a
proposal intake left out (`docs/backlog/intake-log.md`, «left for Aika»: re-propose it in the accepted format) —
decide: the recommendation,
or without one the option easiest to undo. Write each decision as a proposal in
`docs/backlog/proposals/$DAY-summary.md` (format `docs/backlog/PROPOSALS.md`: narrow `paths`, a `verify` command,
`expect`; the description starts with `Decision (without Aika): <choice>; not taken: <options>; undo: <how>` and
tells the executor to add that bullet to `DECISIONS.md`). Intake turns them into tasks. Only what needs money, keys,
sign-ups or external accounts is not decided.

Un-park what you decided, so the task moves again (a parked task otherwise stays parked forever):
- a parked PR: comment `Decision (without Aika): …` on it (with `<!-- backlog-decision -->`), remove the label
  `parked`, and close the PR (`gh api -X PATCH repos/Aika8/wardrobe/pulls/<n> -f state=closed`) — the task becomes
  ready again and the next executor reads the decision there;
- a `[parked] <id>` issue: comment the decision the same way and close the issue.
Comment, label and close are the only GitHub writes you make besides your own PR.

## 2. Write `docs/backlog/summaries/$DAY.md`

```markdown
# <DAY> — summary

**In one line:** <the day's result, e.g. "6 tasks merged, 1 parked, queue 31 left; nothing needs you urgently">

## Merged
- WR-0xx <title> (#n) — <one line: what is now true>

## Решено без тебя — посмотри после отпуска
- <the question> → <the choice> (не взято: <options>; отменить: <how>) — <task id, or the proposal in
  `proposals/<DAY>-summary.md`>

## После отпуска (деньги, ключи, регистрации, внешние аккаунты)
- <only these; everything else is decided above>

## Open / stuck
- #n <branch>: <why>

## Numbers
Queue: <done>/<total>, ready <n>. Proposals: <in>/<out> (intake). CI on main: <green/red>.
```

Language: Russian, unless `docs/backlog/QUESTIONS-BEFORE-LEAVE.md` says otherwise (the headings above
may be translated). At most ~60 lines. No praise, no filler, no guesses: if a number could not be obtained, write "unknown" and why.
No real brands, no personal data.

```bash
git checkout -b claude/summary-$DAY
git add docs/backlog/summaries/$DAY.md docs/backlog/proposals/$DAY-summary.md 2>/dev/null; git add docs/backlog/summaries/$DAY.md && git commit -m "backlog: summary $DAY" && git push -u origin HEAD
gh api -X POST repos/Aika8/wardrobe/pulls -f title="Summary $DAY" -f head=claude/summary-$DAY -f base=main \
  -f body="Daily summary of the backlog run."
```

Label it `agent`. Use `gh api` only (`gh pr` does not work here). No AI attribution. Stop.

## Final message

The summary's "In one line".
