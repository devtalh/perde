# F1B-3 retained diagnostic excerpts

2026-10-30, Europe/Istanbul. These are excerpts from the repository Gradle consoles, not a claim of clean logs. Full raw logs remain in ignored `build/f1b-3-verification/`. No player account or external game data was read for this evidence.

## Full world run

Command: `.\gradlew.bat runClient '-Pperde.compat=full' -I run/f1b-3-diagnostics/thread-only.init.gradle --console=plain`, with repository-local `GRADLE_USER_HOME`. The temporary init adds only an AWT uncaught-handler watcher, not a Perde dependency or renderer change.

```text
[23:15:43] [main/INFO] (FabricLoader) Loading 73 mods:
    - cloth-config 26.2.155
    - distanthorizons 3.3.3
    - entityculling 1.11.2
    - iris 1.11.4+mc26.2
    - lithium 0.25.3+mc26.2
    - moreculling 1.8.1
    - reeses-sodium-options 2.2.4+mc26.2
    - sodium 0.9.2+mc26.2
    - sodium-extra 0.9.4+mc26.2
[23:16:03] [Render thread/INFO] (Iris) DH Iris events bound.
[23:16:04] [Render thread/INFO] (Iris) Profile: HIGH (+0 options changed by user)
[23:16:05] [Render thread/INFO] (Iris) Using shaderpack: ComplementaryReimagined_r5.9.3.zip
[23:16:46] [Render thread/INFO] (GlDhMetaRenderer) Renderer setup complete
```

The Distant Horizons shutdown exception, including the complete printed stack:

```text
[23:35:05] [DH-Regen Queue [minecraft:overworld] Thread[0]/ERROR] (FullDataUpdatePropagatorV2) Regen queueing exception: java.sql.SQLException: stmt pointer is closed

java.lang.RuntimeException: java.sql.SQLException: stmt pointer is closed
    at knot//com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo.getChildPositionsToRegen(FullDataSourceV2Repo.java:568)
    at knot//com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataUpdatePropagatorV2.queueRegeneration(FullDataUpdatePropagatorV2.java:601)
    at knot//com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataUpdatePropagatorV2.lambda$tryQueueRegeneration$0(FullDataUpdatePropagatorV2.java:543)
    at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1090)
    at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:614)
    at java.base/java.lang.Thread.run(Thread.java:1474)
Caused by: java.sql.SQLException: stmt pointer is closed
    at knot//dh_sqlite.core.SafeStmtPtr.ensureOpen(SafeStmtPtr.java:148)
    at knot//dh_sqlite.core.SafeStmtPtr.safeRunInt(SafeStmtPtr.java:83)
    at knot//dh_sqlite.jdbc3.JDBC3ResultSet.getInt(JDBC3ResultSet.java:331)
    at knot//dh_sqlite.jdbc3.JDBC3ResultSet.getInt(JDBC3ResultSet.java:336)
    at knot//com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo.getChildPositionsToRegen(FullDataSourceV2Repo.java:556)
    ... 5 more
```

Observed during DH level/world shutdown. This localizes the failure to a closed DH SQLite statement used by its regen task. An exact race/data-integrity conclusion would require upstream reproduction. No Perde frame appears, and no fix or suppression was applied.

```text
[23:35:05] [AWT-EventQueue-0/INFO] (Minecraft) [STDOUT]: shutdown on Thread[#94,AWT-EventQueue-0,6,InnocuousForkJoinWorkerThreadGroup]
[23:35:05] [Server thread/INFO] (Minecraft) ThreadedAnvilChunkStorage: All dimensions are saved
[23:35:10] [Render thread/INFO] (AbstractDhRepo) Closing all [3] databases...
[23:35:11] [Render thread/INFO] (ChunkBuilder) Stopping worker threads
BUILD SUCCESSFUL in 19m 32s
8 actionable tasks: 1 executed, 7 up-to-date
Exception in thread "AWT-EventQueue-0" Exception in thread "AWT-EventQueue-0" Exception in thread "AWT-EventQueue-0"
```

The repeated AWT prefixes are all that the merged console printed. The direct event-thread handler produced no capture file. Earlier default-handler instrumentation and a title-only run with JVM `-Xlog:exceptions=info` also failed to recover the missing stack. **AWT exception type and root cause remain unexplained.** Do not conflate these with the separately identified DH exception.

## Resumed Iris shader-on play verification

```text
[23:13:57] [Render thread/INFO] (perde) Perde spike: world screen placed at (1.5, 71.62000000476837, 29.5), facing north.
[23:14:38] [Render thread/INFO] (perde) Perde spike: world browser JavaScript requested: pause.
[23:15:08] [Render thread/INFO] (perde) Perde spike: world browser JavaScript requested: play.
[23:15:22] [Render thread/INFO] (Minecraft) [System] [CHAT] Saved screenshot as 2026-10-30_23.15.22.png
[23:15:36] [Render thread/INFO] (perde) Perde spike: world browser closed (client stopping).
BUILD SUCCESSFUL in 5m 28s
Exception in thread "AWT-EventQueue-0" Exception in thread "AWT-EventQueue-0" Exception in thread "AWT-EventQueue-0"
```

Commands are supporting evidence only. Frozen/changing video frames were visually checked, and F2 images are linked in the report. Neither AWT occurrence prevented the observed pause/play result or normal Gradle completion.

## Other logged diagnostics

OSHI performance-counter registry errors, dev Realms/profile-key authentication failures, Chromium registry/extension discovery messages, JCEF SemVer and dev refmap warnings recur. Iris warns about a nonexistent stone-slab `variant` mapping. Full adds DH/Iris deprecated API caller-name warnings, shader stage-type warnings and advice about G1/high vanilla render distance. The report keeps these separate from render passes and does not describe the logs as error-free.
