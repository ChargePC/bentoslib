package net.randomcara.bentoslib.world.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CategorizedMobSpawnTable<C> {
    private final Map<C, List<ResourceLocation>> entriesByCategory;
    private final String persistentTag;
    private final Logger logger;

    public CategorizedMobSpawnTable(Map<C, List<ResourceLocation>> entriesByCategory, String persistentTag, Logger logger) {
        this.entriesByCategory = Map.copyOf(entriesByCategory);
        this.persistentTag = persistentTag;
        this.logger = logger;
    }

    public boolean spawnRandom(ServerLevel level, C category, double x, double y, double z, float yRot, float xRot, float yHeadRot) {
        List<ResourceLocation> candidates = this.getExistingEntityIds(category);
        RandomSource random = level.getRandom();

        while (!candidates.isEmpty()) {
            ResourceLocation entityId = candidates.remove(random.nextInt(candidates.size()));
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if (entityType != null && this.trySpawn(level, entityType, entityId, x, y, z, yRot, xRot, yHeadRot)) {
                return true;
            }
        }

        this.logger.warn("Failed to spawn an entity for category {}", category);
        return false;
    }

    private boolean trySpawn(ServerLevel level, EntityType<?> entityType, ResourceLocation entityId, double x, double y, double z, float yRot, float xRot, float yHeadRot) {
        try {
            Entity entity = entityType.create(level);
            if (!(entity instanceof Mob mob)) {
                if (entity != null) {
                    entity.discard();
                }

                return false;
            }

            BlockPos spawnPos = BlockPos.containing(x, y, z);
            DifficultyInstance difficulty = level.getCurrentDifficultyAt(spawnPos);

            mob.moveTo(x, y, z, yRot, xRot);
            mob.setYHeadRot(yHeadRot);
            mob.setYBodyRot(yRot);
            mob.setPersistenceRequired();

            if (this.persistentTag != null) {
                mob.getPersistentData().putBoolean(this.persistentTag, true);
            }

            mob.finalizeSpawn(level, difficulty, MobSpawnType.STRUCTURE, null, null);

            if (level.addFreshEntity(mob)) {
                return true;
            }

            mob.discard();
        } catch (Exception e) {
            this.logger.error("Failed to spawn entity {}, trying another one", entityId, e);
        }

        return false;
    }

    private List<ResourceLocation> getExistingEntityIds(C category) {
        List<ResourceLocation> result = new ArrayList<>();

        for (ResourceLocation id : this.entriesByCategory.getOrDefault(category, List.of())) {
            if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                result.add(id);
            }
        }

        return result;
    }
}
