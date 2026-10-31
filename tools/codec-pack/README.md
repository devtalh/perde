# Perde codec pack (H.264 / AAC for MCEF)

> **WARNING / UYARI**
> The codec files are **private**. They contain patent-encumbered H.264/AAC decoders.
> **Never** commit, push, upload, attach or share them publicly (GitHub, Modrinth, Discord, file hosts, modpacks).
> Codec dosyaları **özeldir**. Patentli H.264/AAC çözücüleri içerir.
> **Asla** git'e eklemeyin, yüklemeyin, paylaşmayın (GitHub, Modrinth, Discord, dosya siteleri, modpack'ler).

This folder only contains scripts. No binaries are in git, and none ever may be.

## English

Stock MCEF (CEF 146 from jcefmaven) is built without proprietary codecs, so H.264/AAC streams (Twitch, Kick, many video sites) fail. These scripts replace the CEF runtime files in a Minecraft instance with files from a self-built CEF that has `proprietary_codecs=true ffmpeg_branding=Chrome`. See `docs/codec-build/report.md` for how the build is made.

### 1. Create the pack (on the build PC)
```powershell
.\tools\codec-pack\New-PerdeCodecPack.ps1 -DistribDir "D:\cef\build\chromium\src\cef\binary_distrib\<cef_binary_..._windows64_minimal>" -OutputDir D:\cef\pack
```
Writes `perde-codecs-cef-<version>-windows-amd64.zip` (+ `.sha256`) with a `perde-codec-manifest.json` (SHA-256 of every file, target MCEF release tag). It refuses to write inside a git working tree.

### 2. Install into an instance
Close Minecraft first. `-Instance` is the folder that contains `config\` (for Prism: `<instance>\.minecraft` or `<instance>\minecraft`). Start the game once without the pack so MCEF downloads its natives.
```powershell
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<instance folder>" -Pack D:\cef\pack\perde-codecs-....zip
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<instance folder>" -Status
```
What it does: checks that `config\mcef-modern\jcef\build_meta.json` has the same MCEF release tag as the pack, backs up `jcef` to `jcef-original-backup` (once, with a SHA-256 inventory), replaces only the files that already exist in both (libcef.dll, chrome_elf.dll, *.pak, icudtl.dat, v8 snapshot, GPU DLLs), verifies all hashes and writes `config\mcef-modern\perde-codecs-installed.json`. `jcef.dll`, `jcef_helper.exe`, `build_meta.json` and `install.lock` are never touched. MCEF only checks `install.lock` and the `build_meta.json` release tag, so it does not re-download.

### 3. Restore stock files
```powershell
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<instance folder>" -Restore
```

Notes:
- A pack only fits one MCEF/CEF version. If MCEF updates, it wipes `jcef` and downloads stock files again. Build a new pack for the new CEF version.
- Deleting `install.lock` or `build_meta.json` also makes MCEF reinstall stock files.
- Windows x64 only.

## Türkçe

Standart MCEF (jcefmaven'in CEF 146 sürümü) patentli codec'ler olmadan derlenir. Bu yüzden H.264/AAC yayınlar (Twitch, Kick, birçok dizi/video sitesi) oynamaz. Bu betikler, bir Minecraft örneğindeki CEF çalışma dosyalarını `proprietary_codecs=true ffmpeg_branding=Chrome` ile kendimiz derlediğimiz CEF dosyalarıyla değiştirir. Derlemenin nasıl yapıldığı `docs/codec-build/report.md` dosyasında.

### 1. Paketi oluştur (derleme yapılan bilgisayarda)
```powershell
.\tools\codec-pack\New-PerdeCodecPack.ps1 -DistribDir "D:\cef\build\chromium\src\cef\binary_distrib\<cef_binary_..._windows64_minimal>" -OutputDir D:\cef\pack
```
`perde-codecs-cef-<sürüm>-windows-amd64.zip` (+ `.sha256`) oluşturur. İçinde her dosyanın SHA-256 değeri ve hedef MCEF sürümü olan `perde-codec-manifest.json` bulunur. Git klasörü içine yazmayı reddeder.

### 2. Bir örneğe kur
Önce Minecraft'ı kapatın. `-Instance`, içinde `config\` olan klasördür (Prism için `<örnek>\.minecraft` veya `<örnek>\minecraft`). MCEF dosyalarını indirmesi için oyunu bir kez paketsiz başlatın.
```powershell
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<örnek klasörü>" -Pack D:\cef\pack\perde-codecs-....zip
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<örnek klasörü>" -Status
```
Yaptıkları: `config\mcef-modern\jcef\build_meta.json` içindeki MCEF sürümünün paketle aynı olduğunu kontrol eder, `jcef` klasörünü bir kez `jcef-original-backup` olarak yedekler (SHA-256 listesiyle), yalnızca iki tarafta da bulunan dosyaları değiştirir (libcef.dll, chrome_elf.dll, *.pak, icudtl.dat, v8 snapshot, GPU DLL'leri), tüm hash'leri doğrular ve `config\mcef-modern\perde-codecs-installed.json` kaydını yazar. `jcef.dll`, `jcef_helper.exe`, `build_meta.json` ve `install.lock` dosyalarına dokunulmaz. MCEF yalnızca `install.lock` ve `build_meta.json` sürüm etiketine baktığı için dosyaları yeniden indirmez.

### 3. Orijinal dosyaları geri yükle
```powershell
.\tools\codec-pack\Install-PerdeCodecs.ps1 -Instance "<örnek klasörü>" -Restore
```

Notlar:
- Bir paket yalnızca tek bir MCEF/CEF sürümüne uyar. MCEF güncellenirse `jcef` klasörünü silip standart dosyaları yeniden indirir. Yeni CEF sürümü için yeni paket derleyin.
- `install.lock` veya `build_meta.json` silinirse de MCEF standart dosyaları yeniden kurar.
- Yalnızca Windows x64.
