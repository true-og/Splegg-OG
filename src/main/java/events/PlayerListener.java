package events;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import main.SpleggOG;
import managers.Game;
import utils.UtilPlayer;
import utils.Utils;

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
		Game game = u.getGame();

		SpleggOG.getPlugin().pm.PLAYERS.remove(player.getName());
		player.getInventory().clear();
		// TODO: Restore pre-game inventory here.

		if (game != null) {

			game.leaveGame(game.getPlayers());

		}
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

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {

		boolean keepGoing = true;
		EquipmentSlot playerHand = null;
		try {

			playerHand = event.getHand();

		}
		catch(NullPointerException error) {

			keepGoing = false;

		}

		if(keepGoing) {

			if(playerHand.equals(EquipmentSlot.HAND)) {

				Player player = (Player) event.getPlayer();
				ItemStack itemInHand = player.getInventory().getItemInMainHand();
				UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
				Game game = u.getGame();
				switch(itemInHand.getType()) {

				case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL:
					Utils.fireEgg(event, u, player, itemInHand);
				break;
				case SLIME_BALL:
					if (game != null) {

						game.leaveGame(game.getPlayers());

					}
					break;
				default:
					if(game != null && itemInHand.getType() == Material.getMaterial(SpleggOG.getPlugin().getConfig().getString("Shop.Item"))) {

						player.openInventory(Utils.getShopInventory());

					}
					break;

				}

			}

		}

	}

}