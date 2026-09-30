package net.randomcara.bentoslib.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.randomcara.bentoslib.client.keybind.BentosLibKeyMappings;

import java.util.List;

public final class ActivatableArtifactTooltipHelper {
    public static void addActivationLine(List<Component> tooltip) {
        Component key = getActivationKeyText().copy().withStyle(Style.EMPTY.withColor(0xFFD966));
        tooltip.add(Component.translatable("bentoslib.tooltip.press_to_activate", key).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
    }

    private static Component getActivationKeyText() {
        Component bound = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> BentosLibKeyMappings.ACTIVATE_CURIO_ITEM.getTranslatedKeyMessage());
        return bound != null ? bound : Component.translatable("bentoslib.tooltip.activation_key");
    }
}
