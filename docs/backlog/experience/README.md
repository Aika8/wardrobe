# Experience log (write-only for agents)

At the end of every executor run that opens or updates a PR, the run adds **one new file**
`docs/backlog/experience/<YYYY-MM-DD>-<task id>-<HHMM>.md` with 2–3 lines: where it stumbled and how it got past it.
The reviewer puts its 2–3 lines in its review comment under "Experience".

Agents never read this folder and never change prompts, rules or the queue because of it: in other teams, agents
that edited their own instructions from such notes made them worse, while write-only notes worked. Aika reads them
after the holiday and decides what becomes a rule.
