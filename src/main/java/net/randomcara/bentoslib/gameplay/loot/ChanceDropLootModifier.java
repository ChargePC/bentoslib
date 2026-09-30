package net.randomcara.bentoslib.gameplay.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public abstract class ChanceDropLootModifier extends LootModifier {
    protected ChanceDropLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    protected abstract boolean matches(Entity entity);

    protected abstract ItemStack createDrop(LootContext context);

    protected abstract float getDropChance(LootContext context);

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity != null && this.matches(entity) && context.getRandom().nextFloat() < this.getDropChance(context)) {
            generatedLoot.add(this.createDrop(context));
        }

        return generatedLoot;
    }
}
