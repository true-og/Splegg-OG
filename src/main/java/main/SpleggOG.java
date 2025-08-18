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
import net.milkbowl.vault.economy.Economy;
import net.trueog.diamondbankog.DiamondBankAPIJava;
import utils.UtilPlayer;
import utils.Utils;

public class SpleggOG extends JavaPlugin {

    DiamondBankAPIJava dbog;

    private static SpleggOG plugin;
    public Economy econ = null;
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

    // TODO: If a shovel in the Splegg shop is too expensive, close the inventory
    // and tell the user about it.
    private boolean setupEconomy() {

        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {

            // ondBankAPI dbog;

            return false;

        } else {

            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager()
                    .getRegistration(Economy.class);
            if (rsp == null) {

                return false;

            } else {

                this.econ = (Economy) rsp.getProvider();

                return this.econ != null;

            }

        }

    }

    public void onEnable() {

        plugin = this;
        this.chat = new Utils();
        if (this.getServer().getPluginManager().getPlugin("WorldEdit") == null) {

            String noWorldEditError = "\"ERROR: WorldEdit not found! Without WorldEdit, Splegg-OG will not function. Please download it from http://dev.bukkit.org/bukkit-plugins/worldedit\"";
            this.getLogger().severe(noWorldEditError);
            this.getLogger().info(noWorldEditError);

            Bukkit.getPluginManager().disablePlugin(this);

        } else {

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
            this.getServer().getPluginManager().registerEvents(new Listeners(), this);

            this.getCommand("splegg").setExecutor(new SpleggCommand());

            if (!this.setupEconomy()) {

                // Inform the user that the plugin will not work due to a missing vault
                // dependency.
                this.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!",
                        this.getPluginMeta().getName()));

                // Disable Splegg-OG for this instance because Vault (a crucial dependency) was
                // not found.
                this.getServer().getPluginManager().disablePlugin(this);

            } else {

                for (Player p : Bukkit.getOnlinePlayers()) {

                    UtilPlayer u = new UtilPlayer(p);

                    this.pm.PLAYERS.put(p.getName(), u);

                }

            }

        }

    }

    public void onDisable() {

        this.disabling = true;

        HandlerList.unregisterAll(new MapListener());
        HandlerList.unregisterAll(new PlayerListener());
        HandlerList.unregisterAll(new SpleggEvents());
        HandlerList.unregisterAll(new SignListener());
        HandlerList.unregisterAll(new Listeners());

        int gameCounter = 0;
        try {

            Iterator<?> gameIterator = this.games.GAMES.values().iterator();
            while (gameIterator.hasNext()) {

                Game game = (Game) gameIterator.next();
                if (game.getStatus() == Status.INGAME) {

                    gameCounter++;

                    this.game.stopGame(game, 1);

                }

            }

            this.getLogger().info("Splegg-OG Shut Down with " + gameCounter + " games running.");

        } catch (NullPointerException error) {

            this.getLogger().info("Splegg-OG Shut Down with 0 games running.");

        }

    }

    public WorldEditPlugin getWorldEdit() {

        Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");

        return worldEdit instanceof WorldEditPlugin ? (WorldEditPlugin) worldEdit : null;

    }

    public static SpleggOG getPlugin() {

        // Pass instance of main to other classes.
        return plugin;

    }

}