package net.randomcara.bentoslib.client.render.area;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AreaVisualRenderEvents {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && AreaVisualClient.hasActiveEffects()) {
            AreaVisualClient.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
        }
    }
}
