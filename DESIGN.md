# mining-engine — Design

Shared "balanced auto-harvest" library extracted from the `trees` mod
(`minecraft-mod-trees`), consumed by `jlt_trees` (auto tree chopping) and
`jlt_ores` (auto ore vein mining), and by any future harvest-style mod.

Companion documents:
- `TODO.md` (this repo) — the master, phase-by-phase execution checklist for all three repos.
- `../trees/MIGRATION.md` — how the trees mod migrates onto this engine.
- `../mining-ores/DESIGN.md` — the ores mod built on this engine.

**Behavioral source of truth:** `trees` repo at commit `4f18707`, file
`src/main/java/media/jlt/minecraft/mods/trees/TreesMod.java`. Extraction is a
*transplant*, not a rewrite — preserve check ordering and edge-case semantics
exactly. When this document and that file disagree, the code wins; note the
discrepancy in `TODO.md`'s deviation log.

## 1. Purpose and non-goals

The engine owns everything about *balance* — pacing, hunger/XP taxation,
durability budgeting and protection, pause/resume, batch caps — plus generic
connected-block scanning. It knows nothing about blocks, items, tags,
translation keys, or mod ids.

Non-goals: event registration, block matching (tags are version-specific),
commands, rendering, translation. Those stay in the mods.

## 2. Hard constraints

1. **No `net.minecraft.*`, no `net.fabricmc.*` imports anywhere.** This is the
   whole point: the engine must build and test without Minecraft or Fabric and
   stay valid across game/loader versions.
2. **Zero runtime dependencies.** Exception: Gson is `compileOnly` (every
   Minecraft version ships Gson on the classpath, so it is always present at
   runtime). Also `testImplementation` for config tests.
3. **No logging dependency.** Engine code returns result/reason objects; the
   mods log.
4. Compile with `options.release = 21` (Gradle toolchain 25 — the only JDK
   installed — but emit Java 21 bytecode so the jar also serves older
   MC/Java-21-era versions).

## 3. Coordinates and layout

- Gradle: `java-library` + `maven-publish`. **No fabric-loom.**
- Group `media.jlt.minecraft`, artifact `mining-engine`, `rootProject.name = 'mining-engine'`.
- Version: semver, independent of Minecraft versions. `0.1.0` = pure-logic move
  (Phase 2), `0.2.0` = scanner + orchestrator (Phases 3–4).
- Publishing: `./gradlew publishToMavenLocal` for development. Mods consume via
  `mavenLocal()` + `implementation` and embed via Loom `include(...)` (jar-in-jar;
  Fabric Loader dedupes the nested jar by version when both mods are installed).

Base package `media.jlt.minecraft.engine`:

| Package | Contents |
|---|---|
| `engine.util` | `HarvestDistance`, `InventoryIdentity`, `MessageRateLimiter`, `ToolTier` |
| `engine.scan` | `Pos3i`, `BlockMatcher`, `ScanLimits`, `ConnectedBlockScanner`, `AdjacentBlockCollector`, `ScanDepthBounds` |
| `engine.balance` | `BalanceSettings`, `DurabilityBudget`, `DurabilitySubstitutionPlan`, `ExperienceMath`, `HarvestPlanner` |
| `engine.schedule` | `HarvestScheduler`, `ScheduledBreak`, `ScheduleTiming`, `HungerPauseTracker`, `InstantBreakBatch`, `HarvestFeedback` |
| `engine.port` | `HarvestAdapter`, `ActivePlayer`, `ToolSnapshot`, `BreakResult` |
| `engine.config` | `JsonConfigStore` |

## 4. Classes moved verbatim from trees (Phase 2)

Source: `../trees/src/main/java/media/jlt/minecraft/mods/trees/logic/`.
Move each class **and its JUnit test**, change only package (and name where
noted). No behavior changes.

