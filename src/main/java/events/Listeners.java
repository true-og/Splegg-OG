package events;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

import gui.SpleggShopGUI;
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

    public static final Set<UUID> manager = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> moneymanager = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> shopmanager = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> woodspade = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> stonespade = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> diamondspade = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> goldspade = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> netheritespade = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> launchEggs = ConcurrentHashMap.newKeySet();

    public Listeners(DiamondBankAPIJava diamondBankAPI) {

        // Constructor retained for compatibility with existing registration call sites.

    }

    public static void clearAll() {

        manager.clear();
        moneymanager.clear();
        shopmanager.clear();
        woodspade.clear();
        stonespade.clear();
        goldspade.clear();
        diamondspade.clear();
        netheritespade.clear();
        launchEggs.clear();

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        final Player player = (Player) event.getWhoClicked();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    public static boolean handleShovelSelection(Player player, String configPath, String successMessagePath,
            Set<UUID> selectedShovelList)
    {

        final UUID playerId = player.getUniqueId();
        if (!shopmanager.contains(playerId)) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought"));

            return false;

        }

        final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price");
        if (priceInDiamonds <= 0) {

            selectPurchasedShovel(playerId, selectedShovelList);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString(successMessagePath));
            player.closeInventory();

            return true;

        }

        if (!isShovelAffordable(player, configPath)) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

            return false;

        }

        final DiamondBankAPIJava diamondBankAPI = SpleggOG.getPlugin().getDiamondBankAPI();
        if (diamondBankAPI == null) {

            UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");

            return false;

        }

        final long priceInShards = diamondBankAPI.diamondsToShards((float) priceInDiamonds);

        try {

            diamondBankAPI.consumeFromPlayer(playerId, priceInShards,
                    "Player " + player.getName() + " purchased a " + currentShovelName(configPath) + " in Splegg.",
                    "Plugin: Splegg-OG");

            selectPurchasedShovel(playerId, selectedShovelList);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString(successMessagePath));
            player.closeInventory();
            return true;

        } catch (InsufficientFundsException insufficientFundsException) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));
            return false;

        } catch (EconomyDisabledException economyDisabledException) {

            restoreDefaultLobbyState(playerId);
            UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");
            return false;

        } catch (InvalidPlayerException | PlayerNotOnlineException playerException) {

            restoreDefaultLobbyState(playerId);
            UtilitiesOG.trueogMessage(player,
                    "&cERROR: Your player account could not be found. Contact an administrator.");
            return false;

        }

    }

    public static boolean handleIronSelection(Player player) {

        restoreDefaultLobbyState(player.getUniqueId());
        Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyIronShovel"));
        player.closeInventory();

        return true;

    }

    public static boolean isShovelAffordable(Player player, String configPath) {

        final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price");
        if (priceInDiamonds <= 0) {

            return true;

        }

        final DiamondBankAPIJava diamondBankAPI = SpleggOG.getPlugin().getDiamondBankAPI();
        if (diamondBankAPI == null) {

            return false;

        }

        try {

            final long priceInShards = diamondBankAPI.diamondsToShards((float) priceInDiamonds);
            final long availableShards = diamondBankAPI.getBankShards(player.getUniqueId())
                    + diamondBankAPI.getInventoryShards(player.getUniqueId());

            return availableShards >= priceInShards;

        } catch (EconomyDisabledException economyDisabledException) {

            return false;

        }

    }

    public static boolean canPurchasePremiumShovel(UUID playerId) {

        return shopmanager.contains(playerId);

    }

    public static void openShop(Player player) {

        new SpleggShopGUI(player).open(true);

    }

    private static void selectPurchasedShovel(UUID playerId, Set<UUID> selectedShovelList) {

        clearSelectedShovels(playerId);
        manager.remove(playerId);
        shopmanager.remove(playerId);
        selectedShovelList.add(playerId);

    }

    private static void clearSelectedShovels(UUID playerId) {

        woodspade.remove(playerId);
        stonespade.remove(playerId);
        goldspade.remove(playerId);
        diamondspade.remove(playerId);
        netheritespade.remove(playerId);

    }

    private static void restoreDefaultLobbyState(UUID playerId) {

        clearSelectedShovels(playerId);
        manager.add(playerId);
        shopmanager.add(playerId);

    }

    private static String currentShovelName(String configPath) {

        return SpleggOG.getPlugin().getConfig().getString(configPath + ".Name").replaceAll("&[0-9A-FK-ORa-fk-or]", "");

    }

    public static Material getSelectedShovelMaterial(UUID playerId) {

        if (woodspade.contains(playerId)) {

            return Material.WOODEN_SHOVEL;

        }

        if (stonespade.contains(playerId)) {

            return Material.STONE_SHOVEL;

        }

        if (goldspade.contains(playerId)) {

            return Material.GOLDEN_SHOVEL;

        }

        if (diamondspade.contains(playerId)) {

            return Material.DIAMOND_SHOVEL;

        }

        if (netheritespade.contains(playerId)) {

            return Material.NETHERITE_SHOVEL;

        }

        return Material.IRON_SHOVEL;

    }

    public static String getSelectedShovelConfigPath(UUID playerId) {

        if (woodspade.contains(playerId)) {

            return "Shovels.Wood";

        }

        if (stonespade.contains(playerId)) {

            return "Shovels.Stone";

        }

        if (goldspade.contains(playerId)) {

            return "Shovels.Gold";

        }

        if (diamondspade.contains(playerId)) {

            return "Shovels.Diamond";

        }

        if (netheritespade.contains(playerId)) {

            return "Shovels.Netherite";

        }

        return "Shovels.Iron";

    }

    public static void finalizePreGameShovelState(UUID playerId) {

        manager.remove(playerId);
        shopmanager.remove(playerId);
        clearSelectedShovels(playerId);
        moneymanager.add(playerId);

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
        if (!(game != null && Listeners.launchEggs.contains(player.getUniqueId()))) {

            return;

        }

        final EquipmentSlot playerHand = playerInteractEvent.getHand();
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

                    openShop(player);

                }

            }

        }

    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncChatEvent asyncChatEvent) {

        final Player sender = asyncChatEvent.getPlayer();
        final SpleggOG splegg = SpleggOG.getPlugin();

        if (!splegg.isSpleggWorld(sender.getWorld())) {

            // Not in a Splegg world, don't filter.
            return;

        }

        final UtilPlayer senderState = splegg.pm.getPlayer(sender);
        final Game senderGame = senderState != null ? senderState.getGame() : null;
        final String senderWorldName = sender.getWorld().getName();

        asyncChatEvent.viewers().removeIf(viewer -> {

            if (!(viewer instanceof Player recipient)) {

                return false;

            }

            if (senderGame != null) {

                final UtilPlayer recipientState = splegg.pm.getPlayer(recipient);
                return recipientState == null || recipientState.getGame() != senderGame;

            }

            return !recipient.getWorld().getName().equals(senderWorldName);

        });

    }

}
