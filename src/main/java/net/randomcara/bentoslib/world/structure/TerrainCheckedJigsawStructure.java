package net.randomcara.bentoslib.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public final class TerrainCheckedJigsawStructure extends Structure {
    public static final Codec<TerrainCheckedJigsawStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
            Codec.intRange(0, 7).fieldOf("size").forGetter(structure -> structure.size),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(structure -> structure.useExpansionHack),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
            Codec.intRange(1, 80).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter),
            Codec.INT.optionalFieldOf("terrain_check_radius", 28).forGetter(structure -> structure.terrainCheckRadius),
            Codec.INT.optionalFieldOf("terrain_check_step", 4).forGetter(structure -> structure.terrainCheckStep),
            Codec.INT.optionalFieldOf("max_terrain_height_difference", 9).forGetter(structure -> structure.maxTerrainHeightDifference),
            Codec.INT.optionalFieldOf("water_check_radius", 38).forGetter(structure -> structure.waterCheckRadius),
            Codec.INT.optionalFieldOf("max_water_depth", 0).forGetter(structure -> structure.maxWaterDepth),
            Codec.BOOL.optionalFieldOf("reject_river_biomes", true).forGetter(structure -> structure.rejectRiverBiomes)
    ).apply(instance, TerrainCheckedJigsawStructure::new));

    private static Supplier<StructureType<?>> structureType;

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final int terrainCheckRadius;
    private final int terrainCheckStep;
    private final int maxTerrainHeightDifference;
    private final int waterCheckRadius;
    private final int maxWaterDepth;
    private final boolean rejectRiverBiomes;

    public TerrainCheckedJigsawStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, int terrainCheckRadius, int terrainCheckStep, int maxTerrainHeightDifference, int waterCheckRadius, int maxWaterDepth, boolean rejectRiverBiomes) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.terrainCheckRadius = Math.max(4, terrainCheckRadius);
        this.terrainCheckStep = Math.max(1, terrainCheckStep);
        this.maxTerrainHeightDifference = Math.max(0, maxTerrainHeightDifference);
        this.waterCheckRadius = Math.max(4, waterCheckRadius);
        this.maxWaterDepth = Math.max(0, maxWaterDepth);
        this.rejectRiverBiomes = rejectRiverBiomes;
    }

    public static void setStructureType(Supplier<StructureType<?>> supplier) {
        structureType = supplier;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!this.hasAcceptableTerrain(context)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int startY = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos startPos = new BlockPos(chunkPos.getMiddleBlockX(), startY, chunkPos.getMiddleBlockZ());
        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.size, startPos, this.useExpansionHack, this.projectStartToHeightmap, this.maxDistanceFromCenter);
    }

    private boolean hasAcceptableTerrain(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int minAllowedY = context.heightAccessor().getMinBuildHeight() + 8;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        int totalRadius = Math.max(this.terrainCheckRadius, this.waterCheckRadius);
        for (int xOffset = -totalRadius; xOffset <= totalRadius; xOffset += this.terrainCheckStep) {
            for (int zOffset = -totalRadius; zOffset <= totalRadius; zOffset += this.terrainCheckStep) {
                int x = centerX + xOffset;
                int z = centerZ + zOffset;
                int terrainHeight = this.getHeight(context, x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
                int oceanFloorHeight = this.getHeight(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG);
                if (terrainHeight <= minAllowedY || oceanFloorHeight <= minAllowedY || (isInsideCheck(xOffset, zOffset, this.waterCheckRadius) && this.hasBadWaterOrBiome(context, x, terrainHeight, z, oceanFloorHeight))) {
                    return false;
                }

                if (isInsideCheck(xOffset, zOffset, this.terrainCheckRadius)) {
                    minHeight = Math.min(minHeight, terrainHeight);
                    maxHeight = Math.max(maxHeight, terrainHeight);

                    if (maxHeight - minHeight > this.maxTerrainHeightDifference) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean hasBadWaterOrBiome(GenerationContext context, int x, int terrainHeight, int z, int oceanFloorHeight) {
        if (this.rejectRiverBiomes && this.isRiverOrOceanBiome(context, x, terrainHeight, z)) {
            return true;
        }

        return terrainHeight - oceanFloorHeight > this.maxWaterDepth;
    }

    private boolean isRiverOrOceanBiome(GenerationContext context, int x, int y, int z) {
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler());
        return biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN);
    }

    private int getHeight(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
        return context.chunkGenerator().getBaseHeight(x, z, heightmap, context.heightAccessor(), context.randomState());
    }

    private static boolean isInsideCheck(int xOffset, int zOffset, int radius) {
        return Math.abs(xOffset) <= radius && Math.abs(zOffset) <= radius;
    }

    @NotNull
    @Override
    public StructureType<?> type() {
        if (structureType == null) {
            throw new NullPointerException("No StructureType was set for TerrainCheckedJigsawStructure");
        }

        return structureType.get();
    }
}
