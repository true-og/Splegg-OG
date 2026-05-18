package signs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * live game for the bound map. Idle (zero games) state advertises "click to
 * join" so a fresh game spawns on demand.
 */
public class LobbySign {

    SpleggOG splegg;
    Map map;

    public LobbySign(Map map, SpleggOG s) {

        this.splegg = s;
        this.map = map;

    }

    private static Collection<Material> signMaterials = new ArrayList<>();
    static {

        signMaterials.add(Material.ACACIA_SIGN);
        signMaterials.add(Material.DARK_OAK_SIGN);
        signMaterials.add(Material.OAK_SIGN);
        signMaterials.add(Material.BIRCH_SIGN);
        signMaterials.add(Material.SPRUCE_SIGN);
        signMaterials.add(Material.JUNGLE_SIGN);
        signMaterials.add(Material.CRIMSON_SIGN);
        signMaterials.add(Material.WARPED_SIGN);
        signMaterials.add(Material.ACACIA_WALL_SIGN);
        signMaterials.add(Material.DARK_OAK_WALL_SIGN);
        signMaterials.add(Material.OAK_WALL_SIGN);
        signMaterials.add(Material.BIRCH_WALL_SIGN);
        signMaterials.add(Material.SPRUCE_WALL_SIGN);
        signMaterials.add(Material.JUNGLE_WALL_SIGN);
        signMaterials.add(Material.CRIMSON_WALL_SIGN);
        signMaterials.add(Material.WARPED_WALL_SIGN);

    }

    public void create(Location location, final Map map) {

        String loc = LobbySignUtils.get().locationToString(location);
        this.splegg.maps.c.addSign(map.getName(), loc);

        if (this.map == null) {

            this.map = map;

        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.splegg, new Runnable() {

            public void run() {

                LobbySign.this.update(map, true);

            }

        }, 5L);

    }

    public void delete(Location location) {

        String loc = LobbySignUtils.get().locationToString(location);
        this.splegg.maps.c.delSign(this.map.getName(), loc);

        this.splegg.maps.c.saveMaps();
        this.map = null;

    }

    public void update(Map map, boolean force) {

        Iterator<?> var4 = this.splegg.maps.c.maps.getStringList("Signs." + map.getName() + ".lobby").iterator();
        while (var4.hasNext()) {

            String loc = (String) var4.next();
            Location location = LobbySignUtils.get().stringToLocation(loc);
            if (!signMaterials.contains(location.getBlock().getType())) {

                location.getBlock().setType(Material.OAK_WALL_SIGN);

            }

            Sign s = (Sign) location.getBlock().getState();

            String[] sign = renderLines(map);

            if (force) {

                String[] flashing = new String[] { splegg.getConfig().getString("Sings.Restarting.1"),
                        splegg.getConfig().getString("Sings.Restarting.2"),
                        splegg.getConfig().getString("Sings.Restarting.3").replaceAll("%map%", map.getName()),
                        splegg.getConfig().getString("Sings.Restarting.4") };
                this.setSign(flashing, s);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this.splegg, new SignDelay(sign, s), 40L);

            } else {

                this.setSign(sign, s);

            }

        }

    }

    private String[] renderLines(Map map) {

        String status = getFancyStatus(map);
        String players = getPlayers(map);

        return new String[] {
                splegg.getConfig().getString("Sings.Format.1").replaceAll("%status%", status)
                        .replaceAll("%map%", map.getName()).replaceAll("%count%/%maxcount%", players),
                splegg.getConfig().getString("Sings.Format.2").replaceAll("%status%", status)
                        .replaceAll("%map%", map.getName()).replaceAll("%count%/%maxcount%", players),
                splegg.getConfig().getString("Sings.Format.3").replaceAll("%map%", map.getName())
                        .replaceAll("%status%", status).replaceAll("%count%/%maxcount%", players),
                splegg.getConfig().getString("Sings.Format.4").replaceAll("%count%/%maxcount%", players)
                        .replaceAll("%status%", status).replaceAll("%map%", map.getName()) };

    }

    private String getPlayers(Map map) {

        if (!map.isUsable(map))
            return "";

        List<Game> games = splegg.games.gamesForMap(map.getName());
        int totalPlayers = 0;
        for (Game g : games)
            totalPlayers += g.getPlayers().size();

        int slotsPerGame = map.getSpawnCount();
        if (slotsPerGame <= 1)
            return "&5Players: &d" + totalPlayers;
        return "&5" + totalPlayers + "&8/&d" + slotsPerGame;

    }

    private String getFancyStatus(Map map) {

        if (!map.isUsable(map))
            return splegg.getConfig().getString("Sings.Status.Disabled");

        List<Game> games = splegg.games.gamesForMap(map.getName());

        // Idle: no live game. Treat as "click to join" so new game spawns on
        // demand. Falls back to the Join string from config.
        if (games.isEmpty())
            return splegg.getConfig().getString("Sings.Status.Join");

        boolean anyJoinable = false;
        boolean anyStarting = false;
        boolean anyInGame = false;
        for (Game g : games) {

            Status st = g.getStatus();
            if (st == Status.LOBBY) {

                if (g.isStarting())
                    anyStarting = true;
                else if (g.getPlayers().size() < map.getSpawnCount())
                    anyJoinable = true;

            } else if (st == Status.INGAME) {

                anyInGame = true;

            }

        }

        if (anyJoinable)
            return splegg.getConfig().getString("Sings.Status.Join");
        if (anyStarting)
            return splegg.getConfig().getString("Sings.Status.Starting", "&e[Starting]");
        if (anyInGame)
            return splegg.getConfig().getString("Sings.Status.Started");

        return splegg.getConfig().getString("Sings.Status.Join");

    }

    private void setSign(String[] lines, Sign s) {

        for (int i = 0; i < lines.length; ++i) {

            final TextComponent signLineContainer = Utils.legacySerializerAnyCase(lines[i] != null ? lines[i] : "");
            s.line(i, signLineContainer);

        }

        s.update();

    }

}
