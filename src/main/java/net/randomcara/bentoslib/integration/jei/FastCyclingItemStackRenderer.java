package net.randomcara.bentoslib.integration.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public final class FastCyclingItemStackRenderer implements IIngredientRenderer<ItemStack> {
    private final List<ItemStack> stacks = new ArrayList<>();
    private final long intervalMillis;

    public FastCyclingItemStackRenderer(List<ItemStack> stacks, long intervalMillis) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                this.stacks.add(stack.copy());
            }
        }

        this.intervalMillis = Math.max(50L, intervalMillis);
    }

    @Override
    public void render(GuiGraphics guiGraphics, @Nullable ItemStack ingredient) {
        ItemStack stack = this.getCurrentStack(ingredient);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
        }
    }

    @Override
    public List<Component> getTooltip(@Nullable ItemStack ingredient, TooltipFlag flag) {
        return this.createTooltip(ingredient, flag);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, @Nullable ItemStack ingredient, TooltipFlag flag) {
        tooltip.addAll(this.createTooltip(ingredient, flag));
    }

    @Override
    public Font getFontRenderer(Minecraft minecraft, @Nullable ItemStack ingredient) {
        return minecraft.font;
    }

    private List<Component> createTooltip(@Nullable ItemStack ingredient, TooltipFlag flag) {
        ItemStack stack = this.getCurrentStack(ingredient);
        return stack.isEmpty() ? List.of() : stack.getTooltipLines(Minecraft.getInstance().player, flag);
    }

    private ItemStack getCurrentStack(@Nullable ItemStack fallback) {
        if (this.stacks.isEmpty()) {
            return fallback == null ? ItemStack.EMPTY : fallback;
        }

        return this.stacks.get((int) ((System.currentTimeMillis() / this.intervalMillis) % this.stacks.size()));
    }
}
