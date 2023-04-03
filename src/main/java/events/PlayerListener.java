package events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.Kits;
import com.earth2me.essentials.User;

import main.SpleggOG;
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

		if (e.getEntity() instanceof Player) {

			Player player = (Player) e.getEntity();
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

		Essentials ess = (Essentials) SpleggOG.getPlugin().getServer().getPluginManager().getPlugin("Essentials");
		User user = new User(player, ess);

		Kits essentialsKitList = new Kits(ess);
		Kit playersKitToRestoreAfterMatch = (Kit) essentialsKitList.getKit(player.getName());
		if(playersKitToRestoreAfterMatch != null) {

			try {

				playersKitToRestoreAfterMatch.expandItems(user);


			}
			catch (Exception error) {

				SpleggOG.getPlugin().getLogger().severe(error.getMessage());

			}

		}
		else {

			SpleggOG.getPlugin().getLogger().severe("ERROR: Kit for player: " + player.getName() + " was not found during the restore attempt!");

		}

	}

	@EventHandler
	public void dropItem(PlayerDropItemEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = new UtilPlayer(player);

		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = new UtilPlayer(player);
		if (u.getGame() != null && u.isAlive() && ! event.getMessage().startsWith("/splegg") && ! player.hasPermission("splegg.admin")) {

			event.setCancelled(true);

			SpleggOG.getPlugin().chat.sendMessage(player, "&6You cannot use that command in &3Splegg&6!");

		}

	}

}