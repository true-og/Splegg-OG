package managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldInventory;

import config.Map;
import main.SpleggOG;

/**
 * Owns the per-game world lifecycle for Splegg matches.
 *
 * Each {@link Game} is bound to a copy of its template world; the copy lives at
 * {@code <worldContainer>/splegg-<gameId>-<templateName>/} and is deleted when
 * the game ends. The original template world remains loaded for admin editing
 * and is never modified by gameplay.
 */
public class GameWorldManager {

    public static final String COPY_PREFIX = "splegg-";

    private final SpleggOG plugin;

    public GameWorldManager(SpleggOG plugin) {

        this.plugin = plugin;

    }

    /**
     * Removes stale per-game world directories left behind from a previous run
     * (e.g. server crash). Safe to call at plugin enable before any games register.
     */
    public void purgeStaleCopies() {

        File container = plugin.getServer().getWorldContainer();
        File[] children = container.listFiles();
        if (children == null)
            return;

        for (File child : children) {

            if (!child.isDirectory())
                continue;
            String name = child.getName();
            if (!name.startsWith(COPY_PREFIX))
                continue;

            if (SpleggOG.isProtectedMainWorld(name)) {

                plugin.getLogger().warning("Refusing to purge protected main world named '" + name + "'.");
                continue;

            }

            try {

                deleteRecursive(child.toPath());
                plugin.getLogger().info("Purged stale Splegg game world directory: " + name);

            } catch (Exception ex) {

                plugin.getLogger().warning("Failed to purge stale game world '" + name + "': " + ex.getMessage());

            }

        }

    }

    /**
     * Copy template -> game world and load via MyWorlds. The template world must be
     * loaded by the server (so we can flush its chunks to disk before reading
     * them). Returns null on failure; caller must abort game creation.
     */
    public World prepareWorld(Game game) {

        Map map = game.getMap();
        String templateName = map.getWorldName();
        if (templateName == null) {

            plugin.getLogger().warning("Cannot prepare world for game '" + game.getGameId() + "': map '" + map.getName()
                    + "' has no configured world.");
            return null;

        }

        if (SpleggOG.isProtectedMainWorld(templateName)) {

            plugin.getLogger().severe("Refusing to copy protected main world '" + templateName + "' for game '"
                    + game.getGameId() + "'.");
            return null;

        }

        World template = Bukkit.getWorld(templateName);
        if (template == null) {

            plugin.getLogger().warning("Cannot prepare world for game '" + game.getGameId() + "': template world '"
                    + templateName + "' is not loaded.");
            return null;

        }

        String copyName = COPY_PREFIX + game.getGameId() + "-" + templateName;
        if (SpleggOG.isProtectedMainWorld(copyName)) {

            plugin.getLogger()
                    .severe("Refusing to use protected world name '" + copyName + "' for a Splegg game copy.");
            return null;

        }

        // Make sure no stale copy with the same name lingers.
        if (Bukkit.getWorld(copyName) != null && !unloadCopy(copyName)) {

            plugin.getLogger().warning("Could not unload existing world '" + copyName + "' before recreating.");
            return null;

        }

        File container = plugin.getServer().getWorldContainer();
        File templateDir = new File(container, templateName);
        File copyDir = new File(container, copyName);

        if (!templateDir.exists() || !templateDir.isDirectory()) {

            plugin.getLogger().warning("Template world directory missing: " + templateDir.getPath());
            return null;

        }

        try {

            // Flush template chunks so the on-disk dir is consistent before copy.
            template.save();

            if (copyDir.exists())
                deleteRecursive(copyDir.toPath());
            copyRecursive(templateDir.toPath(), copyDir.toPath());

            // Force a fresh world UUID + drop any held session lock so MC treats
            // the copy as a brand new world rather than re-attaching to the
            // template.
            File uid = new File(copyDir, "uid.dat");
            if (uid.exists())
                Files.deleteIfExists(uid.toPath());
            File lock = new File(copyDir, "session.lock");
            if (lock.exists())
                Files.deleteIfExists(lock.toPath());

        } catch (Exception ex) {

            plugin.getLogger()
                    .severe("Failed to copy template '" + templateName + "' -> '" + copyName + "': " + ex.getMessage());
            try {

                if (copyDir.exists())
                    deleteRecursive(copyDir.toPath());

            } catch (Exception ignored) {

            }

            return null;

        }

        WorldConfig wc = WorldConfig.get(copyName);
        World copy;
        try {

            copy = wc.loadWorld();

        } catch (Exception ex) {

            plugin.getLogger().severe("Exception loading game world '" + copyName + "': " + ex.getMessage());
            try {

                deleteRecursive(copyDir.toPath());

            } catch (Exception ignored) {

            }

            return null;

        }

        if (copy == null) {

            plugin.getLogger().severe("MyWorlds returned null when loading game world '" + copyName + "'.");
            return null;

        }

        // Per-game world joins the in-game inventory bundle so players keep the
        // splegg in-game inventory across template -> per-game world teleports.
        // Detach first to clear any stray bundle membership, then merge with
        // the template.
        try {

            WorldInventory.detach(Collections.singletonList(copyName));
            WorldInventory.merge(java.util.Arrays.asList(templateName, copyName));

        } catch (Throwable t) {

            plugin.getLogger().warning(
                    "Failed to attach MyWorlds inventory bundle for game world '" + copyName + "': " + t.getMessage());

        }

        plugin.getLogger().info("Prepared Splegg game world '" + copyName + "' for game " + game.getGameId());
        return copy;

    }

