package stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import main.SpleggOG;

// Persistent per-player Splegg points behind the sp_score and sp_rank placeholders.
// Kept in memory and flushed to stats.yml off the main thread.
public class SpleggStats {

    private static final long AUTOSAVE_TICKS = 20L * 30L;

    private static SpleggStats instance;

    private final SpleggOG plugin;
    private final File file;
    private final Map<UUID, Integer> points = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private volatile UUID topPlayer;
    private volatile int topPoints;
    private BukkitTask autosaveTask;

    public SpleggStats(SpleggOG plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        instance = this;

    }

    public static SpleggStats get() {

        return instance;

    }

    // Points awarded per block an egg destroys.
    public static int blockPoints() {

        return SpleggOG.getPlugin().getConfig().getInt("Points.BlockBroken", 1);

    }

    // Points awarded to the match winner.
    public static int winPoints() {

        return SpleggOG.getPlugin().getConfig().getInt("Points.Win", 50);

    }

    public void load() {

        points.clear();
        names.clear();
        topPlayer = null;
        topPoints = 0;

        if (!file.exists()) {

            return;

        }

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {

            return;

        }

        for (String key : players.getKeys(false)) {

            final UUID uuid;
            try {

                uuid = UUID.fromString(key);

            } catch (IllegalArgumentException invalid) {

                plugin.getLogger().warning("Skipping stats.yml entry with an invalid UUID: " + key);
                continue;

            }

            points.put(uuid, Math.max(players.getInt(key + ".points", 0), 0));
            final String name = players.getString(key + ".name");
            if (name != null) {

                names.put(uuid, name);

            }

            bumpTop(uuid);

        }

        plugin.getLogger().info("Loaded Splegg points for " + points.size() + " players.");

    }

    public void startAutosave() {

        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

            if (dirty.compareAndSet(true, false)) {

                save();

            }

        }, AUTOSAVE_TICKS, AUTOSAVE_TICKS);

    }

    // Final synchronous flush; safe to call from onDisable.
    public void shutdown() {

        if (autosaveTask != null) {

            autosaveTask.cancel();
            autosaveTask = null;

        }

        if (dirty.compareAndSet(true, false)) {

            save();

        }

        if (instance == this) {

            instance = null;

        }

    }

    public int getPoints(UUID uuid) {

        return points.getOrDefault(uuid, 0);

    }

    public void addPoints(Player player, int amount) {

        if (player == null || amount <= 0) {

            return;

        }

        final UUID uuid = player.getUniqueId();
        points.merge(uuid, amount, Integer::sum);
        names.put(uuid, player.getName());
        bumpTop(uuid);
        dirty.set(true);

    }

    // Same rule as TheHerobrine-OG's Death Bringer: the reserved rank belongs to
    // the top scorer.
    public SpleggRank getRank(UUID uuid) {

        final int score = getPoints(uuid);
        if (uuid.equals(topPlayer) && score >= SpleggRank.topPlayerGate()) {

            return SpleggRank.EGGSPERT;

        }

        return SpleggRank.findRank(score);

    }

    public UUID getTopPlayer() {

        return topPlayer;

    }

    private void bumpTop(UUID uuid) {

        final int score = getPoints(uuid);
        if (uuid.equals(topPlayer) || score > topPoints) {

            topPlayer = uuid;
            topPoints = score;

        }

    }

    private synchronized void save() {

        final YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : new HashMap<>(points).entrySet()) {

            final String base = "players." + entry.getKey();
            config.set(base + ".points", entry.getValue());
            final String name = names.get(entry.getKey());
            if (name != null) {

                config.set(base + ".name", name);

            }

        }

        try {

            config.save(file);

        } catch (IOException error) {

            plugin.getLogger().severe("An error occured while saving stats.yml: " + error.getMessage());
            dirty.set(true);

        }

    }

}
