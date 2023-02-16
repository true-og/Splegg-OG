package managers;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;

import config.Map;
import events.Listeners;
import main.SpleggOG;
import utils.SpleggPlayer;
import utils.Utils;

public class GameManager {

	SpleggOG splegg;
	
	public GameManager() {

		this.splegg = SpleggOG.getPlugin();
		
	}

	public void startGame(Game game) {

		this.splegg.chat.log("New game commencing..");
		game.startGameTimer();
		Bukkit.getScheduler().cancelTask(game.counter);
		game.status = Status.INGAME;
		game.time = 901;
		game.setLobbyCount(31);
		int c = 1;
		game.loadFloors();
		Map map = game.getMap();

		Iterator<?> playersInGame = game.players.values().iterator();
		SpleggPlayer sp;
		while(playersInGame.hasNext()) {

			sp = (SpleggPlayer) playersInGame.next();
			sp.getPlayer().setLevel(0);
			sp.getUtilPlayer().setAlive(true);

			if (c > map.getSpawnCount()) {

				c = 1;

			}

			sp.getPlayer().teleport(map.getSpawn(c));
			c++;
			sp.getPlayer().setLevel(0);
			sp.getPlayer().setExp(0.0F);
			sp.getPlayer().setGameMode(GameMode.ADVENTURE);

			// TODO: Add all available shovels here.
			if (! Listeners.manager.contains(sp.getPlayer().getName())) {

				if (Listeners.goldspade.contains(sp.getPlayer().getName())) {

					sp.getPlayer().getInventory().clear();
					sp.getPlayer().getInventory().setItem(0, Utils.getItem(Material.GOLDEN_SHOVEL, splegg.getConfig().getString("Shovels.Gold.Name").replaceAll("&", "§"), splegg.getConfig().getString("Shovels.Gold.Lore").replaceAll("&", "§")));
					sp.getPlayer().updateInventory();

					Listeners.manager.remove(sp.getPlayer().getName());
					Listeners.goldspade.remove(sp.getPlayer().getName());
					Listeners.diamondspade.remove(sp.getPlayer().getName());
					Listeners.shopmanager.remove(sp.getPlayer().getName());
					Listeners.moneymanager.add(sp.getPlayer().getName());

				}

				if (Listeners.diamondspade.contains(sp.getPlayer().getName())) {

					sp.getPlayer().getInventory().clear();
					sp.getPlayer().getInventory().setItem(0, Utils.getItem(Material.DIAMOND_SHOVEL, splegg.getConfig().getString("Shovels.Diamond.Name").replaceAll("&", "§"), splegg.getConfig().getString("Shovels.Diamond.Lore").replaceAll("&", "§")));
					sp.getPlayer().updateInventory();

					Listeners.manager.remove(sp.getPlayer().getName());
					Listeners.goldspade.remove(sp.getPlayer().getName());
					Listeners.diamondspade.remove(sp.getPlayer().getName());
					Listeners.shopmanager.remove(sp.getPlayer().getName());
					Listeners.moneymanager.add(sp.getPlayer().getName());

				}

			}
			else {

				sp.getPlayer().getInventory().clear();
				sp.getPlayer().getInventory().setItem(0, Utils.getItem(Material.IRON_SHOVEL, splegg.getConfig().getString("Shovels.Iron.Name").replaceAll("&", "§"), splegg.getConfig().getString("Shovels.Iron.Lore").replaceAll("&", "§")));
				sp.getPlayer().updateInventory();

				Listeners.manager.remove(sp.getPlayer().getName());
				Listeners.goldspade.remove(sp.getPlayer().getName());
				Listeners.diamondspade.remove(sp.getPlayer().getName());
				Listeners.shopmanager.remove(sp.getPlayer().getName());
				Listeners.moneymanager.add(sp.getPlayer().getName());

			}

		}

		game.getSign().update(map, false);

		this.splegg.chat.bc(this.splegg.getConfig().getString("Messages.InstructionsGame").replaceAll("&", "§"), game);

	}

	public void stopGame(Game game, int r) {

		this.splegg.chat.log("Commencing shutdown of " + game.getMap().getName() + ".");

		game.status = Status.ENDING;
		game.stopGameTimer();
		game.setLobbyCount(31);
		game.time = 601;
		game.setStatus(Status.LOBBY);
		game.resetArena();
		game.data.clear();
		game.floor.clear();
		game.setStarting(false);
		HashMap<String, SpleggPlayer> h = new HashMap<String, SpleggPlayer>(game.players);
		game.players.clear();

		Iterator<?> var5 = h.values().iterator();
		while(var5.hasNext()) {

			SpleggPlayer sp = (SpleggPlayer) var5.next();
			game.leaveGame(sp.getPlayer());

		}

		if (! this.splegg.disabling) {

			game.getSign().update(game.map, true);

		}

		this.splegg.chat.log("Map " + game.map.getName() + " was reset.");

	}

	public String getDigitTime(int count) {

		int minutes = count / 60;
		int seconds = count % 60;

		String disMinu = (minutes < 10 ? "0" : "") + minutes;
		String disSec = (seconds < 10 ? "0" : "") + seconds;
		String formattedTime = disMinu + ":" + disSec;

		return formattedTime;

	}

	public void ingameTimer(int count, HashMap<String, SpleggPlayer> players) {

		Iterator<?> playersInGame = players.values().iterator();
		while(playersInGame.hasNext()) {

			SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
			this.splegg.chat.sendMessage(sp.getPlayer(), "&6Splegg is ending in §5§l" + this.splegg.game.getDigitTime(count));

		}

	}

}