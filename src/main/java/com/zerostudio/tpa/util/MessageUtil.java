package com.zerostudio.tpa.util;

import com.zerostudio.tpa.ZeroStudioTPA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MessageUtil {
    private final ZeroStudioTPA plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageUtil(ZeroStudioTPA plugin) {
        this.plugin = plugin;
    }

    public Component component(String text, Map<String, String> values) {
        List<TagResolver> resolvers = new ArrayList<>();
        String prefix = plugin.getMessages().getString("prefix", "");
        resolvers.add(Placeholder.parsed("prefix", prefix));
        values.forEach((key, value) -> resolvers.add(Placeholder.unparsed(key, value)));
        return miniMessage.deserialize(text, TagResolver.resolver(resolvers));
    }

    public Component component(String text) {
        return component(text, Map.of());
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> values) {
        String text = plugin.getMessages().getString(path);
        if (text != null) {
            sender.sendMessage(component(text, values));
        }
    }

    public void sendHelp(CommandSender sender) {
        for (String line : plugin.getMessages().getStringList("request.help")) {
            sender.sendMessage(component(line, Map.of()));
        }
    }
}