package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldInventory;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import nl.skbotnl.chatog.api.ChatOGAPI;
import stats.SpleggPlaceholders;
import stats.SpleggStats;

import chat.SpleggChatFormatter;
import commands.ForceStartCommand;
import commands.HubCommand;
import commands.HubCommandListener;
import commands.JoinLobbyCommand;
import commands.SpleggCommand;
import commands.VoteCommand;
import commands.VoteCommandListener;
import config.MapUtilities;
import events.Listeners;
import events.MapListener;
import events.PlayerListener;
import events.PreJoinLocationListener;
import events.SignListener;
import events.SpleggEvents;
import managers.Game;
import managers.GameManager;
import managers.GameUtilities;
import managers.GameWorldManager;
import managers.LobbyScoreboard;
import managers.Status;
import managers.VoidChunkGenerator;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import signs.JoinSignUpdater;
import utils.PreJoinLocationStore;
import utils.ScoreboardOGBridge;
import utils.Utils;

public class SpleggOG extends JavaPlugin {

    // Vanilla overworld dimension worlds. Splegg refuses to read, write, or
    // configure these under any circumstance: no map can be created in them, no
    // inventory group is built for them, no listener treats them as splegg
    // territory. Their inventory grouping is left to the server admin / MyWorlds
    // config. Match casing on disk is irrelevant -- the comparison is
    // case-insensitive so admins cannot accidentally bypass the guard by
    // renaming the world directory.
    public static final Set<String> PROTECTED_MAIN_WORLDS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("world", "world_nether", "world_the_end")));

    // Plugin names MyWorlds is published under, in the order they are tried.
    private static final String[] MY_WORLDS_PLUGIN_NAMES = { "MyWorlds", "My_Worlds" };

    private static SpleggOG plugin;
    public Utils chat;
    public MapUtilities maps;
    public GameUtilities games;
    public GameManager game;
    public Utils pm;
    public Utils utils;
    public Utils config;
    public SpleggStats stats;
    public boolean updateOut = false;
    public String newVer = "";
    public boolean disabling = false;
    boolean economy = true;
    private DiamondBankAPIJava diamondBankAPI;
    private static MyWorlds myWorlds;
    private GameWorldManager gameWorldManager;
    private PreJoinLocationStore preJoinLocations;

    // TODO: If a shovel in the Splegg shop is too expensive, close the inventory
    // and tell the user about it.

    @Override
    public void onEnable() {

        plugin = this;
        this.chat = new Utils();
        if (this.getServer().getPluginManager().getPlugin("WorldEdit") == null) {

            final String noWorldEditError = "\"ERROR: WorldEdit not found! Without WorldEdit, Splegg-OG will not function. Please download it from http://dev.bukkit.org/bukkit-plugins/worldedit\"";

            this.getLogger().severe(noWorldEditError);
            this.getLogger().info(noWorldEditError);

            Bukkit.getPluginManager().disablePlugin(this);

        } else if (findMyWorldsPlugin() == null) {

            final String noMyWorldsError = "ERROR: MyWorlds not found! Without MyWorlds, Splegg-OG will not function.";

            this.getLogger().severe(noMyWorldsError);

            Bukkit.getPluginManager().disablePlugin(this);

        } else {

            final RegisteredServiceProvider<DiamondBankAPIJava> provider = getServer().getServicesManager()
                    .getRegistration(DiamondBankAPIJava.class);

            if (provider == null) {

                getLogger().severe("DiamondBank-OG API is null – disabling Splegg.");

                Bukkit.getPluginManager().disablePlugin(this);

                return;

            } else {

                diamondBankAPI = provider.getProvider();

            }

            // Initialize TrueOG APIs.
            myWorlds = findMyWorldsPlugin();

            this.maps = new MapUtilities();
            this.games = new GameUtilities();
            this.game = new GameManager();
            this.gameWorldManager = new GameWorldManager(this);
            this.pm = new Utils();
            this.utils = new Utils();
            this.config = new Utils();

            Bukkit.getOnlinePlayers().forEach(this.pm::track);

            this.maps.c.setup();
            this.config.setup();
            // Maps are known now, so the purge only touches copies of real templates.
            this.gameWorldManager.purgeStaleCopies();
            this.preJoinLocations = new PreJoinLocationStore(this);

            this.getConfig().options().copyDefaults(true);
            this.saveConfig();

            // Points persist across restarts and feed the sp_score and sp_rank
            // placeholders.
            this.stats = new SpleggStats(this);
            this.stats.load();
            this.stats.startAutosave();
            SpleggPlaceholders.register();

            // World provisioning runs on the first tick rather than here: loading a
            // world pumps the chunk system, and the resulting ChunkLoadEvent reaches
            // plugins that have not finished their own startup yet.
            this.getServer().getScheduler().runTask(this, this::provisionWorlds);

            this.getServer().getPluginManager().registerEvents(new MapListener(), this);
            this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            this.getServer().getPluginManager().registerEvents(new PreJoinLocationListener(this), this);
            this.getServer().getPluginManager().registerEvents(new SpleggEvents(), this);
            this.getServer().getPluginManager().registerEvents(new SignListener(), this);
            getServer().getPluginManager().registerEvents(new Listeners(diamondBankAPI), this);
            final SpleggCommand spleggCommand = new SpleggCommand();
            this.getCommand("splegg").setExecutor(spleggCommand);
            this.getCommand("splegg").setTabCompleter(spleggCommand);
            this.getCommand("hub").setExecutor(new HubCommand());
            this.getCommand("spforcestart").setExecutor(new ForceStartCommand());
            final JoinLobbyCommand joinLobbyCommand = new JoinLobbyCommand();
            this.getCommand("spjoin").setExecutor(joinLobbyCommand);
            this.getCommand("spjoin").setTabCompleter(joinLobbyCommand);
            // /vote and /v are claimed inside Splegg territory only, so VotingPlugin keeps
            // the labels everywhere else; nothing is registered in plugin.yml for them.
            this.getServer().getPluginManager().registerEvents(new VoteCommandListener(new VoteCommand()), this);
            // /hub, /lobby and /spawn are claimed inside Splegg territory the same way, so
            // another minigame's /hub can never pull a Splegg player out of a match.
            this.getServer().getPluginManager().registerEvents(new HubCommandListener(), this);
            this.registerChatFormatter();

            // Redraw join signs once a second, TheHerobrine-OG style. First run
            // waits a second so world provisioning has kicked off.
            new JoinSignUpdater(this).runTaskTimer(this, 20L, 20L);

        }

    }

    @Override
    public void onDisable() {

        this.disabling = true;

        // JavaPlugin auto-unregisters our listeners; no manual unregister needed.

        int gameCounter = 0;
        if (this.games != null) {

            for (Game game : new ArrayList<>(this.games.GAMES.values())) {

                if (game.getStatus() == Status.DISABLED)
                    continue;
                if (game.getPlayers().isEmpty() && game.getStatus() != Status.INGAME && game.getGameWorld() == null)
                    continue;
                gameCounter++;
                this.game.stopGame(game, 1);

            }

            this.games.clear();

        }

        Listeners.clearAll();
        LobbyScoreboard.detachAll();
        ScoreboardOGBridge.releaseAll();

        if (this.stats != null) {

            this.stats.shutdown();
            this.stats = null;

        }

        // Last write wins: anyone the teardown could not move keeps their spot on
        // disk so the next boot can still return them.
        if (this.preJoinLocations != null) {

            this.preJoinLocations.shutdown();

        }

        this.getLogger().info("Splegg-OG shut down; " + gameCounter + " lobbies were active.");

    }

    public WorldEditPlugin getWorldEdit() {

        final Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");

        return worldEdit instanceof WorldEditPlugin ? (WorldEditPlugin) worldEdit : null;

    }

    public static SpleggOG getPlugin() {

        // Pass instance of main to other classes.
        return plugin;

    }

    // Refresh templates from cold storage, load the configured worlds, then wire
    // the inventory groups onto the worlds that actually came up. Runs one tick
    // after enable so no world is created while other plugins are still starting.
    private void provisionWorlds() {

        if (this.disabling || !this.isEnabled())
            return;

        // Templates are refreshed before the inventory groups are wired, so the
        // bundle is attached to the freshly loaded copies, not stale leftovers.
        refreshTemplatesFromMapBase();
        configureMyWorlds();
        createLobbies();
        claimShortAlias();
        JoinSignUpdater.redrawNow(this);

    }

    // One lobby per configured hub world: SP1-hub becomes lobby SP1. The hub has
    // to be loaded, so this runs after configureMyWorlds brought the worlds up.
    private void createLobbies() {

        for (String name : getLobbyWorlds()) {

            if (name == null || name.isBlank() || isProtectedMainWorld(name)) {

                continue;

            }

            final World hub = this.gameWorldManager.ensureWorldLoaded(name);
            if (hub == null) {

                this.getLogger().warning("No lobby was created for '" + name + "': its hub world is not loaded.");
                continue;

            }

            this.games.createLobby(hub.getName());

        }

        if (this.games.all().isEmpty()) {

            this.getLogger().warning(
                    "No Splegg lobbies exist. List hub worlds named <GamePrefix><number>-hub under Worlds.Lobby.");

        }

    }

    // /sp is declared as an alias of /splegg, but another plugin that registers
    // first (WorldGuard, loaded ahead of us through Utilities-OG) keeps the bare
    // label and Splegg only gets splegg-og:sp. Once every plugin has enabled, point
    // the bare label at /splegg so /sp join 1 reaches Splegg.
    private void claimShortAlias() {

        final PluginCommand splegg = this.getCommand("splegg");
        if (splegg == null) {

            return;

        }

        final java.util.Map<String, Command> known = Bukkit.getCommandMap().getKnownCommands();
        final Command current = known.get("sp");
        if (current == splegg) {

            return;

        }

        known.put("sp", splegg);
        this.getLogger()
                .info("Claimed /sp for Splegg" + (current == null ? "."
                        : " (it was " + current.getName() + " from another plugin; that command stays reachable as /"
                                + current.getLabel() + " through its own namespace)."));

        // Anyone already online has the old command tree; nobody is online on a
        // normal boot, this covers /reload.
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);

    }

    private void configureMyWorlds() {

        // Required for any per-world inventory isolation to take effect at all
        // (lobby vs. in-game vs. SMP). Splegg does not, however, manage the SMP
        // ("Main") inventory group itself -- vanilla overworld/nether/end stay
        // entirely under MyWorlds admin control. See PROTECTED_MAIN_WORLDS.
        myWorlds.setUseWorldInventories(true);

        // Create inventory group for lobby worlds.
        final List<String> lobbyWorlds = sanitizeForGroup(getLobbyWorlds(), "Worlds.Lobby");
        if (!lobbyWorlds.isEmpty()) {

            createInventoryGroup(lobbyWorlds, "lobby");

        }

        // Create inventory group for in-game worlds.
        final List<String> inGameWorlds = sanitizeForGroup(getInGameWorlds(), "Worlds.InGame");
        if (!inGameWorlds.isEmpty()) {

            createInventoryGroup(inGameWorlds, "in-game");

        }

    }

    // Strip protected SMP worlds from a configured list and load whatever is not
    // loaded yet before handing it to MyWorlds. An admin pasting "world" into
    // Worlds.Lobby would otherwise cause splegg to claim ownership of the
    // overworld -- exactly what PROTECTED_MAIN_WORLDS exists to prevent.
    private List<String> sanitizeForGroup(List<String> worlds, String configPath) {

        final List<String> filtered = new ArrayList<>();
        for (String name : worlds) {

            if (name == null || name.isBlank()) {

                continue;

            }

            if (isProtectedMainWorld(name)) {

                this.getLogger().warning("Refusing to add protected SMP world '" + name + "' from " + configPath
                        + " to a Splegg inventory group. Remove it from config.yml.");
                continue;

            }

            // Splegg owns its worlds: bring the world up itself instead of asking an
            // admin to run /mw load, and leave it out of the group when that fails.
            final World loaded = this.gameWorldManager == null ? null : this.gameWorldManager.ensureWorldLoaded(name);
            if (loaded == null) {

                this.getLogger().warning("Configured Splegg world '" + name + "' from " + configPath
                        + " could not be loaded; leaving it out of the inventory group.");
                continue;

            }

            // Group by the name the server uses, not the spelling in config.yml.
            filtered.add(loaded.getName());

        }

        return filtered;

    }

    private void createInventoryGroup(List<String> worlds, String groupType) {

        // Detach first: a Splegg world that is still a member of the server's main
        // bundle would otherwise share the SMP inventory with everyone in it.
        try {

            WorldInventory.detach(worlds);

        } catch (Throwable t) {

            this.getLogger().warning("Failed to detach " + groupType + " worlds from their MyWorlds inventory bundle: "
                    + t.getMessage());

        }

        final WorldInventory inventory = WorldInventory.create(worlds.get(0));
        for (int i = 1; i < worlds.size(); i++) {

            inventory.add(worlds.get(i));

        }

        this.getLogger().info("Created MyWorlds inventory group for " + groupType + " worlds: " + worlds);

    }

    // SMP/main-network worlds that splegg must never touch. Exposed for
    // teleport-back helpers (e.g. returning a player to overworld spawn after
    // leaving a match); not derived from config so a typo in config.yml cannot
    // unprotect them.
    public List<String> getMainWorlds() {

        return new ArrayList<>(PROTECTED_MAIN_WORLDS);

    }

    // Chat-OG routes a world to a game by the letter prefix of its name, which is
    // the
    // same prefix Splegg builds per-game worlds from, so one registration covers
    // both
    // the configured lobby worlds and every match copy.
    private void registerChatFormatter() {

        if (this.getServer().getPluginManager().getPlugin("Chat-OG") == null) {

            this.getLogger().info("Chat-OG is not installed, so Splegg chat keeps its default formatting.");
            return;

        }

        final String prefix = this.getGameWorldPrefix();
        if (prefix == null || prefix.isEmpty()) {

            this.getLogger().warning("Worlds.GamePrefix is empty, so no Splegg chat formatter was registered.");
            return;

        }

        if (!ChatOGAPI.setFormatter(prefix, new SpleggChatFormatter())) {

            this.getLogger().warning("Chat-OG was not ready, so no chat formatter was registered for " + prefix + ".");

        }

        this.warnAboutUnroutedWorlds(prefix);

    }

    // Chat-OG only routes a world whose name is <letters><digits>-<rest>, so a
    // lobby
    // world named anything else silently stays in global chat while Splegg still
    // treats it as its own territory.
    //
    // Only Worlds.Lobby is checked. Worlds.InGame holds match templates that are
    // copied per game, and prepareWorld names each copy
    // <prefix><gameId>-<template>,
    // which routes regardless of what the template is called. Nobody queues or
    // plays
    // in a template, so warning about one would be a false alarm -- and renaming it
    // to satisfy the pattern would only make the copies read SP1-SP1-Loss.
    private void warnAboutUnroutedWorlds(String prefix) {

        final List<String> unrouted = new ArrayList<>();
        for (String worldName : this.getLobbyWorlds()) {

            if (!isProtectedMainWorld(worldName) && !isChatOGRoutable(worldName)) {

                unrouted.add(worldName);

            }

        }

        if (unrouted.isEmpty()) {

            return;

        }

        this.getLogger()
                .warning("These Splegg lobby worlds are not named <" + prefix
                        + "><number>-<name>, so Chat-OG leaves them in global chat while Splegg still treats them as "
                        + "its own: " + String.join(", ", unrouted) + ".");

    }

    // Mirrors Chat-OG's own parse: letters, then at least one digit, then '-',
    // then a non-empty remainder.
    private static boolean isChatOGRoutable(String worldName) {

        if (worldName == null) {

            return false;

        }

        int index = 0;
        while (index < worldName.length() && Character.isLetter(worldName.charAt(index))) {

            index++;

        }

        if (index == 0) {

            return false;

        }

        final int digitStart = index;
        while (index < worldName.length() && Character.isDigit(worldName.charAt(index))) {

            index++;

        }

        if (index == digitStart || index >= worldName.length() || worldName.charAt(index) != '-') {

            return false;

        }

        return index + 1 < worldName.length();

    }

    public List<String> getLobbyWorlds() {

        return this.getConfig().getStringList("Worlds.Lobby");

    }

    public List<String> getInGameWorlds() {

        return this.getConfig().getStringList("Worlds.InGame");

    }

    // Per-game worlds are named <prefix><gameId>-<map> so Chat-OG can link them to
    // a Discord channel.
    public String getGameWorldPrefix() {

        return this.getConfig().getString("Worlds.GamePrefix", "SP");

    }

    // Returns the configured MapBase directory containing cold-storage world
    // templates, or null when the feature is opt-out (empty/missing config).
    // Relative paths resolve against the server's world container (which equals
    // the server root for default Bukkit/Purpur installs).
    public File getMapBaseDir() {

        final String raw = this.getConfig().getString("Worlds.MapBase", "");
        if (raw == null || raw.isBlank())
            return null;
        final File asPath = new File(raw);
        if (asPath.isAbsolute())
            return asPath;
        return new File(this.getServer().getWorldContainer(), raw);

    }

    // Lobby worlds are refreshed alongside in-game worlds: both are disposable
    // copies of a template, and nothing a match writes into them is meant to last.
    private void refreshTemplatesFromMapBase() {

        final File mapBaseDir = getMapBaseDir();
        if (mapBaseDir == null)
            return;

        if (this.gameWorldManager == null) {

            this.getLogger().warning("Template refresh skipped: GameWorldManager not initialized yet.");
            return;

        }

        this.gameWorldManager.refreshTemplatesFromMapBase(getLobbyWorlds(), mapBaseDir, "Worlds.Lobby");
        this.gameWorldManager.refreshTemplatesFromMapBase(getInGameWorlds(), mapBaseDir, "Worlds.InGame");

    }

    // Whether copied worlds get the bundled void chunk generator pinned onto them.
    public boolean isVoidGeneratorEnabled() {

        return this.getConfig().getBoolean("Worlds.VoidGenerator", true);

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {

        // MyWorlds passes the chunk-generator id set on the WorldConfig.
        // Anything (or "void") yields the void generator.
        return new VoidChunkGenerator();

    }

    public List<String> getEnabledSpleggWorlds() {

        final List<String> all = new ArrayList<>();
        for (String name : getLobbyWorlds()) {

            if (!isProtectedMainWorld(name)) {

                all.add(name);

            }

        }

        for (String name : getInGameWorlds()) {

            if (!isProtectedMainWorld(name)) {

                all.add(name);

            }

        }

        return all;

    }

    public static boolean isProtectedMainWorld(String worldName) {

        if (worldName == null) {

            return false;

        }

        return PROTECTED_MAIN_WORLDS.contains(worldName.toLowerCase(Locale.ROOT));

    }

    public static boolean isProtectedMainWorld(World world) {

        return world != null && isProtectedMainWorld(world.getName());

    }

    public boolean isMainWorld(String worldName) {

        return isProtectedMainWorld(worldName);

    }

    public boolean isMainWorld(World world) {

        return isProtectedMainWorld(world);

    }

    public boolean isSpleggWorld(String worldName) {

        if (worldName == null || isProtectedMainWorld(worldName)) {

            return false;

        }

        if (getEnabledSpleggWorlds().contains(worldName))
            return true;

        // Per-game world copies are ephemeral and not in config; ask the live
        // game registry. Guard against early-boot calls before games exists.
        return this.games != null && this.games.isActiveGameWorld(worldName);

    }

    public GameWorldManager getGameWorldManager() {

        return this.gameWorldManager;

    }

    public boolean isSpleggWorld(World world) {

        return world != null && this.isSpleggWorld(world.getName());

    }

    public boolean isSpleggWorld(Location location) {

        return location != null && this.isSpleggWorld(location.getWorld());

    }

    public boolean isSpleggLobbyWorld(String worldName) {

        if (worldName == null || isProtectedMainWorld(worldName)) {

            return false;

        }

        for (String lobbyWorld : getLobbyWorlds()) {

            if (lobbyWorld != null && lobbyWorld.equalsIgnoreCase(worldName)) {

                return true;

            }

        }

        return false;

    }

    public boolean isSpleggLobbyWorld(World world) {

        return world != null && this.isSpleggLobbyWorld(world.getName());

    }

    public boolean isSpleggInGameWorld(String worldName) {

        if (worldName == null || isProtectedMainWorld(worldName)) {

            return false;

        }

        return getInGameWorlds().contains(worldName);

    }

    public boolean isSpleggInGameWorld(World world) {

        return world != null && this.isSpleggInGameWorld(world.getName());

    }

    // Every world Splegg owns: configured lobby and template worlds plus per-match
    // copies.
    public boolean isSpleggTerritory(String worldName) {

        if (worldName == null || isProtectedMainWorld(worldName))
            return false;

        if (isSpleggWorld(worldName))
            return true;

        return this.gameWorldManager != null && this.gameWorldManager.isGameCopyName(worldName);

    }

    // Records the spot a player came from. Only a world outside Splegg territory
    // is stored, so hops between Splegg worlds keep it.
    public void savePreJoinLocation(UUID playerId, Location location) {

        if (this.preJoinLocations == null || location == null || location.getWorld() == null)
            return;
        if (isSpleggTerritory(location.getWorld().getName()))
            return;
        this.preJoinLocations.put(playerId, location);

    }

    public boolean hasPreJoinLocation(UUID playerId) {

        return this.preJoinLocations != null && this.preJoinLocations.has(playerId)
                && !isSpleggTerritory(this.preJoinLocations.getWorldName(playerId));

    }

    // Resolves the stored spot, loading its world through MyWorlds if needed.
    public Location resolvePreJoinLocation(UUID playerId) {

        if (!hasPreJoinLocation(playerId))
            return null;
        return this.preJoinLocations.getOrLoadWorld(playerId);

    }

    public void removePreJoinLocation(UUID playerId) {

        if (this.preJoinLocations != null)
            this.preJoinLocations.remove(playerId);

    }

    // Returns a player to their pre-join spot, or main spawn when fallbackToSpawn
    // is set and nothing is recorded. The spot is dropped once a teleport lands.
    public boolean returnPlayer(Player player, boolean fallbackToSpawn) {

        if (player == null || !player.isOnline())
            return false;

        if (player.isDead())
            player.spigot().respawn();

        final Location saved = resolvePreJoinLocation(player.getUniqueId());
        if (saved != null) {

            if (!player.teleport(saved))
                return false;

            removePreJoinLocation(player.getUniqueId());
            return true;

        }

        if (!fallbackToSpawn)
            return false;

        final World mainWorld = findMainWorld();
        return mainWorld != null && player.teleport(mainWorld.getSpawnLocation());

    }

    // MyWorlds' main world first, then the protected list, then whatever Bukkit
    // loaded first.
    public World findMainWorld() {

        World mainWorld = MyWorlds.getMainWorld();
        if (mainWorld != null)
            return mainWorld;

        for (String worldName : getMainWorlds()) {

            mainWorld = Bukkit.getWorld(worldName);
            if (mainWorld != null)
                return mainWorld;

        }

        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

    }

    public DiamondBankAPIJava getDiamondBankAPI() {

        return diamondBankAPI;

    }

    // Getter for MyWorlds API.
    public static MyWorlds getMyWorlds() {

        return myWorlds;

    }

    // MyWorlds also ships under the fork name My_Worlds, so both are accepted.
    private MyWorlds findMyWorldsPlugin() {

        for (String pluginName : MY_WORLDS_PLUGIN_NAMES) {

            final Plugin candidate = this.getServer().getPluginManager().getPlugin(pluginName);
            if (candidate == null || !candidate.isEnabled())
                continue;

            if (candidate instanceof MyWorlds)
                return (MyWorlds) candidate;

            this.getLogger()
                    .severe("Detected '" + pluginName + "' but it is not a MyWorlds implementation Splegg can use.");
            return null;

        }

        return null;

    }

}
