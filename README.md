# Perde

Perde is a Minecraft Java Edition Fabric mod designed to show web pages and
synchronized videos on a resizable screen in front of a single block. Inspired
by WebDisplays, it aims for one block and one control panel, with shared
playback, pause and seeking for everyone watching.

Status: **early development**. The current scaffold only logs common and client
initialization; gameplay and browser integration are planned.

See [the specification](docs/SPEC.md) for the architecture and roadmap, and
[the progress log](docs/PROGRESS.md) for verified work and the next task.

## Build

Requirements: a **Java 25 JDK**. The Gradle wrapper downloads Gradle 9.5.1 and
the build dependencies; no system Gradle installation is needed.

Windows PowerShell, from the repository root:

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat build
```

Linux/macOS:

```sh
GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew build
```

Jars are written to `build/libs/`. The build targets Minecraft **26.2**, Fabric
Loader **0.19.5** and Fabric API **0.161.0+26.2**. Install Fabric API alongside
the mod when using the built jar.

For development, run `.\gradlew.bat runClient` or `.\gradlew.bat runServer`
in the same PowerShell session. Both use the gitignored `run/` folder. The
server requires accepting the Minecraft EULA in `run/eula.txt` before it can
start; enter `stop` in its console to shut it down.

## Türkçe özet

Perde, Minecraft içinde tek blok önünde boyutlandırılabilir bir ekranda web
sayfaları ve senkron video göstermeyi hedefleyen bir Fabric modudur. Proje
erken geliştirme aşamasındadır; bu sürüm yalnızca derlenebilir mod iskeletini
içerir. Derlemek için Java 25 JDK ve yukarıdaki Gradle wrapper komutlarını kullanın.
