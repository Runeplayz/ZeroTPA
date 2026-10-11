package com.zerostudio.tpa.service;

import com.zerostudio.tpa.ZeroStudioTPA;
import com.zerostudio.tpa.model.TpaRequest;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TpaService {
    private final ZeroStudioTPA plugin;
    private final Map<UUID, LinkedHashMap<UUID, TpaRequest>> incoming = new HashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, TpaRequest>> outgoing = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> accepting = new HashMap<>();
    private final Map<UUID, Boolean> autoAccept = new HashMap<>();
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Map<UUID, Countdown> countdowns = new HashMap<>();

    public TpaService(ZeroStudioTPA plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target, TpaRequest.Type type) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            plugin.messages().send(sender, "request.self");
            return;
        }
        if (isWorldDisabled(sender.getWorld()) || isWorldDisabled(target.getWorld())) {
            plugin.messages().send(sender, "request.disabled-world");
            return;
        }
        if (!accepting.getOrDefault(target.getUniqueId(), true)) {
            plugin.messages().send(sender, "request.disabled");
            return;
        }
        LinkedHashMap<UUID, TpaRequest> fromSender = outgoing.computeIfAbsent(sender.getUniqueId(), ignored -> new LinkedHashMap<>());
        if (fromSender.containsKey(target.getUniqueId())) {
            plugin.messages().send(sender, "request.duplicate");
            return;
        }
        if (!sender.hasPermission(plugin.getConfig().getString("permissions.bypass-cooldown", "zerostudio.bypasscooldown"))) {
            long remaining = cooldowns.getOrDefault(sender.getUniqueId(), 0L)
                    + plugin.getConfig().getLong("requests.cooldown-seconds", 3) * 1000L - System.currentTimeMillis();
            if (remaining > 0) {
                plugin.messages().send(sender, "request.cooldown", Map.of("seconds", String.valueOf((remaining + 999) / 1000)));
                return;
            }
        }
        int limit = Math.max(1, plugin.getConfig().getInt("requests.max-outgoing", 3));
        if (fromSender.size() >= limit) {
            plugin.messages().send(sender, "request.limit");
            return;
        }

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), sender.getName(), target.getName(), type, System.currentTimeMillis());
        fromSender.put(target.getUniqueId(), request);
        incoming.computeIfAbsent(target.getUniqueId(), ignored -> new LinkedHashMap<>()).put(sender.getUniqueId(), request);
        cooldowns.put(sender.getUniqueId(), System.currentTimeMillis());

        plugin.messages().send(sender, "request.sent", Map.of("target", target.getName()));
        plugin.sounds().play(sender, "request-sent");
        String receivedKey = type == TpaRequest.Type.TPA ? "request.received" : "request.received-here";
        plugin.messages().send(target, receivedKey, Map.of("sender", sender.getName()));
        plugin.sounds().play(target, "request-received");

        if (autoAccept.getOrDefault(target.getUniqueId(), false)) {
            accept(target, sender.getUniqueId());
        } else {
            plugin.requestGui().open(target, request);
        }
    }

    public List<TpaRequest> getIncoming(Player player) {
        return new ArrayList<>(incoming.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values());
    }

    public TpaRequest findIncoming(Player receiver, String senderName) {
        return incoming.getOrDefault(receiver.getUniqueId(), new LinkedHashMap<>()).values().stream()
                .filter(request -> request.senderName().equalsIgnoreCase(senderName)).findFirst().orElse(null);
    }

    public void accept(Player receiver, UUID senderId) {
        TpaRequest request = findRequest(receiver.getUniqueId(), senderId);
        if (request == null) {
            plugin.messages().send(receiver, "request.none");
            return;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        Player teleporting = request.type() == TpaRequest.Type.TPA ? sender : receiver;
        if (teleporting == null || Bukkit.getPlayer(request.targetId()) == null) {
            removeRequest(request);
            plugin.messages().send(receiver, "player-not-found");
            return;
        }
        if (countdowns.containsKey(teleporting.getUniqueId())) {
            plugin.messages().send(receiver, "request.busy");
            return;
        }

        Player destination = request.type() == TpaRequest.Type.TPA ? receiver : sender;
        removeRequest(request);
        plugin.messages().send(receiver, "request.accepted-by-you", Map.of("sender", request.senderName()));
        plugin.messages().send(sender, "request.accepted", Map.of("receiver", receiver.getName()));
        plugin.sounds().play(receiver, "accepted");
        plugin.sounds().play(sender, "accepted");
        startCountdown(teleporting, destination);
    }

    public void deny(Player receiver, UUID senderId) {
        TpaRequest request = findRequest(receiver.getUniqueId(), senderId);
        if (request == null) {
            plugin.messages().send(receiver, "request.none");
            return;
        }
        removeRequest(request);
        Player sender = Bukkit.getPlayer(request.senderId());
        plugin.messages().send(receiver, "request.denied-by-you", Map.of("sender", request.senderName()));
        if (sender != null) {
            plugin.messages().send(sender, "request.denied", Map.of("receiver", receiver.getName()));
            plugin.sounds().play(sender, "denied");
        }
        plugin.sounds().play(receiver, "denied");
    }

    public void cancelOutgoing(Player player) {
        List<TpaRequest> requests = new ArrayList<>(outgoing.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values());
        if (requests.isEmpty()) {
            plugin.messages().send(player, "request.cancel-none");
            return;
        }
        requests.forEach(this::removeRequest);
        plugin.messages().send(player, "request.cancelled");
    }

    public void toggleRequests(Player player) {
        boolean enabled = !accepting.getOrDefault(player.getUniqueId(), true);
        accepting.put(player.getUniqueId(), enabled);
        plugin.messages().send(player, enabled ? "request.toggle-on" : "request.toggle-off");
    }

    public void toggleAutoAccept(Player player) {
        boolean enabled = !autoAccept.getOrDefault(player.getUniqueId(), false);
        autoAccept.put(player.getUniqueId(), enabled);
        plugin.messages().send(player, enabled ? "request.auto-on" : "request.auto-off");
    }

    public void expireRequests() {
        long now = System.currentTimeMillis();
        long expiration = Math.max(1, plugin.getConfig().getLong("requests.expiration-seconds", 60)) * 1000L;
        List<TpaRequest> expired = outgoing.values().stream().flatMap(map -> map.values().stream())
                .filter(request -> now - request.createdAt() >= expiration).toList();
        for (TpaRequest request : expired) {
            removeRequest(request);
            Player sender = Bukkit.getPlayer(request.senderId());
            Player target = Bukkit.getPlayer(request.targetId());
            if (sender != null) {
                plugin.messages().send(sender, "request.expired", Map.of("target", request.targetName()));
                plugin.sounds().play(sender, "expired");
            }
            if (target != null) {
                plugin.messages().send(target, "request.expired-receiver", Map.of("sender", request.senderName()));
            }
        }
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        List<TpaRequest> affected = new ArrayList<>();
        affected.addAll(incoming.getOrDefault(id, new LinkedHashMap<>()).values());
        affected.addAll(outgoing.getOrDefault(id, new LinkedHashMap<>()).values());
        affected.forEach(this::removeRequest);
        Countdown countdown = countdowns.get(id);
        if (countdown != null) {
            countdown.cancelWithMessage("you disconnected", false);
        }
        for (Countdown active : new ArrayList<>(countdowns.values())) {
            if (active.destinationId.equals(id)) {
                active.cancelWithMessage("the destination disconnected", true);
            }
        }
        accepting.remove(id);
        autoAccept.remove(id);
        cooldowns.remove(id);
        combatTags.remove(id);
    }

    public void handleDeath(Player player) {
        Countdown countdown = countdowns.get(player.getUniqueId());
        if (countdown != null) {
            countdown.cancelWithMessage("you died", true);
        }
        for (Countdown active : new ArrayList<>(countdowns.values())) {
            if (active.destinationId.equals(player.getUniqueId())) {
                active.cancelWithMessage("the destination died", true);
            }
        }
    }

    public void markCombat(Player first, Player second) {
        if (!plugin.getConfig().getBoolean("combat.enabled", false)) {
            return;
        }
        long until = System.currentTimeMillis() + plugin.getConfig().getLong("combat.tag-seconds", 10) * 1000L;
        combatTags.put(first.getUniqueId(), until);
        combatTags.put(second.getUniqueId(), until);
    }

    public void handlePlayerDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = event.getDamager() instanceof Player directPlayer ? directPlayer
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooterPlayer ? shooterPlayer : null;
        if (attacker != null && attacker != victim) {
            markCombat(victim, attacker);
            if (plugin.getConfig().getBoolean("teleport.cancel-on-damage", false)) {
                Countdown countdown = countdowns.get(victim.getUniqueId());
                if (countdown != null) {
                    countdown.cancelWithMessage("you took damage", true);
                }
            }
        }
    }

    private TpaRequest findRequest(UUID targetId, UUID senderId) {
        return incoming.getOrDefault(targetId, new LinkedHashMap<>()).get(senderId);
    }

    private void removeRequest(TpaRequest request) {
        LinkedHashMap<UUID, TpaRequest> toTarget = incoming.get(request.targetId());
        if (toTarget != null) {
            toTarget.remove(request.senderId());
            if (toTarget.isEmpty()) incoming.remove(request.targetId());
        }
        LinkedHashMap<UUID, TpaRequest> fromSender = outgoing.get(request.senderId());
        if (fromSender != null) {
            fromSender.remove(request.targetId());
            if (fromSender.isEmpty()) outgoing.remove(request.senderId());
        }
    }

    private boolean isWorldDisabled(World world) {
        Set<String> worlds = Set.copyOf(plugin.getConfig().getStringList("teleport.disabled-worlds").stream()
                .map(name -> name.toLowerCase(Locale.ROOT)).toList());
        return worlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    private void startCountdown(Player teleporting, Player destination) {
        int duration = Math.max(1, plugin.getConfig().getInt("teleport.countdown-seconds", 5));
        Countdown countdown = new Countdown(teleporting, destination, duration);
        countdowns.put(teleporting.getUniqueId(), countdown);
        plugin.messages().send(teleporting, "request.countdown");
        countdown.runTaskTimer(plugin, 0L, 20L);
    }

    private Location safeLocation(Location destination) {
        if (!plugin.getConfig().getBoolean("teleport.safe-destination.enabled", true)) {
            return destination.clone();
        }
        int radius = Math.max(0, plugin.getConfig().getInt("teleport.safe-destination.search-radius", 2));
        World world = destination.getWorld();
        int baseX = destination.getBlockX();
        int baseY = destination.getBlockY();
        int baseZ = destination.getBlockZ();
        for (int distance = 0; distance <= radius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        int x = baseX + dx;
                        int y = baseY + dy;
                        int z = baseZ + dz;
                        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) continue;
                        Block feet = world.getBlockAt(x, y, z);
                        if (feet.isPassable() && world.getBlockAt(x, y + 1, z).isPassable()
                                && world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                            return new Location(world, x + 0.5, y, z + 0.5, destination.getYaw(), destination.getPitch());
                        }
                    }
                }
            }
        }
        return null;
    }

    private final class Countdown extends BukkitRunnable {
        private final Player player;
        private final UUID destinationId;
        private final Location start;
        private final UUID startWorld;
        private int remaining;

        private Countdown(Player player, Player destination, int duration) {
            this.player = player;
            this.destinationId = destination.getUniqueId();
            this.start = player.getLocation().clone();
            this.startWorld = player.getWorld().getUID();
            this.remaining = duration;
        }

        @Override
        public void run() {
            Player destination = Bukkit.getPlayer(destinationId);
            if (!player.isOnline() || player.isDead()) {
                cancelWithMessage("you are no longer available", false);
                return;
            }
            if (destination == null || !destination.isOnline() || destination.isDead() || isWorldDisabled(player.getWorld()) || isWorldDisabled(destination.getWorld())) {
                cancelWithMessage("the destination is no longer available", true);
                return;
            }
            if (plugin.getConfig().getBoolean("teleport.cancel-on-world-change", true) && !player.getWorld().getUID().equals(startWorld)) {
                cancelWithMessage("you changed worlds", true);
                return;
            }
            if (plugin.getConfig().getBoolean("teleport.cancel-on-movement", true)
                    && !player.hasPermission(plugin.getConfig().getString("permissions.bypass-movement", "zerostudio.bypassmovement"))) {
                double distance = plugin.getConfig().getDouble("teleport.movement-distance", 0.2);
                if (player.getLocation().distanceSquared(start) > distance * distance) {
                    cancelWithMessage("you moved", true);
                    return;
                }
            }
            if ((plugin.getConfig().getBoolean("teleport.cancel-on-damage", false)
                    || plugin.getConfig().getBoolean("combat.enabled", false))
                    && combatTags.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
                cancelWithMessage("you entered combat", true);
                return;
            }
            if (remaining <= 0) {
                Location destinationLocation = safeLocation(destination.getLocation());
                if (destinationLocation == null) {
                    plugin.messages().send(player, "request.destination-invalid");
                    cancelWithMessage("the destination is not safe", true);
                    return;
                }
                boolean teleported = player.teleport(destinationLocation);
                if (teleported) {
                    plugin.messages().send(player, "request.teleport-success");
                    plugin.sounds().play(player, "teleport");
                    plugin.sounds().play(destination, "teleport");
                }
                finish();
                return;
            }

            Map<String, String> values = Map.of("seconds", String.valueOf(remaining));
            String title = plugin.getMessages().getString("request.teleporting-title", "<aqua>TELEPORTING</aqua>");
            String subtitle = plugin.getMessages().getString("request.teleporting-subtitle", "<gray>Teleporting in <seconds> seconds</gray>");
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(100));
            player.showTitle(Title.title(plugin.messages().component(title, values), plugin.messages().component(subtitle, values), times));
            plugin.sounds().play(player, "countdown");
            remaining--;
        }

        private void cancelWithMessage(String reason, boolean notify) {
            if (notify && player.isOnline()) {
                plugin.messages().send(player, "request.teleport-cancelled", Map.of("reason", reason));
                plugin.sounds().play(player, "cancelled");
            }
            finish();
        }

        private void finish() {
            cancel();
            countdowns.remove(player.getUniqueId(), this);
        }
    }
}