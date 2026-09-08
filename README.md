# KoHs Hands Position

[![GitHub](https://img.shields.io/badge/GitHub-Hands--Position-6f2cff?style=for-the-badge&logo=github)](https://github.com/kerlycanelita/KoHs-Hands-Position)
[![Issues](https://img.shields.io/badge/Report-Issues-a855f7?style=for-the-badge&logo=githubissues)](https://github.com/kerlycanelita/KoHs-Hands-Position/issues)
[![Discord](https://img.shields.io/badge/Join-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/9t2VxEF7UU)

<p align="center">
  <img src="src/main/resources/assets/kohs_hands_position/icon.png" alt="KoHs Hands Position icon" width="220">
</p>

**First-person hand size and spacing, adjusted against a live preview of your own
skin and the items you are actually holding.**

Client-side Fabric mod for Minecraft Java **26.1.2**, by **Zymekoh**.

## What it changes

Two settings, both purely presentational:

- **Hand size — 50 % to 150 %.** Starts at 100 %.
- **Hand spacing — −40 % to +40 %.** Negative values bring the hands closer
  together, positive values push them apart. Starts at 0 %. It is a relative
  visual offset, not an absolute position.

Both affect the first-person presentation of the hands and the items they hold.

## What it does not change

Gameplay animations still run through Minecraft's own renderer. Nothing here
touches interactions, reach, timing, packets or the items themselves — only how
the hands are drawn in first person.

## The preview

The configuration screen renders your real skin and your real equipment above a
translucent purple background, with no blur.

- The offhand appears in the preview only when it holds an item.
- The controls fall into two rows when the available width is small.
- The screen pauses singleplayer worlds while the hands are being adjusted.
- Maps are shown one-handed in the preview when the offhand is empty; in play
  they keep their usual animation.

The preview uses a rest pose and uniform lighting, which is what makes small
adjustments readable.

Changes appear in the preview immediately and are used in first person during
play. They are saved when the screen is closed with **Done** or **Esc**, to
`config/kohs_hands_position.json`.

Opening the configuration from the main menu works and the values can be
adjusted, but a world has to be joined to see the hands themselves.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.3 or newer and use
   Java 25.
2. Install Fabric API for 26.1.2.
3. Put `kohs-hands-position-1.0.0+mc26.1.2.jar` in the `mods` folder.
4. Install Mod Menu 18 to reach the configuration screen.

Then open **Mods → KoHs Hands Position → Configure** from inside a world.

## Building

With JDK 25 and `JAVA_HOME` pointing at it:

```powershell
.\gradlew.bat build
```

The installable output is `build/libs/kohs-hands-position-1.0.0+mc26.1.2.jar`.
The file ending in `-sources.jar` contains the source code and is not the
playable build.

## Verification of this release

- Builds cleanly with JDK 25 and Fabric Loom 1.17.19.
- Minecraft 26.1.2 starts cleanly with Fabric API 0.155.2 and Mod Menu 18.
- Checked in Mod Menu: name, author Zymekoh, final icon, and the screen opening
  with both sliders and the Spanish translation.
- The in-world test is left to the player by express request: the hand preview
  has not been visually validated in this release.

## Development references

- [Fabric changes for Minecraft 26.1](https://www.fabricmc.net/2026/03/14/261.html)
- [Mod Menu: configuration screen integration](https://github.com/TerraformersMC/ModMenu)

## License

All rights reserved. See [LICENSE](LICENSE).

## Credits

Made by **zymekoh**.
