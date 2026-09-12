package events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import main.SpleggOG;

// Players reach Splegg territory by /mw tp, portals and other plugins too. Every
// route records a return spot so nobody lands at main spawn afterwards.
public class PreJoinLocationListener implements Listener {

    private final SpleggOG plugin;

    public PreJoinLocationListener(SpleggOG plugin) {

        this.plugin = plugin;

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null)
            return;
        if (from.getWorld().equals(to.getWorld()))
            return;

        boolean fromManaged = plugin.isSpleggTerritory(from.getWorld().getName());
        boolean toManaged = plugin.isSpleggTerritory(to.getWorld().getName());

        if (toManaged && !fromManaged) {

            plugin.savePreJoinLocation(event.getPlayer().getUniqueId(), from);
            return;

        }

        // Left Splegg territory for a real world by any route, so the recorded spot
        // has served its purpose.
        if (fromManaged && !toManaged)
            plugin.removePreJoinLocation(event.getPlayer().getUniqueId());

    }

    // A player still on the death screen when their match world is deleted would
    // otherwise respawn at main world spawn.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        if (!plugin.hasPreJoinLocation(player.getUniqueId()))
            return;

        Location respawn = event.getRespawnLocation();
        if (respawn != null && respawn.getWorld() != null && plugin.isSpleggTerritory(respawn.getWorld().getName()))
            return;

        Location saved = plugin.resolvePreJoinLocation(player.getUniqueId());
        if (saved == null)
            return;

        event.setRespawnLocation(saved);
        plugin.removePreJoinLocation(player.getUniqueId());

    }

    // A relog inside Splegg territory, or after the world the player quit in was
    // deleted, goes back to the recorded spot; the main spawn is the fallback.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        boolean inTerritory = player.getWorld() != null && plugin.isSpleggTerritory(player.getWorld().getName());
        if (!inTerritory && !plugin.hasPreJoinLocation(player.getUniqueId()))
            return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline())
                return;

            if (!plugin.returnPlayer(player, inTerritory))
                plugin.getLogger().warning("Could not return " + player.getName() + " after a relog.");

        }, 1L);

    }

}
