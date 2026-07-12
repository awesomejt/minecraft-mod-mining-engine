# Master TODO — mining-engine extraction + mining-ores implementation

**Executor:** any capable Claude model (Opus/Sonnet). Planned by Fable on
2026-07-11; the design is settled — execute it, don't redesign it.

**Read first, before writing any code:**
1. `DESIGN.md` (this repo) — engine architecture and the tick-loop contract.
2. `../trees/MIGRATION.md` — trees-side changes.
3. `../mining-ores/DESIGN.md` — ores mod spec.
4. `../trees/src/main/java/media/jlt/minecraft/mods/trees/TreesMod.java` at
   commit `4f18707` — the behavioral source of truth for Phase 4.

**Repos** (all under `/shared/projects/minecraft/`, each its own git repo,
work on `main`):
- `mining-engine` → github.com/awesomejt/minecraft-mod-mining-engine (this repo)
- `trees` → github.com/awesomejt/minecraft-mod-trees (working mod, keep green)
- `mining-ores` → github.com/awesomejt/minecraft-mod-mining-ores (empty)
- `reseed` — reference only: has working Fabric gametest wiring to copy (do not modify)

**Ground rules**
- The engine must never import `net.minecraft.*` or `net.fabricmc.*`. If you
  feel you need to, the abstraction is wrong — stop and re-read DESIGN.md §7.
- Phase 4 is a *transplant*: preserve the ordering and semantics of
  `TreesMod.onServerTick` exactly. Do not "improve" balance behavior.
- Trees must stay user-invisible: same mod id, same flat `jlt_trees.json`
  config shape, same translation keys, same `/jlt_trees reload` command.
- Run the relevant repo's `./gradlew test` before every commit. Commit at each
  numbered checkpoint with a descriptive message (do **not** push unless Jason
  asks).
- Port-interface signatures in DESIGN.md §7 are sketches: minor shape
  adjustments are fine if an edge case demands it, but log every deviation in
  the Deviation log at the bottom of this file, and never leak a Minecraft
  type into the engine.
- Items marked **[Jason]** need a human (in-game smoke tests). Ask; don't fake.
- Keep checkboxes in this file updated as you go.

Environment: only JDK is Temurin 25 (`java -version` → 25.0.3). Engine
compiles with `options.release = 21` on that JDK. Trees/ores use the Fabric
toolchain from the trees repo unchanged (Gradle wrapper 9.5.1, Loom
1.17-SNAPSHOT, MC 26.2, loader 0.19.3, fabric-api 0.154.2+26.2).

---

## Phase 1 — Engine skeleton

- [x] 1.1 Copy `gradlew`, `gradlew.bat`, `gradle/wrapper/` from `../trees` into this repo.
- [x] 1.2 `settings.gradle`: `rootProject.name = 'mining-engine'` (no pluginManagement block needed — no Loom).
- [x] 1.3 `build.gradle`:
  ```gradle
  plugins {
      id 'java-library'
      id 'maven-publish'
  }

  group = 'media.jlt.minecraft'
  version = '0.1.0'

  repositories { mavenCentral() }

  dependencies {
      compileOnly 'com.google.code.gson:gson:2.11.0'
      testImplementation 'com.google.code.gson:gson:2.11.0'
      testImplementation platform('org.junit:junit-bom:5.14.2')
      testImplementation 'org.junit.jupiter:junit-jupiter'
      testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  }

  java {
      withSourcesJar()
      toolchain { languageVersion = JavaLanguageVersion.of(25) }
  }

  tasks.withType(JavaCompile).configureEach { options.release = 21 }

  test { useJUnitPlatform() }

  publishing {
      publications { create('mavenJava', MavenPublication) { from components.java } }
  }
  ```
- [x] 1.4 Copy `LICENSE` from trees.
- [x] 1.5 `./gradlew build` passes (empty src is fine).
- [x] 1.6 **Commit** ("Scaffold plain-Java library build").

## Phase 2 — Move pure logic into the engine

- [x] 2.1 Create packages per DESIGN.md §3 and move the 11 classes per the
      table in DESIGN.md §4 (copy from trees, fix package/name; for `ToolTier`
      delete `fromAxeItem`; for `MessageRateLimiter` widen the key parameter to
      `Object`).
