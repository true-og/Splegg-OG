package chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.skbotnl.chatog.api.WorldChatFormatter;
import org.bukkit.entity.Player;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import net.trueog.utilitiesog.UtilitiesOG;
import utils.SpleggPlayer;
import utils.UtilPlayer;

// Renders Splegg chat now that Chat-OG owns delivery and the Discord relay.
//
// The game is resolved from the sender rather than from the lobby id Chat-OG passes: a Splegg game
// is a live object keyed by game id, and its worlds are per-match copies, so the player is the only
// reliable handle. The lobby id is still the right thing for Chat-OG to route on, it just is not
// what identifies a Game here.
//
// The name segment mirrors TheHerobrine-OG's formatter: the standard TrueOG
// union bracket tag, display name, and LuckPerms suffix, expanded through
// MiniPlaceholders with the sender as the audience so the same content reaches
// the in-game view and the Discord relay.
public class SpleggChatFormatter implements WorldChatFormatter {

    private static final String NAME_SEGMENT = "<simpleclans_union_bracket_tag><player_display_name><luckperms_suffix> ";

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

        final Component prefix = UtilitiesOG.trueogExpand(buildPrefix(sender, trackedPlayer, game), sender);

        // The caret never inherits bold or colors from the prefix.
        final Component caret = UtilitiesOG.trueogExpand(getCaretColor(sender) + "> &r", sender)
                .decoration(TextDecoration.BOLD, false);

        // The message is already sanitised by Chat-OG, so it is composed with rather
        // than re-parsed.
        return Component.join(JoinConfiguration.noSeparators(), prefix, caret, message);

    }

    private String buildPrefix(Player player, UtilPlayer trackedPlayer, Game game) {

        if (game.getStatus() == Status.LOBBY) {

            final int max = game.getMap() == null ? 0 : game.getMap().getSpawnCount();
            return "&e" + game.getPlayers().size() + "&7/&e" + max + "&8 ▏ " + NAME_SEGMENT;

        }

        if (game.getStatus() != Status.INGAME) {

            return NAME_SEGMENT;

        }

        final SpleggPlayer spleggPlayer = game.getPlayers().get(player.getUniqueId());
        final int broken = spleggPlayer == null ? 0 : spleggPlayer.getBroken();

        if (!trackedPlayer.isAlive()) {

            return "&e" + broken + "&8 ▍ &4OUT &8▏ " + NAME_SEGMENT;

        }

        return "&e" + broken + "&8 ▏ " + NAME_SEGMENT;

    }

    // TODO: Expose PlayerUtils.getMessageColor in Chat-OG as a supported API.
    private String getCaretColor(Player player) {

        try {

            final net.kyori.adventure.text.format.TextColor color = nl.skbotnl.chatog.util.PlayerUtils.INSTANCE
                    .getMessageColor(player.getUniqueId());
            if (color != null) {

                if (color.equals(NamedTextColor.WHITE)) {

                    return "&f";

                } else if (color.equals(NamedTextColor.GRAY)) {

                    return "&7";

                }

            }

        } catch (Throwable ignored) {

            // Chat-OG internals moved or are absent -- fall back to dark gray.

        }

        return "&8";

    }

}
