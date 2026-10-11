package com.zerostudio.tpa.service;

import com.zerostudio.tpa.ZeroStudioTPA;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SoundManager {
    private final ZeroStudioTPA plugin;

    public SoundManager(ZeroStudioTPA plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String key) {
        String path = "sounds." + key;
        if (!plugin.getSoundsConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        String name = plugin.getSoundsConfig().getString(path + ".name", "");
        if (name == null || name.isBlank()) {
            return;
        }
        NamespacedKey soundKey = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
        var sound = Registry.SOUNDS.get(soundKey);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound '" + name + "' in sounds.yml (" + key + ").");
            return;
        }
        player.playSound(player.getLocation(), sound,
                (float) plugin.getSoundsConfig().getDouble(path + ".volume", 1.0),
                (float) plugin.getSoundsConfig().getDouble(path + ".pitch", 1.0));
    }
}