package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import events.Listeners;
import main.SpleggOG;
import net.kyori.adventure.text.TextComponent;
import net.trueog.gxui.GUIButton;
import net.trueog.gxui.GUIBase;
import net.trueog.gxui.GUIItem;
import net.trueog.utilitiesog.UtilitiesOG;

public class SpleggShopGUI extends GUIBase {

    public SpleggShopGUI(Player player) {

        super(SpleggOG.getPlugin(), player, SpleggOG.getPlugin().getConfig().getString("GUI.Shop.Title"),
                normalizeSize(SpleggOG.getPlugin().getConfig().getInt("GUI.Shop.Size")), true);

    }

    private static int normalizeSize(int configuredSize) {

        if (configuredSize <= 0) {

            return 9;

        }

        final int clamped = Math.min(54, configuredSize);
        final int remainder = clamped % 9;
        return remainder == 0 ? clamped : clamped + (9 - remainder);

    }

    @Override
    public void setupItems() {

        addShovel(0, Material.WOODEN_SHOVEL, "GUI.Shop.WoodShovel", "Messages.BuyWoodShovel", Listeners.woodspade);
        addShovel(1, Material.STONE_SHOVEL, "GUI.Shop.StoneShovel", "Messages.BuyStoneShovel", Listeners.stonespade);
        addShovel(2, Material.IRON_SHOVEL, "GUI.Shop.IronShovel", "Messages.BuyIronShovel", null);
        addShovel(3, Material.GOLDEN_SHOVEL, "GUI.Shop.GoldShovel", "Messages.BuyGoldShovel", Listeners.goldspade);
        addShovel(4, Material.DIAMOND_SHOVEL, "GUI.Shop.DiamondShovel", "Messages.BuyDiamondShovel",
                Listeners.diamondspade);
        addShovel(5, Material.NETHERITE_SHOVEL, "GUI.Shop.NetheriteShovel", "Messages.BuyNetheriteShovel",
                Listeners.netheritespade);

    }

    private void addShovel(int slot, Material material, String configPath, String successMessagePath,
            Set<UUID> selectedShovelList)
    {

        final Player player = getPlayer();
        final boolean affordable = Listeners.isShovelAffordable(player, configPath);

        final GUIItem item = new GUIItem(material, 1, SpleggOG.getPlugin().getConfig().getString(configPath + ".Name"));
        item.lore(buildLore(player, configPath, affordable));
        item.button(buildButton(player, configPath, successMessagePath, selectedShovelList));

        if (affordable) {

            item.enchantment(Enchantment.DURABILITY, 1);

        }

        addItem(slot, item);

    }

    private GUIButton buildButton(Player player, String configPath, String successMessagePath,
            Set<UUID> selectedShovelList)
    {

        return new GUIButton() {

            @Override
            public boolean leftClick() {

                return handleClick();

            }

            @Override
            public boolean leftClickShift() {

                return handleClick();

            }

            @Override
            public boolean rightClick() {

                return handleClick();

            }

            @Override
            public boolean rightClickShift() {

                return handleClick();

            }

            private boolean handleClick() {

                if (selectedShovelList == null) {

                    return Listeners.handleIronSelection(player);

                }

                return Listeners.handleShovelSelection(player, configPath, successMessagePath, selectedShovelList);

            }

        };

    }

    private List<TextComponent> buildLore(Player player, String configPath, boolean affordable) {

        final List<TextComponent> lore = new ArrayList<>();
        final String description = SpleggOG.getPlugin().getConfig().getString(configPath + ".Description")
                .replaceAll("%price%", "&B" + SpleggOG.getPlugin().getConfig().getInt(configPath + ".Price"));

        lore.add(UtilitiesOG.trueogExpand(description));
        lore.add(UtilitiesOG.trueogExpand(affordable ? "&aAffordable right now." : "&cUnaffordable right now."));

        if (!Listeners.canPurchasePremiumShovel(player.getUniqueId())) {

            lore.add(UtilitiesOG.trueogExpand("&6Premium shovel already chosen for this round."));

        }

        return lore;

    }

}
