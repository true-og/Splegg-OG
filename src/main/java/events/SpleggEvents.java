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
import org.bukkit.event.entity.EntityDamageEvent;
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
	public void eggLand(ProjectileHitEvent event) {

		ProjectileSource shooter = event.getEntity().getShooter();
		if (shooter instanceof Player && event.getEntity() instanceof Egg) {

			Player player = (Player) shooter;
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
			Block hitBlock = event.getHitBlock();
			if(hitBlock != null) {
				Location hitLocation = null;
				BlockIterator bi = new BlockIterator(hitBlock.getWorld(), event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0.0D, 4);
				if (u.getGame() != null && u.isAlive()) {

					Block hit = null;
					while(bi.hasNext()) {

						hit = bi.next();

						if (hit.getType() != Material.AIR) {

							break;

						}
						else {

							// Get location if it is a block.
							hitLocation = hitBlock.getLocation();

							// Cancel the collision.
							event.setCancelled(true);

							// Deletes the egg entity.
							event.getEntity().remove();

						}

					}

					if (hit.getType() == Material.TNT) {

						hitBlock.getWorld().createExplosion(hitLocation, 3.0F);

						Iterator<?> entityIterator = hitBlock.getWorld().getEntities().iterator();
						while(entityIterator.hasNext()) {

							Entity drop = (Entity) entityIterator.next();

							if (drop.getType() == EntityType.DROPPED_ITEM) {

								drop.remove();

							}

						}

					}

					Game game = u.getGame();
					if (game.getFloor().contains(hitLocation)) {

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

	}

	// TODO: Implement spectator compass here.
	@EventHandler
	public void eggHatch(PlayerEggThrowEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

		if (u.getGame() != null && u.isAlive()) {

			event.setHatching(false);

		}

	}

	@EventHandler
	public void onFall(EntityDamageEvent event) {

		if (event.getEntity() instanceof Player) {

			Player player = (Player) event.getEntity();
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

			if (u.getGame() != null && u.isAlive()) {

				event.setCancelled(true);

			}


		}

	}

	@EventHandler
	public void onKnockout(PlayerMoveEvent event) {

		Player player = event.getPlayer();
		UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
		Game game = u.getGame();
		if (game != null && u.isAlive() && ((double) player.getLocation().getBlockY() < -5.0D || player.getLocation().getBlockY() < game.getLowestPossible()) && game.getStatus() == Status.INGAME) {

			SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.KnockoutPlayer").replaceAll("%player%", player.getName()));
			SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.PlayersRemaining").replaceAll("%count%", String.valueOf(game.getPlayers().size() - 1)), game);

			player.setFallDistance(0.0F);
			game.leaveGame(u);

			SpleggOG.getPlugin().games.checkWinner(game);

		}

	}

}