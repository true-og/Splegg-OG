package events;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import main.SpleggOG;
import net.milkbowl.vault.economy.EconomyResponse;
import utils.UtilPlayer;
import utils.Utils;

public class Listeners implements Listener {

	public static ArrayList manager = new ArrayList();
	public static ArrayList moneymanager = new ArrayList();
	public static ArrayList shopmanager = new ArrayList();
	public static ArrayList diamondspade = new ArrayList();
	public static ArrayList goldspade = new ArrayList();
	public static ArrayList launchEggs = new ArrayList();

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player)event.getWhoClicked();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		ItemStack stack = event.getCurrentItem();
		if (event.getView().getTitle().equals(SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"))) {
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
					player.closeInventory();
					if (SpleggOG.getPlugin().econ.getBalance(player.getName()) >= (double)SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price")) {
						r = SpleggOG.getPlugin().econ.withdrawPlayer(player.getName(), (double)SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price"));
						if (r.transactionSuccess()) {
							goldspade.add(player.getName());
							diamondspade.remove(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());
							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyGoldShovel").replaceAll("&", "�"));
						} else {
							player.sendMessage(String.format("An error occured: %s", r.errorMessage));
							shopmanager.add(player.getName());
							manager.add(player.getName());
							goldspade.remove(player.getName());
							diamondspade.remove(player.getName());
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney").replaceAll("&", "�"));
					}
				}

				if (selectedType == Material.DIAMOND_SHOVEL) {
					player.closeInventory();
					if (SpleggOG.getPlugin().econ.getBalance(player.getName()) >= (double)SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price")) {
						r = SpleggOG.getPlugin().econ.withdrawPlayer(player.getName(), (double)SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price"));
						if (r.transactionSuccess()) {
							goldspade.remove(player.getName());
							diamondspade.add(player.getName());
							shopmanager.remove(player.getName());
							manager.remove(player.getName());
							SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyDiamondShovel").replaceAll("&", "�"));
						} else {
							player.sendMessage(String.format("An error occured: %s", r.errorMessage));
							shopmanager.add(player.getName());
							manager.add(player.getName());
							goldspade.remove(player.getName());
							diamondspade.remove(player.getName());
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney").replaceAll("&", "�"));
					}
				}

			} else {
				SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought").replaceAll("&", "�"));
				player.closeInventory();
			}
		}

		if (u.getGame() != null && u.isAlive()) {
			event.setCancelled(true);
		}

	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack hand = player.getInventory().getItemInMainHand();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (hand.getType() == Material.IRON_SHOVEL && u.getGame() != null && u.isAlive() && launchEggs.contains(player.getName())) {
				player.launchProjectile(Egg.class);
				player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);
			}

			Integer i;
			if (hand.getType() == Material.GOLDEN_SHOVEL && u.getGame() != null && u.isAlive() && launchEggs.contains(player.getName())) {
				for(i = 0; i < 2; i = i + 1) {
					player.launchProjectile(Egg.class);
				}

				player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);
			}

			if (hand.getType() == Material.DIAMOND_SHOVEL && u.getGame() != null && u.isAlive() && launchEggs.contains(player.getName())) {
				for(i = 0; i < 3; i = i + 1) {
					player.launchProjectile(Egg.class);
				}

				player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);
			}

			if (hand.getType() == Material.COMPASS && u.getGame() != null && u.isAlive()) {
				player.openInventory(this.getSpecInventory());
			}

			if (hand.getType() == Material.MAGMA_CREAM && u.getGame() != null && u.isAlive()) {
				u.getGame().leaveGame(u);
			}

			if (hand.getType() == Material.getMaterial(SpleggOG.getPlugin().getConfig().getString("Shop.Item")) && u.getGame() != null && u.isAlive()) {
				player.openInventory(this.getShopInventory());
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
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		if (u.getGame() != null && u.isAlive()) {
			event.setCancelled(true);
		}

	}

	@EventHandler
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
	}

	private Inventory getShopInventory() {
		Inventory shop = Bukkit.createInventory((InventoryHolder) null, SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.Size"), SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"));
		shop.setItem(0, Utils.getItem(Material.GOLDEN_SHOVEL, 0, SpleggOG.getPlugin().getConfig().getString("GUI.Shop.GoldShovel.Name").replaceAll("&", "�"), SpleggOG.getPlugin().getConfig().getString("GUI.Shop.GoldShovel.Description").replaceAll("&", "�").replaceAll("%price%", String.valueOf(SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price")))));
		shop.setItem(1, Utils.getItem(Material.DIAMOND_SHOVEL, 0, SpleggOG.getPlugin().getConfig().getString("GUI.Shop.DiamondShovel.Name").replaceAll("&", "�"), SpleggOG.getPlugin().getConfig().getString("GUI.Shop.DiamondShovel.Description").replaceAll("&", "�").replaceAll("%price%", String.valueOf(SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price")))));
		return shop;
	}

	private Inventory getSpecInventory() {
		Inventory spec = Bukkit.createInventory((InventoryHolder) null, 27, "Splegg - Spectators");
		return spec;
	}
}