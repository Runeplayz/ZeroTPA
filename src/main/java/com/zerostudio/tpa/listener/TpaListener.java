package com.zerostudio.tpa.listener;

import com.zerostudio.tpa.ZeroStudioTPA;
import com.zerostudio.tpa.gui.RequestGui;
import com.zerostudio.tpa.service.TpaService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TpaListener implements Listener {
    private final ZeroStudioTPA plugin;
    private final TpaService service;
    private final RequestGui requestGui;

    public TpaListener(ZeroStudioTPA plugin, TpaService service, RequestGui requestGui) {
        this.plugin = plugin;
        this.service = service;
        this.requestGui = requestGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        requestGui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        requestGui.handleDrag(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        service.handleDeath(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        service.handlePlayerDamage(event);
    }
}