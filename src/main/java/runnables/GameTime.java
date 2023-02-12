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

		if (this.game.getCount() > 0) {

			this.splegg.games.checkWinner(this.game);

			if (this.game.getCount() % 300 == 0) {

				this.splegg.game.ingameTimer(this.game.getCount(), this.game.getPlayers());

			}

			if (this.game.getCount() % 30 == 0 && this.game.getCount() < 60) {

				this.splegg.game.ingameTimer(this.game.getCount(), this.game.getPlayers());

			}

			if (this.game.getCount() <= 5 && this.game.getCount() >= 1) {

				this.splegg.chat.bc(splegg.getConfig().getString("Messages.EndingTimer").replaceAll("&", "§").replaceAll("%timer%", String.valueOf(this.game.getCount())), this.game);

			}

		}
		else {

			this.game.stopGameTimer();

			this.splegg.chat.bc(splegg.getConfig().getString("Messages.Timelimitreached"), this.game);
			this.splegg.game.stopGame(this.game, 1);

		}

	}

}