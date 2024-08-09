package events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

import main.SpleggOG;
import utils.UtilPlayer;

public class MapListener implements Listener {

	@EventHandler
	public void blockBreak(BlockBreakEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void blockPlace(BlockPlaceEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void hangingEntityBreak(HangingBreakByEntityEvent event) {

		Player player = (Player) event.getRemover();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

}