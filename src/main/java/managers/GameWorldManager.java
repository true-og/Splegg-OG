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
import java.util.Locale;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldInventory;

import config.Map;
import main.SpleggOG;

// Owns the per-game world lifecycle for Splegg matches.
// Each game is bound to a copy of its template world; the copy lives at
// <worldContainer>/splegg-<gameId>-<templateName>/ and is deleted when the game
// ends. The original template world remains loaded for admin editing and is
// never modified by gameplay.
public class GameWorldManager {

    public static final String COPY_PREFIX = "splegg-";

    // Per-game world names, splegg-<gameId>-<template>. Safe to purge wholesale
    // before any game registers: every match builds its own copy from scratch.
    private static final Pattern COPY_NAME = Pattern.compile("^" + Pattern.quote(COPY_PREFIX) + ".+$",
            Pattern.CASE_INSENSITIVE);

    private final SpleggOG plugin;

    public GameWorldManager(SpleggOG plugin) {

        this.plugin = plugin;

    }

    // Removes stale per-game world directories left behind from a previous run
    // (e.g. server crash). Safe to call at plugin enable before any games register.
    // Returns how many worlds were removed.
    public int purgeStaleCopies() {

        File container = plugin.getServer().getWorldContainer();
        File[] children = container.listFiles(File::isDirectory);
        if (children == null)
            return 0;

        int purged = 0;
        for (File child : children) {

            String name = child.getName();
            if (!COPY_NAME.matcher(name).matches())
                continue;

            if (SpleggOG.isProtectedMainWorld(name)) {

                plugin.getLogger().warning("Refusing to purge protected main world named '" + name + "'.");
                continue;

            }

            if (deleteStaleCopy(name, child))
                purged++;

        }

        if (purged > 0)
            plugin.getLogger().info("Purged " + purged + " stale Splegg game world(s) from a previous run.");

        return purged;

    }

    // MyWorlds owns the world registration, so ask it to drop the world first and
    // only fall back to deleting the directory when it cannot.
    private boolean deleteStaleCopy(String name, File worldDir) {

        // A reload can leave players standing in a copy no game claims yet.
        // Pulling it out from under them is worse than keeping the directory.
        World live = Bukkit.getWorld(name);
        if (live != null && !live.getPlayers().isEmpty()) {

            plugin.getLogger().warning("Not purging stale game world '" + name + "': it still has players in it.");
            return false;

        }

        WorldConfig worldConfig = WorldConfig.getIfExists(name);
        if (worldConfig != null) {

            if (worldConfig.isLoaded() && !worldConfig.unloadWorld()) {

                plugin.getLogger().warning("Could not unload stale game world '" + name + "'; leaving it in place.");
                return false;

            }

            if (worldConfig.deleteWorld())
                return true;

        }

        try {

            deleteRecursive(worldDir.toPath());
            return true;

        } catch (Exception ex) {

            plugin.getLogger().warning("Failed to purge stale game world '" + name + "': " + ex.getMessage());
            return false;

        }

    }

    // World folder lookup that tolerates the spelling an admin actually typed:
    // exact name first, then case-insensitive, then ignoring separators as well so
    // 'shake-it' still finds 'Shake_It' on a case-sensitive filesystem. Returns
    // null when nothing matches.
    public static File resolveWorldDir(File baseDir, String name) {

        if (baseDir == null || name == null || name.isBlank())
            return null;

        File exact = new File(baseDir, name);
        if (exact.isDirectory())
            return exact;

        File[] candidates = baseDir.listFiles(File::isDirectory);
        if (candidates == null)
            return null;

        for (File candidate : candidates)
            if (candidate.getName().equalsIgnoreCase(name))
                return candidate;

        String flattened = flattenName(name);
        for (File candidate : candidates)
            if (flattenName(candidate.getName()).equals(flattened))
                return candidate;

        return null;

    }

    private static String flattenName(String name) {

        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

    }

    // Lists the world folders that do exist under a directory, for error messages.
    public static String describeWorldDirs(File baseDir) {

        File[] candidates = baseDir == null ? null : baseDir.listFiles(File::isDirectory);
        if (candidates == null || candidates.length == 0)
            return "No world folders found in " + (baseDir == null ? "(unset)" : baseDir.getPath()) + ".";

        StringBuilder sb = new StringBuilder("Available world folders in " + baseDir.getPath() + ": ");
        for (int i = 0; i < candidates.length; i++) {

            if (i > 0)
                sb.append(", ");
            sb.append(candidates[i].getName());

        }

        return sb.append(".").toString();

    }

