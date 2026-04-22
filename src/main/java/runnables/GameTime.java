package runnables;

import main.SpleggOG;
import managers.Game;
import managers.Status;

public class GameTime implements Runnable {

    SpleggOG splegg;
    Game game;

    public GameTime(SpleggOG splegg, Game game) {

        this.splegg = splegg;
        this.game = game;

    }

    public void run() {

        if (game.getStatus() != Status.INGAME) {

            game.stopGameTimer();
            return;

        }

        if (splegg.games.checkWinner(game)) {

            return;

        }

        final int remaining = game.tickTime();
        if (remaining <= 0) {

            splegg.chat.bc(splegg.getConfig().getString("Messages.Timelimitreached"), game);
            splegg.game.stopGame(game, game.getPlayers().size());
            return;

        }

        if (remaining % 300 == 0 || remaining % 30 == 0 && remaining < 60) {

            splegg.game.ingameTimer(remaining, game.getPlayers());

        }

        if (remaining <= 5) {

            splegg.chat.bc(splegg.getConfig().getString("Messages.EndingTimer").replaceAll("%timer%",
                    String.valueOf(remaining)), game);

        }

    }

}
