# Tech-debt register

Problems a run found **outside its own task**. The run does not fix them; it records them and goes on. The intake
turns each record into a task by itself — behaviour bugs first — so nothing waits for Aika.

Each finding is its own file, so parallel PRs never conflict: `docs/backlog/tech-debt/<YYYY-MM-DD>-<task id>-<n>.md`,
added in the PR of the task during which it was found. It is written **as a proposal** (`docs/backlog/PROPOSALS.md`),
so the intake turns it into a task. `kind` is `bug` (wrong behaviour — the task gets `priority: bug` and is picked
before every other ready task) or `debt` (everything else):

```markdown
### <short imperative title of the fix>
- kind: bug
- size: S
- paths: `the/file/to/fix.java`, `the/new/test/FixTest.java`
- verify: `<command>`
- expect: <what proves the fix; for a bug: the new test failed before the fix>
- why: `path/to/file:line` — what is wrong and why it matters; found by <task id>, <executor|reviewer>, <date>

<what to do, in plain words, for a fresh agent>
```

Older notes in the free form (`# <what is wrong>` + where/why) are not taken by the intake; they were turned into
tasks by hand on 27 November.

The reviewer does not commit: it opens one GitHub issue per finding titled `[tech-debt] …` with the label
`tech-debt`, its body in the same format (the intake reads open `tech-debt` issues by the owner or collaborators and
closes each once all its proposals are tasks). The merge gate allows adding these files to any task PR
and refuses changing or deleting existing ones.

How a finding is classified (all runs):

| Finding | What to do |
|---|---|
| A regression inside the task's own change | fix it in this PR |
| The task itself is wrong or contradicts the intent/ADRs | park the task with the reason |
| Anything else (outside the task's scope) | a tech-debt file; do not fix |
