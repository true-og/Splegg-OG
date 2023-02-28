package utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import com.earth2me.essentials.Kit;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class Utils {

	// Enable the conversion of text from config.yml to objects.
	public FileConfiguration config = SpleggOG.getPlugin().getConfig();

	public HashMap<String, UtilPlayer> PLAYERS = new HashMap<String, UtilPlayer>();
	private File f;
	private String prefix = config.getString("Messages.Prefix").replaceAll("&", "§");

	public String getPrefix() {

		return this.prefix;

	}

	public void log(String s) {

		Bukkit.getConsoleSender().sendMessage(this.prefix + s);

	}

	public void sendMessage(Player player, String s) {

		player.sendMessage(this.prefix + ChatColor.translateAlternateColorCodes('&', s));

	}

	public void bc(String s) {

		TextComponent prefixContainer = Component.text(this.prefix + ChatColor.translateAlternateColorCodes('&', s));
		Bukkit.broadcast(prefixContainer);

	}

	public void bc(String string, Game game) {

		Iterator<?> playerIterator = SpleggOG.getPlugin().pm.PLAYERS.values().iterator();
		while(playerIterator.hasNext()) {

			UtilPlayer u = (UtilPlayer) playerIterator.next();
			if (u.getGame() == game && u.isAlive()) {

				sendMessage(u.getPlayer(), string);

			}

		}

	}

	public void setup() {

		this.f = new File(SpleggOG.getPlugin().getDataFolder(), "spawns.yml");

		try {

			if (! this.f.exists()) {

				this.f.createNewFile();

			}

		}
		catch (IOException error) {

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

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().severe("An error occured while saving spawns.yml.");

		}

	}

	public void setLobby(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		float yaw = l.getYaw();
		float pitch = l.getPitch();
		String worldName = l.getWorld().getName();

		this.config.set("Spawns.lobby.world", worldName);
		this.config.set("Spawns.lobby.x", x);
		this.config.set("Spawns.lobby.y", y);
		this.config.set("Spawns.lobby.z", z);
		this.config.set("Spawns.lobby.pitch", pitch);
		this.config.set("Spawns.lobby.yaw", yaw);

		saveSpawns();

	}

	public Location getLobby(Player player) {

		int x = this.config.getInt("Spawns.lobby.x");
		int y = this.config.getInt("Spawns.lobby.y");
		int z = this.config.getInt("Spawns.lobby.z");

		float yaw = (float) this.config.getInt("Spawns.lobby.yaw");
		float pitch = (float) this.config.getInt("Spawns.lobby.pitch");

		World worldName = player.getWorld();

		return new Location(worldName, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

	public UtilPlayer getPlayer(String name) {

		return (UtilPlayer) this.PLAYERS.get(name);

	}

	public UtilPlayer getPlayer(Player player) {

		return (UtilPlayer) this.PLAYERS.get(player.getName());

	}

	public void clearInventory(Player player) {

		// TODO: Save this inventory and give it back to the player after the game.
		PlayerInventory pInv = player.getInventory();
		Kit preGameInventory = pInv;
		pInv.setArmorContents((ItemStack[]) null);
		pInv.clear();

		player.setFireTicks(0);
		clearPotions(player);

	}

	public void clearPotions(Player player) {

		Iterator<?> activePotionEffects = player.getActivePotionEffects().iterator();
		while(activePotionEffects.hasNext()) {

			PotionEffect effect = (PotionEffect) activePotionEffects.next();

			player.removePotionEffect(effect.getType());

		}

	}

	public static ItemStack getItem(Material material, String name, String lore) {

		ItemStack stack = new ItemStack(material, 1);
		ItemMeta meta = stack.getItemMeta();
		TextComponent nameContainer = null;
		TextComponent loreContainer = null;
		try {

			nameContainer = Component.text(name);
			loreContainer = Component.text(lore);

		}
		catch(NullPointerException error) {

			SpleggOG.getPlugin().getLogger().severe("Failed to get Item information for: " + material + ".");

		}

		meta.displayName(nameContainer);
		meta.lore(Arrays.asList(loreContainer));
		stack.setItemMeta(meta);

		return stack;

	}

	public static void fireEgg(PlayerInteractEvent event, UtilPlayer u, Player player, ItemStack itemInHand) {

		if(u.getGame() != null) {

			if(u.getGame().getStatus().equals(Status.INGAME) && u.isAlive()) {

				if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {

					switch(itemInHand.getType()) {

					case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL:
						player.launchProjectile(Egg.class);
					eggSound(player);
					break;
					case DIAMOND_SHOVEL:
						for(int i = 0; i < 2; i = i + 1) {

							player.launchProjectile(Egg.class);

						}
						eggSound(player);
						break;
					case NETHERITE_SHOVEL:
						for(int i = 0; i < 3; i = i + 1) {

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

		TextComponent shopTitle = Component.text(SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title").replaceAll("&", "§"));
		Inventory shop = Bukkit.createInventory((InventoryHolder) null, InventoryType.CHEST, shopTitle);

		shop.setItem(0, Utils.getItem(Material.GOLDEN_SHOVEL, SpleggOG.getPlugin().getConfig().getString("GUI.Shop.GoldShovel.Name").replaceAll("&", "§"), SpleggOG.getPlugin().getConfig().getString("GUI.Shop.GoldShovel.Description").replaceAll("&", "§").replaceAll("%price%", String.valueOf(SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price")))));
		shop.setItem(1, Utils.getItem(Material.DIAMOND_SHOVEL, SpleggOG.getPlugin().getConfig().getString("GUI.Shop.DiamondShovel.Name").replaceAll("&", "§"), SpleggOG.getPlugin().getConfig().getString("GUI.Shop.DiamondShovel.Description").replaceAll("&", "§").replaceAll("%price%", String.valueOf(SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price")))));

		return shop;

	}

	// TODO: Spectator mode in listener
	/*private static Inventory getSpecInventory() {

		TextComponent spectatorTitle = Component.text("Splegg - Spectators");
		Inventory spec = Bukkit.createInventory((InventoryHolder) null, InventoryType.CHEST, spectatorTitle);

		return spec;

	}*/

}