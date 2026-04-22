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

    public static ArrayList<String> manager = new ArrayList<>();
    public static ArrayList<String> moneymanager = new ArrayList<>();
    public static ArrayList<String> shopmanager = new ArrayList<>();
    public static ArrayList<String> woodspade = new ArrayList<>();
    public static ArrayList<String> stonespade = new ArrayList<>();
    public static ArrayList<String> diamondspade = new ArrayList<>();
    public static ArrayList<String> goldspade = new ArrayList<>();
    public static ArrayList<String> netheritespade = new ArrayList<>();
    public static ArrayList<String> launchEggs = new ArrayList<>();

    public Listeners(DiamondBankAPIJava diamondBankAPI) {

        // Constructor retained for compatibility with existing registration call sites.

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
            ArrayList<String> selectedShovelList)
    {

        if (!shopmanager.contains(player.getName())) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought"));

            return false;

        }

        final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price");
        if (priceInDiamonds <= 0) {

            selectPurchasedShovel(player.getName(), selectedShovelList);
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

            diamondBankAPI.consumeFromPlayer(player.getUniqueId(), priceInShards,
                    "Player " + player.getName() + " purchased a " + currentShovelName(configPath) + " in Splegg.",
                    "Plugin: Splegg-OG");

            selectPurchasedShovel(player.getName(), selectedShovelList);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString(successMessagePath));
            player.closeInventory();
            return true;

        } catch (InsufficientFundsException insufficientFundsException) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));
            return false;

        } catch (EconomyDisabledException economyDisabledException) {

            restoreDefaultLobbyState(player.getName());
            UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");
            return false;

        } catch (InvalidPlayerException | PlayerNotOnlineException playerException) {

            restoreDefaultLobbyState(player.getName());
            UtilitiesOG.trueogMessage(player,
                    "&cERROR: Your player account could not be found. Contact an administrator.");
            return false;

        }

    }

    public static boolean handleIronSelection(Player player) {

        restoreDefaultLobbyState(player.getName());
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

    public static boolean canPurchasePremiumShovel(String playerName) {

        return shopmanager.contains(playerName);

    }

    public static void openShop(Player player) {

        new SpleggShopGUI(player).open(true);

    }

    private static void selectPurchasedShovel(String playerName, ArrayList<String> selectedShovelList) {

        clearSelectedShovels(playerName);
        manager.remove(playerName);
        shopmanager.remove(playerName);
        if (!selectedShovelList.contains(playerName)) {

            selectedShovelList.add(playerName);

        }

    }

    private static void clearSelectedShovels(String playerName) {

        woodspade.remove(playerName);
        stonespade.remove(playerName);
        goldspade.remove(playerName);
        diamondspade.remove(playerName);
        netheritespade.remove(playerName);

    }

    private static void restoreDefaultLobbyState(String playerName) {

        clearSelectedShovels(playerName);
        if (!manager.contains(playerName)) {

            manager.add(playerName);

        }

        if (!shopmanager.contains(playerName)) {

            shopmanager.add(playerName);

        }

    }

    private static String currentShovelName(String configPath) {

        return SpleggOG.getPlugin().getConfig().getString(configPath + ".Name").replaceAll("&[0-9A-FK-ORa-fk-or]", "");

    }

    public static Material getSelectedShovelMaterial(String playerName) {

        if (woodspade.contains(playerName)) {

            return Material.WOODEN_SHOVEL;

        }

        if (stonespade.contains(playerName)) {

            return Material.STONE_SHOVEL;

        }

        if (goldspade.contains(playerName)) {

            return Material.GOLDEN_SHOVEL;

        }

        if (diamondspade.contains(playerName)) {

            return Material.DIAMOND_SHOVEL;

        }

        if (netheritespade.contains(playerName)) {

            return Material.NETHERITE_SHOVEL;

        }

        return Material.IRON_SHOVEL;

    }

    public static String getSelectedShovelConfigPath(String playerName) {

        if (woodspade.contains(playerName)) {

            return "Shovels.Wood";

        }

        if (stonespade.contains(playerName)) {

            return "Shovels.Stone";

        }

        if (goldspade.contains(playerName)) {

            return "Shovels.Gold";

        }

        if (diamondspade.contains(playerName)) {

            return "Shovels.Diamond";

        }

        if (netheritespade.contains(playerName)) {

            return "Shovels.Netherite";

        }

        return "Shovels.Iron";

    }

    public static void finalizePreGameShovelState(String playerName) {

        manager.remove(playerName);
        shopmanager.remove(playerName);
        clearStaticSelectedShovels(playerName);
        if (!moneymanager.contains(playerName)) {

            moneymanager.add(playerName);

        }

    }

    private static void clearStaticSelectedShovels(String playerName) {

        woodspade.remove(playerName);
        stonespade.remove(playerName);
        goldspade.remove(playerName);
        diamondspade.remove(playerName);
        netheritespade.remove(playerName);

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
