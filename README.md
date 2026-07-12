# Mining Engine

`mining-engine` is the Minecraft-independent Java library shared by JLT Trees
and JLT Ores. It owns the deterministic parts of connected-block harvesting:
scanning, durability and XP planning, hunger/timing policy, scheduled work, and
configuration persistence. Consumer mods keep all Fabric and Minecraft types
behind a small port layer.

The current Maven coordinate is:

```text
media.jlt.minecraft:mining-engine:0.2.0
```

## What consumers provide

A consumer creates a `HarvestScheduler<L, T>` with:

- a `HarvestAdapter<L, T>` that resolves a player, checks whether a queued
  block still matches, breaks it with the bound tool, and displays feedback;
- an `ActivePlayer<T>` implementation exposing position, hunger, XP, inventory,
  tool state, and Efficiency level without importing Minecraft into the engine;
- a supplier of current `BalanceSettings`, so a config reload affects queued
  work without rebuilding the scheduler.

The mod handles its platform event and classification logic, uses
`ConnectedBlockScanner` and `HarvestPlanner`, and passes the approved positions
to `HarvestScheduler.schedule`. It must call `tick` once per server tick and
`clearAll` when the server stops. `trees` and `mining-ores` are the reference
adapters for Minecraft/Fabric integrations.

## Local development

The engine produces Java 21-compatible bytecode. A JDK 25 installation is used
by the build toolchain.

```bash
./gradlew clean test build
./gradlew publishToMavenLocal
```

Sibling consumers resolve the library from Maven Local and can embed it in
their Fabric jar:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation "media.jlt.minecraft:mining-engine:${project.engine_version}"
    include "media.jlt.minecraft:mining-engine:${project.engine_version}"
}
```

After changing engine code, publish it locally before rebuilding a consumer.

## Version policy

The engine follows semantic versioning. Patch releases contain compatible
fixes, minor releases add backward-compatible API or behavior, and major
releases may break consumer ports. Consumer mods pin an exact engine version
and bump that property deliberately.

The artifact has no Minecraft or Fabric dependency and its version is not tied
to a Minecraft release. Platform compatibility belongs to each consumer mod;
the engine changes version only when its own API or behavior changes.
