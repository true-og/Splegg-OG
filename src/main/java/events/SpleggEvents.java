package events;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import utils.UtilPlayer;

public class SpleggEvents implements Listener {

	@EventHandler
	public void eggLand(ProjectileHitEvent e) {

		ProjectileSource shooter = e.getEntity().getShooter();
		if (shooter instanceof Player && e.getEntity() instanceof Egg) {

			Player player = (Player) shooter;
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
			if (u.getGame() != null && u.isAlive()) {

				// If the egg hit an entity, do this.
				if(e.getHitEntity() != null) {

					// Cancel the hit.
					e.setCancelled(true);

				}

				BlockIterator bi = new BlockIterator(e.getEntity().getWorld(), e.getEntity().getLocation().toVector(), e.getEntity().getVelocity().normalize(), 0.0D, 4);
				Block hit = null;
				while(bi.hasNext()) {

					hit = bi.next();

					if (hit.getType() != Material.AIR) {

						break;

					}

				}

				if (hit.getType() == Material.TNT) {

					e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), 3.0F);

					Iterator<?> entityIterator = e.getEntity().getWorld().getEntities().iterator();
					while(entityIterator.hasNext()) {

						Entity drop = (Entity) entityIterator.next();

						if (drop.getType() == EntityType.DROPPED_ITEM) {

							drop.remove();
							drop.remove();
							drop.remove();
							drop.remove();
							drop.remove();
							drop.remove();

						}

					}

				}

				Location hitLocation = hit.getLocation();
				if (u.getGame().getFloor().contains(hitLocation)) {

					Game game = u.getGame();
					// If the game is in-progress, do this.
					if (game.getStatus() == Status.INGAME) {

						// Play chicken egg sound upon block hit for every player nearby the event.
						player.getWorld().playSound(hitLocation, Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);

						// Spawn spell particles when a block is destroyed to indicate vaporization.
						player.getWorld().spawnParticle(Particle.SPELL, hitLocation, 12);
						player.getWorld().spawnParticle(Particle.BLOCK_DUST, hitLocation, 6, hit.getBlockData());

						// Destroy the block that was hit by an egg.
						hit.setType(Material.AIR);

					}

				}

			}

		}

	}

	@EventHandler
	public void onKnockout(PlayerMoveEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		Game game = u.getGame();
		if (game != null && u.isAlive() && game.getStatus() == Status.INGAME) {

			SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.PlayersRemaining").replaceAll("&", "§").replaceAll("%count%", String.valueOf(game.getPlayers().size())), game);

			Listeners.launchEggs.remove(player.getName());
			Listeners.manager.remove(player.getName());
			Listeners.shopmanager.remove(player.getName());
			Listeners.goldspade.remove(player.getName());
			Listeners.diamondspade.remove(player.getName());
			Listeners.moneymanager.remove(player.getName());

			game.leaveGame(game.getPlayers());

			player.setFallDistance(0.5F);

		}

	}
	
	// TODO: Implement spectator compass here.

	@EventHandler
	public void eggHatch(PlayerEggThrowEvent e) {

		Player player = e.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

		if (u.getGame() != null && u.isAlive()) {

			e.setHatching(false);

		}

	}
}