# KoHs Hands Position documentation

[Back to overview](../README.md)

## Guides

- [Development and test notes](DEVELOPMENT.md)
- [Regression tests](../src/test/java/dev/zymekoh/handposition)

## Root build

| Setting | Value |
| --- | --- |
| Minecraft | `26.1.2` |
| Mod version | `1.1.1` |
| Loader | Fabric `0.19.3` |
| Loom | `1.17.19` |
| JDK | 25 |

Official Minecraft names for the 26.x root target.
The source of truth is [gradle.properties](../gradle.properties) and
[build.gradle](../build.gradle); each additional target declares its own dependencies.

## Working folders

Run build commands from the repository root unless a target's instructions say
otherwise. `build/`, `.gradle/` and `run/` hold local build or game state and are
excluded from Git. Preserve saves, configuration and logs when organizing files.
Audits describe the version and checks recorded at the time; they do not imply
that every later change has been tested in a running game.
