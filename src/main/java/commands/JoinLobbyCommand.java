package commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import config.Map;
import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;
import utils.Utils;

// Joins a Splegg lobby by its id (SP1). Maps are not accepted; with no argument
// the player is put into the best open lobby, creating one when none is up.
public class JoinLobbyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("You are unable to use this command.");
            return true;

        }

        if (!player.hasPermission("splegg.join")) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoPermission"));
            return true;

        }

        final UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        if (trackedPlayer == null) {

            Utils.spleggOGMessage(player, "&cERROR: You are not tracked by Splegg yet. Try again in a moment.");
            return true;

        }

        if (trackedPlayer.getGame() != null) {

            Utils.spleggOGMessage(player, "&cERROR: You are already playing.");
            return true;

        }

        if (args.length == 0) {

            joinBestLobby(player, trackedPlayer);
            return true;

        }

        final Game game = SpleggOG.getPlugin().games.resolveLobby(args[0]);
        if (game == null) {

            Utils.spleggOGMessage(player, "&c" + args[0] + " does not exist.");
            sendLobbyList(player);
            return true;

        }

        game.joinGame(trackedPlayer);
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

    // No lobby was named, so reuse an open one before spinning a new map up.
    private void joinBestLobby(Player player, UtilPlayer trackedPlayer) {

        final Map chosen = SpleggOG.getPlugin().maps.getRandomMap();
        if (chosen == null) {

            Utils.spleggOGMessage(player, "&cERROR: No playable maps are currently available.");
            return;

        }

        final Game game = SpleggOG.getPlugin().games.findOrCreateForMap(chosen);
        if (game == null) {

            Utils.spleggOGMessage(player, "&cERROR: Failed to start a game on &e" + chosen.getName() + "&c.");
            return;

        }

        Utils.spleggOGMessage(player, "&aJoining lobby &e" + SpleggOG.getPlugin().games.getLobbyId(game) + "&a.");
        game.joinGame(trackedPlayer);

    }

    private void sendLobbyList(Player player) {

        final List<String> ids = SpleggOG.getPlugin().games.getLobbyIds();
        if (ids.isEmpty()) {

            Utils.spleggOGMessage(player, "&6No lobbies are open. Use &e/spjoin&6 to open one.");
            return;

        }

        Utils.spleggOGMessage(player, "&6Join a lobby with /spjoin <lobby>.");
        for (String id : ids) {

            final Game game = SpleggOG.getPlugin().games.resolveLobby(id);
            if (game == null || game.getMap() == null) {

                continue;

            }

            Utils.spleggOGMessage(player, "&6&l" + id + " &6- &b" + game.getMap().getName() + " &6(&b"
                    + game.getPlayers().size() + "&6/&b" + game.getMap().getSpawnCount() + "&6)");

        }

    }

}
