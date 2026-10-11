package com.zerostudio.tpa.gui;

import com.cryptomorin.xseries.XMaterial;
import com.zerostudio.tpa.ZeroStudioTPA;
import com.zerostudio.tpa.model.TpaRequest;
import com.zerostudio.tpa.service.TpaService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RequestGui {
    private final ZeroStudioTPA plugin;
    private final TpaService service;

    public RequestGui(ZeroStudioTPA plugin, TpaService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void open(Player receiver, TpaRequest request) {
        int size = Math.max(9, Math.min(54, plugin.getGuiConfig().getInt("size", 27)));
        size -= size % 9;
        String title = plugin.getGuiConfig().getString("title", "Teleport Request");
        Map<String, String> values = placeholders(request);
        RequestMenu holder = new RequestMenu(request);
        Inventory inventory = Bukkit.createInventory(holder, size, plugin.messages().component(title, values));
        holder.inventory = inventory;

        if (plugin.getGuiConfig().getBoolean("fill-empty-slots", true)) {
            ItemStack filler = createItem("items.filler", values);
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler.clone());
            }
        }
        setItem(inventory, "items.target", values, receiver, request.senderName());
        setItem(inventory, "items.accept", values, null, null);
        setItem(inventory, "items.deny", values, null, null);
        receiver.openInventory(inventory);
        plugin.sounds().play(receiver, "gui-open");
    }

    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RequestMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == plugin.getGuiConfig().getInt("items.accept.slot", 11)) {
            service.accept(player, menu.request.senderId());
            player.closeInventory();
        } else if (slot == plugin.getGuiConfig().getInt("items.deny.slot", 15)) {
            service.deny(player, menu.request.senderId());
            player.closeInventory();
        }
    }

    public void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RequestMenu) {
            event.setCancelled(true);
        }
    }

    private void setItem(Inventory inventory, String path, Map<String, String> values, Player owner, String ownerName) {
        int slot = plugin.getGuiConfig().getInt(path + ".slot", -1);
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        ItemStack item = createItem(path, values);
        if (owner != null && item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            item.setItemMeta(skullMeta);
        }
        inventory.setItem(slot, item);
    }

    private ItemStack createItem(String path, Map<String, String> values) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection(path);
        String materialName = section == null ? "PAPER" : section.getString("material", "PAPER");
        Material material = XMaterial.matchXMaterial(materialName)
                .map(XMaterial::parseMaterial).orElse(Material.PAPER);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (section != null && meta != null) {
            String name = section.getString("name");
            if (name != null) {
                meta.displayName(plugin.messages().component(name, values));
            }
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(line -> plugin.messages().component(line, values)).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Map<String, String> placeholders(TpaRequest request) {
        Map<String, String> values = new HashMap<>();
        values.put("sender", request.senderName());
        values.put("target", request.targetName());
        values.put("type", request.type() == TpaRequest.Type.TPA ? "Teleport to player" : "Come to sender");
        return values;
    }

    public static final class RequestMenu implements InventoryHolder {
        private final TpaRequest request;
        private Inventory inventory;

        private RequestMenu(TpaRequest request) {
            this.request = request;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}