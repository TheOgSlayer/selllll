package com.sellchest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SellListener implements Listener {

    private final SellChest plugin;

    public SellListener(SellChest plugin) {
        this.plugin = plugin;
    }

    private static boolean isSellMenu(Inventory inv) {
        return inv.getHolder() instanceof SellHolder;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        if (!isSellMenu(top)) {
            return;
        }

        // Double-click "collect to cursor" could otherwise pull the sell button out of its slot.
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() == plugin.buttonMaterial()) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getRawSlot() == SellChest.BUTTON_SLOT) {
            event.setCancelled(true);
            if ((event.isLeftClick() || event.isRightClick()) && event.getWhoClicked() instanceof Player player) {
                plugin.sell(player, top);
            }
            return;
        }

        plugin.refreshLater(top);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getInventory();
        if (!isSellMenu(top)) {
            return;
        }
        if (event.getRawSlots().contains(SellChest.BUTTON_SLOT)) {
            event.setCancelled(true);
            return;
        }
        plugin.refreshLater(top);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!isSellMenu(inv) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        plugin.returnItems(player, inv);
        plugin.playSound(player, "close");
    }
}
