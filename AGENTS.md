# AGENTS.md - instructions for coding agents (Codex)

You are working on **Perde**, a Minecraft Java Fabric mod. Read these files before doing anything:

1. `docs/SPEC.md` - the specification and decided architecture (source of truth for design)
2. `docs/PROGRESS.md` - what is done, what is next, known issues (source of truth for status)
3. The existing code in this repository (source of truth for implementation)

Do not assume you remember earlier sessions. Rebuild context from these files every time.

## Hard rules
- You run locally on Talha's Windows PC. Work ONLY inside this repository folder. Never touch Prism Launcher instances, the `.minecraft` folder or the KuzCraft server. Game test runs use Gradle's `run/` folder only.
- If a Java 25 JDK is missing, STOP and tell Talha. Never install JDKs or other software system-wide.
- Work only on the task you were given, on its own branch, and open a PR at the end.
- Do not change decisions in `docs/SPEC.md` silently. If something in the spec is impossible or conflicts with the code, stop, explain in the PR description and in `docs/PROGRESS.md`.
- Keep client-only code (MCEF, rendering, screens, keybinds) in the client source set / client packages. The dedicated server must boot without MCEF installed.
- Never trust the client: every control packet is validated and permission-checked on the server.
- Only `http`/`https` URLs are allowed.
- No out-of-scope refactors. Minimal safe change.
- No force push, no history rewrite, no deleting branches or data.
- Do not weaken or delete tests or assertions to make a build pass. Find the root cause.
- No hardcoded machine-specific paths.
- Verify APIs from real sources (MCEF Modern source, Fabric API, Minecraft code available through Loom). Do not invent method names. If you cannot verify something, say so in the PR.
- Code, comments and commit messages in English.

## Definition of done for every task
- `./gradlew build` passes (run it and paste the result summary in the PR).
- Any extra checks the task lists are done by you (you can run `gradlew.bat runClient` / `runServer` and take in-game screenshots with F2). Visual proof goes into the repo (docs folder) and is referenced in the PR. Talha only does a final manual check.
- `docs/PROGRESS.md` updated: task entry (status, what changed, verification, what is left, known issues) and "Current state".
- PR description: summary, files changed, how to test in game step by step, risks.

## If your session may end early
Commit and push small, working increments often. If you are stopped mid-task, the next session will continue from your branch, so keep `docs/PROGRESS.md` honest (mark the task `partial` and list exactly what is left).
