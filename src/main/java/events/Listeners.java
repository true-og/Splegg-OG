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
import net.trueog.diamondbankog.DiamondBankException.EconomyDisabledException;
import net.trueog.diamondbankog.DiamondBankException.InsufficientFundsException;
import net.trueog.diamondbankog.DiamondBankException.InvalidPlayerException;
import net.trueog.diamondbankog.DiamondBankException.PlayerNotOnlineException;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import net.trueog.utilitiesog.UtilitiesOG;
import utils.UtilPlayer;
import utils.Utils;

public class Listeners implements Listener {

    public static ArrayList<String> manager = new ArrayList<>();
    public static ArrayList<String> moneymanager = new ArrayList<>();
    public static ArrayList<String> shopmanager = new ArrayList<>();
    public static ArrayList<String> diamondspade = new ArrayList<>();
    public static ArrayList<String> goldspade = new ArrayList<>();
    public static ArrayList<String> launchEggs = new ArrayList<>();

    private final DiamondBankAPIJava diamondBankAPI;

    public Listeners(DiamondBankAPIJava diamondBankAPI) {

        this.diamondBankAPI = diamondBankAPI;

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        final Player player = (Player) event.getWhoClicked();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

        if (shopmanager.contains(player.getName())) {

            if (event.getCurrentItem().getType() == Material.GOLDEN_SHOVEL) {

                player.getInventory().close();

                final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.GoldShovel.Price");
                final long priceInShards = diamondBankAPI.diamondsToShards((float) priceInDiamonds);

                try {

                    diamondBankAPI.consumeFromPlayer(player.getUniqueId(), priceInShards,
                            "Player " + player.getName() + " purchased a Gold Shovel in Splegg.", "Plugin: Splegg-OG");

                    goldspade.add(player.getName());
                    diamondspade.remove(player.getName());
                    shopmanager.remove(player.getName());
                    manager.remove(player.getName());

                    Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyGoldShovel"));

                } catch (InsufficientFundsException insufficientFundsException) {

                    player.getInventory().close();

                    Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

                } catch (EconomyDisabledException economyDisabledException) {

                    UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");

                    shopmanager.add(player.getName());
                    manager.add(player.getName());
                    goldspade.remove(player.getName());
                    diamondspade.remove(player.getName());

                } catch (InvalidPlayerException | PlayerNotOnlineException playerException) {

                    UtilitiesOG.trueogMessage(player,
                            "&cERROR: Your player account could not be found. Contact an administrator.");

                    shopmanager.add(player.getName());
                    manager.add(player.getName());
                    goldspade.remove(player.getName());
                    diamondspade.remove(player.getName());

                }

            }

            if (event.getCurrentItem().getType() == Material.DIAMOND_SHOVEL) {

                player.getInventory().close();

                final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.DiamondShovel.Price");
                final long priceInShards = diamondBankAPI.diamondsToShards((float) priceInDiamonds);

                try {

                    diamondBankAPI.consumeFromPlayer(player.getUniqueId(), priceInShards,
                            "Player " + player.getName() + " purchased a Diamond Shovel in Splegg.",
                            "Plugin: Splegg-OG");

                    goldspade.remove(player.getName());
                    diamondspade.add(player.getName());
                    shopmanager.remove(player.getName());
                    manager.remove(player.getName());

                    Utils.spleggOGMessage(player,
                            SpleggOG.getPlugin().getConfig().getString("Messages.BuyDiamondShovel"));

                } catch (InsufficientFundsException insufficientFundsException) {

                    player.getInventory().close();

                    Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

                } catch (EconomyDisabledException economyDisabledException) {

                    UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");

                    shopmanager.add(player.getName());
                    manager.add(player.getName());
                    goldspade.remove(player.getName());
                    diamondspade.remove(player.getName());

                } catch (InvalidPlayerException | PlayerNotOnlineException playerException) {

                    UtilitiesOG.trueogMessage(player,
                            "&cERROR: Your player account could not be found. Contact an administrator.");

                    shopmanager.add(player.getName());
                    manager.add(player.getName());
                    goldspade.remove(player.getName());
                    diamondspade.remove(player.getName());

                }

            }

        } else {

            player.getInventory().close();

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought"));

        }

        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {

        final Player player = playerQuitEvent.getPlayer();
        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        final Game game = u.getGame();

        if (game != null) {

            game.leaveGame(u);

        }

    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent entityPickupItemEvent) {

        if (entityPickupItemEvent.getEntityType() != EntityType.PLAYER) {

            return;

        }

        final Player player = (Player) entityPickupItemEvent.getEntity();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            entityPickupItemEvent.setCancelled(true);

        }

    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementCriterionGrantEvent playerAdvancementCriterionGrantEvent) {

        final Player player = playerAdvancementCriterionGrantEvent.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            playerAdvancementCriterionGrantEvent.setCancelled(true);

        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent playerInteractEvent) {

        final Player player = playerInteractEvent.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        final Game game = u.getGame();
        if (!(game != null && Listeners.launchEggs.contains(u.getName()))) {

            return;

        }

        EquipmentSlot playerHand = null;
        try {

            playerHand = playerInteractEvent.getHand();

        } catch (NullPointerException nullPointerException) {

            SpleggOG.getPlugin().getLogger().severe("ERROR: Player's hand returned null.");
            nullPointerException.printStackTrace();

        }

        if (playerHand != EquipmentSlot.HAND) {

            return;

        }

        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        switch (itemInHand.getType()) {

            case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL ->
                Utils.fireEgg(playerInteractEvent, u, player, itemInHand);
            case SLIME_BALL -> game.leaveGame(u);
            default -> {

                if (itemInHand.getType() == Material
                        .getMaterial(SpleggOG.getPlugin().getConfig().getString("Shop.Item")))
                {

                    player.openInventory(Utils.getShopInventory());

                }

            }

        }

    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncChatEvent asyncChatEvent) {

        // TODO: Direct chat to world-specific channel here.

    }

}
