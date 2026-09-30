package net.randomcara.bentoslib.curio;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public final class CurioActivationHelper {
    public static Optional<SlotResult> findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).resolve().flatMap(handler -> handler.findFirstCurio(item));
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return findEquipped(entity, item).isPresent();
    }

    public static ItemStack getEquippedStack(LivingEntity entity, Item item) {
        return findEquipped(entity, item).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }
}
