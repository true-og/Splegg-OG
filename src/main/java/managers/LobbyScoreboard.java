package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import main.SpleggOG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import utils.ScoreboardOGBridge;
import utils.SpleggPlayer;
import utils.TrueOGBoard;
import utils.Utils;

// The sidebar a Splegg player sees while queued and while playing. With Scoreboard-OG
// present it renders through that plugin's sidebar; otherwise a Bukkit board is used.
public class LobbyScoreboard {

    private static final HashMap<UUID, Scoreboard> BOARDS = new HashMap<>();
    private static final HashMap<UUID, Game> GAMES = new HashMap<>();

    private LobbyScoreboard() {

        // Static utility.

    }

    public static void attach(Player player, Game game) {

        GAMES.put(player.getUniqueId(), game);

        if (ScoreboardOGBridge.isAvailable()) {

            ScoreboardOGBridge.claim(player, LobbyScoreboard::title, LobbyScoreboard::lines);
            return;

        }

        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {

            return;

        }

        final Scoreboard board = manager.getNewScoreboard();
        final Objective objective = board.registerNewObjective("splegg_lobby", Criteria.DUMMY, title(player));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        BOARDS.put(player.getUniqueId(), board);
        player.setScoreboard(board);

        render(player, game);

    }

    public static void detach(Player player) {

        GAMES.remove(player.getUniqueId());

        if (ScoreboardOGBridge.isAvailable()) {

            ScoreboardOGBridge.release(player);
            return;

        }

        // Hand the player back to the server's main scoreboard rather than a
        // fresh empty one: Scoreboard-OG's world-change hook only reclaims its
        // sidebar when the player's Bukkit board is the main scoreboard.
        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {

            player.setScoreboard(manager.getMainScoreboard());

        }

        BOARDS.remove(player.getUniqueId());

    }

    public static void detachAll() {

        for (UUID playerId : new ArrayList<>(GAMES.keySet())) {

            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                detach(player);

            }

        }

        GAMES.clear();
        BOARDS.clear();

    }

    // Scoreboard-OG polls the provider itself; only the Bukkit boards need
    // redrawing.
    public static void refreshGame(Game game) {

        if (game == null || ScoreboardOGBridge.isAvailable()) {

            return;

        }

        final Iterator<SpleggPlayer> it = game.getPlayers().values().iterator();
        while (it.hasNext()) {

            final SpleggPlayer sp = it.next();
            render(sp.getPlayer(), game);

        }

    }

    private static TextComponent title(Player player) {

        return Utils.legacySerializerAnyCase(TrueOGBoard.TITLE);

    }

    private static List<Component> lines(Player player) {

        final List<Component> out = new ArrayList<>();
        final Game game = GAMES.get(player.getUniqueId());
        if (game == null) {

            return out;

        }

        for (String line : rawLines(player, game)) {

            out.add(Utils.legacySerializerAnyCase(line));

        }

        return out;

    }

    // Top to bottom in the network board's layout: a blank, labelled blocks split
    // by blanks, then the site footer. Queue card counts down; match card scores.
    private static List<String> rawLines(Player player, Game game) {

        final List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("&eMap:");
        lines.add(game.getMap() != null ? "&f" + TrueOGBoard.fit(game.getMap().getName(), TrueOGBoard.VALUE_WIDTH)
                : "&7Voting...");
        lines.add("");

        if (game.getStatus() == Status.INGAME) {

            final SpleggPlayer self = game.getPlayers().get(player.getUniqueId());
            final int broken = self == null ? 0 : self.getBroken();
            lines.add("&aAlive: &f" + game.getPlayers().size());
            lines.add("");
            lines.add("&eBlocks: &f" + TrueOGBoard.compact(broken));
            lines.add("");
            lines.add("&6Time: &f" + TrueOGBoard.clock(game.getCount()));

        } else {

            final int currentPlayers = game.getPlayers().size();
            lines.add("&6Players:");
            lines.add("&f" + currentPlayers + "&7/&f" + game.getMaxPlayers());
            lines.add("");
            lines.add("&6Starting in:");
            if (game.isStarting()) {

                lines.add("&f" + TrueOGBoard.clock(game.getLobbyCount()));

            } else {

                final int required = Math.max(2, SpleggOG.getPlugin().getConfig().getInt("Options.AutoStartPlayers"));
                final int needed = Math.max(0, required - currentPlayers);
                lines.add(needed == 0 ? "&aReady" : "&7Need " + needed + " more");

            }

        }

        lines.add("");
        lines.add(TrueOGBoard.FOOTER);
        return lines;

    }

    private static void render(Player player, Game game) {

        final Scoreboard board = BOARDS.get(player.getUniqueId());
        if (board == null) {

            return;

        }

        final Objective objective = board.getObjective("splegg_lobby");
        if (objective == null) {

            return;

        }

        for (String entry : board.getEntries()) {

            board.resetScores(entry);

        }

        // Bukkit boards score from the bottom, so the top line gets the highest score.
        // Entries must be unique, so repeated blanks get a different width each.
        final List<String> lines = rawLines(player, game);
        final StringBuilder blank = new StringBuilder();
        int score = lines.size();
        for (String line : lines) {

            String entry = line;
            if (line.isEmpty()) {

                blank.append(' ');
                entry = blank.toString();

            }

            objective.getScore(Utils.legacySectionize(entry)).setScore(score--);

        }

    }

}