    // Pins the void chunk generator onto a world about to be loaded. A copied
    // world directory carries no MyWorlds generator setting, so without this the
    // copy generates vanilla terrain outside the saved region even though the
    // template was created with /mw create <world> void.
    private void applyVoidGenerator(WorldConfig wc) {

        if (!plugin.isVoidGeneratorEnabled())
            return;

        // An admin who already picked a generator for this world keeps it; only a
        // world with no generator of its own (every fresh copy) gets the void one.
        String existing = wc.getChunkGeneratorName();
        if (existing != null && !existing.isBlank())
            return;

        wc.setChunkGeneratorName(plugin.getName() + ":void");

    }

    // Loads a configured Splegg world through MyWorlds when the server does not
    // have it loaded already. Returns the live world, or null when it could not be
    // loaded. The returned name is the one the server actually uses, which is the
    // folder name and may differ in case from the configured one.
    public World ensureWorldLoaded(String name) {

        if (name == null || name.isBlank())
            return null;

        if (SpleggOG.isProtectedMainWorld(name)) {

            plugin.getLogger().warning("Refusing to load protected main world '" + name + "' as a Splegg world.");
            return null;

        }

        World existing = Bukkit.getWorld(name);
        if (existing != null)
            return existing;

        File container = plugin.getServer().getWorldContainer();
        File worldDir = resolveWorldDir(container, name);
        if (worldDir == null) {

            plugin.getLogger()
                    .warning("Configured Splegg world '" + name + "' has no world folder in " + container.getPath()
                            + ". Create it with MyWorlds (/mw create " + name + " void) or add it to "
                            + "the MapBase directory.");
            return null;

        }

        if (!worldDir.getName().equals(name))
            plugin.getLogger().info("Configured Splegg world '" + name + "' resolved to folder '" + worldDir.getName()
                    + "'; rename it or fix config.yml to match exactly.");

        WorldConfig wc = WorldConfig.get(worldDir.getName());
        applyVoidGenerator(wc);
        World loaded;
        try {

            loaded = wc.loadWorld();

        } catch (Exception ex) {

            plugin.getLogger().severe("Exception loading Splegg world '" + name + "': " + ex.getMessage());
            return null;

        }

        if (loaded == null) {

            plugin.getLogger().severe("MyWorlds returned null when loading Splegg world '" + name + "'.");
            return null;

        }

        plugin.getLogger().info("Loaded Splegg world '" + loaded.getName() + "' via MyWorlds.");
        return loaded;

    }

    // Copy template -> game world and load via MyWorlds. The template world must
    // be loaded by the server (so its chunks can be flushed to disk before they
    // are read). Returns null on failure; caller must abort game creation.
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

