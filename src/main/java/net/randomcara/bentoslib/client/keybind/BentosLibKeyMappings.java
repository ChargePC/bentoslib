package net.randomcara.bentoslib.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.randomcara.bentoslib.BentosLib;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BentosLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BentosLibKeyMappings {
    public static final String CATEGORY = "key.categories.bentoslib";
    public static final KeyMapping ACTIVATE_CURIO_ITEM = new KeyMapping("key.bentoslib.activate_curio_item", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_CURIO_ITEM);
    }
}