| Trees class | Engine class | Change |
|---|---|---|
| `ChopDistance` | `util.HarvestDistance` | rename only |
| `TreeScanBounds` | `scan.ScanDepthBounds` | rename only |
| `DurabilityBudget` | `balance.DurabilityBudget` | none |
| `DurabilitySubstitutionPlan` | `balance.DurabilitySubstitutionPlan` | none |
| `ExperienceMath` | `balance.ExperienceMath` | none |
| `HungerPauseTracker` | `schedule.HungerPauseTracker` | none |
| `InstantBreakBatch` | `schedule.InstantBreakBatch` | none |
| `InventoryIdentity` | `util.InventoryIdentity` | none (already generic via `IntFunction<T>`) |
| `MessageRateLimiter` | `util.MessageRateLimiter` | key type becomes `Object` (mods key hints by enum or string) |
| `ScheduleTiming` | `schedule.ScheduleTiming` | none |
| `TreeToolTier` | `util.ToolTier` | rename; **delete `fromAxeItem(ItemStack)`** (Minecraft type). Mods call `ToolTier.fromMaxDamage(stack.getMaxDamage())`. Keep `fromMaxDamage`, `fromConfigName`, `configName`, `isLowerThan`, and the durability-bucket thresholds comment. |

Stays in trees (tree-domain): `TreeType`, `LeafCollectionPolicy`, `TreeScanner`
(becomes an adapter), `ScannedTree` classification (`treeType`, 2×2 detection).

## 5. Generic scanning (Phase 3)

Extracted from `TreeScanner.scanConnectedLogs` / `collectLeafLikeBlocks`.

```java
public record Pos3i(int x, int y, int z) { /* offset(dx,dy,dz), equals/hash from record */ }

@FunctionalInterface
public interface BlockMatcher { boolean matches(int x, int y, int z); }

public record ScanLimits(
    int maxBlocks,                 // e.g. maxConnectedLogBlocks / maxVeinBlocks
    int maxHorizontalRadius,       // XZ euclidean, squared compare (long math)
    OptionalInt maxDepthBelowOrigin // empty = unbounded below (ore veins); present for trees
) {}

public final class ConnectedBlockScanner {
    /** 26-neighbor BFS from origin. Seeds origin plus its 26 neighbors up front
     *  (AFTER-break events see the origin as air). Returns matches sorted
     *  top-down (descending Y) — preserves TreeScanner.sortByTopDown order. */
    public static List<Pos3i> scan(Pos3i origin, BlockMatcher matcher, ScanLimits limits) {…}
}

public final class AdjacentBlockCollector {
    /** For each primary block, collect matcher hits within Chebyshev distance 2,
     *  excluding primaries, capped at maxBlocks, sorted top-down.
     *  (TreeScanner.collectLeafLikeBlocks generalized; unused by ores initially.) */
    public static List<Pos3i> collect(Collection<Pos3i> primaries, BlockMatcher matcher, int maxBlocks) {…}
}
```

Preserve BFS details exactly: visited-set discipline, horizontal-radius check
before depth check before matcher call, `maxBlocks` checked on `found.size()`,
long math in the squared-distance compare.

## 6. Balance planning (Phase 4)

`HarvestPlanner` extracts the pre-harvest budget math from
`TreesMod.runAutoChop` (lines ~357–415 at the reference commit):

```java
public final class HarvestPlanner {
    public static PlanResult plan(
        int requestedBlocks,
        boolean creative,
        boolean toolDamageable,
        int remainingDurability,        // maxDamage - damage
        boolean protectionActive,       // mod computes from settings mode + tool enchanted state
        int durabilityCostPerBlock,     // max(1, durabilityMultiplier)
        BalanceSettings settings,       // floor + substitution knobs
        int availableXpAboveFloor       // Integer.MAX_VALUE if creative
    ) {…}

    public record PlanResult(
        Outcome outcome,                // PROCEED | BLOCKED_DURABILITY_FLOOR | BLOCKED_INSUFFICIENT_DURABILITY
        int blocksSupported,            // trimmed count when limited by floor
        boolean trimmed,                // supported < requested
        int substitutedBlocks,
        int xpCostPerSubstitutedBlock,
        int predictedRemainingDurability, // Integer.MAX_VALUE when creative/undamageable
        int requiredDurability            // for BLOCKED_INSUFFICIENT_DURABILITY message args
    ) {}
}
```

