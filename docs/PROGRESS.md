# Perde - Progress Log

Every Codex task MUST update this file before finishing (in the same PR).
This is the handoff point between Codex sessions and accounts: the next session starts from here.

## Current state
- Phase: 0 (prep)
- Last merged task: none
- Active task: F0-3 Fabric mod scaffold (branch `chore/f0-3-scaffold`)
- Next task: F1B-1 MCEF dependency + GUI smoke test
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

### F0-3 - Fabric mod scaffold
- Branch / PR: `chore/f0-3-scaffold` (PR pending)
- Status: partial (verification in progress)
- What changed: added the Fabric 26.2 Gradle scaffold, Java 25 toolchain, split common/client source sets, logging-only entrypoints, English/Turkish placeholder translations, build CI, README, MIT license, ignore rules and line-ending rules. No gameplay or MCEF dependency.
- Version evidence: Fabric API `0.161.0+26.2` POM returned HTTP 200 from [Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.161.0+26.2/fabric-api-0.161.0+26.2.pom). Loom `1.17-SNAPSHOT` and Gradle `9.5.1` match [MCEF Modern commit df61843](https://github.com/DimasKama/mcef-modern/tree/df61843); [Fabric's 26.2 example](https://github.com/FabricMC/fabric-example-mod/tree/26.2) confirms the plugin, `implementation` dependencies and split source sets. Loader compilation version and runtime minimum are both `0.19.5`.
- How it was verified (commands + result):
  - `java -version` and `javac -version`: Temurin JDK 25.0.4. `git pull`: clean main fast-forwarded to the docs bootstrap.
  - Gradle wrapper jar SHA-256 matched the official checksum; distribution SHA-256 is pinned. `GRADLE_USER_HOME` was set to the repo-local `.gradle-user-home/` for every Gradle command.
  - `.\gradlew.bat build --console=plain`: passed in 2m 2s. A license-renaming closure's `Task.project` deprecation was fixed; `.\gradlew.bat build --console=plain --warning-mode all` then passed in 2s without warnings. There are no unit test sources yet (`test NO-SOURCE`).
  - Jar inspection: both entrypoints, expanded metadata, both lang files and MIT license are present. `javap -verbose` confirms Java 25 bytecode (major version 69).
  - `.\gradlew.bat runServer --console=plain`: passed, with `run/eula.txt` set to `eula=true` and the test server bound to `127.0.0.1:25565`. Exactly one common init log at 16:17:08, no client init log, `Done (2.415s)!` at 16:17:13, then console `stop` at 16:17:29; Gradle exited successfully. All times are 2026-10-30 Europe/Istanbul.
  - `git diff --cached --check`: passed. `AGENTS.md` and `docs/SPEC.md` are unchanged; game data and Gradle caches are gitignored. Client verification is pending.
- Not done / left for next task: finish client title screen/F2 proof, then push/open PR. MCEF work belongs to F1B-1.
- Known issues / risks: Loom uses the upstream reference's mutable `1.17-SNAPSHOT` (resolved to 1.17.21 during these checks). Minecraft's OSHI library logs Windows performance-counter registry warnings/errors during server startup; the server nevertheless reaches Done and shuts down cleanly. No system settings were changed.

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
