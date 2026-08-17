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

    // Resolves the map a registered join sign belongs to, or null when the
    // location is not a registered sign. Signs are recognized by location, not
    // by their rendered text, so reformatting a sign cannot orphan it.
    private static String owningMap(Location location) {

        for (config.Map candidate : SpleggOG.getPlugin().maps.getMaps()) {

            if (LobbySignUtils.get().isLobbySign(location, candidate.getName())) {

                return candidate.getName();

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

        final String map = PlainTextComponentSerializer.plainText().serialize(event.line(1)).trim();
        if (map.isEmpty()) {

            Utils.spleggOGMessage(player, "&cLine 2 must be the map name.");
            event.setCancelled(true);
            return;

        }

        if (!SpleggOG.getPlugin().maps.mapExists(map)) {

            Utils.spleggOGMessage(player, "&cMap '" + map + "' does not exist.");
            event.setCancelled(true);
            return;

        }

        LobbySign ls = new LobbySign(SpleggOG.getPlugin().maps.getMap(map), SpleggOG.getPlugin());
        ls.create(event.getBlock().getLocation(), SpleggOG.getPlugin().maps.getMap(map));

        // Provisional lines; the sign updater redraws with live data within a
        // second.
        event.line(0, Component.text("§4Splegg"));
        event.line(1, Component.text("§6" + map));
        event.line(2, Component.text("§7Loading..."));
        event.line(3, Component.text(""));

        Utils.spleggOGMessage(player,
                SpleggOG.getPlugin().getConfig().getString("Messages.CreateSign").replaceAll("%map%", map));

    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {

        if (!e.hasBlock() || e.getAction() != Action.RIGHT_CLICK_BLOCK || !isSign(e.getClickedBlock().getType())) {

            return;

        }

        final String map = owningMap(e.getClickedBlock().getLocation());
        if (map == null) {

            return;

        }

        e.setCancelled(true);

        final Player player = e.getPlayer();
        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u == null) {

            return;

        }

        if (u.getGame() != null) {

            Utils.spleggOGMessage(player, "&cERROR: You are already playing.");
            return;

        }

        config.Map targetMap = SpleggOG.getPlugin().maps.getMap(map);
        if (targetMap == null || !targetMap.isUsable(targetMap)) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Mapnotexist"));
            return;

        }

        managers.Game game = SpleggOG.getPlugin().games.findOrCreateForMap(targetMap);
        if (game != null) {

            game.joinGame(u);

        } else {

            Utils.spleggOGMessage(player,
                    "&cERROR: Failed to start a game for map &e" + map + "&c. Check the server console.");

        }

        player.updateInventory();

    }

    @EventHandler
    public void signBreak(BlockBreakEvent e) {

        if (!isSign(e.getBlock().getType())) {

            return;

        }

        final String owningMap = owningMap(e.getBlock().getLocation());
        if (owningMap == null) {

            return;

        }

        final Player player = e.getPlayer();
        if (player.hasPermission("splegg.admin")) {

            final LobbySign sign = new LobbySign(SpleggOG.getPlugin().maps.getMap(owningMap), SpleggOG.getPlugin());
            sign.delete(e.getBlock().getLocation());

            Utils.spleggOGMessage(player,
                    SpleggOG.getPlugin().getConfig().getString("Messages.RemovedSign").replaceAll("%map%", owningMap));

        } else {

            e.setCancelled(true);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotBreakSign"));

        }

    }

}
