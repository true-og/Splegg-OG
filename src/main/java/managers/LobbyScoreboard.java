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

        final String rawTitle = SpleggOG.getPlugin().getConfig().getString("Scoreboard.Title");
        return Utils.legacySerializerAnyCase(rawTitle != null ? rawTitle : "&2Splegg&r-&4OG");

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

    // Top to bottom. The queue card shows the countdown; the match card shows the
    // survivors, the player's own block count and the time left.
    private static List<String> rawLines(Player player, Game game) {

        final List<String> lines = new ArrayList<>();
        lines.add("&7&m                ");
        lines.add(configLine("Scoreboard.Map", "&eMap:"));
        lines.add("&f" + (game.getMap() != null ? game.getMap().getName() : "&7Voting..."));
        lines.add("");

        if (game.getStatus() == Status.INGAME) {

            final SpleggPlayer self = game.getPlayers().get(player.getUniqueId());
            final int broken = self == null ? 0 : self.getBroken();
            lines.add(configLine("Scoreboard.Alive", "&aPlayers Alive:"));
            lines.add("&f" + game.getPlayers().size());
            lines.add(" ");
            lines.add(configLine("Scoreboard.BrokenBlocks", "&eBlocks Broken:"));
            lines.add("&f" + broken);
            lines.add("  ");
            lines.add(configLine("Scoreboard.TimeLeft", "&6Time Left:"));
            lines.add("&f" + SpleggOG.getPlugin().game.getDigitTime(Math.max(0, game.getCount())));

        } else {

            final int maxPlayers = game.getMaxPlayers();
            final int currentPlayers = game.getPlayers().size();
            lines.add(configLine("Scoreboard.Queue", "&6Players Waiting:"));
            lines.add("&f" + currentPlayers + "&7/&f" + maxPlayers);
            lines.add(" ");
            lines.add(configLine("Scoreboard.Starting", "&6Starting in:"));
            if (game.isStarting()) {

                lines.add("&f" + game.getLobbyCount() + "s");

            } else {

                final int required = Math.max(2, SpleggOG.getPlugin().getConfig().getInt("Options.AutoStartPlayers"));
                final int needed = Math.max(0, required - currentPlayers);
                lines.add(needed == 0 ? "&aReady" : "&f" + needed + " &7more");

            }

        }

        lines.add("&7&m               ");
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
        final List<String> lines = rawLines(player, game);
        int score = lines.size();
        for (String line : lines) {

            objective.getScore(Utils.legacySectionize(line.isEmpty() ? " " : line)).setScore(score--);

        }

    }

    private static String configLine(String path, String fallback) {

        final String value = SpleggOG.getPlugin().getConfig().getString(path);
        return value != null ? value : fallback;

    }

}
