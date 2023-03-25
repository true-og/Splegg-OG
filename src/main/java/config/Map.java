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
	int spawncount;
	int floorcount;
	boolean usable;
	
	// Enable the conversion of text from config.yml to objects.
	public FileConfiguration config = SpleggOG.getPlugin().getConfig();

	public Map(SpleggOG splegg, String name) {

		this.splegg = splegg;
		this.name = name;
		this.floorcount = 0;
		this.spawncount = 0;

	}

	public void load() {

		SpleggOG.getPlugin().getLogger().info("Loading map " + this.name + "...");

		this.file = new File(SpleggOG.getPlugin().getDataFolder(), this.name + ".yml");
		try {

			if (! this.file.exists()) {

				this.file.createNewFile();

			}

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().severe("ERROR: Issue creating " + this.name + ".yml: " + error.getMessage());

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

		config.set("Spawns.count", this.spawncount);
		config.set("Floors.count", this.floorcount);

	}

	public void save() {

		try {

			config.save(this.file);

		}
		catch (IOException error) {

			SpleggOG.getPlugin().getLogger().severe("An error occured while saving " + this.name + ".yml.");

		}

	}

	public void setSpawn(Map map, int id, Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		config.set("Spawns." + id + ".world", worldname);
		config.set("Spawns." + id + ".x", x);
		config.set("Spawns." + id + ".y", y);
		config.set("Spawns." + id + ".z", z);
		config.set("Spawns." + id + ".pitch", pitch);
		config.set("Spawns." + id + ".yaw", yaw);

		this.save();
		
		usableDecider(map);

	}

	public Location getSpawn(int id) {

		int x = config.getInt("Spawns." + id + ".x");
		int y = config.getInt("Spawns." + id + ".y");
		int z = config.getInt("Spawns." + id + ".z");

		float yaw = (float) config.getInt("Spawns." + id + ".yaw");
		float pitch = (float) config.getInt("Spawns." + id + ".pitch");

		World world = Bukkit.getWorld(config.getString("Spawns." + id + ".world"));

		return new Location(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

	public void setSpec(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		config.set("Spec.world", worldname);
		config.set("Spec.x", x);
		config.set("Spec.y", y);
		config.set("Spec.z", z);
		config.set("Spec.pitch", pitch);
		config.set("Spec.yaw", yaw);

	}

	public Location getSpawnSpec() {

		int x = config.getInt("Spec.x");
		int y = config.getInt("Spec.y");
		int z = config.getInt("Spec.z");

		float yaw = (float) config.getInt("Spec.yaw");
		float pitch = (float) config.getInt("Spec.pitch");

		World world = Bukkit.getWorld(config.getString("Spec.world"));

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

		return config.getInt("Spawns.count");

	}

	public int getFloors() {

		return config.getInt("Floors.count");

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

		return config.isString("Spawns.lobby.world");

	}

	public void setLobby(Location l) {

		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();

		float pitch = l.getPitch();
		float yaw = l.getYaw();

		String worldname = l.getWorld().getName();

		config.set("Spawns.lobby.world", worldname);
		config.set("Spawns.lobby.x", x);
		config.set("Spawns.lobby.y", y);
		config.set("Spawns.lobby.z", z);
		config.set("Spawns.lobby.pitch", pitch);
		config.set("Spawns.lobby.yaw", yaw);

		this.save();

	}

	public Location getLobby() {

		int x = config.getInt("Spawns.lobby.x");
		int y = config.getInt("Spawns.lobby.y");
		int z = config.getInt("Spawns.lobby.z");

		float yaw = (float) config.getInt("Spawns.lobby.yaw");
		float pitch = (float) config.getInt("Spawns.lobby.pitch");

		World world = Bukkit.getWorld(config.getString("Spawns.lobby.world"));

		return new Location(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, yaw, pitch);

	}

}