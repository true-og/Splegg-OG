package commands;

import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import main.SpleggOG;
import utils.UtilPlayer;

// Claims /hub, /lobby and /spawn for anyone in a Splegg game or standing in
// Splegg territory. Splegg-OG, TheHerobrine-OG and BuildBattle-OG all declare
// /hub, and Bukkit hands the bare label to whichever registered first, so
// without this a Splegg player's /hub could run another minigame's command and
// teleport them out while Splegg still counts them as playing. /spawn from
// Spawn-OG would do the same. Everywhere else the event is left alone.
public class HubCommandListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {

        final String label = commandLabel(event.getMessage());
        if (!label.equals("hub") && !label.equals("lobby") && !label.equals("spawn")) {

            return;

        }

        final Player player = event.getPlayer();
        if (!isSpleggTerritory(player)) {

            return;

        }

        event.setCancelled(true);
        HubCommand.handle(player);

    }

    // The bare command name in lower case, without the leading slash or a
    // plugin namespace such as splegg-og:hub.
    static String commandLabel(String message) {

        if (message == null || message.length() < 2 || message.charAt(0) != '/') {

            return "";

        }

        final String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0) {

            return "";

        }

        String label = parts[0].toLowerCase(Locale.ROOT);
        final int colon = label.indexOf(':');
        if (colon >= 0) {

            label = label.substring(colon + 1);

        }

        return label;

    }

    private static boolean isSpleggTerritory(Player player) {

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

        return plugin.isSpleggTerritory(player.getWorld().getName());

    }

}
