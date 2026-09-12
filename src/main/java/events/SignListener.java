package events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import main.SpleggOG;
import managers.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import signs.LobbySign;
import signs.LobbySignUtils;
import utils.Utils;
import utils.UtilPlayer;

public class SignListener implements Listener {

    private static boolean isSign(Material material) {

        return Tag.SIGNS.isTagged(material) || Tag.WALL_SIGNS.isTagged(material);

    }

    // Resolves the stored key a registered join sign was saved under, or null when
    // the location is not a registered sign. Signs are recognized by location,
    // not by their rendered text, so reformatting a sign cannot orphan it.
    private static String storedKey(Location location) {

        for (String key : SpleggOG.getPlugin().maps.c.getSignKeys()) {

            if (LobbySignUtils.get().isLobbySign(location, key)) {

                return key;

            }

        }

        return null;

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void signPlace(SignChangeEvent event) {

        Player player = event.getPlayer();
        final String header = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim();

        if (!header.equalsIgnoreCase("[Splegg]"))
            return;

        if (!player.hasPermission("splegg.admin")) {

            Utils.spleggOGMessage(player, "&cYou don't have permission to create Splegg signs.");
            event.setCancelled(true);
            return;

        }

        final String target = PlainTextComponentSerializer.plainText().serialize(event.line(1)).trim();
        final String key;
        if (target.isEmpty() || target.equalsIgnoreCase(LobbySign.ANY) || target.equalsIgnoreCase("join")) {

            key = LobbySign.ANY;

        } else {

            final Game game = SpleggOG.getPlugin().games.resolveLobby(target);
            if (game == null) {

                Utils.spleggOGMessage(player, "&cLobby '" + target
                        + "' does not exist. Use a lobby id such as SP1, or leave line 2 blank for any lobby.");
                event.setCancelled(true);
                return;

            }

            key = game.getLobbyId();

        }

        new LobbySign(key, SpleggOG.getPlugin()).create(event.getBlock().getLocation());

        // Provisional lines; the sign updater redraws with live data within a
        // second.
        event.line(0, Component.text("§4Splegg"));
        event.line(1, Component.text("§6" + (LobbySign.ANY.equals(key) ? "Any" : key)));
        event.line(2, Component.text("§7Loading..."));
        event.line(3, Component.text(""));

        Utils.spleggOGMessage(player,
                SpleggOG.getPlugin().getConfig().getString("Messages.CreateSign").replaceAll("%map%", key));

    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {

        if (!e.hasBlock() || e.getAction() != Action.RIGHT_CLICK_BLOCK || !isSign(e.getClickedBlock().getType())) {

            return;

        }

        final String key = storedKey(e.getClickedBlock().getLocation());
        if (key == null) {

            return;

        }

        e.setCancelled(true);

        final Player player = e.getPlayer();
        if (!player.hasPermission("splegg.join")) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoPermission"));
            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u == null) {

            return;

        }

        if (u.getGame() != null) {

            Utils.spleggOGMessage(player, "&cERROR: You are already playing.");
            return;

        }

        final Game game = new LobbySign(key, SpleggOG.getPlugin()).resolveTarget(player);
        if (game == null) {

            Utils.spleggOGMessage(player, "&cNo Splegg lobby is open to join right now.");
            return;

        }

        game.joinGame(u);
        player.updateInventory();

    }

    @EventHandler
    public void signBreak(BlockBreakEvent e) {

        if (!isSign(e.getBlock().getType())) {

            return;

        }

        final String key = storedKey(e.getBlock().getLocation());
        if (key == null) {

            return;

        }

        final Player player = e.getPlayer();
        if (player.hasPermission("splegg.admin")) {

            new LobbySign(key, SpleggOG.getPlugin()).delete(key, e.getBlock().getLocation());

            Utils.spleggOGMessage(player,
                    SpleggOG.getPlugin().getConfig().getString("Messages.RemovedSign").replaceAll("%map%", key));

        } else {

            e.setCancelled(true);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotBreakSign"));

        }

    }

}
