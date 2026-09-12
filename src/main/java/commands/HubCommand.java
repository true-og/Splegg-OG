package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;
import utils.Utils;

// /hub and /lobby. Leaves the Splegg lobby or match the player is in and sends
// them back where they came from, or to main spawn. HubCommandListener routes
// /hub, /lobby and /spawn here for anyone inside Splegg territory, whichever
// plugin owns the bare label.
public class HubCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("Only players can use this command.");
            return true;

        }

        handle(player);
        return true;

    }

    public static void handle(Player player) {

        final SpleggOG plugin = SpleggOG.getPlugin();
        final UtilPlayer trackedPlayer = plugin.pm.track(player);

        // leaveGame already returns the player to their pre-join spot.
        final Game game = trackedPlayer.getGame();
        if (game != null) {

            game.leaveGame(trackedPlayer);
            return;

        }

        if (plugin.hasPreJoinLocation(player.getUniqueId())) {

            if (!plugin.returnPlayer(player, false)) {

                Utils.spleggOGMessage(player, "&cUnable to return you to your previous location.");
                return;

            }

            Utils.spleggOGMessage(player, "&aReturned to your previous location.");
            return;

        }

        if (plugin.findMainWorld() == null) {

            Utils.spleggOGMessage(player, "&cNo main world is available.");
            return;

        }

        // MyWorlds' main world spawn is what Spawn-OG's /setspawn writes, so this
        // lands on the server spawn.
        if (!plugin.returnPlayer(player, true)) {

            Utils.spleggOGMessage(player, "&cUnable to return you to the hub.");
            return;

        }

        Utils.spleggOGMessage(player, "&aReturned to the hub.");

    }

}
