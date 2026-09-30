package net.randomcara.bentoslib.gameplay.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class EntityDropReplacer {
    private final ResourceLocation sourceEntityId;
    private final String replacedNamespace;
    private final float replacementChance;
    private final List<ResourceLocation> replacements;
    private final Logger logger;

    public EntityDropReplacer(ResourceLocation sourceEntityId, String replacedNamespace, float replacementChance, List<ResourceLocation> replacements, Logger logger) {
        this.sourceEntityId = sourceEntityId;
        this.replacedNamespace = replacedNamespace;
        this.replacementChance = replacementChance;
        this.replacements = List.copyOf(replacements);
        this.logger = logger;
    }

    public void onLivingDrops(LivingDropsEvent event) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (!this.sourceEntityId.equals(entityId) || event.getEntity().getRandom().nextFloat() >= this.replacementChance) {
            return;
        }

        List<ItemEntity> candidates = this.getReplaceableDrops(event);
        if (candidates.isEmpty()) {
            return;
        }

        Item replacement = this.getRandomReplacement(event);
        if (replacement != null) {
            ItemEntity drop = candidates.get(event.getEntity().getRandom().nextInt(candidates.size()));
            drop.setItem(new ItemStack(replacement));
        }
    }

    private List<ItemEntity> getReplaceableDrops(LivingDropsEvent event) {
        List<ItemEntity> drops = new ArrayList<>();

        for (ItemEntity drop : event.getDrops()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(drop.getItem().getItem());
            if (itemId != null && this.replacedNamespace.equals(itemId.getNamespace())) {
                drops.add(drop);
            }
        }

        return drops;
    }

    @Nullable
    private Item getRandomReplacement(LivingDropsEvent event) {
        ResourceLocation itemId = this.replacements.get(event.getEntity().getRandom().nextInt(this.replacements.size()));
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            this.logger.warn("Missing replacement item for {} drop injection: {}", this.sourceEntityId, itemId);
        }

        return item;
    }
}
