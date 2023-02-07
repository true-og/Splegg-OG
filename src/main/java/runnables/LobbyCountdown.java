package runnables;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Sound;

import main.SpleggOG;
import managers.Game;
import utils.SpleggPlayer;

public class LobbyCountdown implements Runnable {

	int lobbycount;
	Game game;

	public LobbyCountdown(SpleggOG splegg, Game game, int lobbycount) {
		this.game = game;
		this.lobbycount = lobbycount;
	}

	public void run() {
		if (this.lobbycount >= 1) {
			--this.lobbycount;
			this.game.setLobbyCount(this.lobbycount);
			Iterator var2 = this.game.getPlayers().values().iterator();

			SpleggPlayer sp;
			while(var2.hasNext()) {
				sp = (SpleggPlayer)var2.next();
				sp.getPlayer().setLevel(this.lobbycount);
				sp.getPlayer().setExp((float)((double)this.game.getLobbyCount() * 0.008D));
			}

			this.game.getSign().update(this.game.getMap(), false);
			if (this.lobbycount % 25 == 0) {
				SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.LobbyTimer").replaceAll("&", "�").replaceAll("%timer%", String.valueOf(this.lobbycount)), this.game);
			}

			if (this.lobbycount <= 10 && this.lobbycount >= 1) {
				SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.LobbyTimer").replaceAll("&", "�").replaceAll("%timer%", String.valueOf(this.lobbycount)), this.game);
				var2 = this.game.getPlayers().values().iterator();

				while(var2.hasNext()) {
					sp = (SpleggPlayer)var2.next();
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F);
				}
			}

			if (this.lobbycount <= 0) {
				var2 = this.game.getPlayers().values().iterator();

				while(var2.hasNext()) {
					sp = (SpleggPlayer)var2.next();
				}
			}
		} else if (this.game.getPlayers().size() > 1) {
			Bukkit.getScheduler().cancelTask(this.game.getCounterID());
			SpleggOG.getPlugin().game.startGame(this.game);
		} else {
			SpleggOG.getPlugin().chat.bc(SpleggOG.getPlugin().getConfig().getString("Messages.NoEnoughPlayers").replaceAll("&", "�"), this.game);
			Bukkit.getScheduler().cancelTask(this.game.getCounterID());
			this.game.setLobbyCount(31);
			this.game.setStarting(false);
			this.game.getSign().update(this.game.getMap(), false);
			this.game.startCountdown();
		}

	}
}