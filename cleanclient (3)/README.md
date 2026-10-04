# CleanClient (starting draft)

Fabric client mod for Minecraft 1.21.4: **Chest ESP**, **Freecam**, clean **ClickGUI + HUD**.

## Build
Requires JDK 21 and Gradle 8.11+ (or copy the wrapper from the official Fabric example mod).

    gradle wrapper --gradle-version 8.11.1
    ./gradlew build

Output: `build/libs/cleanclient-0.1.0.jar` -> drop into `.minecraft/mods` with Fabric Loader + Fabric API.
Dev run: `./gradlew runClient`.

## Controls
- Right Shift: open GUI (drag header, click rows to toggle)
- H: toggle Freecam (WASD / Space / Shift, Ctrl to go faster)

## Known limitations / next steps
- Not compiled/tested yet. Minecraft rendering and GUI internals change between versions; if a method name
  doesn't resolve, check Yarn mappings for your version.
- Freecam: mouse look still rotates the real player. Proper fix: mixin into `Mouse#updateMouse` / `Entity#changeLookDirection`
  to rotate the camera entity instead while Freecam is on.
- Chest ESP draws each half of a double chest separately; no per-module settings yet (colors, range, tracers).
- Check the rules of any server before using ESP/freecam; many anti-cheats and rule sets prohibit them.
