# BentosLib

Shared building blocks for Bento's Minecraft mods. Forge 1.20.1.

This is a library, not a content mod. On its own it adds nothing to the game: no items, no
entities, no worldgen, no network channel, no gameplay events. Everything in here is switched on
by the mod that depends on it.

The one exception is the curio activation keybind, which registers itself on the client, because
a `KeyMapping` has to exist before a tooltip can name it.

## Requirements

* Minecraft `1.20.1`, Forge `47.4.9`, Java 17
* [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) `5.14.1+1.20.1`, optional. Only the curio
  activation framework needs it, so mods that skip it don't pull Curios in
* JEI is optional and only needed if you use `FastCyclingItemStackRenderer`

## Depending on it

Publish it to your local Maven:

```bash
./gradlew publishToMavenLocal
```

Then, in your `build.gradle`:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation fg.deobf('net.randomcara.bentoslib:bentoslib:0.3-1.20.1')
}
```

And in your `mods.toml`:

```toml
[[dependencies.yourmod]]
modId="bentoslib"
mandatory=true
versionRange="[0.3,)"
ordering="BEFORE"
side="BOTH"
```

## What's in it

### Curios

`IActivatableCurioItem` is the whole contract for an item that answers the activation key.
Implement it, then wire the packet to a channel you already own:

```java
ActivateCurioItemPacket.register(YOUR_CHANNEL, nextPacketId++);
```

`CurioActivationHelper` looks up equipped curios.

### Client

`TooltipHelper` for shift-to-expand descriptions and `ActivatableArtifactTooltipHelper` for the
activation line. `BannerBackLayer` renders a banner on a mob's back.

`AreaVisualClient` draws a ground marker around the player, the kind of thing an activated item
throws down. Register your items with it during client setup and register the renderer on the
Forge bus:

```java
AreaVisualClient.register(YourItems.SOME_ARTIFACT.get(), 0xFF5533, 8.0F, 200);
MinecraftForge.EVENT_BUS.register(AreaVisualRenderEvents.class);
```

Call `AreaVisualClient.clearActiveEffects()` when leaving a world.

### Gameplay

`EventBossBarController` keeps boss bars for temporary events, addressed by id.
`ChestLootInjector` adds a pool to existing loot tables and `ChanceDropLootModifier` is a base
for chance-based entity drops. `EntityDropReplacer` swaps another mod's drop for one of yours
without compiling against it.

Both `ChestLootInjector` and `EntityDropReplacer` take the Forge event from your own
`@Mod.EventBusSubscriber`, they do not hook themselves up.

### World

`TerrainCheckedJigsawStructure` is a jigsaw that only generates on flat, dry ground away from
rivers and oceans. Register the `StructureType` in your namespace and tie it back with
`setStructureType`.

`CategorizedMobSpawnTable` spawns from a category table and skips ids that are not in the
registry, so entries from optional mods do not break anything.

### Compat

`ModGatedPackLoader` mounts a bundled datapack only when the mod it targets is installed. It
exists because `LootDataManager` parses every loot table under `data/` eagerly, so a table naming
another mod's function throws before any `forge:conditions` gets a say.

### Integration

`FastCyclingItemStackRenderer` for JEI.

## Notes

Classes take your mod id and `Logger` as parameters instead of importing anything of yours, and
registry ids stay on your side, so saved worlds do not break if the lib moves.

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`.

## License

MIT. Full text in [LICENSE.txt](LICENSE.txt), which also ships inside the jar.
