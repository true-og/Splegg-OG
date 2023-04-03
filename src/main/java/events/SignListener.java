package events;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.ChatColor;
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
import signs.LobbySign;
import signs.LobbySignUtils;

public class SignListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void signPlace(SignChangeEvent e) {

		Player player = e.getPlayer();
		if (e.line(0).toString().equalsIgnoreCase("[splegg]") && e.line(1).toString().equalsIgnoreCase("join") && player.hasPermission("splegg.admin")) {

			String map = e.line(2).toString();
			if (SpleggOG.getPlugin().maps.mapExists(map)) {

				LobbySign ls = new LobbySign(SpleggOG.getPlugin().maps.getMap(map), SpleggOG.getPlugin());
				ls.create(e.getBlock().getLocation(), SpleggOG.getPlugin().maps.getMap(map));

				SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.CreateSign").replaceAll("%map%", map));

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

		if (e.hasBlock() && signMaterials.contains(e.getClickedBlock().getType()) && e.getAction() == Action.RIGHT_CLICK_BLOCK) {

			Sign s = (Sign)e.getClickedBlock().getState();
			Player player = e.getPlayer();
			if (s.line(0).toString().equalsIgnoreCase(SpleggOG.getPlugin().getConfig().getString("Sings.Format.1"))) {

				String map = ChatColor.stripColor(s.line(2).toString());

				if (SpleggOG.getPlugin().maps.mapExists(map)) {

					player.chat("/splegg join " + map);
					player.updateInventory();

					e.setCancelled(true);

				}
				else {

					SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Mapnotexist"));

					e.setCancelled(true);

				}

			}

		}

	}

	@EventHandler
	public void signBreak(BlockBreakEvent e) {

		Player player = e.getPlayer();
		if(signMaterials.contains(e.getBlock().getType())) {

			Sign s = (Sign) e.getBlock().getState();
			String[] lines = (String[]) s.lines().toArray();
			String map = ChatColor.stripColor(lines[1]);
			if (LobbySignUtils.get().isLobbySign(e.getBlock().getLocation(), map)) {

				if (player.hasPermission("splegg.admin")) {

					LobbySign sign = new LobbySign(SpleggOG.getPlugin().maps.getMap(map), SpleggOG.getPlugin());
					sign.delete(e.getBlock().getLocation());

					SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.RemovedSign").replaceAll("%map%", map));

				}
				else {

					e.setCancelled(true);

					SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotBreakSign"));

				}

			}

		}

	}

}