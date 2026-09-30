package net.randomcara.bentoslib.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

public final class TooltipHelper {
    public static void addShiftDescription(List<Component> tooltip, Component... descriptionLines) {
        if (Screen.hasShiftDown()) {
            for (Component line : descriptionLines) {
                tooltip.add(line);
            }
        } else {
            tooltip.add(Component.translatable("bentoslib.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable("bentoslib.tooltip.for_details").withStyle(ChatFormatting.GRAY)));
        }
    }

    public static Component line(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color));
    }
}
