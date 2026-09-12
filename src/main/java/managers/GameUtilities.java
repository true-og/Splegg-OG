package managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import main.SpleggOG;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import utils.SpleggPlayer;
import utils.Utils;

// Registry of every Splegg lobby, keyed by lobby number. Lobbies are created
// once per configured hub world (SP1-hub is lobby SP1) and live for the whole
// server session; a finished match resets its lobby instead of removing it.
public class GameUtilities {

    public java.util.Map<String, Game> GAMES = new ConcurrentHashMap<>();

    // Lobby ids are <Worlds.GamePrefix><number>, the same shape as the hub
    // world name without its -hub suffix. Returns the number, or null when the
    // world name is not <prefix><digits>-<name>.
    public static String lobbyNumberFromWorldName(String worldName) {

        if (worldName == null) {

            return null;

        }

        final String prefix = SpleggOG.getPlugin().getGameWorldPrefix();
        if (prefix == null || prefix.isEmpty() || !worldName.regionMatches(true, 0, prefix, 0, prefix.length())) {

            return null;

        }

        int index = prefix.length();
        final int digitStart = index;
        while (index < worldName.length() && Character.isDigit(worldName.charAt(index))) {

            index++;

        }

        if (index == digitStart || index >= worldName.length() || worldName.charAt(index) != '-'
                || index + 1 >= worldName.length())
        {

            return null;

        }

        return String.valueOf(Integer.parseInt(worldName.substring(digitStart, index)));

    }

    // Brings a lobby online for a loaded hub world, or returns the one that
    // already owns that number.
    public Game createLobby(String hubWorldName) {

        final String number = lobbyNumberFromWorldName(hubWorldName);
        if (number == null) {

            SpleggOG.getPlugin().getLogger()
                    .warning("Lobby world '" + hubWorldName + "' is not named <"
                            + SpleggOG.getPlugin().getGameWorldPrefix()
                            + "><number>-<name> (for example SP1-hub), so no lobby was created for it.");
            return null;

        }

        final Game existing = GAMES.get(number);
        if (existing != null) {

            if (!existing.getHubWorldName().equalsIgnoreCase(hubWorldName)) {

                SpleggOG.getPlugin().getLogger().warning("Lobby world '" + hubWorldName + "' clashes with '"
                        + existing.getHubWorldName() + "': both claim lobby " + existing.getLobbyId() + ".");

            }

            return existing;

        }

        final Game game = new Game(SpleggOG.getPlugin(), number, hubWorldName);
        GAMES.put(number, game);
        SpleggOG.getPlugin().getLogger()
                .info("Lobby " + game.getLobbyId() + " is ready in hub world '" + hubWorldName + "'.");
        return game;

    }

    public void clear() {

        GAMES.clear();

    }

    public Game getGame(String lobbyIdOrNumber) {

        return resolveLobby(lobbyIdOrNumber);

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

    // Lobbies whose chosen or running map is the given one.
    public List<Game> gamesForMap(String mapName) {

        List<Game> out = new ArrayList<>();
        if (mapName == null)
            return out;
        for (Game g : GAMES.values())
            if (g.getMap() != null && mapName.equals(g.getMap().getName()))
                out.add(g);
        return out;

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

        final String number = String.valueOf(Integer.parseInt(query));
        return GAMES.get(number);

    }

    // The best lobby for a player who did not name one: a starting lobby with
    // room and the most players, else a waiting lobby with room and the most
    // players. Live lobbies cannot be joined.
    public Game findBestLobby(Player player) {

        Game bestStarting = null;
        Game bestWaiting = null;
        int bestStartingFill = -1;
        int bestWaitingFill = -1;

        for (Game g : all()) {

            if (g.getStatus() != Status.LOBBY || g.getHubWorld() == null)
                continue;

            final int fill = g.getPlayers().size();
            if (fill >= g.getMaxPlayers() && !player.hasPermission("splegg.joinfull"))
                continue;

            if (g.isStarting()) {

                if (fill > bestStartingFill) {

                    bestStartingFill = fill;
                    bestStarting = g;

                }

            } else if (fill > bestWaitingFill) {

                bestWaitingFill = fill;
                bestWaiting = g;

            }

        }

        return bestStarting != null ? bestStarting : bestWaiting;

    }

    public String getLobbyId(Game game) {

        return game == null ? null : game.getLobbyId();

    }

    public List<String> getLobbyIds() {

        final List<String> ids = new ArrayList<>();
        for (Game g : all())
            ids.add(g.getLobbyId());
        return ids;

    }

    // Every lobby, ordered by number.
    public List<Game> all() {

        final List<Game> games = new ArrayList<>(GAMES.values());
        games.sort(Comparator.comparingInt(g -> Integer.parseInt(g.getGameId())));
        return Collections.unmodifiableList(games);

    }

    // The lobby list players see, TheHerobrine-OG style, with clickable joins.
    public void sendLobbyMessage(Player player) {

        final List<Game> games = all();
        if (games.isEmpty()) {

            Utils.spleggOGMessage(player, "&cThere are no Splegg lobbies. Check Worlds.Lobby in config.yml.");
            return;

        }

        Utils.spleggOGMessage(player, "&6Join a lobby with /splegg join <id>.");
        Utils.spleggOGMessage(player, "&6Lobbies available to join:");

        final boolean overfill = player.hasPermission("splegg.joinfull");
        for (Game game : games) {

            final String id = game.getLobbyId();
            final int fill = game.getPlayers().size();
            final int max = game.getMaxPlayers();
            final String line;
            final boolean joinable;
            switch (game.getStatus()) {

                case LOBBY -> {

                    final String state = game.isStarting() ? "&d&lSTARTING " : "&e&lWAITING ";
                    joinable = fill < max || overfill;
                    line = "&b" + id + ": &e" + fill + "/" + max + "&7 - &r" + state + "&r"
                            + (fill < max ? "&a(JOIN)" : "&c&lFULL &r" + (overfill ? "&a(JOIN)" : ""));

                }
                case INGAME -> {

                    joinable = false;
                    line = "&b" + id + ": &e" + fill + " remaining&8 - &b&lLIVE &r&7(" + game.getMapDisplayName() + ")";

                }
                case ENDING -> {

                    joinable = false;
                    line = "&b" + id + ": &8&lENDING";

                }
                default -> {

                    joinable = false;
                    line = "&b" + id + ": &c&lDISABLED";

                }

            }

            TextComponent component = Utils.legacySerializerAnyCase(Utils.prefix + line);
            if (joinable) {

                component = component
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Utils.legacySerializerAnyCase("&6Click here to join &b" + id)))
                        .clickEvent(ClickEvent.runCommand("/splegg join " + id));

            }

            player.sendMessage(component);

        }

    }

    public int howManyOpenGames() {

        int n = 0;
        for (Game g : GAMES.values())
            if (g.getStatus() == Status.LOBBY)
                n++;
        return n;

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

}
