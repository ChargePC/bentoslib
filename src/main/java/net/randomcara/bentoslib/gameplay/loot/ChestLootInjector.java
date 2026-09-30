package net.randomcara.bentoslib.gameplay.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class ChestLootInjector {
    private final Set<ResourceLocation> targetTables;
    private final List<Supplier<? extends Item>> items;
    private final String poolName;
    private final BooleanSupplier enabled;
    private final boolean enabledFallback;
    private final DoubleSupplier chance;
    private final double chanceFallback;

    private ChestLootInjector(Builder builder) {
        this.targetTables = Set.copyOf(builder.targetTables);
        this.items = List.copyOf(builder.items);
        this.poolName = builder.poolName;
        this.enabled = builder.enabled;
        this.enabledFallback = builder.enabledFallback;
        this.chance = builder.chance;
        this.chanceFallback = builder.chanceFallback;
    }

    public void onLootTableLoad(LootTableLoadEvent event) {
        if (!this.targetTables.contains(event.getName()) || !this.isEnabled()) {
            return;
        }

        float rolledChance = (float) Math.min(1.0D, this.getChance());
        if (rolledChance <= 0.0F) {
            return;
        }

        LootPool.Builder pool = LootPool.lootPool().name(this.poolName).setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(rolledChance));

        for (Supplier<? extends Item> entry : this.items) {
            pool.add(LootItem.lootTableItem(entry.get()).setWeight(1));
        }

        event.getTable().addPool(pool.build());
    }

    private boolean isEnabled() {
        try {
            return this.enabled.getAsBoolean();
        } catch (IllegalStateException e) {
            return this.enabledFallback;
        }
    }

    private double getChance() {
        try {
            return this.chance.getAsDouble();
        } catch (IllegalStateException e) {
            return this.chanceFallback;
        }
    }

    public static final class Builder {
        private final Set<ResourceLocation> targetTables;
        private final List<Supplier<? extends Item>> items;
        private final String poolName;
        private BooleanSupplier enabled = () -> true;
        private boolean enabledFallback = true;
        private DoubleSupplier chance = () -> 1.0D;
        private double chanceFallback = 1.0D;

        private Builder(Set<ResourceLocation> targetTables, List<Supplier<? extends Item>> items, String poolName) {
            this.targetTables = targetTables;
            this.items = items;
            this.poolName = poolName;
        }

        public static Builder create(Set<ResourceLocation> targetTables, List<Supplier<? extends Item>> items, String poolName) {
            return new Builder(targetTables, items, poolName);
        }

        public Builder enabledWhen(BooleanSupplier enabled, boolean fallback) {
            this.enabled = enabled;
            this.enabledFallback = fallback;
            return this;
        }

        public Builder chance(DoubleSupplier chance, double fallback) {
            this.chance = chance;
            this.chanceFallback = fallback;
            return this;
        }

        public ChestLootInjector build() {
            return new ChestLootInjector(this);
        }
    }
}
