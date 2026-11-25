# 0001. Record architecture decisions as ADRs

- Status: Accepted
- Date: 2026-11-25
- Pattern(s): Architecture Decision Record (Michael Nygard, 2011)

## Context
The code is written mostly by AI agents, often alone in the cloud while Aika is away. Without a record nobody
remembers why a choice was made, and agents drift apart. Small behaviour choices need a place too, but they do
not deserve an ADR.

## Decision
Each decision that is costly to reverse gets a numbered file in `docs/adr/` from the template, **before** the code.
An Accepted ADR is never edited; it is superseded by a new one. Small behaviour choices go to `DECISIONS.md`, one
bullet each. Code implementing a named pattern links its ADR in a doc comment.

## Why this and not the alternatives
| Alternative | Why not |
|---|---|
| Only commit messages | Not discoverable, no alternatives or risks |
| A page in Notion or the vault | Drifts away from the code, not reviewed with the change |
| One big design document | Goes stale, cannot supersede one part |

## Pros
- Decisions are reviewed in the same change as the code.
- Agents in the cloud read the same reasons Aika had.

## Risks and costs
- ADRs written after the fact to justify code. Mitigation: ADR first, then code; the merge gate freezes Accepted ADRs.
- Too many trivial ADRs. Rule: only what is costly to reverse; the rest is a `DECISIONS.md` bullet.

## How to explain it (teaching note)
"Next to the code we keep short records: what we chose, which alternatives we rejected and what it costs. New
people and AI agents read them instead of guessing the intent from the code."
