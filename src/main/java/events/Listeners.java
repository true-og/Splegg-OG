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
    public static ArrayList<String> woodspade = new ArrayList<>();
    public static ArrayList<String> stonespade = new ArrayList<>();
    public static ArrayList<String> diamondspade = new ArrayList<>();
    public static ArrayList<String> goldspade = new ArrayList<>();
    public static ArrayList<String> netheritespade = new ArrayList<>();
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
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

        final ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) {

            return;

        }

        final Material clickedMaterial = currentItem.getType();
        if (!isShopShovel(clickedMaterial)) {

            return;

        }

        if (!shopmanager.contains(player.getName())) {

            player.closeInventory();

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.Haveyoueverbought"));

            return;

        }

        switch (clickedMaterial) {

            case WOODEN_SHOVEL -> purchaseShovel(player, "GUI.Shop.WoodShovel", "Messages.BuyWoodShovel", woodspade);
            case STONE_SHOVEL -> purchaseShovel(player, "GUI.Shop.StoneShovel", "Messages.BuyStoneShovel", stonespade);
            case IRON_SHOVEL -> selectIronShovel(player);
            case GOLDEN_SHOVEL -> purchaseShovel(player, "GUI.Shop.GoldShovel", "Messages.BuyGoldShovel", goldspade);
            case DIAMOND_SHOVEL ->
                purchaseShovel(player, "GUI.Shop.DiamondShovel", "Messages.BuyDiamondShovel", diamondspade);
            case NETHERITE_SHOVEL ->
                purchaseShovel(player, "GUI.Shop.NetheriteShovel", "Messages.BuyNetheriteShovel", netheritespade);
            default -> {

                return;

            }

        }

    }

    private boolean isShopShovel(Material material) {

        return switch (material) {

            case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL -> true;
            default -> false;

        };

    }

    private void purchaseShovel(Player player, String configPath, String successMessagePath,
            ArrayList<String> selectedShovelList)
    {

        player.closeInventory();

        final int priceInDiamonds = SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price");
        if (priceInDiamonds <= 0) {

            selectPurchasedShovel(player.getName(), selectedShovelList);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString(successMessagePath));

            return;

        }

        final long priceInShards = diamondBankAPI.diamondsToShards((float) priceInDiamonds);

        try {

            diamondBankAPI.consumeFromPlayer(player.getUniqueId(), priceInShards,
                    "Player " + player.getName() + " purchased a " + currentShovelName(configPath) + " in Splegg.",
                    "Plugin: Splegg-OG");

            selectPurchasedShovel(player.getName(), selectedShovelList);
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString(successMessagePath));

        } catch (InsufficientFundsException insufficientFundsException) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughMoney"));

        } catch (EconomyDisabledException economyDisabledException) {

            restoreDefaultLobbyState(player.getName());
            UtilitiesOG.trueogMessage(player, "&cERROR: The Diamond economy is currently unavailable.");

        } catch (InvalidPlayerException | PlayerNotOnlineException playerException) {

            restoreDefaultLobbyState(player.getName());
            UtilitiesOG.trueogMessage(player,
                    "&cERROR: Your player account could not be found. Contact an administrator.");

        }

    }

    private void selectIronShovel(Player player) {

        player.closeInventory();
        restoreDefaultLobbyState(player.getName());
        Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.BuyIronShovel"));

    }

    private void selectPurchasedShovel(String playerName, ArrayList<String> selectedShovelList) {

        clearSelectedShovels(playerName);
        manager.remove(playerName);
        shopmanager.remove(playerName);
        if (!selectedShovelList.contains(playerName)) {

            selectedShovelList.add(playerName);

        }

    }

    private void clearSelectedShovels(String playerName) {

        woodspade.remove(playerName);
        stonespade.remove(playerName);
        goldspade.remove(playerName);
        diamondspade.remove(playerName);
        netheritespade.remove(playerName);

    }

    private void restoreDefaultLobbyState(String playerName) {

        clearSelectedShovels(playerName);
        if (!manager.contains(playerName)) {

            manager.add(playerName);

        }

        if (!shopmanager.contains(playerName)) {

            shopmanager.add(playerName);

        }

    }

    private String currentShovelName(String configPath) {

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

                    player.openInventory(Utils.getShopInventory());

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

        // Restrict chat to players in the same world type (lobby or in-game).
        final boolean senderInLobby = splegg.isSpleggLobbyWorld(sender.getWorld());
        final boolean senderInGame = splegg.isSpleggInGameWorld(sender.getWorld());

        asyncChatEvent.viewers().removeIf(viewer -> {

            if (!(viewer instanceof Player recipient)) {

                return false;

            }

            if (senderInLobby) {

                return !splegg.isSpleggLobbyWorld(recipient.getWorld());

            }

            if (senderInGame) {

                return !splegg.isSpleggInGameWorld(recipient.getWorld());

            }

            return false;

        });

    }

}
