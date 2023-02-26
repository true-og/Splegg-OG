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
import org.bukkit.inventory.PlayerInventory;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import main.SpleggOG;
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
		ItemStack stack = event.getCurrentItem();
		if (event.getView().title().toString().equals(SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"))) {

			if (shopmanager.contains(player.getName())) {

				EconomyResponse r;
				Material selectedType;
				try {

					selectedType = stack.getType();

				}
				catch(NullPointerException error) {

					selectedType = null;

				}

				if (selectedType == Material.GOLDEN_SHOVEL) {

					PlayerInventory inventory = player.getInventory();
					inventory.close();

					if (SpleggOG.getPlugin().econ.getBalance(player) >= (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price")) {

						r = SpleggOG.getPlugin().econ.withdrawPlayer(player, (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price"));
						if (r.transactionSuccess()) {

							goldspade.add(player.getName());
							diamondspade.remove(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());

							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyGoldShovel").replaceAll("&", "§"));

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

						inventory.close();

						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney").replaceAll("&", "§"));

					}

				}

				if (selectedType == Material.DIAMOND_SHOVEL) {

					PlayerInventory inventory = player.getInventory();
					inventory.close();

					if (SpleggOG.getPlugin().econ.getBalance(player) >= (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price")) {

						r = SpleggOG.getPlugin().econ.withdrawPlayer(player, (double) SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price"));
						if (r.transactionSuccess()) {

							goldspade.remove(player.getName());
							diamondspade.add(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());

							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyDiamondShovel").replaceAll("&", "§"));

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

						inventory.close();

						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney").replaceAll("&", "§"));

					}

				}

			}
			else {

				PlayerInventory inventory = player.getInventory();
				inventory.close();

				SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought").replaceAll("&", "§"));

			}

		}

		if (u.getGame() != null && u.isAlive()) {

			event.setCancelled(true);

		}

	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		
		if(event.getHand().equals(EquipmentSlot.HAND)) {
			
			Player player = (Player) event.getPlayer();
			if (launchEggs.contains(player.getName())) {

				// Fire an egg.
				Utils.fireEgg(event);

			}
			
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {

		Player player = event.getPlayer();

		manager.remove(player.getName());
		shopmanager.remove(player.getName());
		goldspade.remove(player.getName());
		diamondspade.remove(player.getName());
		launchEggs.remove(player.getName());
		moneymanager.remove(player.getName());

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
	public void onAsyncPlayerChat(AsyncChatEvent event) {

		// TODO: Direct chat to world-specific channel here.

	}

}