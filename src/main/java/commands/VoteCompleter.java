package commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;

public class VoteCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player player)) {

            return null;

        }

        final UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        final Game game = trackedPlayer != null ? trackedPlayer.getGame() : null;
        if (game == null) {

            return null;

        }

        final List<String> completions = new ArrayList<>();
        game.getVotingMaps().keySet().forEach(id -> completions.add(id.toString()));
        return completions;

    }

}
