# Changelog

## 0.0.11-beta

- Fixed transparent/missing adjacent block faces next to ME machines, including the ME Metallurgic Infuser case reported with AE2 Additions wireless transceivers.
- Restored Mekanism's original custom block shapes for ME machines and factories so adjacent blocks cull faces correctly.
- Applied the same shape fix across Mekanism, Mekanism Extras factories, and Mekanism: MoreMachine shape-sensitive machines.
- Verified the update with Gradle build.

## 0.0.10-beta

- Fixed ME machine dismantling so wrench removal keeps Mekanism machine data and drops installed AE patterns instead of deleting them.
- Fixed ME Factory Installer upgrades so installed upgrades, machine data, side configuration, energy, and AE patterns are preserved during conversion.
- Fixed Isotopic Centrifuge upgrade paths, including ME Isotopic Centrifuge to ME Basic Centrifuging Factory and Mekanism: MoreMachine centrifuging factories.
- Updated ME centrifuging factory models to use this mod's redesigned local factory indicator bars.
- Fixed machine light occlusion so ME machines, factories, and tall machines match Mekanism's original lighting behavior.
- Verified the update with Gradle build.

## 0.0.9-beta

- Fixed AE-network energy usage for Mekanism machines so recipe execution can consume AE power without displaying network power as stored FE.
- Added AE-aware recipe energy handling for Mekanism: MoreMachine base machines, regular factories, and advanced factories.
- Added AE-aware recipe energy handling for Mekanism Extras regular factories, MoreMachine-derived factories, and advanced factories.
- Kept original Mekanism/compat-mod recipe logic intact while wrapping only the cached recipe energy view.
- Verified the update with `compileJava`.

## 0.0.8-beta

- Added ME Planting Station and ME Replicator base machines for Mekanism: MoreMachine, so the ME Factory Installer maps the original base machines to their matching ME base versions instead of a basic factory.
- Improved ME Factory Installer target resolution and blocked it from remapping machines that are already ME machines.
- Added bounding-block handling for the ME Factory Installer when converting or interacting with tall machines.
- Fixed MekMM planting/replicating base machine AE support initialization during block entity construction.
- Refactored machine and factory registration so tile registration and AE grid-node capability registration share centralized descriptors.
- Matched the ME Planting Station item display transform with the ME planting factory item models.
