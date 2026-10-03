package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.files.PluginFile;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.ObjectUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameItems {
    private static GameItems instance;
    private final Config config;
    private final CatchTheBeacon plugin;
    private final Map<Integer, ItemStack> defaultItems;

    public GameItems(CatchTheBeacon plugin) {
        instance = this;
        this.plugin = plugin;
        this.config = new Config();
        this.defaultItems = new HashMap<>();
        ConfigurationSection section = this.config.getSection("");
        if (section == null || section.getKeys(false).isEmpty()) {
            Debug.info("Using the default items (set your own with /ctb config items set)");
            this.defaultItems.putAll(defaultKit());
            return;
        }

        for (String row : section.getKeys(false)) {
            int slot = ObjectUtils.parseInt(row.replace("slot_", ""));
            if (slot == -1) continue;
            ItemStack item = ObjectUtils.parseItemStack(config.getData().get("inventory." + row));
            if (item.getType().equals(Material.AIR)) {
                Debug.warn("Invalid configuration in 'items.yml': Couldn't parse item for slot " + slot);
                continue;
            }
            defaultItems.put(slot, item);
        }
    }

    /**
     * the items if none are set in items.yml - leather armor and wool get the color of the team
     * (slots 36-39 are boots, leggings, chestplate and helmet)
     */
    private static Map<Integer, ItemStack> defaultKit() {
        Map<Integer, ItemStack> kit = new HashMap<>();
        kit.put(0, new ItemStack(Material.STONE_SWORD));
        kit.put(1, new ItemStack(Material.BOW));
        kit.put(2, new ItemStack(Material.STONE_PICKAXE));
        kit.put(3, new ItemStack(Material.STONE_AXE));
        kit.put(4, new ItemStack(Material.WHITE_WOOL, 64));
        kit.put(5, new ItemStack(Material.WHITE_WOOL, 64));
        kit.put(7, new ItemStack(Material.COOKED_BEEF, 16));
        // not more: the community didn't want every fight to be a bow fight
        kit.put(8, new ItemStack(Material.ARROW, 16));
        kit.put(36, new ItemStack(Material.LEATHER_BOOTS));
        kit.put(37, new ItemStack(Material.LEATHER_LEGGINGS));
        kit.put(38, new ItemStack(Material.LEATHER_CHESTPLATE));
        kit.put(39, new ItemStack(Material.LEATHER_HELMET));
        return kit;
    }

    public static GameItems getInstance() {
        return instance;
    }

    public static void setDefaultItems(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        Config config = GameItems.getInstance().getConfig();
        config.getData().set("inventory", "");
        config.save();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                config.getData().set("inventory.slot_" + i, item);
            }
        }
        config.save();
        new GameItems(CatchTheBeacon.getInstance());
    }

    public static void setItems(GamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();
        player.getInventory().clear();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot : instance.defaultItems.keySet()) {
            ItemStack item = instance.defaultItems.get(slot).clone();
            if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(gamePlayer.getTeam().getLeatherColor());
                item.setItemMeta(meta);
            } else if (item.getType().name().endsWith("_WOOL")) {
                item = item.withType(gamePlayer.getTeam().getItem().getType());
            }
            contents[slot] = item;
        }
        Game game = gamePlayer.getGame();
        Variant variant = game == null ? Variant.NORMAL : game.getVariant();
        if (variant == Variant.BOW_ONLY) giveInfiniteBow(contents);
        player.getInventory().setContents(contents);
        if (instance.plugin.getFileManager().getConfigFile().getBoolean("game.beacon-compass")) {
            player.getInventory().addItem(InteractionItems.getBeaconCompass());
        }
        player.updateInventory();
        // variant rush: mining takes half the time (UserStorage restores it)
        AttributeInstance breakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (breakSpeed != null) {
            breakSpeed.setBaseValue(variant == Variant.RUSH ? Variant.RUSH_BREAK_SPEED : DEFAULT_BLOCK_BREAK_SPEED);
        }
        // game.extra-hearts: more health than outside of the game (UserStorage restores it)
        int extraHearts = Math.max(0, instance.plugin.getFileManager().getConfigFile().getInt("game.extra-hearts"));
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20 + extraHearts * 2);
            player.setHealth(maxHealth.getValue());
        }
        // game.combat: legacy = 1.8 pvp, attacks have no cooldown
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            boolean legacy = "legacy".equalsIgnoreCase(instance.plugin.getFileManager().getConfigFile().getString("game.combat"));
            attackSpeed.setBaseValue(legacy ? LEGACY_ATTACK_SPEED : DEFAULT_ATTACK_SPEED);
        }
    }

    /**
     * variant bow-only: the bows get infinity (a bow and an arrow are added if the kit has none)
     */
    private static void giveInfiniteBow(ItemStack[] contents) {
        boolean bow = false;
        boolean arrow = false;
        for (ItemStack item : contents) {
            if (item == null) continue;
            if (item.getType() == Material.BOW) {
                item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
                bow = true;
            }
            if (item.getType() == Material.ARROW) arrow = true;
        }
        for (int i = 0; i < 36 && (!bow || !arrow); i++) {
            if (contents[i] != null) continue;
            if (!bow) {
                ItemStack item = new ItemStack(Material.BOW);
                item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
                contents[i] = item;
                bow = true;
            } else {
                contents[i] = new ItemStack(Material.ARROW);
                arrow = true;
            }
        }
    }

    /**
     * the block break speed of players in vanilla
     */
    public static final double DEFAULT_BLOCK_BREAK_SPEED = 1.0;
    /**
     * the attack speed of players in vanilla
     */
    public static final double DEFAULT_ATTACK_SPEED = 4.0;
    /**
     * so high that the attack cooldown is always over
     */
    public static final double LEGACY_ATTACK_SPEED = 1024.0;

    public Config getConfig() {
        return config;
    }

    private static class Config extends PluginFile {

        public Config() {
            super(new File(FileManager.getPluginFolder(), "items.yml"), "inventory");
        }


        @Override
        public void createFile() throws IOException {
            if (!super.getSourceFile().createNewFile()) {
                throw new Error("Couldn't create file 'arenas.yml'!");
            }
        }

        @Override
        public void update() throws IOException {
            //no update necessary
        }

    }

}
