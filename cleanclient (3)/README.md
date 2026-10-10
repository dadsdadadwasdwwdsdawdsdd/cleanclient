# Block Outlines (Meteor addon)

Module `block-outlines` (Render): smooth coloured outline on the targeted block; when it breaks, a black/white SVG icon pops and spins.

Settings (group Render): color, alpha (0.1-1), smooth, speed (2-30).

Build: JDK 21, `./gradlew build` -> `build/libs/`. Replace `src/main/resources/assets/blockoutlines/icon.svg` to change the icon (supports `<polygon points fill>` and `<rect>`; fill brighter than grey = white, otherwise black).

## Cosmetics category
- `wings` - animated feathered wings (Angel / Demon / Energy / Custom); flap on jump/fall, spread when gliding, fold when crouching.
- `china-hat` - glowing cone hat that follows your head (Rainbow / Gradient / Solid).
- `animated-sky` - flowing sky colors via Meteor's Ambience (your Ambience settings are restored when turned off).
- `pets` - bee, turtle and allay that follow you. Client side only.