- [x] 2.2 Move their tests from `../trees/src/test/...` (rename
      `ChopDistanceTest`→`HarvestDistanceTest`, `TreeScanBoundsTest`→`ScanDepthBoundsTest`,
      `TreeToolTierTest`→`ToolTierTest` dropping any `ItemStack`-based cases —
      those assertions move to trees if they test mod behavior).
- [x] 2.3 `./gradlew test` green in engine. **Commit**, tag nothing, then
      `./gradlew publishToMavenLocal` (engine 0.1.0).
- [x] 2.4 Trees: add to `gradle.properties` `engine_version=0.1.0`; in
      `build.gradle` add `mavenLocal()` to `repositories` and
      ```gradle
      implementation "media.jlt.minecraft:mining-engine:${project.engine_version}"
      include "media.jlt.minecraft:mining-engine:${project.engine_version}"
      ```
- [x] 2.5 Trees: delete the 11 moved classes + their tests; update all imports
      (`TreesMod`, `TreeScanner`, `ModConfig`, remaining tests).
      `TreeToolTier.fromAxeItem(stack)` call sites become
      `ToolTier.fromMaxDamage(stack.getMaxDamage())`. Keep `TreeType`,
      `LeafCollectionPolicy`, `TreeScanner`, `ScannedTree` + their tests.
- [x] 2.6 Trees: `./gradlew test build` green; confirm the built jar under
      `build/libs/` contains the nested engine jar (`META-INF/jars/`).
- [x] 2.7 **Commit trees** ("Consume mining-engine 0.1.0 for pure logic").
      ✅ **Checkpoint A: trees behavior-identical on engine 0.1.0.**

## Phase 3 — Generic scanner in the engine

- [x] 3.1 Implement `Pos3i`, `BlockMatcher`, `ScanLimits`,
      `ConnectedBlockScanner`, `AdjacentBlockCollector` per DESIGN.md §5,
      transplanting BFS/collection logic from trees `TreeScanner` at `4f18707`.
- [x] 3.2 Engine tests: count cap, horizontal radius, optional depth bound
      (empty = unbounded below), origin+26-neighbor seeding, top-down sort,
      collector cap/exclusion.
- [x] 3.3 **Commit engine.**

## Phase 4 — Balance orchestrator in the engine

Transplant from `TreesMod` per DESIGN.md §6–§9. Suggested order:

- [x] 4.1 `BalanceSettings` (+ `TaxMode`, `DurabilityProtectionMode`,
      `delayTicksForEfficiencyLevel`, `isProtectionActive`).
- [x] 4.2 `HarvestPlanner` + tests (§6 semantics; §10 test list).
- [x] 4.3 Ports: `HarvestAdapter`, `ActivePlayer`, `ToolSnapshot`, `BreakResult`
      (drop `BreakResult` if a plain `boolean` suffices — note in deviation log).
- [x] 4.4 `HarvestFeedback` enum.
- [x] 4.5 `ScheduledBreak<L,T>` + `HarvestScheduler<L,T>` implementing the tick
      contract in DESIGN.md §7.1–7.2 **exactly** (cascade sets, resumed-skip,
      restagger, +20-tick defers, five removeIf passes, orphan cleanup).
- [x] 4.6 Scheduler tests with fake adapter/player — cover every path listed in
      DESIGN.md §10 (this is the acceptance bar for the phase).
- [x] 4.7 `JsonConfigStore` extracted from trees `ModConfig` (Path-based, no
      FabricLoader, no Logger) + tests (missing file → defaults written,
      malformed JSON → error result, sanitize hook runs).
- [x] 4.8 Bump engine to `0.2.0`, `./gradlew test publishToMavenLocal`.
- [x] 4.9 **Commit engine.** ✅ **Checkpoint B: engine 0.2.0, orchestrator fully
      unit-tested without Minecraft.**

## Phase 5 — Rewire trees onto the orchestrator

Follow `../trees/MIGRATION.md` §3–§5.

- [x] 5.1 Bump `engine_version=0.2.0`.
- [x] 5.2 `TreeScanner` → thin adapter over `ConnectedBlockScanner` /
      `AdjacentBlockCollector` (keeps `isLogLike`/`isLeafLike` matchers,
      BlockPos↔Pos3i conversion, `ScannedTree` + classification). Existing
      trees tests must keep passing unchanged.
