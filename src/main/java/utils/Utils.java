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
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import main.SpleggOG;
import managers.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class Utils {

	// Enable the conversion of text from config.yml to objects.
	public FileConfiguration config = SpleggOG.getPlugin().getConfig();

	public HashMap<String, UtilPlayer> PLAYERS = new HashMap<String, UtilPlayer>();
	public FileConfiguration spawns;
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

	public void bcNotForPlayer(Player player, String string, Game game) {

		Iterator<?> var5 = SpleggOG.getPlugin().pm.PLAYERS.values().iterator();
		while(var5.hasNext()) {

			UtilPlayer u = (UtilPlayer) var5.next();

			if (u.getGame() == game && u.isAlive() && u.getPlayer() != player) {

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

			SpleggOG.getPlugin().getLogger().info("An error occured while creating spawns.yml.");

		}

		reloadSpawns();
		saveSpawns();
		reloadSpawns();

	}

	private void reloadSpawns() {

		this.spawns = YamlConfiguration.loadConfiguration(f);

	}

	private void saveSpawns() {

		try {

			this.spawns.save(f);

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().info("An error occured while saving spawns.yml.");

		}

	}

	public void setLobby(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		float yaw = l.getYaw();
		float pitch = l.getPitch();
		String worldName = l.getWorld().getName();

		this.spawns.set("Spawns.lobby.world", worldName);
		this.spawns.set("Spawns.lobby.x", x);
		this.spawns.set("Spawns.lobby.y", y);
		this.spawns.set("Spawns.lobby.z", z);
		this.spawns.set("Spawns.lobby.pitch", pitch);
		this.spawns.set("Spawns.lobby.yaw", yaw);

		saveSpawns();

	}

	public Location getLobby() {

		int x = this.spawns.getInt("Spawns.lobby.x");
		int y = this.spawns.getInt("Spawns.lobby.y");
		int z = this.spawns.getInt("Spawns.lobby.z");

		float yaw = (float) this.spawns.getInt("Spawns.lobby.yaw");
		float pitch = (float) this.spawns.getInt("Spawns.lobby.pitch");
		World worldName = Bukkit.getWorld(this.spawns.getString("Spawns.lobby.world"));

		return new Location(worldName, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

	public UtilPlayer getPlayer(String name) {

		return (UtilPlayer) this.PLAYERS.get(name);

	}

	public UtilPlayer getPlayer(Player player) {

		return (UtilPlayer) this.PLAYERS.get(player.getName());

	}

	public void clearInventory(Player player) {

		PlayerInventory pInv = player.getInventory();
		pInv.setArmorContents((ItemStack[]) null);
		pInv.clear();

		player.setFireTicks(0);
		clearPotions(player);

	}

	public void clearPotions(Player player) {

		Iterator<?> var3 = player.getActivePotionEffects().iterator();
		while(var3.hasNext()) {

			PotionEffect effect = (PotionEffect) var3.next();

			player.removePotionEffect(effect.getType());

		}

	}

	public static ItemStack getItem(Material material, String name, String lore) {

		ItemStack stack = new ItemStack(material, 1);
		ItemMeta meta = stack.getItemMeta();

		TextComponent nameContainer = Component.text(name);
		TextComponent loreContainer = Component.text(lore);

		meta.displayName(nameContainer);
		meta.lore(Arrays.asList(loreContainer));
		stack.setItemMeta(meta);

		return stack;

	}

}