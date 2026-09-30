package net.randomcara.bentoslib.gameplay.bossbar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EventBossBarController {
    private static final Map<UUID, Entry> BOSS_BARS = new HashMap<>();

    public static void create(UUID eventId, Component initialName, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, ServerPlayer owner) {
        ServerBossEvent bossBar = new ServerBossEvent(initialName, color, overlay);
        bossBar.setVisible(true);
        bossBar.setColor(color);
        bossBar.setProgress(0.0F);

        if (owner != null) {
            bossBar.addPlayer(owner);
        }

        BOSS_BARS.put(eventId, new Entry(bossBar, color));
    }

    public static void update(UUID eventId, Component name, float progress, ServerPlayer owner) {
        Entry entry = BOSS_BARS.get(eventId);
        if (entry == null) {
            return;
        }

        ServerBossEvent bossBar = entry.bossBar();
        if (owner != null && !bossBar.getPlayers().contains(owner)) {
            bossBar.addPlayer(owner);
        }

        bossBar.setName(name);
        bossBar.setProgress(progress);
        bossBar.setColor(entry.color());
    }

    public static void remove(UUID eventId) {
        Entry entry = BOSS_BARS.remove(eventId);
        if (entry != null) {
            entry.bossBar().removeAllPlayers();
        }
    }

    public static void clear() {
        for (Entry entry : BOSS_BARS.values()) {
            entry.bossBar().removeAllPlayers();
        }

        BOSS_BARS.clear();
    }

    private record Entry(ServerBossEvent bossBar, BossEvent.BossBarColor color) {
    }
}
