package events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;

public class PlayerListener implements Listener {

	String[] cmds = new String[]{""};

	@EventHandler
	public void onFood(FoodLevelChangeEvent e) {

		if (e.getEntity() instanceof Player) {

			Player player = (Player) e.getEntity();
			UtilPlayer u = new UtilPlayer(player);
			if (u.getGame() != null && u.isAlive()) {

				e.setCancelled(true);
				e.setFoodLevel(20);

			}

		}

	}

	@EventHandler
	public void entityDamage(EntityDamageEvent e) {

		Entity ent = e.getEntity();
		if (ent instanceof Player) {

			Player player = (Player) ent;
			UtilPlayer u = new UtilPlayer(player);
			if (u.getGame() != null && u.isAlive()) {

				e.setCancelled(true);

			}

		}

	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = new UtilPlayer(player);

		SpleggOG.getPlugin().pm.PLAYERS.put(player.getName(), u);

	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = new UtilPlayer(player);
		if (u.getGame() != null && u.isAlive()) {

			// TODO: Remove dev logger
			SpleggOG.getPlugin().getLogger().info("Player leaving game onQuit: " + u.getGame() + " with map " + u.getGame().getMap().getName());

			Game game = u.getGame();
			game.leaveGame(u);

		}

		SpleggOG.getPlugin().pm.PLAYERS.remove(player.getName());

	}

	@EventHandler
	public void dropItem(PlayerDropItemEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = new UtilPlayer(player);

		if (u.getGame() != null && u.isAlive()) {

			e.setCancelled(true);

		}

	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = new UtilPlayer(player);
		if (u.getGame() != null && u.isAlive() && ! e.getMessage().startsWith("/splegg") && ! player.hasPermission("splegg.admin")) {

			e.setCancelled(true);

			SpleggOG.getPlugin().chat.sendMessage(player, "&6You cannot use that command in &3Splegg&6!");

		}

	}

}