package utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Utils {

    // Enable the conversion of text from config.yml to objects.
    public FileConfiguration config = SpleggOG.getPlugin().getConfig();
    public HashMap<String, UtilPlayer> PLAYERS = new HashMap<>();
    private File f;
    public static String prefix = "&7[&eSplegg&f-&4OG&7] ";

    public void bc(String s) {

        final TextComponent prefixContainer = legacySerializerAnyCase(prefix + s);
        Bukkit.broadcast(prefixContainer);

    }

    public void bc(String string, Game game) {

        final Iterator<?> playerIterator = SpleggOG.getPlugin().pm.PLAYERS.values().iterator();
        while (playerIterator.hasNext()) {

            final UtilPlayer u = (UtilPlayer) playerIterator.next();
            if (u.getGame() == game && u.isAlive()) {

                spleggOGMessage(u.getPlayer(), string);

            }

        }

    }

    public void setup() {

        this.f = new File(SpleggOG.getPlugin().getDataFolder(), "spawns.yml");

        try {

            if (!this.f.exists()) {

                this.f.createNewFile();

            }

        } catch (IOException error) {

            SpleggOG.getPlugin().getLogger().severe("An error occured while creating spawns.yml.");

        }

        reloadSpawns();
        saveSpawns();
        reloadSpawns();

    }

    private void reloadSpawns() {

        this.config = YamlConfiguration.loadConfiguration(f);

    }

    private void saveSpawns() {

        try {

            this.config.save(f);

        } catch (IOException error) {

            SpleggOG.getPlugin().getLogger().severe("An error occured while saving spawns.yml.");

        }

    }

    public void setLobby(Location l) {

        setSpawn("Spawns.lobby", l);

    }

    public Location getLobby(Player player) {

        return getSpawn("Spawns.lobby");

    }

    private void setSpawn(String path, Location location) {

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        String worldName = location.getWorld().getName();

        this.config.set(path + ".world", worldName);
        this.config.set(path + ".x", x);
        this.config.set(path + ".y", y);
        this.config.set(path + ".z", z);
        this.config.set(path + ".pitch", pitch);
        this.config.set(path + ".yaw", yaw);

        saveSpawns();

    }

    private boolean hasSpawn(String path) {

        return this.config.isString(path + ".world");

    }

    private Location getSpawn(String path) {

        if (!hasSpawn(path)) {

            return null;

        }

        int x = this.config.getInt(path + ".x");
        int y = this.config.getInt(path + ".y");
        int z = this.config.getInt(path + ".z");

        float yaw = (float) this.config.getInt(path + ".yaw");
        float pitch = (float) this.config.getInt(path + ".pitch");

        World worldName = Bukkit.getWorld(this.config.getString(path + ".world"));
        if (worldName == null) {

            return null;

        }

        return new Location(worldName, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

    }

    public UtilPlayer getPlayer(String name) {

        return (UtilPlayer) this.PLAYERS.get(name);

    }

    public UtilPlayer getPlayer(Player player) {

        return (UtilPlayer) this.PLAYERS.get(player.getName());

    }

    // Sends a formatted message to the player (including name replacement).
    public static void spleggOGMessage(Player p, String message) {

        p.sendMessage(legacySerializerAnyCase(prefix + message));

    }

    // Convert "&"-prefixed legacy colors into the "§" section form Bukkit APIs
    // that still take raw Strings (sign lines, scoreboard entries) understand.
    public static String legacySectionize(String subject) {

        return LegacyComponentSerializer.legacySection().serialize(legacySerializerAnyCase(subject));

    }

    public static TextComponent legacySerializerAnyCase(String subject) {

        int count = 0;
        // Count the number of '&' characters to determine the size of the array
        for (char c : subject.toCharArray()) {

            if (c == '&') {

                count++;

            }

        }

        // Create an array to store the positions of '&' characters
        int[] positions = new int[count];
        int index = 0;
        // Find the positions of '&' characters and store in the array
        for (int i = 0; i < subject.length(); i++) {

            if (subject.charAt(i) == '&') {

                if (isUpperBukkitCode(subject.charAt(i + 1))) {

                    subject = replaceCharAtIndex(subject, (i + 1), Character.toLowerCase(subject.charAt(i + 1)));

                }

                positions[index++] = i;

            }

        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(subject);

    }

    private static boolean isUpperBukkitCode(char input) {

        char[] bukkitColorCodes = { 'A', 'B', 'C', 'D', 'E', 'F', 'K', 'L', 'M', 'N', 'O', 'R' };
        boolean match = false;

        // Loop through each character in the array.
        for (char c : bukkitColorCodes) {

            // Check if the current character in the array is equal to the input character.
            if (c == input) {

                match = true;

            }

        }

        return match;

    }

    private static String replaceCharAtIndex(String original, int index, char newChar) {

        // Check if the index is valid
        if (index >= 0 && index < original.length()) {

            // Create a new string with the replaced character
            return original.substring(0, index) + newChar + original.substring(index + 1);

        }

        // If the index is invalid, return the original string
        return original;

    }

    public static ItemStack getItem(Material material, String name, String lore) {

        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        TextComponent nameContainer = null;
        TextComponent loreContainer = null;
        try {

            nameContainer = Component.text(name);
            loreContainer = Component.text(lore);

        } catch (NullPointerException error) {

            SpleggOG.getPlugin().getLogger().severe("Failed to get Item information for: " + material + ".");

        }

        meta.displayName(nameContainer);
        meta.lore(Arrays.asList(loreContainer));
        stack.setItemMeta(meta);

        return stack;

    }

    public static void fireEgg(PlayerInteractEvent event, UtilPlayer u, Player player, ItemStack itemInHand) {

        if (u.getGame() != null) {

            if (u.getGame().getStatus().equals(Status.INGAME) && u.isAlive()) {

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {

                    switch (itemInHand.getType()) {

                        case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL:
                            player.launchProjectile(Egg.class);
                            eggSound(player);
                            break;
                        case DIAMOND_SHOVEL:
                            for (int i = 0; i < 2; i = i + 1) {

                                player.launchProjectile(Egg.class);

                            }
                            eggSound(player);
                            break;
                        case NETHERITE_SHOVEL:
                            for (int i = 0; i < 3; i = i + 1) {

                                player.launchProjectile(Egg.class);

                            }
                            eggSound(player);
                            break;
                        default:
                            break;

                    }

                }

            }

        }

    }

    private static void eggSound(Player player) {

        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.10F, 2.0F);

    }

    public static Inventory getShopInventory() {

        // Create the GUI title.
        TextComponent shopTitleComponent = legacySerializerAnyCase(
                SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"));
        String shopTitle = LegacyComponentSerializer.legacyAmpersand().serialize(shopTitleComponent);

        // Create the inventory.
        Inventory shop = Bukkit.createInventory((InventoryHolder) null, 9, Component.text(shopTitle));

        shop.setItem(0, getShopItem(Material.WOODEN_SHOVEL, "GUI.Shop.WoodShovel"));
        shop.setItem(1, getShopItem(Material.STONE_SHOVEL, "GUI.Shop.StoneShovel"));
        shop.setItem(2, getShopItem(Material.IRON_SHOVEL, "GUI.Shop.IronShovel"));
        shop.setItem(3, getShopItem(Material.GOLDEN_SHOVEL, "GUI.Shop.GoldShovel"));
        shop.setItem(4, getShopItem(Material.DIAMOND_SHOVEL, "GUI.Shop.DiamondShovel"));
        shop.setItem(5, getShopItem(Material.NETHERITE_SHOVEL, "GUI.Shop.NetheriteShovel"));

        // Pass the shop to the player.
        return shop;

    }

    private static ItemStack getShopItem(Material material, String configPath) {

        final String name = LegacyComponentSerializer.legacyAmpersand()
                .serialize(legacySerializerAnyCase(SpleggOG.getPlugin().getConfig().getString(configPath + ".Name")));
        final String description = LegacyComponentSerializer.legacyAmpersand().serialize(
                legacySerializerAnyCase(SpleggOG.getPlugin().getConfig().getString(configPath + ".Description")
                        .replaceAll("%price%", "&B" + SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price"))));

        return Utils.getItem(material, name, description);

    }

    // TODO: Spectator mode in listener.
    /*
     * private static Inventory getSpecInventory() {
     * 
     * TextComponent spectatorTitle = Component.text("Splegg - Spectators");
     * Inventory spec = Bukkit.createInventory((InventoryHolder) null,
     * InventoryType.CHEST, spectatorTitle);
     * 
     * return spec;
     * 
     * }
     */

}
