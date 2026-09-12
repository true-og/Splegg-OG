package commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import main.SpleggOG;
import utils.UtilPlayer;

// /spjoin [lobby]: the same as /splegg join. Accepts SP1, sp1 or a bare 1; with
// no argument the lobby list is shown.
public class JoinLobbyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("You are unable to use this command.");
            return true;

        }

        final UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.track(player);
        SpleggCommand.join(player, trackedPlayer, args.length > 0 ? args[0] : null);
        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length != 1) {

            return null;

        }

        final String prefix = args[0].toLowerCase(Locale.ROOT);
        final List<String> completions = new ArrayList<>();
        for (String id : SpleggOG.getPlugin().games.getLobbyIds()) {

            if (id.toLowerCase(Locale.ROOT).startsWith(prefix)) {

                completions.add(id);

            }

        }

        return completions;

    }

}
