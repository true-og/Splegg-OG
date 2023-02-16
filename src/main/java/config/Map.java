package config;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import main.SpleggOG;
import managers.Game;
import managers.Status;

public class Map {

	SpleggOG splegg;
	String name;
	File file;
	private FileConfiguration config;
	int spawncount;
	int floorcount;
	boolean usable;

	public Map(SpleggOG splegg, String name) {

		this.splegg = splegg;
		this.name = name;
		this.floorcount = 0;
		this.spawncount = 0;

	}

	public void load() {

		SpleggOG.getPlugin().chat.log("Loading map " + this.name + "...");

		this.file = new File(SpleggOG.getPlugin().getDataFolder(), this.name + ".yml");
		try {

			if (! this.file.exists()) {

				this.file.createNewFile();

			}

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().info("ERROR: Issue creating " + this.name + ".yml: " + error.getMessage());

		}

		this.setConfig(YamlConfiguration.loadConfiguration(this.file));
		this.save();

		this.loadSpawns();
		this.loadFloors();

		SpleggOG.getPlugin().getLogger().info("File set: " + this.file.getName() + ".");

	}

	public void usableDecider(Map map) {

		if (this.spawncount > 0 && this.floorcount > 0) {

			SpleggOG.getPlugin().getLogger().info("Floor and spawn point(s) detected. The map is ready to go!");
			this.usable = true;
			SpleggOG.getPlugin().games.getGame(this.getName()).setStatus(Status.LOBBY);

		}
		else {

			this.usable = false;

		}

	}

	public boolean isUsable(Map map) {

		usableDecider(map);
		return this.usable;

	}

	public void delete() {

		this.file.delete();

	}

	public void savenumbers() {

		this.getConfig().set("Spawns.count", this.spawncount);
		this.getConfig().set("Floors.count", this.floorcount);

	}

	public void save() {

		try {

			this.getConfig().save(this.file);

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().info("An error occured while saving " + this.name + ".yml.");

		}

	}

	public void setSpawn(Map map, int id, Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		this.getConfig().set("Spawns." + id + ".world", worldname);
		this.getConfig().set("Spawns." + id + ".x", x);
		this.getConfig().set("Spawns." + id + ".y", y);
		this.getConfig().set("Spawns." + id + ".z", z);
		this.getConfig().set("Spawns." + id + ".pitch", pitch);
		this.getConfig().set("Spawns." + id + ".yaw", yaw);

		this.save();
		
		usableDecider(map);

	}

	public Location getSpawn(int id) {

		int x = this.getConfig().getInt("Spawns." + id + ".x");
		int y = this.getConfig().getInt("Spawns." + id + ".y");
		int z = this.getConfig().getInt("Spawns." + id + ".z");

		float yaw = (float) this.getConfig().getInt("Spawns." + id + ".yaw");
		float pitch = (float) this.getConfig().getInt("Spawns." + id + ".pitch");

		World world = Bukkit.getWorld(this.getConfig().getString("Spawns." + id + ".world"));

		return new Location(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

	public void setSpec(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		this.getConfig().set("Spec.world", worldname);
		this.getConfig().set("Spec.x", x);
		this.getConfig().set("Spec.y", y);
		this.getConfig().set("Spec.z", z);
		this.getConfig().set("Spec.pitch", pitch);
		this.getConfig().set("Spec.yaw", yaw);

	}

	public Location getSpawnSpec() {

		int x = this.getConfig().getInt("Spec.x");
		int y = this.getConfig().getInt("Spec.y");
		int z = this.getConfig().getInt("Spec.z");

		float yaw = (float) this.getConfig().getInt("Spec.yaw");
		float pitch = (float) this.getConfig().getInt("Spec.pitch");

		World world = Bukkit.getWorld(this.getConfig().getString("Spec.world"));

		return new Location(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

	public String getName() {

		return this.name;

	}

	public void loadSpawns() {

		this.spawncount = getCount();

	}

	public void loadFloors() {

		this.floorcount = getFloors();

	}

	public void addSpawn(Location l, Game game) {

		this.spawncount++;
		this.savenumbers();
		this.setSpawn(game.getMap(), this.spawncount, l);

	}

	public void addFloor(Location p1, Location p2, Game game) {

		this.floorcount++;
		this.savenumbers();

		this.config.set("Floors." + this.floorcount + ".p1.x", p1.getBlockX());
		this.config.set("Floors." + this.floorcount + ".p1.y", p1.getBlockY());
		this.config.set("Floors." + this.floorcount + ".p1.z", p1.getBlockZ());
		this.config.set("Floors." + this.floorcount + ".p1.world", p1.getWorld().getName());
		this.config.set("Floors." + this.floorcount + ".p2.x", p2.getBlockX());
		this.config.set("Floors." + this.floorcount + ".p2.y", p2.getBlockY());
		this.config.set("Floors." + this.floorcount + ".p2.z", p2.getBlockZ());
		this.config.set("Floors." + this.floorcount + ".p2.world", p2.getWorld().getName());

		this.save();

		usableDecider(game.getMap());

	}

	public Location getFloor(int id, String pos) {

		int x = this.config.getInt("Floors." + id + ".p" + pos + ".x");
		int y = this.config.getInt("Floors." + id + ".p" + pos + ".y");
		int z = this.config.getInt("Floors." + id + ".p" + pos + ".z");

		String world = this.config.getString("Floors." + id + ".p" + pos + ".world");

		return new Location(Bukkit.getWorld(world), (double) x, (double) y, (double) z);

	}

	public int getCount() {

		return this.getConfig().getInt("Spawns.count");

	}

	public int getFloors() {

		return this.getConfig().getInt("Floors.count");

	}

	public int getSpawnCount() {

		return this.spawncount;

	}

	public FileConfiguration getConfig() {

		return this.config;

	}

	public void setConfig(FileConfiguration config) {

		this.config = config;

	}

	public boolean lobbySet() {

		return this.getConfig().isString("Spawns.lobby.world");

	}

	public void setLobby(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		this.getConfig().set("Spawns.lobby.world", worldname);
		this.getConfig().set("Spawns.lobby.x", x);
		this.getConfig().set("Spawns.lobby.y", y);
		this.getConfig().set("Spawns.lobby.z", z);
		this.getConfig().set("Spawns.lobby.pitch", pitch);
		this.getConfig().set("Spawns.lobby.yaw", yaw);

		this.save();

	}

	public Location getLobby() {

		int x = this.getConfig().getInt("Spawns.lobby.x");
		int y = this.getConfig().getInt("Spawns.lobby.y");
		int z = this.getConfig().getInt("Spawns.lobby.z");

		float yaw = (float) this.getConfig().getInt("Spawns.lobby.yaw");
		float pitch = (float) this.getConfig().getInt("Spawns.lobby.pitch");

		World world = Bukkit.getWorld(this.getConfig().getString("Spawns.lobby.world"));

		return new Location(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

}