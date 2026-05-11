package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldInventory;
import com.earth2me.essentials.Essentials;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import commands.SpleggCommand;
import config.MapUtilities;
import events.Listeners;
import events.MapListener;
import events.PlayerListener;
import events.SignListener;
import events.SpleggEvents;
import managers.Game;
import managers.GameManager;
import managers.GameUtilities;
import managers.Status;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import utils.UtilPlayer;
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

    private static SpleggOG plugin;
    public Utils chat;
    public MapUtilities maps;
    public GameUtilities games;
    public GameManager game;
    public Utils pm;
    public Utils utils;
    public Utils config;
    public boolean updateOut = false;
    public String newVer = "";
    public boolean disabling = false;
    boolean economy = true;
    private DiamondBankAPIJava diamondBankAPI;
    private static Essentials essentials;
    private static MyWorlds myWorlds;

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

        } else if (this.getServer().getPluginManager().getPlugin("Essentials-OG") == null) {

            final String noEssentialsOGError = "\"ERROR: Essentials-OG not found! Splegg-OG requires Essentials-OG for inventory handling and will now disable.\"";

            this.getLogger().severe(noEssentialsOGError);
            this.getLogger().info(noEssentialsOGError);

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
            essentials = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials-OG");
            myWorlds = findMyWorldsPlugin();

            this.maps = new MapUtilities();
            this.games = new GameUtilities();
            this.game = new GameManager();
            this.pm = new Utils();
            this.utils = new Utils();
            this.config = new Utils();

            Bukkit.getOnlinePlayers().forEach((Player p) -> {

                final UtilPlayer u = new UtilPlayer(p);

                this.pm.PLAYERS.put(p.getName(), u);

            });

            this.maps.c.setup();
            this.config.setup();

            this.getConfig().options().copyDefaults(true);
            this.saveConfig();

            // Configure MyWorlds inventory isolation for Splegg worlds.
            configureMyWorlds();

            this.getServer().getPluginManager().registerEvents(new MapListener(), this);
            this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            this.getServer().getPluginManager().registerEvents(new SpleggEvents(), this);
            this.getServer().getPluginManager().registerEvents(new SignListener(), this);
            getServer().getPluginManager().registerEvents(new Listeners(diamondBankAPI), this);
            final SpleggCommand spleggCommand = new SpleggCommand();
            this.getCommand("splegg").setExecutor(spleggCommand);
            this.getCommand("splegg").setTabCompleter(spleggCommand);

        }

    }

    @Override
    public void onDisable() {

        this.disabling = true;

        // JavaPlugin auto-unregisters our listeners; no manual unregister needed.

        int gameCounter = 0;
        if (this.games != null) {

            for (Game game : this.games.GAMES.values()) {

                if (game.getStatus() == Status.INGAME) {

                    gameCounter++;

                    this.game.stopGame(game, 1);

                }

            }

        }

        Listeners.clearAll();

        this.getLogger().info("Splegg-OG Shut Down with " + gameCounter + " games running.");

    }

    public WorldEditPlugin getWorldEdit() {

        final Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");

        return worldEdit instanceof WorldEditPlugin ? (WorldEditPlugin) worldEdit : null;

    }

    public static SpleggOG getPlugin() {

        // Pass instance of main to other classes.
        return plugin;

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

    // Strip protected SMP worlds and unloaded worlds from a configured list
    // before handing it to MyWorlds. An admin pasting "world" into
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

            if (Bukkit.getWorld(name) == null) {

                this.getLogger().warning("Configured Splegg world '" + name + "' from " + configPath
                        + " is not loaded. Load it via MyWorlds (/mw load " + name + ") before starting matches.");

            }

            filtered.add(name);

        }

        return filtered;

    }

    private void createInventoryGroup(List<String> worlds, String groupType) {

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

    public List<String> getLobbyWorlds() {

        return this.getConfig().getStringList("Worlds.Lobby");

    }

    public List<String> getInGameWorlds() {

        return this.getConfig().getStringList("Worlds.InGame");

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

        return getEnabledSpleggWorlds().contains(worldName);

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

        return getLobbyWorlds().contains(worldName);

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

    // Getter for Essentials-OG API.
    public static Essentials getEssentials() {

        return essentials;

    }

    public DiamondBankAPIJava getDiamondBankAPI() {

        return diamondBankAPI;

    }

    // Getter for MyWorlds API.
    public static MyWorlds getMyWorlds() {

        return myWorlds;

    }

    private MyWorlds findMyWorldsPlugin() {

        final Plugin plugin = this.getServer().getPluginManager().getPlugin("MyWorlds");
        return plugin instanceof MyWorlds ? (MyWorlds) plugin : null;

    }

}
