package managers;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import main.SpleggOG;
import net.kyori.adventure.text.TextComponent;
import utils.SpleggPlayer;
import utils.Utils;

public class LobbyScoreboard {

    private static final HashMap<String, Scoreboard> BOARDS = new HashMap<>();

    private LobbyScoreboard() {

        // Static utility.

    }

    public static void attach(Player player, Game game) {

        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {

            return;

        }

        final Scoreboard board = manager.getNewScoreboard();
        final String rawTitle = SpleggOG.getPlugin().getConfig().getString("Scoreboard.Title");
        final TextComponent title = Utils.legacySerializerAnyCase(rawTitle != null ? rawTitle : "&2Splegg&r-&4OG");

        final Objective objective = board.registerNewObjective("splegg_lobby", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        BOARDS.put(player.getName(), board);
        player.setScoreboard(board);

        render(player, game);

    }

    public static void detach(Player player) {

        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {

            player.setScoreboard(manager.getNewScoreboard());

        }

        BOARDS.remove(player.getName());

    }

    public static void refreshGame(Game game) {

        if (game == null) {

            return;

        }

        final Iterator<SpleggPlayer> it = game.getPlayers().values().iterator();
        while (it.hasNext()) {

            final SpleggPlayer sp = it.next();
            render(sp.getPlayer(), game);

        }

    }

    private static void render(Player player, Game game) {

        final Scoreboard board = BOARDS.get(player.getName());
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

        final String queueLabel = configLine("Scoreboard.Queue", "&6Players Waiting:");
        final String startingLabel = configLine("Scoreboard.Starting", "&6Starting in:");
        final String mapLabel = "&eMap:";

        final int maxPlayers = game.getMap().getSpawnCount();
        final int currentPlayers = game.getPlayers().size();

        final String mapValue = "&f" + game.getMap().getName();
        final String queueValue = "&f" + currentPlayers + "&7/&f" + maxPlayers;
        final String startingValue;
        if (game.isStarting()) {

            startingValue = "&f" + game.getLobbyCount() + "s";

        } else {

            final int required = Math.max(2, SpleggOG.getPlugin().getConfig().getInt("Options.AutoStartPlayers"));
            final int needed = Math.max(0, required - currentPlayers);
            startingValue = needed == 0 ? "&aReady" : "&f" + needed + " &7more";

        }

        // Higher score = higher on sidebar.
        setLine(objective, 7, "&7&m                ");
        setLine(objective, 6, mapLabel);
        setLine(objective, 5, mapValue);
        setLine(objective, 4, queueLabel);
        setLine(objective, 3, queueValue);
        setLine(objective, 2, startingLabel);
        setLine(objective, 1, startingValue);
        setLine(objective, 0, "&7&m               ");

    }

    private static void setLine(Objective objective, int score, String rawLine) {

        final String text = Utils.legacySectionize(rawLine);
        objective.getScore(text).setScore(score);

    }

    private static String configLine(String path, String fallback) {

        final String value = SpleggOG.getPlugin().getConfig().getString(path);
        return value != null ? value : fallback;

    }

}