Semantics (transplanted, in order):
- creative or not damageable → `PROCEED`, all blocks, predicted `MAX_VALUE`.
- protection active → `spendable = max(0, remaining - protectionFloor)`;
  delegate to `DurabilitySubstitutionPlan.create(...)`; supported `<= 0` →
  `BLOCKED_DURABILITY_FLOOR`; supported `<` requested → trimmed;
  `predicted = remaining - (nonSubstituted × costPerBlock) - substituted`.
- else → `required = requested × costPerBlock`; `remaining < required` →
  `BLOCKED_INSUFFICIENT_DURABILITY`; `predicted = remaining - required`.

The leaf-phase budget check (`DurabilityBudget.canSpend(predicted, leafCost,
reserve)`) stays a direct call from trees; it is already engine code.

## 7. Scheduler (Phase 4 — the core)

`HarvestScheduler<L, T>` owns the queue and the per-tick state machine from
`TreesMod.onServerTick` / `restaggerScheduledBreaks` / `applyResourceTax` /
`applyHungerTax` / `applyXpTax` / `canContinueAutoChop`. `L` = opaque level
handle, `T` = opaque tool handle (mods bind `ServerLevel` / `ItemStack`; the
engine never inspects them, only passes them to the adapter).

```java
public final class ScheduledBreak<L, T> {   // fields mirror TreesMod.ScheduledBreak
    UUID playerId; L level; Pos3i pos; long executeAtTick; boolean instantBatch;
    int extraDurability; int substitutionXpCost; boolean applyBaseDurability;
    ToolTier requiredTier; T boundTool; int matchKind;   // mod-defined constant (LOG=0/LEAF=1; ORE=0)
    float exhaustionPerBlock; int xpCostPerBlock;
}

public final class HarvestScheduler<L, T> {
    public HarvestScheduler(HarvestAdapter<L, T> adapter, Supplier<BalanceSettings> settings) {}
    public void schedule(List<Pos3i> positions, /* per-entry params as in TreesMod.scheduleOrBreakNow */) {}
    public boolean hasScheduledWork(UUID playerId) {}
    public void tick(long currentTick) {}
    public void clearAll() {}                 // server-stopped reset (also clears pauses)
    public void clearHungerPause(UUID id) {}  // called on new harvest start
}
```

Ports (`engine.port`):

```java
public interface HarvestAdapter<L, T> {
    /** Player online AND in `level`, else null (entry dropped, pause cleared). */
    ActivePlayer<T> resolve(UUID playerId, L level);
    boolean blockMatches(L level, Pos3i pos, int matchKind);
    /** Bound-tool slot-swap break (TreesMod.breakAndDropWithBoundTool stays mod-side
     *  inside this implementation). */
    boolean breakBlock(ActivePlayer<T> player, int toolSlot, T boundTool, Pos3i pos,
                       boolean applyBaseDurability, int extraDurability);
    void feedback(ActivePlayer<T> player, HarvestFeedback reason, Object... args);
}

public interface ActivePlayer<T> {
    UUID id();
    boolean isDeadOrDying();
    double x(); double y(); double z();
    boolean isCreative();
    int foodLevel(); float saturation();
    void addExhaustion(float amount);
    int totalExperience();                    // ExperienceMath-based estimate, mod-side
    void giveExperiencePoints(int delta);     // negative = spend
    int findToolSlot(T boundTool);            // identity search; -1 = gone
    boolean isValidTool(T tool, ToolTier requiredTier); // type gate (axe/pickaxe) + tier
    ToolSnapshot toolSnapshot(T tool);        // damageable, enchanted, maxDamage, damage, isEmpty
    int efficiencyLevel(T tool);              // for resume restagger delay
}
```

