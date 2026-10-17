package com.sellchest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SellChest extends JavaPlugin {

    /** Double chest. */
    static final int SIZE = 54;
    /** Last slot: the green sell button. */
    static final int BUTTON_SLOT = SIZE - 1;

    private final PriceStore prices = new PriceStore();
    private final Set<String> badSounds = new HashSet<>();
    private Economy economy;
    private Material buttonMaterial = Material.GREEN_STAINED_GLASS_PANE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "prices.yml").exists()) {
            saveResource("prices.yml", false);
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) {
            getLogger().severe("No Vault economy found (is EssentialsX installed?). Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();

        loadAll();
        getServer().getPluginManager().registerEvents(new SellListener(this), this);
        if (getCommand("sell") != null) {
            getCommand("sell").setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        // Give items back to anyone who has the menu open during a stop / reload.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof SellHolder) {
                p.closeInventory();
            }
        }
    }

    private void loadAll() {
        reloadConfig();
        prices.load(new File(getDataFolder(), "prices.yml"), getLogger());
        Material m = Material.matchMaterial(getConfig().getString("button-material", "GREEN_STAINED_GLASS_PANE"));
        buttonMaterial = (m != null && m.isItem()) ? m : Material.GREEN_STAINED_GLASS_PANE;
    }

    Material buttonMaterial() {
        return buttonMaterial;
    }

    // ---------------------------------------------------------------- /sell

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sellchest.reload")) {
                sender.sendMessage(text("you don't have permission to do that.", NamedTextColor.RED));
                return true;
            }
            loadAll();
            sender.sendMessage(text("sellchest reloaded.", NamedTextColor.GREEN));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /sell.");
            return true;
        }
        open(player);
        return true;
    }

    void open(Player p) {
        SellHolder holder = new SellHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, text("sell items", NamedTextColor.DARK_GRAY));
        holder.setInventory(inv);
        inv.setItem(BUTTON_SLOT, button(0));
        p.openInventory(inv);
        playSound(p, "open");
    }

    // ---------------------------------------------------------------- selling

    /** Sells everything sellable in slots 0..52 and pays the player. */
    void sell(Player p, Inventory inv) {
        double total = 0;
        int count = 0;
        int skipped = 0;
        List<Integer> sold = new ArrayList<>();

        for (int i = 0; i < BUTTON_SLOT; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Double price = priceOf(item);
            if (price == null) {
                skipped += item.getAmount();
                continue;
            }
            total += price * item.getAmount();
            count += item.getAmount();
            sold.add(i);
        }

        if (count == 0) {
            p.sendMessage(text("there is nothing to sell in here.", NamedTextColor.RED));
            return;
        }

        EconomyResponse response = economy.depositPlayer(p, total);
        if (!response.transactionSuccess()) {
            p.sendMessage(text("payment failed, your items were not taken.", NamedTextColor.RED));
            return;
        }
        for (int slot : sold) {
            inv.setItem(slot, null);
        }

        p.sendMessage(text("sold " + count + " items for ", NamedTextColor.GREEN)
            .append(Component.text("$" + fmt(total), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        if (skipped > 0) {
            p.sendMessage(text(skipped + " items can't be sold and stay in the menu.", NamedTextColor.GRAY));
        }
        playSound(p, "sell");
        refresh(inv);
    }

    /** Price per item, or null if this stack can't be sold. */
    private Double priceOf(ItemStack item) {
        if (item.hasItemMeta() && !getConfig().getBoolean("sell-modified-items", false)) {
            return null;
        }
        return prices.get(item.getType());
    }

    /** Gives back whatever is still in the menu (unsold items). */
    void returnItems(Player p, Inventory inv) {
        for (int i = 0; i < BUTTON_SLOT; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            inv.setItem(i, null);
            Map<Integer, ItemStack> overflow = p.getInventory().addItem(item);
            for (ItemStack extra : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), extra);
            }
        }
    }

    // ---------------------------------------------------------------- button

    /** Re-runs on the next tick, after the click has actually moved the items. */
    void refreshLater(Inventory inv) {
        Bukkit.getScheduler().runTask(this, () -> refresh(inv));
    }

    void refresh(Inventory inv) {
        double total = 0;
        for (int i = 0; i < BUTTON_SLOT; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Double price = priceOf(item);
            if (price != null) {
                total += price * item.getAmount();
            }
        }
        inv.setItem(BUTTON_SLOT, button(total));
    }

    private ItemStack button(double total) {
        ItemStack stack = new ItemStack(buttonMaterial);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(text("sell all", NamedTextColor.GREEN));
        List<Component> lore = new ArrayList<>();
        lore.add(text("put items in the chest,", NamedTextColor.GRAY));
        lore.add(text("then click here to sell.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(text("total value: ", NamedTextColor.GRAY)
            .append(Component.text("$" + fmt(total), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    // ---------------------------------------------------------------- helpers

    /** Small caps, never bold, never italic. */
    static Component text(String s, TextColor color) {
        return Component.text(SmallCaps.of(s), color)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false);
    }

    private static String fmt(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    /** Plays sounds.<name> from config.yml to the player. */
    void playSound(Player p, String name) {
        String key = getConfig().getString("sounds." + name, "");
        if (key == null || key.isBlank()) {
            return;
        }
        float volume = (float) getConfig().getDouble("sounds.volume", 1.0);
        float pitch = (float) getConfig().getDouble("sounds.pitch", 1.0);
        try {
            p.playSound(Sound.sound(Key.key(key.trim()), Sound.Source.MASTER, volume, pitch));
        } catch (RuntimeException e) {
            if (badSounds.add(key)) {
                getLogger().warning("config.yml: \"" + key + "\" is not a valid sound key.");
            }
        }
    }
}
