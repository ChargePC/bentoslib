package net.randomcara.bentoslib.client.render.area;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AreaVisualClient {
    private static final Map<UUID, AreaEffect> ACTIVE_EFFECTS = new HashMap<>();
    private static final Map<Item, AreaPreset> AREA_BY_ITEM = new HashMap<>();

    private static final double GROUND_LIFT = 0.065D;
    private static final float FLOOR_ALPHA = 0.20F;
    private static final float FLOOR_GLOW_ALPHA = 0.10F;
    private static final double COLOR_LINE_WIDTH = 0.30D;
    private static final double WHITE_LINE_WIDTH = 0.095D;
    private static final double WALL_HEIGHT = 2.50D;
    private static final float WALL_BOTTOM_ALPHA = 0.34F;
    private static final float WALL_MID_ALPHA = 0.17F;
    private static final float WALL_TOP_ALPHA = 0.0F;
    private static final float WHITE_R = 0.92F;
    private static final float WHITE_G = 1.0F;
    private static final float WHITE_B = 0.96F;

    private static final double WALL_INSET = 0.075D;

    public static void register(Item item, int argbColor, float halfSize, int durationTicks) {
        AREA_BY_ITEM.put(item, new AreaPreset(argbColor, halfSize, durationTicks));
    }

    public static boolean hasActiveEffects() {
        return !ACTIVE_EFFECTS.isEmpty();
    }

    public static void clearActiveEffects() {
        ACTIVE_EFFECTS.clear();
    }

    public static void start(UUID ownerId, int argbColor, float halfSize, int durationTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        long now = level.getGameTime();
        ACTIVE_EFFECTS.put(ownerId, new AreaEffect(ownerId, argbColor, halfSize, now, now + durationTicks));
    }

    public static void startFromActivatedStack(Player player, ItemStack stack) {
        AreaPreset preset = AREA_BY_ITEM.get(stack.getItem());
        if (player != null && preset != null) {
            start(player.getUUID(), preset.argbColor(), preset.halfSize(), preset.durationTicks());
        }
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        ACTIVE_EFFECTS.values().removeIf(effect -> effect.endTick() <= now);

        if (ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (AreaEffect effect : ACTIVE_EFFECTS.values()) {
            renderArea(level, matrix, buffer, effect, partialTick, now);
        }

        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static void renderArea(ClientLevel level, Matrix4f matrix, BufferBuilder buffer, AreaEffect effect, float partialTick, long now) {
        Player owner = level.getPlayerByUUID(effect.ownerId());
        if (owner == null) {
            return;
        }

        float effectAlpha = effect.getAlpha(now, partialTick);
        if (effectAlpha <= 0.01F) {
            return;
        }

        double ownerX = Mth.lerp(partialTick, owner.xOld, owner.getX());
        double ownerY = Mth.lerp(partialTick, owner.yOld, owner.getY());
        double ownerZ = Mth.lerp(partialTick, owner.zOld, owner.getZ());
        double centerX = Math.floor(ownerX) + 0.5D;
        double centerZ = Math.floor(ownerZ) + 0.5D;
        double halfSize = effect.halfSize();
        int minX = Mth.floor(centerX - halfSize);
        int maxX = Mth.floor(centerX + halfSize);
        int minZ = Mth.floor(centerZ - halfSize);
        int maxZ = Mth.floor(centerZ + halfSize);
        int ownerBlockY = Mth.floor(ownerY);
        double barrierBaseY = findGroundYNearPlayer(level, Mth.floor(ownerX), Mth.floor(ownerZ), ownerBlockY) + GROUND_LIFT + 0.018D;
        double barrierTopY = barrierBaseY + WALL_HEIGHT;

        float r = effect.red();
        float g = effect.green();
        float b = effect.blue();
        renderFloor(level, matrix, buffer, minX, maxX, minZ, maxZ, ownerBlockY, r, g, b, effectAlpha);
        renderBorderAndWalls(level, matrix, buffer, minX, maxX, minZ, maxZ, ownerBlockY, barrierBaseY, barrierTopY, r, g, b, effectAlpha);
    }

    private static void renderFloor(ClientLevel level, Matrix4f matrix, BufferBuilder buffer, int minX, int maxX, int minZ, int maxZ, int ownerBlockY, float r, float g, float b, float effectAlpha) {
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                double y = findGroundYNearPlayer(level, x, z, ownerBlockY) + GROUND_LIFT;
                addHorizontalQuad(buffer, matrix, x, y, z, x + 1.0D, z + 1.0D, r, g, b, FLOOR_ALPHA * effectAlpha);
                addHorizontalQuad(buffer, matrix, x + 0.10D, y + 0.012D, z + 0.10D, x + 0.90D, z + 0.90D, r, g, b, FLOOR_GLOW_ALPHA * effectAlpha);
            }
        }
    }

    private static void renderBorderAndWalls(ClientLevel level, Matrix4f matrix, BufferBuilder buffer, int minX, int maxX, int minZ, int maxZ, int ownerBlockY, double barrierBaseY, double barrierTopY, float r, float g, float b, float effectAlpha) {
        for (int x = minX; x < maxX; x++) {
            renderXEdgeSegment(level, matrix, buffer, x, minZ, minZ, ownerBlockY, barrierBaseY, barrierTopY, r, g, b, effectAlpha);
            renderXEdgeSegment(level, matrix, buffer, x, maxZ - 1, maxZ, ownerBlockY, barrierBaseY, barrierTopY, r, g, b, effectAlpha);
        }

        for (int z = minZ; z < maxZ; z++) {
            renderZEdgeSegment(level, matrix, buffer, minX, z, minX, ownerBlockY, barrierBaseY, barrierTopY, r, g, b, effectAlpha);
            renderZEdgeSegment(level, matrix, buffer, maxX - 1, z, maxX, ownerBlockY, barrierBaseY, barrierTopY, r, g, b, effectAlpha);
        }
    }

    private static void renderXEdgeSegment(ClientLevel level, Matrix4f matrix, BufferBuilder buffer, int blockX, int insideZ, int edgeZ, int ownerBlockY, double barrierBaseY, double barrierTopY, float r, float g, float b, float effectAlpha) {
        double groundY = findGroundYNearPlayer(level, blockX, insideZ, ownerBlockY) + GROUND_LIFT + 0.018D;
        double colorHalfWidth = COLOR_LINE_WIDTH * 0.5D;
        double whiteHalfWidth = WHITE_LINE_WIDTH * 0.5D;

        addHorizontalQuad(buffer, matrix, blockX, groundY + 0.012D, edgeZ - colorHalfWidth, blockX + 1.0D, edgeZ + colorHalfWidth, r, g, b, 0.78F * effectAlpha);
        addHorizontalQuad(buffer, matrix, blockX, groundY + 0.032D, edgeZ - whiteHalfWidth, blockX + 1.0D, edgeZ + whiteHalfWidth, WHITE_R, WHITE_G, WHITE_B, effectAlpha);

        double wallZ = insideZ >= edgeZ ? edgeZ + WALL_INSET : edgeZ - WALL_INSET;
        double wallBaseY = Math.min(barrierBaseY, groundY);
        double wallTopY = Math.max(barrierTopY, groundY + WALL_HEIGHT);
        double midY = wallBaseY + (wallTopY - wallBaseY) * 0.45D;
        addVerticalQuadX(buffer, matrix, blockX, blockX + 1.0D, wallBaseY, midY, wallZ, r, g, b, WALL_BOTTOM_ALPHA * effectAlpha, WALL_MID_ALPHA * effectAlpha);
        addVerticalQuadX(buffer, matrix, blockX, blockX + 1.0D, midY, wallTopY, wallZ, r, g, b, WALL_MID_ALPHA * effectAlpha, WALL_TOP_ALPHA);
    }

    private static void renderZEdgeSegment(ClientLevel level, Matrix4f matrix, BufferBuilder buffer, int insideX, int blockZ, int edgeX, int ownerBlockY, double barrierBaseY, double barrierTopY, float r, float g, float b, float effectAlpha) {
        double groundY = findGroundYNearPlayer(level, insideX, blockZ, ownerBlockY) + GROUND_LIFT + 0.018D;
        double colorHalfWidth = COLOR_LINE_WIDTH * 0.5D;
        double whiteHalfWidth = WHITE_LINE_WIDTH * 0.5D;

        addHorizontalQuad(buffer, matrix, edgeX - colorHalfWidth, groundY + 0.012D, blockZ, edgeX + colorHalfWidth, blockZ + 1.0D, r, g, b, 0.78F * effectAlpha);
        addHorizontalQuad(buffer, matrix, edgeX - whiteHalfWidth, groundY + 0.032D, blockZ, edgeX + whiteHalfWidth, blockZ + 1.0D, WHITE_R, WHITE_G, WHITE_B, effectAlpha);

        double wallX = insideX >= edgeX ? edgeX + WALL_INSET : edgeX - WALL_INSET;
        double wallBaseY = Math.min(barrierBaseY, groundY);
        double wallTopY = Math.max(barrierTopY, groundY + WALL_HEIGHT);
        double midY = wallBaseY + (wallTopY - wallBaseY) * 0.45D;
        addVerticalQuadZ(buffer, matrix, wallX, wallBaseY, midY, blockZ, blockZ + 1.0D, r, g, b, WALL_BOTTOM_ALPHA * effectAlpha, WALL_MID_ALPHA * effectAlpha);
        addVerticalQuadZ(buffer, matrix, wallX, midY, wallTopY, blockZ, blockZ + 1.0D, r, g, b, WALL_MID_ALPHA * effectAlpha, WALL_TOP_ALPHA);
    }

    private static void addHorizontalQuad(BufferBuilder buffer, Matrix4f matrix, double x0, double y, double z0, double x1, double z1, float r, float g, float b, float a) {
        vertex(buffer, matrix, x0, y, z0, r, g, b, a);
        vertex(buffer, matrix, x1, y, z0, r, g, b, a);
        vertex(buffer, matrix, x1, y, z1, r, g, b, a);
        vertex(buffer, matrix, x0, y, z1, r, g, b, a);

        vertex(buffer, matrix, x0, y, z1, r, g, b, a);
        vertex(buffer, matrix, x1, y, z1, r, g, b, a);
        vertex(buffer, matrix, x1, y, z0, r, g, b, a);
        vertex(buffer, matrix, x0, y, z0, r, g, b, a);
    }

    private static void addVerticalQuadX(BufferBuilder buffer, Matrix4f matrix, double x0, double x1, double y0, double y1, double z, float r, float g, float b, float bottomAlpha, float topAlpha) {
        vertex(buffer, matrix, x0, y0, z, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x1, y0, z, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x1, y1, z, r, g, b, topAlpha);
        vertex(buffer, matrix, x0, y1, z, r, g, b, topAlpha);

        vertex(buffer, matrix, x1, y0, z, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x0, y0, z, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x0, y1, z, r, g, b, topAlpha);
        vertex(buffer, matrix, x1, y1, z, r, g, b, topAlpha);
    }

    private static void addVerticalQuadZ(BufferBuilder buffer, Matrix4f matrix, double x, double y0, double y1, double z0, double z1, float r, float g, float b, float bottomAlpha, float topAlpha) {
        vertex(buffer, matrix, x, y0, z0, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x, y0, z1, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x, y1, z1, r, g, b, topAlpha);
        vertex(buffer, matrix, x, y1, z0, r, g, b, topAlpha);

        vertex(buffer, matrix, x, y0, z1, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x, y0, z0, r, g, b, bottomAlpha);
        vertex(buffer, matrix, x, y1, z0, r, g, b, topAlpha);
        vertex(buffer, matrix, x, y1, z1, r, g, b, topAlpha);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z).color(r, g, b, a).endVertex();
    }

    private static double findGroundYNearPlayer(ClientLevel level, int blockX, int blockZ, int ownerBlockY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight(), ownerBlockY - 10);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, ownerBlockY);
        for (int y = maxY; y >= minY; y--) {
            pos.set(blockX, y, blockZ);
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES) && (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty())) {
                return y + 1.0D;
            }
        }

        return ownerBlockY;
    }

    private record AreaPreset(int argbColor, float halfSize, int durationTicks) {
    }

    private record AreaEffect(UUID ownerId, int argbColor, float halfSize, long startTick, long endTick) {
        float red() {
            return ((this.argbColor >> 16) & 255) / 255.0F;
        }

        float green() {
            return ((this.argbColor >> 8) & 255) / 255.0F;
        }

        float blue() {
            return (this.argbColor & 255) / 255.0F;
        }

        float getAlpha(long now, float partialTick) {
            float age = (now - this.startTick) + partialTick;
            float remaining = (this.endTick - now) - partialTick;
            return Mth.clamp(age / 6.0F, 0.0F, 1.0F) * Mth.clamp(remaining / 18.0F, 0.0F, 1.0F);
        }
    }
}