```java
public enum HarvestFeedback {
    CANCELLED_DEATH, CANCELLED_DISTANCE, CANCELLED_TOOL_MISSING,
    CANCELLED_HUNGER_TIMEOUT, HUNGER_PAUSED, HUNGER_RESUMED,
    NO_SUBSTITUTION_XP, NO_XP, TOOL_BROKE, DURABILITY_FLOOR /* arg: floor */
}
```

Mods map `HarvestFeedback` → their own translation keys. **The engine never
sees a translation key or mod id.**

### 7.1 Tick contract (transplant of `onServerTick`, order is load-bearing)

Given `currentTick`; queue is insertion-ordered.

0. Queue empty → `hungerPauses.removeOrphans(∅)`, return.
1. Per-tick cascade sets: `timedOut`, `noXp`, `invalidTool`, `durabilityFloor`,
   `inactive`, `resumed`; map `instantBreakCounts`.
2. For each entry, in order:
   a. Player in `invalidTool`/`durabilityFloor`/`inactive` → remove, continue.
   b. Player in `resumed` → leave entry, continue.
   c. `executeAtTick > currentTick` → continue.
   d. `adapter.resolve` null → clear player's hunger pause, remove, continue.
   e. Dead → `inactive` + `CANCELLED_DEATH`, remove, continue.
   f. `HarvestDistance.isWithinRange(player, pos, settings.maxHarvestDistance)`
      false → `inactive` + `CANCELLED_DISTANCE`, remove, continue.
   g. `instantBatch` entry without `InstantBreakBatch.hasCapacity(count,
      settings.maxInstantBlocksPerTick)` → `executeAtTick = currentTick + 1`, continue.
   h. `findToolSlot < 0` or `!isValidTool(boundTool, requiredTier)` →
      `invalidTool` + `CANCELLED_TOOL_MISSING`, remove, continue.
   i. Tax mode `HUNGER_ONLY` and player is hunger-paused:
      - can continue (food > 0 or saturation > 0) → clear pause, recompute
        `delayPerBlockTicks` from efficiency + settings, restagger **all** of the
        player's entries via `ScheduleTiming.resumeTick(currentTick, delay, queuePos)`
        (queuePos starts at 1, insertion order), add to `resumed`,
        `HUNGER_RESUMED`, continue (entry kept).
      - else past pause deadline → `timedOut` + clear pause +
        `CANCELLED_HUNGER_TIMEOUT`, remove, continue.
      - else → `executeAtTick = max(executeAtTick, currentTick + 20)`, continue.
   j. Tax mode `HUNGER_ONLY`, hunger tax enabled, not creative, cannot continue
      → mark pause (deadline `currentTick + hungerResumeTimeoutTicks`, only if
      absent), `HUNGER_PAUSED`, `executeAtTick = max(executeAtTick, currentTick + 20)`,
      continue.
   k. `!adapter.blockMatches(level, pos, matchKind)` → remove silently, continue.
   l. Cannot afford automated durability (protection active and
      `!DurabilityBudget.canSpend(remaining, (applyBase?1:0)+max(0,extra), floor)`;
      creative or protection inactive always affords) → `durabilityFloor` +
      `DURABILITY_FLOOR(floor)`, remove, continue.
   m. `substitutionXpCost > 0` and not creative and
      `totalXp - cost < settings.xpTaxFloor` → `noXp` + `NO_SUBSTITUTION_XP`,
      remove, continue.
   n. `adapter.breakBlock(...)` false → remove, continue.
   o. `instantBatch` → increment `instantBreakCounts`.
   p. `substitutionXpCost > 0` and not creative → `giveExperiencePoints(-cost)`.
   q. Resource tax (§7.2): `PAUSE_HUNGER` → mark pause + `HUNGER_PAUSED`;
      `STOP_NO_XP` → `NO_XP` + `noXp`.
   r. Bound tool snapshot now empty → `TOOL_BROKE` + `invalidTool`.
   s. Remove entry.