- [x] 5.3 `ModConfig`: delegate IO to `JsonConfigStore`; add
      `toBalanceSettings()`. JSON on disk stays byte-compatible (field names
      and `_docs` untouched).
- [x] 5.4 `TreesMod`: implement the ports; replace queue/tick/tax/pause code
      with `HarvestScheduler`; replace budget block with `HarvestPlanner`; map
      `HarvestFeedback` → translation keys per MIGRATION.md §5. Keep
      entry-gate logic (sneak/axe/natural/tier checks) and both-phase
      (stem/leaf) orchestration mod-side.
- [x] 5.5 Delete the now-dead private methods from `TreesMod` (everything the
      scheduler/planner absorbed). Target: file well under half its current
      830 lines.
- [x] 5.6 `./gradlew test build` green.
- [ ] 5.7 **[Jason]** In-game parity smoke test (PrismLauncher instance
      "26.2"): chop small + 2×2 tree; hunger pause then eat → resume; pause
      timeout cancel; durability floor trim + block; tool-broke mid-chop;
      `/jlt_trees reload`; overlay hints still rate-limited.
- [ ] 5.8 Bump trees `mod_version` to `1.4.0`. **Commit trees.**
      ✅ **Checkpoint C: trees 1.4.0 on engine 0.2.0, user-invisible.**

## Phase 6 — Implement mining-ores

Follow `../mining-ores/DESIGN.md` throughout.

- [x] 6.1 Scaffold from trees: copy wrapper, `settings.gradle`
      (`rootProject.name = 'mod-mining-ores'`), `build.gradle`
      (`archivesName = 'mod-mining-ores'`, engine dep + `include`),
      `gradle.properties` (`mod_version=0.1.0`, `engine_version=0.2.0`, same
      MC/loader/api versions), fabric-specific `.gitignore` entries, LICENSE.
- [ ] 6.2 `fabric.mod.json`: id `jlt_ores`, entrypoint
      `media.jlt.minecraft.mods.ores.OresMod`, same depends block as trees.
- [ ] 6.3 `OreFamily` enum + matcher (DESIGN §3; specific-blocks-before-tags
      ordering — see the nether-gold note) + tests.
- [ ] 6.4 `ModConfig` (flat, per DESIGN §5, delegating to `JsonConfigStore`,
      `toBalanceSettings()`) + sanitization tests; `config/jlt_ores.example.json`
      with `_docs`, mirroring trees' example file style.
- [ ] 6.5 `OresMod` entrypoint per DESIGN §4 (event → gates → vein scan →
      tier gate → planner → schedule; ports implementation mirrors trees —
      pickaxe instead of axe, single phase, no leaf pass, no natural-tree
      check).
- [ ] 6.6 `assets/jlt_ores/lang/en_us.json` per DESIGN §6.
- [ ] 6.7 `./gradlew test build` green; nested engine jar present.
- [ ] 6.8 **[Jason]** In-game smoke test: sneak-mine a coal vein
      (stone pick), deepslate-variant vein continuity, tier gate refusal
      (e.g. iron pick on diamond ore → hint), hunger pause/resume, instant
      batch with Efficiency V, `/jlt_ores reload`.
- [ ] 6.9 **Commit mining-ores** ("Initial jlt_ores mod on mining-engine 0.2.0").
      ✅ **Checkpoint D: ores 0.1.0.**

## Phase 7 — Wrap-up

- [ ] 7.1 Engine `README.md`: what it is, the ports a consumer implements,
      version policy (semver, MC-independent), publishToMavenLocal workflow.
- [ ] 7.2 Trees `README` (if present) + `MIGRATION.md`: mark migration done,
      note engine version.
- [ ] 7.3 Optional stretch (skip unless Jason asks): gametests for ores copied
      from `../reseed` wiring; large-vein tier bump (ores DESIGN §7);
      GitHub Actions `./gradlew test` in each repo.

---

## Deviation log

Record every place the implementation deviates from DESIGN.md / MIGRATION.md /
ores DESIGN.md, with one line of why.

- `DurabilityProtectionMode.ENCHANTED_ONLY` uses config name `enchanted_only`,
  matching trees commit `4f18707` and its existing JSON contract; DESIGN §8
  previously abbreviated this as `enchanted` and was corrected.
- `BreakResult` was dropped because the transplanted tick loop needs only the
  success/failure boolean already returned by `HarvestAdapter.breakBlock`.
