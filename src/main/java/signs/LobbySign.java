package signs;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import main.SpleggOG;
import managers.Game;
import managers.Status;
import net.kyori.adventure.text.TextComponent;
import utils.Utils;

/**
 * Renders join signs. A sign is bound to one lobby (SP1) or to any lobby, in
 * which case it shows the lobby a click would put the player in,
 * TheHerobrine-OG style. A {@link JoinSignUpdater} redraws every sign once a
 * second, so event-driven update calls only shorten the latency.
 */
public class LobbySign {

    // The sign key that means "whichever lobby is best to join".
    public static final String ANY = "any";

    // Sign lines hold 15 visible characters; longer names would clip
    // mid-color-code, so they are truncated before rendering.
    private static final int MAX_LINE_LENGTH = 15;

    private final SpleggOG splegg;
    private final String key;

    public LobbySign(String key, SpleggOG splegg) {

        this.splegg = splegg;
        this.key = normalizeKey(key);

    }

    // A key that names a lobby stays a lobby id; anything else (including a map
    // name from an older version) means any lobby.
    public static String normalizeKey(String key) {

        if (key == null || key.isBlank()) {

            return ANY;

        }

        final Game game = SpleggOG.getPlugin().games == null ? null : SpleggOG.getPlugin().games.resolveLobby(key);
        return game != null ? game.getLobbyId() : ANY;

    }

    public String getKey() {

        return this.key;

    }

    public void create(Location location) {

        this.splegg.maps.c.addSign(this.key, LobbySignUtils.get().locationToString(location));
        this.splegg.getServer().getScheduler().runTaskLater(this.splegg, () -> updateAll(this.splegg), 5L);

    }

    public void delete(String storedKey, Location location) {

        this.splegg.maps.c.delSign(storedKey, LobbySignUtils.get().locationToString(location));

    }

    // Redraws every registered sign under every stored key.
    public static void updateAll(SpleggOG splegg) {

        if (splegg == null || splegg.maps == null || splegg.maps.c == null || splegg.games == null) {

            return;

        }

        for (String storedKey : splegg.maps.c.getSignKeys()) {

            new LobbySign(storedKey, splegg).update(storedKey);

        }

    }

    public void update(String storedKey) {

        final String[] lines = renderLines();

        for (String loc : this.splegg.maps.c.getSigns(storedKey)) {

            final Location location = LobbySignUtils.get().stringToLocation(loc);
            final World world = location.getWorld();
            if (world == null) {

                continue;

            }

            // Never force a chunk load just to redraw a sign.
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {

                continue;

            }

            final BlockState state = location.getBlock().getState();
            if (!(state instanceof Sign)) {

                continue;

            }

            this.setSign(lines, (Sign) state);

        }

    }

    // The lobby a click on this sign would join, or null.
    public Game resolveTarget(org.bukkit.entity.Player player) {

        if (ANY.equals(this.key)) {

            return this.splegg.games.findBestLobby(player);

        }

        return this.splegg.games.resolveLobby(this.key);

    }

    private String[] renderLines() {

        final String lobby;
        final String map;
        final String status;
        final int count;
        final int maxCount;

        if (ANY.equals(this.key)) {

            final List<Game> games = this.splegg.games.all();
            Game display = null;
            int displayPriority = Integer.MAX_VALUE;
            int players = 0;
            for (Game game : games) {

                players += game.getPlayers().size();
                final int priority = priority(game);
                if (priority < displayPriority) {

                    displayPriority = priority;
                    display = game;

                }

            }

            count = players;
            maxCount = games.isEmpty() ? 0 : games.get(0).getMaxPlayers() * games.size();
            lobby = display == null ? "Any" : display.getLobbyId();
            map = display == null ? "Splegg" : display.getMapDisplayName();
            status = aggregateStatus(games);

        } else {

            final Game game = this.splegg.games.resolveLobby(this.key);
            if (game == null) {

                lobby = this.key;
                map = this.key;
                count = 0;
                maxCount = 0;
                status = configStatus("Disabled", "&cDISABLED");

            } else {

                lobby = game.getLobbyId();
                map = game.getMapDisplayName();
                count = game.getPlayers().size();
                maxCount = game.getMaxPlayers();
                status = statusFor(game);

            }

        }

        final String[] lines = new String[4];
        final String[] defaults = { "&4Splegg", "&6%map%", "%status%", "&0%count%&8/&0%maxcount%" };
        for (int i = 0; i < 4; i++) {

            final String raw = this.splegg.getConfig().getString("Sings.Format." + (i + 1), defaults[i]);
            lines[i] = raw.replace("%status%", status).replace("%map%", truncate(map))
                    .replace("%lobby%", truncate(lobby)).replace("%count%", String.valueOf(count))
                    .replace("%maxcount%", String.valueOf(maxCount));

        }

        return lines;

    }

