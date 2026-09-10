# KoHs Hands Position development

[Back to overview](../README.md)

## Release 1.1.1 verification notes

- Builds cleanly with JDK 25 and Fabric Loom 1.17.19.
- Automated tests cover config migration and persistence, independent values,
  overlapping-hand selection, rapid press/drag/release sequences, cancellation,
  normalized movement and layouts from 320 × 180 to 1920 × 1080 logical pixels.
- A regression test exercises Minecraft's actual `CommandEncoder` validation:
  vanilla preview textures reject pixel reads without `USAGE_COPY_SRC`. Version
  1.1.1 grants this usage only to this mod's hand preview color textures.
- The original 1.1.1 verification did not launch Minecraft. Actual
  in-game visuals and GPU rendering still need the user's instance test.

## Development references

- [Fabric changes for Minecraft 26.1](https://www.fabricmc.net/2026/03/14/261.html)
- [Mod Menu: configuration screen integration](https://github.com/TerraformersMC/ModMenu)
