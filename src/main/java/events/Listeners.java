package events;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import main.SpleggOG;
import managers.Game;
import net.milkbowl.vault.economy.EconomyResponse;
import utils.UtilPlayer;
import utils.Utils;

public class Listeners implements Listener {

	public static ArrayList<String> manager = new ArrayList<String>();
	public static ArrayList<String> moneymanager = new ArrayList<String>();
	public static ArrayList<String> shopmanager = new ArrayList<String>();
	public static ArrayList<String> diamondspade = new ArrayList<String>();
	public static ArrayList<String> goldspade = new ArrayList<String>();
	public static ArrayList<String> launchEggs = new ArrayList<String>();

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {

		Player player = (Player) event.getWhoClicked();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (event.getView().title().toString().equals(SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"))) {

			if (shopmanager.contains(player.getName())) {

				EconomyResponse r;

				if (event.getCurrentItem().getType() == Material.GOLDEN_SHOVEL) {

					player.getInventory().close();

					if (SpleggOG.getPlugin().econ.getBalance(player) >= (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price")) {

						r = SpleggOG.getPlugin().econ.withdrawPlayer(player, (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price"));
						if (r.transactionSuccess()) {

							goldspade.add(player.getName());
							diamondspade.remove(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());

							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyGoldShovel"));

						}
						else {

							player.sendMessage(String.format("An error occured: %s", r.errorMessage));

							shopmanager.add(player.getName());
							manager.add(player.getName());
							goldspade.remove(player.getName());
							diamondspade.remove(player.getName());

						}

					}
					else {

						player.getInventory().close();

						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

					}

				}

				if (event.getCurrentItem().getType() == Material.DIAMOND_SHOVEL) {

					player.getInventory().close();

					if (SpleggOG.getPlugin().econ.getBalance(player) >= (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price")) {

						r = SpleggOG.getPlugin().econ.withdrawPlayer(player, (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price"));
						if (r.transactionSuccess()) {

							goldspade.remove(player.getName());
							diamondspade.add(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());

							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyDiamondShovel"));

						}
						else {

							player.sendMessage(String.format("An error occured: %s", r.errorMessage));

							shopmanager.add(player.getName());
							manager.add(player.getName());
							goldspade.remove(player.getName());
							diamondspade.remove(player.getName());

						}

					}
					else {

						player.getInventory().close();

						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

					}

				}

			}
			else {

				player.getInventory().close();

				SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought"));

			}

		}

		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		Game game = u.getGame();

		if(game != null) { 

			game.leaveGame(u);

		}

	}

	@EventHandler
	public void onPlayerPickupItem(EntityPickupItemEvent event) {

		if(event.getEntityType() == EntityType.PLAYER) {

			Player player = (Player) event.getEntity();
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
			if (u.getGame() != null && u.isAlive()) {

				event.setCancelled(true);

			}

		}

	}

	@EventHandler
	public void onPlayerAdvancement(PlayerAdvancementCriterionGrantEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if(u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {

		Player player = (Player) event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		Game game = u.getGame();
		if (game != null && Listeners.launchEggs.contains(u.getName())) {

			EquipmentSlot playerHand = null;
			try {

				playerHand = event.getHand();

			}
			catch(NullPointerException error) {

				SpleggOG.getPlugin().getLogger().severe("ERROR: Player's hand returned null.");

			}

			if(playerHand.equals(EquipmentSlot.HAND)) {

				ItemStack itemInHand = player.getInventory().getItemInMainHand();
				switch(itemInHand.getType()) {

				case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL:
					Utils.fireEgg(event, u, player, itemInHand);
				break;
				case SLIME_BALL:

					game.leaveGame(u);

					break;
				default:

					if(itemInHand.getType() == Material.getMaterial(SpleggOG.getPlugin().getConfig().getString("Shop.Item"))) {

						player.openInventory(Utils.getShopInventory());

					}

					break;
				}

			}

		}

	}

	@EventHandler
	public void onAsyncPlayerChat(AsyncChatEvent event) {

		// TODO: Direct chat to world-specific channel here.

	}

}