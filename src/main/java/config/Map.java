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

public class Map {

    SpleggOG splegg;
    String name;
    File file;
    int spawncount;
    int floorcount;
    boolean usable;
    // isUsable runs on every sign refresh and every voting-map pick, so the lobby
    // world warning below is latched instead of logged per call.
    // isUsable runs on every sign refresh and every voting-map pick, so the lobby
    // world warning is latched rather than logged each time. Cleared by load().
    private boolean warnedLobbyWorldMismatch;

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
        this.warnedLobbyWorldMismatch = false;

        this.file = new File(SpleggOG.getPlugin().getDataFolder(), this.name + ".yml");
        try {

            if (!this.file.exists()) {

                this.file.createNewFile();

            }

        } catch (IOException error) {

            SpleggOG.getPlugin().getLogger()
                    .severe("ERROR: Issue creating " + this.name + ".yml: " + error.getMessage());

        }

        this.setConfig(YamlConfiguration.loadConfiguration(this.file));
        this.save();

        this.loadSpawns();
        this.loadFloors();

        SpleggOG.getPlugin().getLogger().info("File set: " + this.file.getName() + ".");

    }

    public void usableDecider(Map map) {

        final boolean valid = this.spawncount >= 2 && this.floorcount > 0 && hasValidConfiguredWorlds();
        if (valid) {

            SpleggOG.getPlugin().getLogger().info("Floor and spawn point(s) detected. The map is ready to go!");
            this.usable = true;

        } else {

            this.usable = false;

        }

    }

    public boolean isUsable(Map map) {

        usableDecider(map);
        return this.usable;

    }

    private boolean hasValidConfiguredWorlds() {

        for (int i = 1; i <= this.spawncount; i++) {

            final String spawnWorld = this.config.getString("Spawns." + i + ".world");
            if (!isWorldEligible(spawnWorld, "spawn " + i)) {

                return false;

            }

        }

        for (int i = 1; i <= this.floorcount; i++) {

            final String floorOneWorld = this.config.getString("Floors." + i + ".p1.world");
            final String floorTwoWorld = this.config.getString("Floors." + i + ".p2.world");
            if (!isWorldEligible(floorOneWorld, "floor " + i + " p1")
                    || !isWorldEligible(floorTwoWorld, "floor " + i + " p2") || !floorOneWorld.equals(floorTwoWorld))
            {

                return false;

            }

        }

        if (this.lobbySet()) {

            final String lobbyWorld = this.config.getString("Spawns.lobby.world");
            if (!isWorldEligible(lobbyWorld, "match lobby")) {

                return false;

            }

            // Not fatal: getQueueLobbyLocation ignores a lobby stored against the
            // wrong world and falls back, so the map still plays. Say so once, since
            // the operator's intended waiting area is silently not being used.
            if (!this.isLobbyInMapWorld() && !this.warnedLobbyWorldMismatch) {

                this.warnedLobbyWorldMismatch = true;

                Bukkit.getLogger()
                        .warning("[Splegg-OG] Map '" + this.getName() + "' has its match lobby saved in world '"
                                + lobbyWorld + "' but its terrain is in '" + this.getTerrainWorldName()
                                + "'. The match lobby is ignored. Re-run /splegg setlobby " + this.getName()
                                + " while standing in the map, or use /splegg setlobby for a global queue lobby.");

            }

        }

        return true;

    }

    // A configured world is eligible for splegg use only when it (1) is loaded
    // by the server, and (2) is NOT in the SMP-protected list. Log loudly when
    // an admin has somehow saved a protected world into a map config -- that is
    // the bug the rest of the safety net is designed to prevent ever happening.
    private boolean isWorldEligible(String worldName, String label) {

        if (worldName == null) {

            return false;

        }

        if (SpleggOG.isProtectedMainWorld(worldName)) {

            SpleggOG.getPlugin().getLogger()
                    .warning("Map '" + this.name + "' has " + label + " configured in protected SMP world '" + worldName
                            + "'. Map will stay DISABLED. Reconfigure it inside a Splegg lobby or in-game world.");
            return false;

        }

        return Bukkit.getWorld(worldName) != null;

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

        } catch (IOException error) {

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

        addSpawn(l);

    }

    public void addSpawn(Location l) {

        this.spawncount++;
        this.savenumbers();
        this.setSpawn(this, this.spawncount, l);

    }

    public void addFloor(Location p1, Location p2, Game game) {

        addFloor(p1, p2);

    }

    public void addFloor(Location p1, Location p2) {

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

        usableDecider(this);

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

    // The world the map's terrain lives in, and therefore the template
    // GameWorldManager copies per match. Spawns and floors define it; the match
    // lobby never does. Reading the lobby first made a lobby set in another world
    // (a shared hub) the thing that got copied as the arena.
    public String getTerrainWorldName() {

        if (this.config.isString("Spawns.1.world")) {

            return this.config.getString("Spawns.1.world");

        }

        if (this.config.isString("Floors.1.p1.world")) {

            return this.config.getString("Floors.1.p1.world");

        }

        return null;

    }

    public String getWorldName() {

        final String terrain = this.getTerrainWorldName();
        if (terrain != null) {

            return terrain;

        }

        // Nothing but a lobby is configured. Such a map has no spawns, so it is
        // already unusable; returning the lobby keeps /splegg info informative.
        return this.lobbySet() ? this.config.getString("Spawns.lobby.world") : null;

    }

    // The match lobby is rebased into each per-game world copy, so its coordinates
    // are only meaningful when they were surveyed in the map's own world. A lobby
    // stored against any other world is a misconfiguration, not a shared hub.
    public boolean isLobbyInMapWorld() {

        if (!this.lobbySet()) {

            return true;

        }

        final String terrain = this.getTerrainWorldName();
        return terrain != null && terrain.equalsIgnoreCase(this.config.getString("Spawns.lobby.world"));

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

    // Rebase helpers: return template-configured coordinates anchored to the
    // supplied per-game world. Used by Game/GameManager so map locations point
    // at the live per-game copy, not the template world.

    public Location getSpawnIn(World world, int id) {

        if (world == null)
            return getSpawn(id);
        int x = config.getInt("Spawns." + id + ".x");
        int y = config.getInt("Spawns." + id + ".y");
        int z = config.getInt("Spawns." + id + ".z");
        float yaw = (float) config.getInt("Spawns." + id + ".yaw");
        float pitch = (float) config.getInt("Spawns." + id + ".pitch");
        return new Location(world, x + 0.5D, y + 0.5D, z + 0.5D, yaw, pitch);

    }

    public Location getFloorIn(World world, int id, String pos) {

        if (world == null)
            return getFloor(id, pos);
        int x = config.getInt("Floors." + id + ".p" + pos + ".x");
        int y = config.getInt("Floors." + id + ".p" + pos + ".y");
        int z = config.getInt("Floors." + id + ".p" + pos + ".z");
        return new Location(world, x, y, z);

    }

    public Location getLobbyIn(World world) {

        if (world == null)
            return getLobby();
        if (!lobbySet())
            return null;
        int x = config.getInt("Spawns.lobby.x");
        int y = config.getInt("Spawns.lobby.y");
        int z = config.getInt("Spawns.lobby.z");
        float yaw = (float) config.getInt("Spawns.lobby.yaw");
        float pitch = (float) config.getInt("Spawns.lobby.pitch");
        return new Location(world, x + 0.5D, y + 0.5D, z + 0.5D, yaw, pitch);

    }

    public Location getSpecIn(World world) {

        if (world == null)
            return getSpawnSpec();
        int x = config.getInt("Spec.x");
        int y = config.getInt("Spec.y");
        int z = config.getInt("Spec.z");
        float yaw = (float) config.getInt("Spec.yaw");
        float pitch = (float) config.getInt("Spec.pitch");
        return new Location(world, x + 0.5D, y + 0.5D, z + 0.5D, yaw, pitch);

    }

    @SuppressWarnings("unused")
    private static Game gameTypeReference() {

        // Anchors the Game import for addSpawn(Game)/addFloor(Game) callers.
        return null;

    }

}
