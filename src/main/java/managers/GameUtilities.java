package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.entity.Player;

import main.SpleggOG;
import utils.SpleggPlayer;

public class GameUtilities {

    public HashMap<String, Game> GAMES = new HashMap<String, Game>();

    public Game getGame(String map) {

        return (Game) this.GAMES.get(map);

    }

    public void addGame(String map, Game game) {

        this.GAMES.put(map, game);

    }

    public SpleggPlayer getPlayer(Player player) {

        if (player == null) {

            return null;

        }

        final java.util.UUID playerId = player.getUniqueId();
        for (Game g : this.GAMES.values()) {

            final SpleggPlayer sp = g.players.get(playerId);
            if (sp != null) {

                return sp;

            }

        }

        return null;

    }

    public Game getMatchedGame(Player player) {

        final java.util.UUID playerId = player.getUniqueId();
        for (Game g : this.GAMES.values()) {

            if (g.players.containsKey(playerId)) {

                return g;

            }

        }

        return null;

    }

    public int howManyOpenGames() {

        ArrayList<Game> all = new ArrayList<Game>();
        Iterator<?> var3 = this.GAMES.values().iterator();
        while (var3.hasNext()) {

            Game games = (Game) var3.next();
            if (games.getStatus() == Status.LOBBY) {

                all.add(games);

            }

        }

        return all.size();

    }

    public boolean checkWinner(Game game) {

        if (game == null || game.getStatus() != Status.INGAME) {

            return false;

        }

        int amountOfPlayersInGame = game.players.size();
        if (amountOfPlayersInGame <= 1) {

            if (amountOfPlayersInGame == 0) {

                game.splegg.game.stopGame(game, 0);

            } else {

                SpleggPlayer winner = game.players.values().iterator().next();

                SpleggOG.getPlugin().getLogger().info("Winner detected: " + winner.getPlayer().getName());

                game.splegg.game.finishGame(game, winner);

            }

            return true;

        }

        return false;

    }

}