3. Remove every remaining entry whose player is in any cascade set
   (five separate removeIf passes, as in the original).
4. `hungerPauses.removeOrphans(players that still have entries)`.

### 7.2 Resource tax (transplant of `applyResourceTax` and helpers)

- `XP_ONLY` → XP tax only.
- `XP_AFTER_HUNGER_DEPLETED` → hunger tax if enabled and not creative and it
  succeeds; otherwise fall through to XP tax.
- `HUNGER_ONLY` → hunger tax; failure = `PAUSE_HUNGER`.

Hunger tax: disabled/creative/`exhaustionPerBlock <= 0` → success (no-op). If
`hungerTaxFloor > 0` and `foodLevel <= floor` and `saturation <= 0` → fail
**before** adding exhaustion. Otherwise `addExhaustion(exhaustionPerBlock)`,
then succeed iff `foodLevel > 0 || saturation > 0`.

XP tax: creative or `cost <= 0` → success. `totalXp - cost < xpTaxFloor` →
fail, else `giveExperiencePoints(-cost)`.

## 8. BalanceSettings

Immutable POJO (or record) of the shared knobs; mods construct it from their
flat JSON configs (trees' JSON stays byte-compatible — see `../trees/MIGRATION.md`).

```java
enum TaxMode { HUNGER_ONLY, XP_ONLY, XP_AFTER_HUNGER_DEPLETED }        // "hunger_only" etc.
enum DurabilityProtectionMode { OFF, ENCHANTED_ONLY, ALL }             // "off"/"enchanted"/"all"

TaxMode taxMode; boolean enableHungerTax; int hungerTaxFloor; int xpTaxFloor;
int hungerResumeTimeoutTicks;
DurabilityProtectionMode durabilityProtectionMode; int durabilityProtectionFloor;
int durabilityMultiplier;
boolean enableDurabilityXpSubstitution; int durabilityXpSubstitutionWindow;
int xpPerSubstitutedDurabilityPoint;
boolean enableTimePenalty; Map<Integer,Integer> delayTicksByEfficiencyLevel;
int maxInstantBlocksPerTick; int maxHarvestDistance;
```

Include `delayTicksForEfficiencyLevel(int level)` (clamp/fallback semantics
copied from `ModConfig.delayTicksForEfficiencyLevel`) and a
`isProtectionActive(boolean toolDamageable, boolean toolEnchanted)` helper
(transplant of `TreesMod.isDurabilityProtectionActive`).

## 9. Config store

`JsonConfigStore` extracts the Fabric-free mechanics of trees' `ModConfig`:
Gson round-trip, write-default-file-if-missing, `load`/`reload` returning a
`ReloadResult<T>` (config or error message), sanitize hook. Signature takes a
`java.nio.file.Path` config directory — **`FabricLoader` stays in the mods**
(they pass `FabricLoader.getInstance().getConfigDir()`). No logger; return
enough info for the mod to log.

## 10. Testing

- Moved classes keep their moved tests (same assertions, new packages).
- New engine tests, all pure JUnit with fake `HarvestAdapter`/`ActivePlayer`:
  - Scanner: BFS bounds (count cap, radius, optional depth), origin seeding,
    top-down order; adjacent collector cap and exclusion of primaries.
  - Planner: creative bypass, floor-blocked, trim, substitution counts,
    predicted durability, insufficient-durability path.
  - Scheduler (the payoff — these paths are untested today): pacing respects
    `executeAtTick`; instant-batch spillover to next tick; hunger pause →
    resume restagger ordering; pause timeout; mid-run durability floor cascade;
    tool missing/broke cascades; death/distance cancellation; offline-player
    cleanup; orphaned pause cleanup; per-mode tax outcomes (all three modes).

## 11. Naming note

"mining-engine" is the repo name; the library is harvest-generic (trees are
"mined" too). Keep `media.jlt.minecraft.engine` as the package — short and
domain-neutral.
