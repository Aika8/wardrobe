# Tech-debt register

Problems a run found **outside its own task**. The run does not fix them; it records them and goes on. Aika (or a
queued task she writes) decides what to do with them.

Each finding is its own file, so parallel PRs never conflict: `docs/backlog/tech-debt/<YYYY-MM-DD>-<task id>-<n>.md`,
added in the PR of the task during which it was found. Format:

```markdown
# <one line: what is wrong>
- where: `path/to/file:line` (or the command that shows it)
- why it matters: <behaviour or risk, one line>
- found by: <task id>, <executor|reviewer>, <date>
```

The reviewer does not commit: it lists such findings in its review comment under "Out of scope (tech debt)", and the
executor of the next fix round of that PR adds them as files. The merge gate allows adding these files to any task PR
and refuses changing or deleting existing ones.

How a finding is classified (all runs):

| Finding | What to do |
|---|---|
| A regression inside the task's own change | fix it in this PR |
| The task itself is wrong or contradicts the intent/ADRs | park the task with the reason |
| Anything else (outside the task's scope) | a tech-debt file; do not fix |
