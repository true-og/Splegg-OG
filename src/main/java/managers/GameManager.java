package managers;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;

import config.Map;
import events.Listeners;
import main.SpleggOG;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

public class GameManager {

	SpleggOG splegg;

	public GameManager() {

		splegg = SpleggOG.getPlugin();

	}

	public void startGame(Game game) {

		Map map = game.getMap();
		SpleggOG.getPlugin().getLogger().info("New game commencing in map: " + map);
		game.startGameTimer();
		Bukkit.getScheduler().cancelTask(game.counter);
		game.status = Status.INGAME;
		game.time = 901;
		game.setLobbyCount(31);
		int c = 1;
		game.loadFloors();

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
			sp.getPlayer().getInventory().clear();

		}

		game.getSign().update(map, false);

		splegg.chat.bc(splegg.getConfig().getString("Messages.InstructionsGame"), game);

	}

	public void stopGame(Game game, int r) {

		SpleggOG.getPlugin().getLogger().info("Commencing shutdown of: " + game.getMap().getName() + ".");

		game.status = Status.ENDING;
		game.stopGameTimer();
		game.time = 601;
		game.setStatus(Status.LOBBY);
		game.resetArena();
		game.data.clear();
		game.floor.clear();
		game.setStarting(false);
		game.players.clear();

		Iterator<?> playersInGame = game.players.values().iterator();
		while(playersInGame.hasNext()) {

			SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
			UtilPlayer u = sp.getUtilPlayer();
			game.leaveGame(u);

		}

		if (! splegg.disabling) {

			game.getSign().update(game.map, true);

		}

		SpleggOG.getPlugin().getLogger().info("Map " + game.map.getName() + " was reset.");

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
			splegg.chat.sendMessage(sp.getPlayer(), "&6Splegg is ending in... " + splegg.game.getDigitTime(count));

		}

	}

}