package signs;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import config.Map;
import main.SpleggOG;
import managers.Game;
import managers.Status;
import net.kyori.adventure.text.TextComponent;
import utils.Utils;

/**
 * Renders the join-sign for a given map. With multi-game support, signs no
 * longer mirror a single {@link Game}'s status -- they aggregate across every
 * live game for the bound map. Idle (zero games) state advertises JOIN so a
 * fresh game spawns on demand. A {@link JoinSignUpdater} redraws every sign
 * once a second, so event-driven update calls only shorten the latency.
 */
public class LobbySign {

    // Sign lines hold 15 visible characters; longer map names would clip
    // mid-color-code, so they are truncated before rendering.
    private static final int MAX_LINE_LENGTH = 15;

    SpleggOG splegg;
    Map map;

    public LobbySign(Map map, SpleggOG s) {

        this.splegg = s;
        this.map = map;

    }

    public void create(Location location, final Map map) {

        String loc = LobbySignUtils.get().locationToString(location);
        this.splegg.maps.c.addSign(map.getName(), loc);

        if (this.map == null) {

            this.map = map;

        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.splegg, new Runnable() {

            public void run() {

                LobbySign.this.update(map);

            }

        }, 5L);

    }

    public void delete(Location location) {

        String loc = LobbySignUtils.get().locationToString(location);
        this.splegg.maps.c.delSign(this.map.getName(), loc);

        this.splegg.maps.c.saveMaps();
        this.map = null;

    }

    public void update(Map map) {

        final String[] lines = renderLines(map);

        for (String loc : this.splegg.maps.c.maps.getStringList("Signs." + map.getName() + ".lobby")) {

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

    private String[] renderLines(Map map) {

        final String status = getFancyStatus(map);
        final String count = String.valueOf(countPlayers(map));
        final String maxCount = String.valueOf(countSlots(map));

        final String[] lines = new String[4];
        final String[] defaults = { "&4Splegg", "&6%map%", "%status%", "&0%count%&8/&0%maxcount%" };
        for (int i = 0; i < 4; i++) {

            final String raw = this.splegg.getConfig().getString("Sings.Format." + (i + 1), defaults[i]);
            lines[i] = raw.replace("%status%", status).replace("%map%", truncate(map.getName()))
                    .replace("%count%", count).replace("%maxcount%", maxCount);

        }

        return lines;

    }

    private static String truncate(String subject) {

        return subject.length() > MAX_LINE_LENGTH ? subject.substring(0, MAX_LINE_LENGTH) : subject;

    }

    private int countPlayers(Map map) {

        int totalPlayers = 0;
        for (Game g : this.splegg.games.gamesForMap(map.getName())) {

            totalPlayers += g.getPlayers().size();

        }

        return totalPlayers;

    }

    private int countSlots(Map map) {

        final List<Game> games = this.splegg.games.gamesForMap(map.getName());
        return map.getSpawnCount() * Math.max(1, games.size());

    }

    // Mirrors TheHerobrine-OG's join-sign status table: STARTING and JOIN win
    // over LIVE, an all-live map reads LIVE, a map stuck ending reads ENDING,
    // and anything else that cannot be joined reads FULL.
    private String getFancyStatus(Map map) {

        if (!map.isUsable(map)) {

            return this.splegg.getConfig().getString("Sings.Status.Disabled", "&cDISABLED");

        }

        final List<Game> games = this.splegg.games.gamesForMap(map.getName());

        // Idle: no live game. A click spawns one on demand, so the sign
        // advertises JOIN.
        if (games.isEmpty()) {

            return this.splegg.getConfig().getString("Sings.Status.Join", "&2&lJOIN");

        }

        boolean anyJoinable = false;
        boolean anyStarting = false;
        int liveCount = 0;
        int endingCount = 0;
        for (Game g : games) {

            final Status st = g.getStatus();
            if (st == Status.LOBBY) {

                if (g.isStarting()) {

                    anyStarting = true;
                    if (g.getPlayers().size() < map.getSpawnCount()) {

                        anyJoinable = true;

                    }

                } else if (g.getPlayers().size() < map.getSpawnCount()) {

                    anyJoinable = true;

                }

            } else if (st == Status.INGAME) {

                liveCount++;

            } else if (st == Status.ENDING) {

                endingCount++;

            }

        }

        if (anyJoinable && anyStarting) {

            return this.splegg.getConfig().getString("Sings.Status.Starting", "&5&lSTARTING");

        }

        if (anyJoinable) {

            return this.splegg.getConfig().getString("Sings.Status.Join", "&2&lJOIN");

        }

        if (liveCount == games.size()) {

            return this.splegg.getConfig().getString("Sings.Status.Started", "&3&lLIVE");

        }

        if (endingCount == games.size()) {

            return this.splegg.getConfig().getString("Sings.Status.Ending", "&8&lENDING");

        }

        return this.splegg.getConfig().getString("Sings.Status.Full", "&4&lFULL");

    }

    private void setSign(String[] lines, Sign s) {

        for (int i = 0; i < lines.length; ++i) {

            final TextComponent signLineContainer = Utils.legacySerializerAnyCase(lines[i] != null ? lines[i] : "");
            s.line(i, signLineContainer);

        }

        s.update(false, false);

    }

}
