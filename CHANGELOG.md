# Changelog

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
