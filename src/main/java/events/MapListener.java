package events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

import main.SpleggOG;
import utils.UtilPlayer;

public class MapListener implements Listener {

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void hangingEntityBreak(HangingBreakByEntityEvent event) {

        if (!(event.getRemover() instanceof Player)) {

            return;

        }

        Player player = (Player) event.getRemover();
        if (!SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return;

        }

        UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
        if (u.getGame() != null && u.isAlive()) {

            event.setCancelled(true);

        }

    }

}
