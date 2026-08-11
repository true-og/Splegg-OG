package managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import main.SpleggOG;
import utils.SpleggPlayer;

/**
 * Registry of all live {@link Game} sessions. Splegg supports N concurrent
 * games per map; each {@link Game} is identified by its {@code gameId}.
 *
 * Backward compatibility: {@link #GAMES} is preserved as a view over gameId
 * -&gt; Game, but legacy callers that looked up by map name should use
 * {@link #getGame(String)} (returns the first/preferred game for that map) or
 * the explicit multi-game helpers below.
 */
public class GameUtilities {

    public java.util.Map<String, Game> GAMES = new ConcurrentHashMap<>();

    public Game getGame(String mapNameOrGameId) {

        if (mapNameOrGameId == null)
            return null;
        Game direct = GAMES.get(mapNameOrGameId);
        if (direct != null)
            return direct;
        for (Game g : GAMES.values())
            if (g.getMap() != null && mapNameOrGameId.equals(g.getMap().getName()))
                return g;
        return null;

    }

    public void addGame(String key, Game game) {

        GAMES.put(key, game);

    }

    public void registerGame(Game game) {

        GAMES.put(game.getGameId(), game);

    }

    public void removeGame(Game game) {

        if (game == null)
            return;
        GAMES.remove(game.getGameId(), game);
        GAMES.entrySet().removeIf(e -> e.getValue() == game);

    }

    public List<Game> gamesForMap(String mapName) {

        List<Game> out = new ArrayList<>();
        if (mapName == null)
            return out;
        for (Game g : GAMES.values())
            if (g.getMap() != null && mapName.equals(g.getMap().getName()))
                out.add(g);
        return out;

    }

    public Game findJoinableForMap(String mapName) {

        Game best = null;
        int bestFill = -1;
        for (Game g : gamesForMap(mapName)) {

            if (g.getStatus() != Status.LOBBY)
                continue;
            if (g.getMap() == null || !g.getMap().isUsable(g.getMap()))
                continue;
            int max = g.getMap().getSpawnCount();
            int fill = g.getPlayers().size();
            if (fill >= max)
                continue;
            if (fill > bestFill) {

                bestFill = fill;
                best = g;

            }

        }

        return best;

    }

    public Game createForMap(config.Map map) {

        if (map == null || !map.isUsable(map))
            return null;
        Game game = new Game(SpleggOG.getPlugin(), map);
        org.bukkit.World world = SpleggOG.getPlugin().getGameWorldManager().prepareWorld(game);
        if (world == null) {

            SpleggOG.getPlugin().getLogger()
                    .warning("Failed to create per-game world for map '" + map.getName() + "'.");
            return null;

        }

        game.setGameWorld(world);
        registerGame(game);
        SpleggOG.getPlugin().getLogger()
                .info("Created Splegg game " + game.getGameId() + " for map '" + map.getName() + "'.");
        return game;

    }

    public Game findOrCreateForMap(config.Map map) {

        if (map == null)
            return null;
        Game joinable = findJoinableForMap(map.getName());
        if (joinable != null)
            return joinable;
        return createForMap(map);

    }

    public SpleggPlayer getPlayer(Player player) {

        if (player == null)
            return null;
        final UUID playerId = player.getUniqueId();
        for (Game g : GAMES.values()) {

            final SpleggPlayer sp = g.players.get(playerId);
            if (sp != null)
                return sp;

        }

        return null;

    }

    public Game getMatchedGame(Player player) {

        if (player == null)
            return null;
        final UUID playerId = player.getUniqueId();
        for (Game g : GAMES.values())
            if (g.players.containsKey(playerId))
                return g;
        return null;

    }

    // Lobby ids are <Worlds.GamePrefix><gameId>, the same shape as the per-game
    // world names, so SP1 names the lobby whose world is SP1-<map>.
    public String getLobbyId(Game game) {

        if (game == null)
            return null;
        return SpleggOG.getPlugin().getGameWorldPrefix() + game.getGameId();

    }

    // Accepts SP1, sp1 or a bare 1. Map names are deliberately not resolvable.
    public Game resolveLobby(String input) {

        if (input == null)
            return null;

        String query = input.trim();
        if (query.isEmpty())
            return null;

        final String prefix = SpleggOG.getPlugin().getGameWorldPrefix();
        if (!prefix.isEmpty() && query.regionMatches(true, 0, prefix, 0, prefix.length()))
            query = query.substring(prefix.length());

        if (query.isEmpty() || !query.chars().allMatch(Character::isDigit))
            return null;

        for (Game g : GAMES.values())
            if (query.equals(g.getGameId()))
                return g;

        return null;

    }

    public List<String> getLobbyIds() {

        final List<String> ids = new ArrayList<>();
        for (Game g : GAMES.values())
            ids.add(getLobbyId(g));
        Collections.sort(ids);
        return ids;

    }

    public int howManyOpenGames() {

        int n = 0;
        for (Game g : GAMES.values())
            if (g.getStatus() == Status.LOBBY)
                n++;
        return n;

    }

    public boolean checkWinner(Game game) {

        if (game == null || game.getStatus() != Status.INGAME)
            return false;

        int amountOfPlayersInGame = game.players.size();
        if (amountOfPlayersInGame <= 1) {

            if (amountOfPlayersInGame == 0)
                game.splegg.game.stopGame(game, 0);
            else {

                SpleggPlayer winner = game.players.values().iterator().next();
                SpleggOG.getPlugin().getLogger().info("Winner detected: " + winner.getPlayer().getName());
                game.splegg.game.finishGame(game, winner);

            }

            return true;

        }

        return false;

    }

    /**
     * Whether the given world name belongs to an active per-game world copy. Used
     * by listeners so events inside ephemeral game worlds resolve as "splegg world"
     * even though the copy is not configured in Worlds.InGame.
     */
    public boolean isActiveGameWorld(String worldName) {

        if (worldName == null)
            return false;
        for (Game g : GAMES.values()) {

            org.bukkit.World w = g.getGameWorld();
            if (w != null && worldName.equals(w.getName()))
                return true;

        }

        return false;

    }

    public List<Game> all() {

        return Collections.unmodifiableList(new ArrayList<>(GAMES.values()));

    }

}