    private static String truncate(String subject) {

        return subject.length() > MAX_LINE_LENGTH ? subject.substring(0, MAX_LINE_LENGTH) : subject;

    }

    private String configStatus(String key, String fallback) {

        return this.splegg.getConfig().getString("Sings.Status." + key, fallback);

    }

    private String statusFor(Game game) {

        switch (game.getStatus()) {

            case LOBBY -> {

                if (game.getPlayers().size() >= game.getMaxPlayers()) {

                    return configStatus("Full", "&4&lFULL");

                }

                return game.isStarting() ? configStatus("Starting", "&5&lSTARTING") : configStatus("Join", "&2&lJOIN");

            }
            case INGAME -> {

                return configStatus("Started", "&3&lLIVE");

            }
            case ENDING -> {

                return configStatus("Ending", "&8&lENDING");

            }
            default -> {

                return configStatus("Disabled", "&cDISABLED");

            }

        }

    }

    // Mirrors TheHerobrine-OG's join-sign status table: STARTING and JOIN win
    // over LIVE, an all-live set reads LIVE, an all-ending set reads ENDING, and
    // anything else that cannot be joined reads FULL.
    private String aggregateStatus(List<Game> games) {

        if (games.isEmpty()) {

            return configStatus("Disabled", "&cDISABLED");

        }

        boolean anyJoinable = false;
        boolean anyStarting = false;
        int liveCount = 0;
        int endingCount = 0;
        for (Game g : games) {

            final Status st = g.getStatus();
            if (st == Status.LOBBY) {

                if (g.getPlayers().size() < g.getMaxPlayers()) {

                    anyJoinable = true;
                    if (g.isStarting()) {

                        anyStarting = true;

                    }

                }

            } else if (st == Status.INGAME) {

                liveCount++;

            } else if (st == Status.ENDING) {

                endingCount++;

            }

        }

        if (anyJoinable && anyStarting) {

            return configStatus("Starting", "&5&lSTARTING");

        }

        if (anyJoinable) {

            return configStatus("Join", "&2&lJOIN");

        }

        if (liveCount == games.size()) {

            return configStatus("Started", "&3&lLIVE");

        }

        if (endingCount == games.size()) {

            return configStatus("Ending", "&8&lENDING");

        }

        return configStatus("Full", "&4&lFULL");

    }

    private static int priority(Game game) {

        final boolean room = game.getPlayers().size() < game.getMaxPlayers();
        if (game.getStatus() == Status.LOBBY && game.isStarting() && room)
            return 0;
        if (game.getStatus() == Status.LOBBY && room)
            return 1;
        if (game.getStatus() == Status.INGAME)
            return 2;
        if (game.getStatus() == Status.LOBBY)
            return 3;
        if (game.getStatus() == Status.ENDING)
            return 4;
        return 5;

    }

    private void setSign(String[] lines, Sign s) {

        for (int i = 0; i < lines.length; ++i) {

            final TextComponent signLineContainer = Utils.legacySerializerAnyCase(lines[i] != null ? lines[i] : "");
            s.line(i, signLineContainer);

        }

        s.update(false, false);

    }

}
