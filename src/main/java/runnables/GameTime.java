package runnables;

import main.SpleggOG;
import managers.Game;

public class GameTime implements Runnable {

	SpleggOG splegg;
	Game game;

	public GameTime(SpleggOG splegg, Game game) {

		this.splegg = splegg;
		this.game = game;

	}

	public void run() {

		if (game.getCount() > 0) {

			splegg.games.checkWinner(game);

			if (game.getCount() % 300 == 0) {

				splegg.game.ingameTimer(game.getCount(), game.getPlayers());

			}

			if (game.getCount() % 30 == 0 && game.getCount() < 60) {

				splegg.game.ingameTimer(game.getCount(), game.getPlayers());

			}

			if (game.getCount() <= 5 && game.getCount() >= 1) {

				splegg.chat.bc(splegg.getConfig().getString("Messages.EndingTimer").replaceAll("%timer%", String.valueOf(game.getCount())), game);

			}

			game.stopGameTimer();

			splegg.chat.bc(splegg.getConfig().getString("Messages.Timelimitreached"), game);

		}

	}

}