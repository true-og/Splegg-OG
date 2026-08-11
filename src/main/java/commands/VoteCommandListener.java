package commands;

import java.util.Arrays;
import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import main.SpleggOG;
import managers.GameWorldManager;
import utils.UtilPlayer;

// Routes /v and /vote to Splegg map voting inside Splegg games and worlds only.
// Without this, VotingPlugin can own the /vote label and swallow map votes.
public class VoteCommandListener implements Listener {

    private final VoteCommand voteCommand;

    public VoteCommandListener(VoteCommand voteCommand) {

        this.voteCommand = voteCommand;

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {

        final String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {

            return;

        }

        final String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {

            return;

        }

        // Drop any namespace prefix (e.g. votingplugin:v) before matching.
        String label = parts[0].toLowerCase(Locale.ROOT);
        final int colon = label.indexOf(':');
        if (colon >= 0) {

            label = label.substring(colon + 1);

        }

        if (!label.equals("v") && !label.equals("vote")) {

            return;

        }

        final Player player = event.getPlayer();
        if (!isSpleggTerritory(player)) {

            return;

        }

        final String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        voteCommand.onCommand(player, null, label, args);
        event.setCancelled(true);

    }

    // Claims the command for a tracked Splegg player, or for anyone standing in a
    // Splegg lobby, in-game, or per-match world. Defers everywhere else.
    private boolean isSpleggTerritory(Player player) {

        final SpleggOG plugin = SpleggOG.getPlugin();
        if (plugin == null) {

            return false;

        }

        if (plugin.pm != null) {

            final UtilPlayer trackedPlayer = plugin.pm.getPlayer(player);
            if (trackedPlayer != null && trackedPlayer.getGame() != null) {

                return true;

            }

        }

        final String worldName = player.getWorld().getName();
        if (plugin.isSpleggWorld(worldName)) {

            return true;

        }

        final GameWorldManager gameWorldManager = plugin.getGameWorldManager();
        return gameWorldManager != null && !SpleggOG.isProtectedMainWorld(worldName)
                && gameWorldManager.isGameCopyName(worldName);

    }

}
