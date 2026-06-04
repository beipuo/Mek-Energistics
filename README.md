# Mek Energistics

[中文说明](README_ZH.md)

Mek Energistics adds AE2-connected Mekanism machines that process encoded patterns, use AE power, and return completed products to the ME network.

## Features

- Adds ME variants of Mekanism machines.
- Adds ME factories and a ME Factory Installer for upgrading supported Mekanism machines.
- Supports factory tiers, while original Mekanism factory installers can continue upgrading ME factory tiers.
- Built-in pattern provider assembly for AE2 encoded patterns.
- Machines act as AE2 crafting providers when connected to an ME network.
- Accepts crafting requests from AE2, pushes ingredients into the machine, and processes Mekanism recipes.
- Returns item, fluid, and chemical outputs to the ME network when configured.
- Supports AE power usage while keeping FE compatibility.
- Supports AE channel passthrough using the default AE channel behavior.
- Preserves Mekanism-style GUIs, side configuration, auto-eject configuration, upgrades, and configurator behavior.
- Adds AE output toggles to Mekanism item, chemical, and fluid side configuration screens.
- Supports JEI recipe viewing and hides duplicate ME factory variants from JEI displays.
- Supports Jade integration for displaying AE network status.

## Required Dependencies

| Mod | Description |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228 or newer |
| Applied Energistics 2 | ME network, channels, power, and pattern provider behavior |
| Mekanism | Base machines, recipes, GUIs, and factory system |
| Applied Mekanistics | Chemical AE key support |

## Compatible Mods

| Mod | Integration |
| --- | --- |
| JEI | Recipe display support and ME machine catalysts |
| Jade | Displays whether a machine is connected to an AE network |
| Mekanism Extras | Higher-tier factories and matching ME variants |
| Mekanism More Machine | Additional machines and factory variants |
| Mekanism Generators | Works with Mekanism power setups |
| AE2 JEI Integration | Works alongside AE2 and JEI recipe viewing |
| Advanced AE | Compatible with AE network extensions |
| Omni Cells | Compatible with AE storage extensions |
| Mekanism Unleashed | Can be loaded alongside this mod |

## Usage

1. Place an ME machine and connect it to an AE network.
2. Open the machine GUI and use the pattern button to access the built-in pattern provider assembly.
3. Insert AE2 encoded patterns.
4. Request the pattern from an AE terminal.
5. The machine receives ingredients, processes the matching Mekanism recipe, and returns completed products according to its configuration.

## Build

```powershell
.\gradlew.bat build --no-problems-report
```
