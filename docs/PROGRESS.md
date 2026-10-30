# Perde - Progress Log

Every Codex task MUST update this file before finishing (in the same PR).
This is the handoff point between Codex sessions and accounts: the next session starts from here.

## Current state
- Phase: 0 (prep)
- Last merged task: none
- Next task: F0-3 Fabric mod scaffold (branch `chore/f0-3-scaffold`)
- Confirmed Minecraft version: 26.2 (Fabric API 0.161.0+26.2, MCEF 26.2 build)

## Task log
Newest first. Format:

```
### <task id> - <title>
- Branch / PR:
- Status: done | partial (session ended) | blocked
- What changed:
- How it was verified (commands + result):
- Not done / left for next task:
- Known issues / risks:
```

### F0-2 - Docs bootstrap
- Branch / PR: committed directly to main by Claude (lead)
- Status: done
- What changed: added AGENTS.md, docs/SPEC.md, docs/PROGRESS.md
- How it was verified: n/a (docs only)
- Not done / left for next task: everything else
- Known issues / risks: none

## Known issues
- (none yet)

## Decisions log (short; full rationale lives in Notion)
- 2026-10-30: Codex accounts are used sequentially, never in parallel. Order: Phase 0, then 1B (feasibility), then 1A only if 1B succeeds.
- 2026-10-30: Radio feature added as Phase 7 (after screen phases). Command root `/radyo`. Separate jar `perde-radyo`.
- 2026-10-30: Target Minecraft 26.2 confirmed. Repo public, license MIT, recipe confirmed.
- 2026-10-30: Codex runs locally on Talha's Windows PC, tests the game itself in Gradle `run/` only, attaches screenshots.
