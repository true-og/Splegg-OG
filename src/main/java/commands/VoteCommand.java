package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;
import utils.Utils;

public class VoteCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("You are unable to use this command.");
            return true;

        }

        final UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        final Game game = trackedPlayer != null ? trackedPlayer.getGame() : null;
        if (game == null) {

            Utils.spleggOGMessage(player, "&cYou must be in a lobby to do this.");
            return true;

        }

        if (!game.isVotingRunning()) {

            Utils.spleggOGMessage(player, "&cYou cannot run this command right now.");
            return true;

        }

        if (args.length == 0) {

            game.sendVotingMessage(player);
            return true;

        }

        final int map;
        try {

            map = Integer.parseInt(args[0]);

        } catch (Exception error) {

            Utils.spleggOGMessage(player, "&cCorrect Usage: /vote <map number>");
            game.sendVotingMessage(player);
            return true;

        }

        game.vote(player, map);
        return true;

    }

}
