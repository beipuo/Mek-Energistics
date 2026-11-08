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
- Publishes a first-party ME pattern-automation SPI so third-party Mekanism machines can take ME pattern-provider upgrades without shipping ME block variants. See [ME Pattern Automation SPI](docs/me-pattern-automation.md).

## Configuration

Mek Energistics uses NeoForge's built-in mod configuration screen: open the Mods list, select Mek Energistics, and press Config. Client options live in `config/`, server options in `<world>/serverconfig/`.

| Option (key) | File | Default | Range | Description |
| --- | --- | --- | --- | --- |
| Pattern Pages (`patternPages`) | server | 2 | 1–16 | Pages in each machine's built-in pattern provider assembly; each page has 36 encoded pattern slots. **Requires a world restart.** Lowering it hides the higher slots but keeps their patterns saved on the machine. |
| Prefer Network Energy (`preferNetworkEnergy`) | server | Enabled | – | When connected to an AE network, drain AE network (and Applied Flux) energy before the machine's local FE buffer. Disable to use the local buffer first. |
| Prefer Applied Flux FE (`preferAppliedFluxNetworkFe`) | server | Disabled | – | With Applied Flux installed, drain FE stored by Applied Flux before AE network energy. |
| Smart Pattern Multiplication by Default (`smartPatternMultiplicationDefault`) | server | Disabled | – | Default for newly placed machines. A machine's own saved setting always wins. |
| Hide JEI Machine Variants (`hideJeiMachineVariants`) | client | Enabled | – | Hides duplicate ME factory variants (and the ME Output Interface upgrade card) from JEI. Recipe pages keep the basic ME machine as the catalyst; the hidden variants stay craftable and usable. |

Server options are per world and synced to connected clients, so in multiplayer only the server operator can change them.

## Required Dependencies

| Mod | Minimum version | Description |
| --- | --- | --- |
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.238 | |
| Applied Energistics 2 | 19.2.17 | ME network, channels, power, and pattern provider behavior |
| Mekanism | 10.7.19 | Base machines, recipes, GUIs, and factory system |
| Applied Mekanistics | 1.6.3 | Chemical AE key support |

## Compatible Mods

| Mod | Integration |
| --- | --- |
| JEI | Recipe display support and ME machine catalysts |
| Jade | Displays whether a machine is connected to an AE network |
| Mekanism Extras | Higher-tier factories and matching ME variants |
| Mekanism More Machine (1.4.0+) | Additional machines, large machines and factory variants |
| Evolved Mekanism / Evolved Mekanism Extras | Alloyer, Chemixer, Solidification Chamber, Thermalizer and higher-tier factories |
| Mekanism Magic | ME upgrade cards for its machines through the pattern automation SPI |
| ExtendedAE | Rename machines in pattern access terminals with the quartz cutting knife |
| DataEnergistics, OmniSequence, NeoECOAE, Thunderbolt Core | Counted / batch crafting CPUs push many copies of a pattern in one operation |
| Applied Flux | Optional network FE storage support |
| Mekanism Generators, AE2 JEI Integration, Advanced AE, Omni Cells, Mekanism Unleashed | Can be loaded alongside this mod |

If an optional mod is installed but its version is not supported, the matching integration switches itself off and the log says so (`Mek Energistics integration ... disabled`).

## Usage

1. Place an ME machine and connect it to an AE network.
2. Open the machine GUI and use the pattern button to access the built-in pattern provider assembly.
3. Insert AE2 encoded patterns.
4. Request the pattern from an AE terminal.
5. The machine receives ingredients, processes the matching Mekanism recipe, and returns completed products according to its configuration.

## ME Upgrade Cards

Plain Mekanism machines can also join the ME network without being replaced, by installing an upgrade card in their Mekanism upgrade slot:

- **ME Pattern Provider Upgrade** – adds the built-in pattern provider assembly.
- **ME Passive Crafting Upgrade** – requires the pattern provider; periodically submits the installed patterns directly from ME storage (interval 1–72000 ticks, multiplier 1–64 per order).
- **ME Output Interface Upgrade** – keeps configured items stocked in the machine from ME storage. Cannot be combined with the other two cards, and cannot be removed while it still holds stock.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Installer does nothing | Read the action-bar message: another player may have the machine's GUI open, the space for a multiblock may be obstructed, or you may lack build permission. |
| ME upgrade card is rejected by an addon machine | Search the log for `Mek Energistics integration ... disabled`: the installed addon version is not supported. |
| Smart pattern multiplication switched itself off | A crafting CPU pushed a pre-batched input set; the log names the machine and pattern. Re-enable it in the pattern window. |
| `Skipping invalid encoded pattern` in the log | The pattern no longer decodes (for example its recipe was removed by a datapack). Re-encode it. |

## Build

```powershell
.\gradlew.bat build --no-problems-report                  # compile, unit tests, jar
.\gradlew.bat runGameTestServer                           # in-game GameTests
.\gradlew.bat runClient -Pmekenergistics.debugRuns=true   # client with DEBUG logging
```
