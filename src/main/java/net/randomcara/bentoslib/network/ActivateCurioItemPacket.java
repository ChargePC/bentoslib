package net.randomcara.bentoslib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import net.randomcara.bentoslib.api.curio.IActivatableCurioItem;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;
import java.util.function.Supplier;

public final class ActivateCurioItemPacket {
    public static void register(SimpleChannel channel, int packetId) {
        channel.messageBuilder(ActivateCurioItemPacket.class, packetId, NetworkDirection.PLAY_TO_SERVER).encoder(ActivateCurioItemPacket::encode).decoder(ActivateCurioItemPacket::decode).consumerMainThread(ActivateCurioItemPacket::handle).add();
    }

    public static void encode(ActivateCurioItemPacket msg, FriendlyByteBuf buf) {
    }

    public static ActivateCurioItemPacket decode(FriendlyByteBuf buf) {
        return new ActivateCurioItemPacket();
    }

    public static void handle(ActivateCurioItemPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                activateFirstEquipped(player);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void activateFirstEquipped(ServerPlayer player) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(player).resolve();
        if (inventory.isEmpty()) {
            return;
        }

        for (SlotResult slotResult : inventory.get().findCurios(stack -> stack.getItem() instanceof IActivatableCurioItem)) {
            ItemStack stack = slotResult.stack();
            if (!player.getCooldowns().isOnCooldown(stack.getItem()) && stack.getItem() instanceof IActivatableCurioItem activatable) {
                activatable.activate(player, stack);
            }

            return;
        }
    }
}
