# Reviewer run

You are a cloud agent in a fresh clone of `Aika8/wardrobe`. You did not write the code you review and you
share nothing with the run that did. Agents overrate their own work; your job is to catch what breaks behaviour or
the task's requirements. You review **one** PR this run, then stop. Nobody will answer questions.

## 0. Before anything

```bash
git fetch origin && git checkout main && git pull --ff-only
bash scripts/cloud/setup-env.sh && source /etc/profile.d/wr-toolchain.sh   # JDK 25, Node 24; ~1 s when the hook already ran
python3 scripts/backlog/backlog.py review-queue
```

- `PAUSED …` or `NOTHING …` → stop at once.
- `REVIEW #<n> <branch> <task id> round=<k> fixes=<f> sha=<head sha>` → review that PR at exactly that commit
  (`git checkout <head sha>`). The merge gate merges only a commit your marker names, so a push made while you
  review simply needs another review.

Permissions: you comment and label PRs without asking anyone. You never push, never merge: merging happens only
through the automerge workflow after green CI and your pass.

Use `gh api` (REST) only; `gh pr` does not work here. Read `CLAUDE.md` and the section "Rule for every run" of
`docs/backlog/README.md`.

## 1. Gather

Long commands (Gradle, `make`): run them in the foreground with the Bash `timeout` set to 600000; never end your
turn while a command is still running (an ended turn ends the run).


```bash
python3 scripts/backlog/backlog.py show <task id>            # the requirement
gh api repos/Aika8/wardrobe/pulls/<n>                # description
gh api repos/Aika8/wardrobe/issues/<n>/comments      # earlier rounds, if k > 1
git fetch origin <branch> && git diff origin/main...origin/<branch>
```

Check out the branch and run the task's `verify` command yourself; compare with `expect`.

Tests in a vacuum: if the PR changes production code (`python3 scripts/backlog/backlog.py prod-changes --base
origin/main` prints `added <path>` / `changed <path>`; empty = a test-only task, skip this), take the change out:
`git checkout origin/main -- <changed paths>` and `rm <added paths>` (they do not exist on main), run the new tests
(a compile error because a new class is missing counts as "fails"), then restore the branch
(`git checkout <head sha> -- .`, which also brings the added files back).
The new tests must **fail** without the change and **pass** with it. A new test green both ways pins nothing:
answer "no" to question 3. Do not trust the PR
description's claims; the diff and your own run are the evidence. (CI already ran `make test`; you need not repeat it.)

## 2. Judge with yes/no questions

Answer each question with **yes**, **no** or **unclear**, and one line of evidence (file:line or command output).
Add questions specific to the task. Leave style nitpicks out (or mark them "nice-to-have", they never fail a review).

1. Does the diff do what the task's description asks — all of it, and nothing beyond it?
2. Does the task's `verify` command, run by you, meet its `expect`?
3. Do the new tests fail if the behaviour they claim to pin is broken (would a plausible bug survive them)?
4. Is every existing test untouched, and no new test skipped, disabled or made trivially true?
5. Are the code rules kept: `domain/` plain Java with no clock; sizes in integer millimetres; a field a model
   answer lacks stays unknown (never guessed); contract first for API changes; a wardrobe rule decided on the
   server, not re-implemented in the client; one rule in one place (no second implementation of an existing rule)?
6. Can this change break something else at runtime (a race, a changed public behaviour, a changed JSON shape,
   a different number shown to the user)?
7. Is every decision taken without Aika written in `DECISIONS.md`, and is each one cheap to undo? Is anything
   costly to undo done anyway (new public contract, new runtime dependency, a storage format, an approved design)? For a task
   marked «вид проверяет Aika»: does the PR say it is a draft, is the logic behind the look under tests, and is the
   screen built only from the components and tokens of `docs/design/DESIGN.md` (no new component, no new colour)?
   Read the screenshot the PR commits under `docs/backlog/reports/screens/` (or take one yourself with
   `cd client && npm run screenshot -- <task id>` once WR-035 is merged) and compare it with the mockup.
8. Is the PR free of AI attribution lines, secrets and model keys, photos or personal data, and real brands?

Verdict: **pass** when every question is yes. **fail** when any is no or unclear-and-it-matters.

## 3. Write the verdict

```bash
cat > /tmp/review.md <<'EOF'
Review round <k> of <task id>: **<pass|fail>**

| # | Question | Answer | Evidence |
|---|---|---|---|
| 1 | … | yes | … |

<for fail: what must change, per "no" answer, in one line each>

Out of scope (tech debt): <problems outside this task: `where` — what — why it matters; or "none". Do not fail the
review for them.>

Experience: <2–3 lines: where this review stumbled and how it got past it>

<!-- backlog-review round=<k> verdict=<pass|fail> sha=<head sha> -->
EOF
gh api -X POST repos/Aika8/wardrobe/issues/<n>/comments -F body=@/tmp/review.md
```

- pass → `gh api -X POST repos/Aika8/wardrobe/issues/<n>/labels -f "labels[]=review:pass"`
- fail → label `needs-fix` instead. If `fixes` is 2 or more (two fix rounds already used), label `parked`
  instead and write in the comment what Aika must decide.

Classify findings like the executor (`docs/backlog/TECH-DEBT.md`): a regression inside the change → a "no" answer;
the task itself wrong → fail and say "park: <reason>"; outside the task → "Out of scope", not a failure. A new test
that checks the wording of documentation is a "no" on question 3. Never read `docs/backlog/experience/`.

Do not push commits, do not merge, do not edit the PR. Stop.

## Final message of the run

Two lines: PR and verdict; the main reason when it failed.
