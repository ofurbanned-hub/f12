# Mod — Fabric Notepad prank + owner-controlled shutdown

**No automatic shutdown on Minecraft startup.** Minecraft startup opens a Notepad file containing the configured message and an owner-control window. A real Windows shutdown is only requested if the PC owner clicks **Start Windows shutdown** in that window, and only if `shutdown_enabled=true`.

## Build on GitHub

Upload this project preserving folder paths. The workflow is `.github/workflows/build.yml`. In GitHub, open Actions → Build Fabric Mod → Run workflow. Download the JAR from the build artifact and install it in the Fabric instance's mods folder.

This source uses Fabric Loom 1.9-SNAPSHOT, Gradle 8.12, and Java 21. It compiles against Minecraft 1.21.1 with minimal Fabric Loader APIs. **Minecraft 1.21.11 has not been verified.** This project has not been runtime-tested in this environment.

## Configuration

The first launch creates `%USERPROFILE%\.minecraft-mod-prank.properties`. Edit it in Notepad and relaunch Minecraft:

```properties
enabled=true
message=Say bye to ur pc :) - Press F12 to cancel
typing_speed_ms=120
countdown_seconds=30
shutdown_enabled=false
```

- `enabled`: enable/disable the Notepad and control-window sequence.
- `message`: text written into the Notepad file. This version **does not simulate character-by-character typing**; it opens the prepared text file to avoid typing into another window.
- `typing_speed_ms`: reserved compatibility setting; not used by this build.
- `countdown_seconds`: Windows shutdown delay, minimum 30 seconds.
- `shutdown_enabled`: enables the **Start Windows shutdown** button. Changing this to true does **not** trigger shutdown on launch.

## Shutdown and cancellation

Only the PC owner should enable shutdown. The control window clearly labels the action. After clicking Start, Windows schedules a shutdown using `shutdown.exe /s /t N`. Click Cancel or press F12 **while the control window has focus** to run `shutdown.exe /a`. F12 is **not** a global hotkey. The window also attempts to abort when closed. If the abort fails, immediately open a command prompt and run `shutdown /a`. A cancellation request may fail if Windows is already shutting down. Save work before testing.

## Default message in GitHub

Edit `SETTINGS.setProperty("message", ...)` in `src/main/java/com/example/mod/ModClient.java`, then rebuild. An existing local `.minecraft-mod-prank.properties` overrides the built-in default.
