package commands;

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

        final SpleggOG plugin = SpleggOG.getPlugin();
        final UtilPlayer trackedPlayer = plugin.pm.track(player);

        // leaveGame already returns the player to their pre-join spot.
        final Game game = trackedPlayer.getGame();
        if (game != null && trackedPlayer.isAlive()) {

            game.leaveGame(trackedPlayer);
            return true;

        }

        if (plugin.hasPreJoinLocation(player.getUniqueId())) {

            if (!plugin.returnPlayer(player, false)) {

                Utils.spleggOGMessage(player, "&cUnable to return you to your previous location.");
                return true;

            }

            Utils.spleggOGMessage(player, "&aReturned to your previous location.");
            return true;

        }

        if (plugin.findMainWorld() == null) {

            Utils.spleggOGMessage(player, "&cNo main world is available.");
            return true;

        }

        if (!plugin.returnPlayer(player, true)) {

            Utils.spleggOGMessage(player, "&cUnable to return you to the hub.");
            return true;

        }

        Utils.spleggOGMessage(player, "&aReturned to the hub.");
        return true;

    }

}
