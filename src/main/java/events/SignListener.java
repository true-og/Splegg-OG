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

public class SignListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void signPlace(SignChangeEvent event) {

        Player player = event.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final String header = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim();
        final String action = PlainTextComponentSerializer.plainText().serialize(event.line(1)).trim();

        if (header.equalsIgnoreCase("[splegg]") && action.equalsIgnoreCase("join")
                && player.hasPermission("splegg.admin"))
        {

            final String map = PlainTextComponentSerializer.plainText().serialize(event.line(2)).trim();
            if (SpleggOG.getPlugin().maps.mapExists(map)) {

                LobbySign ls = new LobbySign(SpleggOG.getPlugin().maps.getMap(map), SpleggOG.getPlugin());
                ls.create(event.getBlock().getLocation(), SpleggOG.getPlugin().maps.getMap(map));

                Utils.spleggOGMessage(player,
                        SpleggOG.getPlugin().getConfig().getString("Messages.CreateSign").replaceAll("%map%", map));

            }

        }

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
            if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

                return;

            }

            final String line0Plain = PlainTextComponentSerializer.plainText().serialize(s.line(0)).trim();
            final String formatHeader = PlainTextComponentSerializer.plainText()
                    .serialize(Utils
                            .legacySerializerAnyCase(SpleggOG.getPlugin().getConfig().getString("Sings.Format.1", "")))
                    .trim();

            if (line0Plain.equalsIgnoreCase(formatHeader)) {

                final String map = PlainTextComponentSerializer.plainText().serialize(s.line(2)).trim();

                if (SpleggOG.getPlugin().maps.mapExists(map)) {

                    player.chat("/splegg join " + map);
                    player.updateInventory();

                    e.setCancelled(true);

                } else {

                    Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Mapnotexist"));

                    e.setCancelled(true);

                }

            }

        }

    }

    @EventHandler
    public void signBreak(BlockBreakEvent e) {

        Player player = e.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

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
