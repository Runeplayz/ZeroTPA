package com.zerostudio.tpa.command;

import com.zerostudio.tpa.ZeroStudioTPA;
import com.zerostudio.tpa.model.TpaRequest;
import com.zerostudio.tpa.service.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class TpaCommand implements CommandExecutor, TabCompleter {
    private final ZeroStudioTPA plugin;
    private final TpaService service;

    public TpaCommand(ZeroStudioTPA plugin, TpaService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("tpa") && args.length > 0 && args[0].equalsIgnoreCase("help")) {
            plugin.messages().sendHelp(sender);
            return true;
        }
        String permission = switch (name) {
            case "tpa", "tpcancel" -> "tpa";
            case "tpahere" -> "tpahere";
            case "tpaccept" -> "accept";
            case "tpdeny" -> "deny";
            case "tpatoggle", "tpauto" -> "toggle";
            case "tpareload" -> "admin";
            default -> "tpa";
        };
        if (!plugin.hasConfiguredPermission(sender, permission)) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (name.equals("tpareload")) {
            plugin.reloadPluginFiles();
            plugin.messages().send(sender, "request.reload");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }

        switch (name) {
            case "tpa", "tpahere" -> sendRequest(player, name, args);
            case "tpaccept" -> resolveRequest(player, args, true);
            case "tpdeny" -> resolveRequest(player, args, false);
            case "tpcancel" -> service.cancelOutgoing(player);
            case "tpatoggle" -> service.toggleRequests(player);
            case "tpauto" -> service.toggleAutoAccept(player);
            default -> plugin.messages().sendHelp(player);
        }
        return true;
    }

    private void sendRequest(Player player, String command, String[] args) {
        if (args.length != 1) {
            plugin.messages().sendHelp(player);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(player, "player-not-found");
            return;
        }
        service.sendRequest(player, target, command.equals("tpa") ? TpaRequest.Type.TPA : TpaRequest.Type.TPA_HERE);
    }

    private void resolveRequest(Player player, String[] args, boolean accept) {
        List<TpaRequest> requests = service.getIncoming(player);
        if (args.length > 1) {
            plugin.messages().sendHelp(player);
            return;
        }
        TpaRequest request;
        if (args.length == 1) {
            request = service.findIncoming(player, args[0]);
        } else if (requests.size() == 1) {
            request = requests.get(0);
        } else if (requests.isEmpty()) {
            plugin.messages().send(player, "request.none-incoming");
            return;
        } else {
            String names = String.join(", ", requests.stream().map(TpaRequest::senderName).toList());
            plugin.messages().send(player, "request.multiple", java.util.Map.of("players", names));
            return;
        }
        if (request == null) {
            plugin.messages().send(player, "request.none");
            return;
        }
        if (accept) {
            service.accept(player, request.senderId());
        } else {
            service.deny(player, request.senderId());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length != 1) return List.of();
        if (name.equals("tpa") && "help".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("help");
        }
        if (name.equals("tpa") || name.equals("tpahere")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(playerName -> playerName.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if ((name.equals("tpaccept") || name.equals("tpdeny")) && sender instanceof Player player) {
            return service.getIncoming(player).stream().map(TpaRequest::senderName)
                    .filter(playerName -> playerName.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}