package main;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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

    // TODO: If a shovel in the Splegg shop is too expensive, close the inventory
    // and tell the user about it.
    private boolean setupEconomy() {

        final RegisteredServiceProvider<DiamondBankAPIJava> diamondBankAPIProvider = getServer().getServicesManager()
                .getRegistration(DiamondBankAPIJava.class);
        if (diamondBankAPIProvider == null) {

            getLogger().severe("DiamondBank-OG API is null");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;

        }

        this.diamondBankAPI = diamondBankAPIProvider.getProvider();

        return true;

    }

    @Override
    public void onEnable() {

        plugin = this;
        this.chat = new Utils();
        if (this.getServer().getPluginManager().getPlugin("WorldEdit") == null) {

            final String noWorldEditError = "\"ERROR: WorldEdit not found! Without WorldEdit, Splegg-OG will not function. Please download it from http://dev.bukkit.org/bukkit-plugins/worldedit\"";
            this.getLogger().severe(noWorldEditError);
            this.getLogger().info(noWorldEditError);

            Bukkit.getPluginManager().disablePlugin(this);

        } else if (this.getServer().getPluginManager().getPlugin("DiamondBank-OG") == null) {

            final String noDiamondBankOGError = "\"ERROR: DiamondBank-OG not found! Without WorldEdit, Splegg-OG will not function. Please download it from http://dev.bukkit.org/bukkit-plugins/worldedit\"";
            this.getLogger().severe(noDiamondBankOGError);
            this.getLogger().info(noDiamondBankOGError);

            Bukkit.getPluginManager().disablePlugin(this);

        } else {

            if (!this.setupEconomy()) {

                // Inform the user that the plugin will not work due to a missing
                // DiamondBank-OG.
                // dependency.
                this.getLogger().severe("[%s] - Disabled due to no DiamomdBank-OG dependency found!"
                        .formatted(this.getPluginMeta().getName()));

                // Disable Splegg-OG for this instance because DiamondBank-OG (a crucial
                // dependency) was
                // not found.
                this.getServer().getPluginManager().disablePlugin(this);

            } else {

                Bukkit.getOnlinePlayers().forEach((Player p) -> {

                    final UtilPlayer u = new UtilPlayer(p);

                    this.pm.PLAYERS.put(p.getName(), u);

                });

            }

            this.maps = new MapUtilities();
            this.games = new GameUtilities();
            this.game = new GameManager();
            this.pm = new Utils();
            this.utils = new Utils();
            this.config = new Utils();

            this.maps.c.setup();
            this.config.setup();

            this.getConfig().options().copyDefaults(true);
            this.saveConfig();

            this.getServer().getPluginManager().registerEvents(new MapListener(), this);
            this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            this.getServer().getPluginManager().registerEvents(new SpleggEvents(), this);
            this.getServer().getPluginManager().registerEvents(new SignListener(), this);
            getServer().getPluginManager().registerEvents(new Listeners(diamondBankAPI), this);
            this.getCommand("splegg").setExecutor(new SpleggCommand());

        }

    }

    @Override
    public void onDisable() {

        this.disabling = true;

        HandlerList.unregisterAll(new MapListener());
        HandlerList.unregisterAll(new PlayerListener());
        HandlerList.unregisterAll(new SpleggEvents());
        HandlerList.unregisterAll(new SignListener());
        HandlerList.unregisterAll(new Listeners(diamondBankAPI));

        int gameCounter = 0;
        try {

            final Iterator<?> gameIterator = this.games.GAMES.values().iterator();
            while (gameIterator.hasNext()) {

                final Game game = (Game) gameIterator.next();
                if (game.getStatus() == Status.INGAME) {

                    gameCounter++;

                    this.game.stopGame(game, 1);

                }

            }

            this.getLogger().info("Splegg-OG Shut Down with " + gameCounter + " games running.");

        } catch (NullPointerException nullPointerException) {

            this.getLogger().info("Splegg-OG Shut Down with 0 games running.");
            nullPointerException.printStackTrace();

        }

    }

    public WorldEditPlugin getWorldEdit() {

        final Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");

        return worldEdit instanceof WorldEditPlugin ? (WorldEditPlugin) worldEdit : null;

    }

    public static SpleggOG getPlugin() {

        // Pass instance of main to other classes.
        return plugin;

    }

}