    /**
     * Evict players, unload via MyWorlds, and delete the game world directory.
     * No-op when the game has no world (game never started its world copy).
     */
    public void cleanupWorld(Game game) {

        World gameWorld = game.getGameWorld();
        if (gameWorld == null)
            return;

        String name = gameWorld.getName();
        if (SpleggOG.isProtectedMainWorld(name)) {

            plugin.getLogger().severe("Refusing to clean protected main world '" + name + "'.");
            game.setGameWorld(null);
            return;

        }

        evictPlayers(gameWorld);

        try {

            WorldInventory.detach(Collections.singletonList(name));

        } catch (Throwable ignored) {

        }

        WorldConfig wc = WorldConfig.get(name);
        if (wc.isLoaded() && !wc.unloadWorld())
            plugin.getLogger().warning("Failed to unload Splegg game world '" + name + "' before deletion.");
        boolean deleted = wc.deleteWorld();
        if (!deleted)
            plugin.getLogger()
                    .warning("WorldConfig.deleteWorld() returned false for '" + name + "'; directory may persist.");

        game.setGameWorld(null);

    }

    private boolean unloadCopy(String name) {

        WorldConfig wc = WorldConfig.get(name);
        if (!wc.isLoaded())
            return true;
        World w = Bukkit.getWorld(name);
        if (w != null)
            evictPlayers(w);
        return wc.unloadWorld();

    }

    private static void deleteRecursive(Path root) throws IOException {

        if (!Files.exists(root))
            return;
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                Files.delete(file);
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

                if (exc != null)
                    throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;

            }

        });

    }

    private static void copyRecursive(Path source, Path target) throws IOException {

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                Path rel = source.relativize(dir);
                Path dest = target.resolve(rel);
                if (!Files.exists(dest))
                    Files.createDirectories(dest);
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                Path rel = source.relativize(file);
                Path dest = target.resolve(rel);
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;

            }

        });

    }

    private void evictPlayers(World world) {

        if (world.getPlayers().isEmpty())
            return;

        List<String> mainNames = plugin.getMainWorlds();
        World fallback = null;
        for (String name : mainNames) {

            fallback = Bukkit.getWorld(name);
            if (fallback != null)
                break;

        }

        if (fallback == null && !Bukkit.getWorlds().isEmpty())
            fallback = Bukkit.getWorlds().get(0);
        if (fallback == null) {

            plugin.getLogger().severe("Cannot evict players from '" + world.getName() + "': no fallback world.");
            return;

        }

        Location dest = fallback.getSpawnLocation();
        for (Player p : new ArrayList<>(world.getPlayers()))
            p.teleport(dest);

    }

}
