# PLAN6 Registry and Enum Audit

## Summary

This audit covers `MeMekanismMachine`, installer upgrade routing, and registry-facing lookup paths after the final hardening cleanup. No code rewrite is recommended in this refactor pass: the current enum is hard to read, but it is also the registry/name compatibility boundary for normal machines, factories, and optional compat machines.

## Findings

- `MeMekanismMachine` uses many overloaded constructors with marker primitive parameters (`char`, `short`, `byte`, `int`, `long`, booleans) to distinguish machine families. This is readable only with local context and remains the largest maintainability issue.
- Factory lookup methods are linear scans over enum constants. This is acceptable for current usage and safer than introducing cached maps while refactoring compatibility code.
- Installer upgrades are already routed through `MeInstallerUpgradeHandler` and `MeInstallerTargetResolver`, with optional compat checks kept outside enum construction. This boundary should stay intact.
- Registry names are derived from existing enum fields and constructor logic. Rewriting constructors or enum constants risks changing block IDs, item IDs, lang keys, or installer target resolution.

## Decisions

- Do not Builder-ize `MeMekanismMachine` in this refactor train.
- Do not reorder enum constants, rename fields, or change constructor arguments.
- Do not add registry-name caches unless profiling or a real bug proves lookup cost matters.
- If future work revisits the enum, start with non-behavioral tests or generated snapshots of registry names, translation keys, base tiers, factory tiers, and installer upgrade targets.

## Future Candidate

A future major-version cleanup can introduce a typed descriptor record for new machines only after snapshot tests prove every current `registryName()`, `translationKey()`, `baseTier()`, `factoryTypeName()`, `requiredModId()`, `getBasicFactory()`, and `getNextFactory()` result is unchanged.
