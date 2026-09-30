package net.randomcara.bentoslib.api.curio;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface IActivatableCurioItem {
    boolean activate(ServerPlayer player, ItemStack stack);
}
