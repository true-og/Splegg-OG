package events;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import main.SpleggOG;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import signs.LobbySign;
import signs.LobbySignUtils;
import utils.Utils;
import utils.UtilPlayer;

public class SignListener implements Listener {

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
        Utils.spleggOGMessage(player,
                SpleggOG.getPlugin().getConfig().getString("Messages.CreateSign").replaceAll("%map%", map));

    }

    private static Collection<Material> signMaterials = new ArrayList<>();
    static {

        signMaterials.add(Material.ACACIA_SIGN);
        signMaterials.add(Material.DARK_OAK_SIGN);
        signMaterials.add(Material.OAK_SIGN);
        signMaterials.add(Material.BIRCH_SIGN);
        signMaterials.add(Material.SPRUCE_SIGN);
        signMaterials.add(Material.JUNGLE_SIGN);
        signMaterials.add(Material.CRIMSON_SIGN);
        signMaterials.add(Material.WARPED_SIGN);
        signMaterials.add(Material.ACACIA_WALL_SIGN);
        signMaterials.add(Material.DARK_OAK_WALL_SIGN);
        signMaterials.add(Material.OAK_WALL_SIGN);
        signMaterials.add(Material.BIRCH_WALL_SIGN);
        signMaterials.add(Material.SPRUCE_WALL_SIGN);
        signMaterials.add(Material.JUNGLE_WALL_SIGN);
        signMaterials.add(Material.CRIMSON_WALL_SIGN);
        signMaterials.add(Material.WARPED_WALL_SIGN);

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (e.hasBlock() && signMaterials.contains(e.getClickedBlock().getType())
                && e.getAction() == Action.RIGHT_CLICK_BLOCK)
        {

            Sign s = (Sign) e.getClickedBlock().getState();
            Player player = e.getPlayer();
            final String line0Plain = PlainTextComponentSerializer.plainText().serialize(s.line(0)).trim();
            final String formatHeader = PlainTextComponentSerializer.plainText()
                    .serialize(Utils
                            .legacySerializerAnyCase(SpleggOG.getPlugin().getConfig().getString("Sings.Format.1", "")))
                    .trim();

            if (line0Plain.equalsIgnoreCase(formatHeader)) {

                String map = null;
                for (config.Map candidate : SpleggOG.getPlugin().maps.getMaps()) {

                    if (LobbySignUtils.get().isLobbySign(e.getClickedBlock().getLocation(), candidate.getName())) {

                        map = candidate.getName();
                        break;

                    }

                }

                e.setCancelled(true);

                if (map == null || !SpleggOG.getPlugin().maps.mapExists(map)) {

                    Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Mapnotexist"));
                    return;

                }

                UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
                if (u == null) {

                    return;

                }

                if (u.getGame() != null) {

                    Utils.spleggOGMessage(player, "&cERROR: You are already playing.");
                    return;

                }

                config.Map targetMap = SpleggOG.getPlugin().maps.getMap(map);
                if (!targetMap.isUsable(targetMap)) {

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

        }

    }

    @EventHandler
    public void signBreak(BlockBreakEvent e) {

        Player player = e.getPlayer();
        if (signMaterials.contains(e.getBlock().getType())) {

            String owningMap = null;
            for (config.Map candidate : SpleggOG.getPlugin().maps.getMaps()) {

                if (LobbySignUtils.get().isLobbySign(e.getBlock().getLocation(), candidate.getName())) {

                    owningMap = candidate.getName();
                    break;

                }

            }

            if (owningMap == null) {

                return;

            }

            if (player.hasPermission("splegg.admin")) {

                final LobbySign sign = new LobbySign(SpleggOG.getPlugin().maps.getMap(owningMap), SpleggOG.getPlugin());
                sign.delete(e.getBlock().getLocation());

                Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.RemovedSign")
                        .replaceAll("%map%", owningMap));

            } else {

                e.setCancelled(true);
                Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotBreakSign"));

            }

        }

    }

}
