package com.zerostudio.tpa;

import com.zerostudio.tpa.command.TpaCommand;
import com.zerostudio.tpa.gui.RequestGui;
import com.zerostudio.tpa.listener.TpaListener;
import com.zerostudio.tpa.service.SoundManager;
import com.zerostudio.tpa.service.TpaService;
import com.zerostudio.tpa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ZeroStudioTPA extends JavaPlugin {
    private FileConfiguration messages;
    private FileConfiguration guiConfig;
    private FileConfiguration soundsConfig;
    private MessageUtil messageUtil;
    private SoundManager soundManager;
    private TpaService tpaService;
    private RequestGui requestGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("gui.yml", false);
        saveResource("sounds.yml", false);
        reloadPluginFiles();

        messageUtil = new MessageUtil(this);
        soundManager = new SoundManager(this);
        tpaService = new TpaService(this);
        requestGui = new RequestGui(this, tpaService);

        TpaCommand command = new TpaCommand(this, tpaService);
        for (String name : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel", "tpatoggle", "tpauto", "tpareload"}) {
            getCommand(name).setExecutor(command);
            getCommand(name).setTabCompleter(command);
        }
        Bukkit.getPluginManager().registerEvents(new TpaListener(this, tpaService, requestGui), this);
        Bukkit.getScheduler().runTaskTimer(this, tpaService::expireRequests, 20L, 20L);
    }

    public void reloadPluginFiles() {
        reloadConfig();
        messages = loadYaml("messages.yml");
        guiConfig = loadYaml("gui.yml");
        soundsConfig = loadYaml("sounds.yml");
    }

    private FileConfiguration loadYaml(String name) {
        File file = new File(getDataFolder(), name);
        return YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasConfiguredPermission(CommandSender sender, String key) {
        return sender.hasPermission(getConfig().getString("permissions." + key, "zerostudio." + key));
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getSoundsConfig() {
        return soundsConfig;
    }

    public MessageUtil messages() {
        return messageUtil;
    }

    public SoundManager sounds() {
        return soundManager;
    }

    public RequestGui requestGui() {
        return requestGui;
    }
}