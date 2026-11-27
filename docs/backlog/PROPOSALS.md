# Proposals: format and how they reach the queue

The analyst and user runs do not change the queue. Each writes its proposals of the day into a **new file**
`docs/backlog/proposals/<YYYY-MM-DD>-<role>.md` (role: `analyst` or `user`; the date is the UTC date of the run),
on a branch `claude/proposals-<role>-<YYYY-MM-DD>`, and opens a PR. Automerge merges such a PR without review when it
adds only that one file. (One file per run instead of one shared file: two runs a day appending to the same file
would conflict.)

After every merge sweep and once a day, code (`scripts/backlog/backlog.py intake`) moves **every** proposal of that
day into `QUEUE.md`, as tasks `AUTO-<YYYYMMDD>-<n>`. A proposal is taken only if all of these hold:

- `size: S` — one run can do it (≤ ~60 minutes, one PR, a few files);
- `paths`: narrow globs in backticks, none protected (CI, Makefile, CLAUDE.md, backlog rules) and none visual
  (`docs/design/approved/`);
- `verify`: a command in backticks that proves it without a screen; `expect`: what it prints or what is true;
- a description of at most ~1500 tokens (6000 characters) that a fresh agent can act on alone;
- its title is not already a task title.

The rest waits for Aika; `docs/backlog/intake-log.md` says why for each. Proposals are taken in file order
(analyst first, then user; within a file, top to bottom), so put the most valuable first.

## Format

```markdown
# Proposals — 2026-12-03 — analyst

### Short imperative title
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/Foo.java`, `domain/src/test/java/wardrobe/domain/match/FooEdgeTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.FooEdgeTest'`
- expect: green; the new test shows …
- why: the contradiction or gap found, with the file and line that show it

What to do, in plain words, self-contained: what to read first, what to change, what not to touch.

### Next proposal
…
```

Field order does not matter; a blank line ends the fields.

A question is never left for Aika («меня ничего не должно ждать»): decide it — the recommendation, or without one
the option easiest to undo — and write the decision as a proposal whose description starts with
`Decision (without Aika): <choice>; not taken: <options>; undo: <how>` and tells the executor to add that bullet to
`DECISIONS.md`. Also list each decision as a plain bullet under `## Decided without Aika` (no `###` heading, so the
intake does not take it twice). Larger ideas are split into size-S proposals. Only what needs money, keys, sign-ups
or external accounts goes under `## After the holiday` as plain bullets.

Files: `<date>-analyst.md`, `<date>-user.md`, and `<date>-summary.md` (the summary run's decisions).
