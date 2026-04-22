package events;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import main.SpleggOG;
import utils.UtilPlayer;
import utils.Utils;

public class PlayerListener implements Listener {

    String[] cmds = new String[] { "" };

    private UtilPlayer getTrackedPlayer(Player player) {

        UtilPlayer trackedPlayer = SpleggOG.getPlugin().pm.getPlayer(player);
        if (trackedPlayer == null) {

            trackedPlayer = new UtilPlayer(player);
            SpleggOG.getPlugin().pm.PLAYERS.put(player.getName(), trackedPlayer);

        }

        return trackedPlayer;

    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {

        if (!(event.getEntity() instanceof Player)) {

            return;

        }

        final Player player = (Player) event.getEntity();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = getTrackedPlayer(player);
        if (!(u.getGame() != null && u.isAlive())) {

            return;

        }

        event.setCancelled(true);
        event.setFoodLevel(20);

    }

    @EventHandler
    public void entityDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) {

            return;

        }

        final Player player = (Player) event.getEntity();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = getTrackedPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        final UtilPlayer u = new UtilPlayer(player);

        SpleggOG.getPlugin().pm.PLAYERS.put(player.getName(), u);

    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = getTrackedPlayer(player);

        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        final Player player = event.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        final UtilPlayer u = getTrackedPlayer(player);
        if (!(u.getGame() != null && u.isAlive() && !StringUtils.startsWith(event.getMessage(), "/splegg")
                && !player.hasPermission("splegg.admin")))
        {

            return;

        }

        event.setCancelled(true);

        Utils.spleggOGMessage(player, "&6You cannot use that command in &3Splegg&6!");

    }

}