            // Bring the template up rather than refusing the match: a template only
            // has to exist on disk, not be loaded by an admin ahead of time.
            template = ensureWorldLoaded(templateName);

        }

        if (template == null) {

            plugin.getLogger().warning("Cannot prepare world for game '" + game.getGameId() + "': template world '"
                    + templateName + "' is not loaded and could not be loaded.");
            return null;

        }

        // From here on use the name the server knows the world by, which is the
        // folder name and may differ in case from the configured one.
        templateName = template.getName();

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
        File templateDir = resolveWorldDir(container, templateName);
        File copyDir = new File(container, copyName);

        if (templateDir == null) {

            plugin.getLogger().warning("Template world directory missing: "
                    + new File(container, templateName).getPath() + ". " + describeWorldDirs(container));
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
        applyVoidGenerator(wc);
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

    // Evict players, unload via MyWorlds, and delete the game world directory.
    // No-op when the game has no world (game never started its world copy).
    public void cleanupWorld(Game game) {

        World gameWorld = game.getGameWorld();
        if (gameWorld == null)
            return;
        cleanupWorld(gameWorld);
        game.setGameWorld(null);

    }

    public void cleanupWorld(World gameWorld) {

        if (gameWorld == null)
            return;

        String name = gameWorld.getName();
        if (SpleggOG.isProtectedMainWorld(name)) {

            plugin.getLogger().severe("Refusing to clean protected main world '" + name + "'.");
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

    // Refreshes each configured Splegg world from its cold-storage template under
    // MapBase. For every name in the list this evacuates any loaded world at the
    // server root, deletes it, copies <mapBase>/<name>/ into place, sanitizes
    // stale uid.dat / session.lock, then re-loads via MyWorlds. Per-match world
    // copies (splegg-<id>-<name>) are untouched and continue to be sourced from
    // the freshly refreshed live world.
    // Silently no-ops when mapBaseDir is null. Logs warnings (does not throw) for
    // individual world failures so one bad template cannot prevent others from
    // refreshing.
    public void refreshTemplatesFromMapBase(List<String> worlds, File mapBaseDir, String configPath) {

        if (mapBaseDir == null)
            return;
        if (!mapBaseDir.isDirectory()) {

            plugin.getLogger().info("MapBase directory '" + mapBaseDir.getPath()
                    + "' does not exist; skipping template refresh. Create it to enable map-folder workflow.");
            return;

        }

        if (worlds == null || worlds.isEmpty())
            return;

        File container = plugin.getServer().getWorldContainer();
        for (String name : worlds) {

            if (name == null || name.isBlank())
                continue;

            if (SpleggOG.isProtectedMainWorld(name)) {

                plugin.getLogger().warning("Refusing to refresh protected world name '" + name + "' from MapBase.");
                continue;

            }

            File source = resolveWorldDir(mapBaseDir, name);
            if (source == null) {

                plugin.getLogger().warning("MapBase template missing for '" + name + "' from " + configPath + ": "
                        + new File(mapBaseDir, name).getPath() + ". " + describeWorldDirs(mapBaseDir));
                continue;

            }

            File levelDat = new File(source, "level.dat");
            if (!levelDat.isFile()) {

                plugin.getLogger().warning("MapBase template is not a world: " + source.getPath()
                        + " (expected level.dat). Skipping refresh for '" + name + "'.");
                continue;

            }

            File dest = new File(container, name);
            // Source under MapBase must not collide with the destination -- otherwise
            // we would delete the source before copying it back.
            try {

                if (dest.exists() && source.getCanonicalFile().equals(dest.getCanonicalFile())) {

                    plugin.getLogger().warning("MapBase entry '" + name + "' resolves to the server world container"
                            + "; refusing to wipe and re-copy onto itself. Move templates outside the world container.");
                    continue;

                }

            } catch (IOException ex) {

                plugin.getLogger().warning("Could not canonicalize paths for template '" + name + "': "
                        + ex.getMessage() + ". Skipping refresh to be safe.");
                continue;

            }

            if (!forceUnloadIfPresent(name)) {

                plugin.getLogger().warning(
                        "Could not unload existing world '" + name + "' before refresh; skipping to avoid data loss.");
                continue;

            }

            try {

                if (dest.exists())
                    deleteRecursive(dest.toPath());
                copyRecursive(source.toPath(), dest.toPath());
                sanitizeWorldFolder(dest);

            } catch (IOException ex) {

                plugin.getLogger().severe("Failed to refresh template '" + name + "' from MapBase: " + ex.getMessage());
                continue;

            }

            WorldConfig wc = WorldConfig.get(name);
            applyVoidGenerator(wc);
            World loaded;
            try {

                loaded = wc.loadWorld();

            } catch (Exception ex) {

                plugin.getLogger().severe("Exception loading refreshed template '" + name + "': " + ex.getMessage());
                continue;

            }

            if (loaded == null) {

                plugin.getLogger().severe("MyWorlds returned null when loading refreshed template '" + name + "'.");
                continue;

            }

            plugin.getLogger().info("Refreshed " + configPath + " world '" + name + "' from " + source.getPath() + ".");

        }

    }

    private boolean forceUnloadIfPresent(String name) {

        WorldConfig wc = WorldConfig.get(name);
        if (!wc.isLoaded())
            return true;
        World existing = Bukkit.getWorld(name);
        if (existing != null)
            evictPlayers(existing);
        boolean unloaded = wc.unloadWorld();
        if (!unloaded)
            plugin.getLogger().warning("Failed to unload world '" + name + "' before template refresh.");
        return unloaded;

    }

    private static void sanitizeWorldFolder(File worldDir) {

        if (worldDir == null)
            return;
        File uid = new File(worldDir, "uid.dat");
        if (uid.exists()) {

            try {

                Files.deleteIfExists(uid.toPath());

            } catch (IOException ignored) {

            }

        }

        File lock = new File(worldDir, "session.lock");
        if (lock.exists()) {

            try {

                Files.deleteIfExists(lock.toPath());

            } catch (IOException ignored) {

            }

        }

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
