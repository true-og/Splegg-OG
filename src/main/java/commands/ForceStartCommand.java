package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import utils.UtilPlayer;
import utils.Utils;

// Starts the lobby countdown regardless of Options.AutoStartPlayers.
public class ForceStartCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("You are unable to use this command.");
            return true;

        }

        if (!player.hasPermission("splegg.admin")) {

            Utils.spleggOGMessage(player, "&cYou do not have permission to use this command.");
            return true;

        }

        final UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        final Game game = trackedPlayer != null ? trackedPlayer.getGame() : null;
        if (game == null) {

            Utils.spleggOGMessage(player, "&cYou must be in a lobby to do this.");
            return true;

        }

        if (game.getStatus() != Status.LOBBY) {

            Utils.spleggOGMessage(player, "&cYou cannot run this command right now.");
            return true;

        }

        if (game.getPlayers().isEmpty()) {

            Utils.spleggOGMessage(player, "&cThere is nobody in the lobby to start a game for.");
            return true;

        }

        int startTime = 0;
        if (args.length > 0) {

            try {

                startTime = Integer.parseInt(args[0]);

            } catch (NumberFormatException error) {

                Utils.spleggOGMessage(player, "&cCorrect Usage: /spforcestart [time]");
                return true;

            }

        }

        final int countdown = game.forceStartCountdown(startTime);
        if (countdown < 0) {

            Utils.spleggOGMessage(player, "&cYou cannot run this command right now.");
            return true;

        }

        SpleggOG.getPlugin().chat.bc("&6The game will start in &b" + countdown + "&6 seconds.", game);
        return true;

    }

}
