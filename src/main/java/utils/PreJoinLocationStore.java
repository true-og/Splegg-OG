package utils;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.bergerkiller.bukkit.mw.WorldConfig;

// Disk-backed record of where each player stood before they entered Splegg territory.
// Same layout as TheHerobrine-OG and BuildBattle-OG so a relog or restart still returns them.
public class PreJoinLocationStore {

    private static final long DAY_MILLIS = 86400000L;
    private static final long FLUSH_INTERVAL_TICKS = 100L;

    private record Entry(String world, double x, double y, double z, float yaw, float pitch, long savedAt) {

        static Entry of(Location location) {

            return new Entry(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(), System.currentTimeMillis());

        }

    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private BukkitTask flushTask;

    public PreJoinLocationStore(JavaPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prejoin-locations.yaml");
        load();
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);

    }

    public synchronized void load() {

        entries.clear();
        if (!file.exists()) {

            return;

        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("locations");
        if (section == null)
            return;

        long expiryDays = Math.max(0, plugin.getConfig().getInt("Options.PreJoinLocationExpiryDays", 30));
        long oldestAllowed = expiryDays == 0 ? 0 : System.currentTimeMillis() - (expiryDays * DAY_MILLIS);
        int expired = 0;

        for (String key : section.getKeys(false)) {

            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null)
                continue;

            String world = s.getString("world");
            if (world == null) {

                plugin.getLogger().warning("Skipping malformed return location entry '" + key + "'.");
                continue;

            }

            UUID id;
            try {

                id = UUID.fromString(key);

            } catch (IllegalArgumentException e) {

                plugin.getLogger().warning("Skipping return location entry with invalid UUID '" + key + "'.");
                continue;

            }

            long savedAt = s.getLong("saved", System.currentTimeMillis());
            if (savedAt < oldestAllowed) {

                expired++;
                continue;

            }

            entries.put(id, new Entry(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                    (float) s.getDouble("yaw"), (float) s.getDouble("pitch"), savedAt));

        }

        if (expired > 0)
            dirty = true;
        plugin.getLogger().info("Loaded " + entries.size() + " stored return location(s)"
                + (expired > 0 ? ", dropped " + expired + " older than " + expiryDays + " day(s)." : "."));

    }

    // Only writes when something actually changed, so the repeating flush is free
    // on an idle server.
    public synchronized void save() {

        if (!dirty)
            return;

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {

            String path = "locations." + e.getKey();
            Entry entry = e.getValue();
            yaml.set(path + ".world", entry.world());
            yaml.set(path + ".x", entry.x());
            yaml.set(path + ".y", entry.y());
            yaml.set(path + ".z", entry.z());
            yaml.set(path + ".yaw", (double) entry.yaw());
            yaml.set(path + ".pitch", (double) entry.pitch());
            yaml.set(path + ".saved", entry.savedAt());

        }

        try {

            if (!plugin.getDataFolder().exists())
                plugin.getDataFolder().mkdirs();
            yaml.save(file);
            dirty = false;

        } catch (Exception e) {

            plugin.getLogger().warning("Failed to save return locations: " + e.getMessage());

        }

    }

    public void put(UUID playerId, Location location) {

        if (playerId == null || location == null || location.getWorld() == null)
            return;
        entries.put(playerId, Entry.of(location));
        dirty = true;

    }

    public void remove(UUID playerId) {

        if (playerId != null && entries.remove(playerId) != null)
            dirty = true;

    }

    public boolean has(UUID playerId) {

        return playerId != null && entries.containsKey(playerId);

    }

    public String getWorldName(UUID playerId) {

        Entry entry = playerId != null ? entries.get(playerId) : null;
        return entry != null ? entry.world() : null;

    }

    // Resolves the stored spot against a loaded world. Null when the world is not
    // loaded right now -- the entry is kept so a later attempt can still use it.
    public Location get(UUID playerId) {

        Entry entry = playerId != null ? entries.get(playerId) : null;
        if (entry == null)
            return null;

        World world = Bukkit.getWorld(entry.world());
        if (world == null)
            return null;

        return new Location(world, entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());

    }

    // Same as get(), but pulls the world back in through MyWorlds when it is
    // unloaded -- a player who came from the End belongs in the End.
    public Location getOrLoadWorld(UUID playerId) {

        Location resolved = get(playerId);
        if (resolved != null)
            return resolved;

        String worldName = getWorldName(playerId);
        if (worldName == null)
            return null;

        WorldConfig wc = WorldConfig.getIfExists(worldName);
        if (wc == null) {

            plugin.getLogger().warning("Stored return world '" + worldName + "' is unknown to MyWorlds.");
            return null;

        }

        plugin.getLogger().info("Loading world '" + worldName + "' to return a player to their pre-join location.");
        if (wc.loadWorld() == null)
            return null;

        return get(playerId);

    }

    public void shutdown() {

        if (flushTask != null) {

            try {

                flushTask.cancel();

            } catch (IllegalStateException ignored) {

            }

            flushTask = null;

        }

        save();

    }

}
