package commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;
import utils.Utils;

public class HubCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("Only players can use this command.");
            return true;

        }

        UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        if (trackedPlayer == null) {

            trackedPlayer = new UtilPlayer(player);
            SpleggOG.getPlugin().pm.PLAYERS.put(player.getName(), trackedPlayer);

        }

        final Game game = trackedPlayer.getGame();
        if (game != null && trackedPlayer.isAlive()) {

            game.leaveGame(trackedPlayer);
            return true;

        }

        final World mainWorld = findMainWorld();
        if (mainWorld == null) {

            Utils.spleggOGMessage(player, "&cNo main world is available.");
            return true;

        }

        final Location destination = mainWorld.getSpawnLocation();
        if (!player.teleport(destination)) {

            Utils.spleggOGMessage(player, "&cUnable to return you to the hub.");
            return true;

        }

        Utils.spleggOGMessage(player, "&aReturned to the hub.");
        return true;

    }

    private World findMainWorld() {

        for (String worldName : SpleggOG.getPlugin().getMainWorlds()) {

            final World world = Bukkit.getWorld(worldName);
            if (world != null) {

                return world;

            }

        }

        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

    }

}
