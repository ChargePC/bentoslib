package net.randomcara.bentoslib.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class ModGatedPackLoader {
    public static void registerModGatedPack(AddPackFindersEvent event, String ownerModId, Logger logger, String requiredModId, String resourcePath, String title) {
        if (event.getPackType() != PackType.SERVER_DATA || !ModList.get().isLoaded(requiredModId)) {
            return;
        }

        ModList.get().getModContainerById(ownerModId).ifPresent(container -> {
            IModFile modFile = container.getModInfo().getOwningFile().getFile();
            Path packRoot = modFile.findResource(resourcePath);
            String packId = ownerModId + "_compat_" + requiredModId;
            Pack pack = Pack.readMetaAndCreate(packId, Component.literal(title), true, id -> new PathPackResources(id, true, packRoot), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack == null) {
                logger.error("Could not build the {} compatibility datapack from {}", requiredModId, resourcePath);
            } else {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        });
    }
}
