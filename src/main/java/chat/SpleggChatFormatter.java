package chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.skbotnl.chatog.api.WorldChatFormatter;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

// Renders Splegg chat now that Chat-OG owns delivery and the Discord relay.
//
// The game is resolved from the sender rather than from the lobby id Chat-OG passes: a Splegg game
// is a live object keyed by game id, and its worlds are per-match copies, so the player is the only
// reliable handle. The lobby id is still the right thing for Chat-OG to route on, it just is not
// what identifies a Game here.
public class SpleggChatFormatter implements WorldChatFormatter {

    @Override
    public Component format(Player sender, Component message, String worldName, String lobbyId) {

        final SpleggOG plugin = SpleggOG.getPlugin();
        if (plugin == null || plugin.pm == null) {

            return null;

        }

        final UtilPlayer trackedPlayer = plugin.pm.getPlayer(sender);
        final Game game = trackedPlayer == null ? null : trackedPlayer.getGame();

        // Standing in a Splegg world without being in a game (staff, a stale copy):
        // let Chat-OG use its default format rather than inventing a game prefix.
        if (game == null) {

            return null;

        }

        // The message is already sanitised by Chat-OG, so it is composed with rather
        // than re-parsed.
        return Component.join(JoinConfiguration.noSeparators(),
                Utils.legacySerializerAnyCase(buildPrefix(sender, trackedPlayer, game)), message);

    }

    private String buildPrefix(Player player, UtilPlayer trackedPlayer, Game game) {

        final String name = "&9" + PlainTextComponentSerializer.plainText().serialize(player.displayName()) + "&8 » &r";

        if (game.getStatus() == Status.LOBBY) {

            final int max = game.getMap() == null ? 0 : game.getMap().getSpawnCount();
            return "&e" + game.getPlayers().size() + "&7/&e" + max + "&8 ▏ " + name;

        }

        if (game.getStatus() != Status.INGAME) {

            return name;

        }

        if (!trackedPlayer.isAlive()) {

            return "&8&lOUT &8▏ &7" + PlainTextComponentSerializer.plainText().serialize(player.displayName())
                    + "&8 » &7";

        }

        final SpleggPlayer spleggPlayer = game.getPlayers().get(player.getUniqueId());
        final int broken = spleggPlayer == null ? 0 : spleggPlayer.getBroken();
        return "&e" + broken + "&8 ▏ " + name;

    }

}
