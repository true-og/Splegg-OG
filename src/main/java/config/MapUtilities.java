package config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import main.SpleggOG;
import managers.Game;

public class MapUtilities {

    public MapConfig c = new MapConfig();
    public HashMap<String, Map> MAPS = new HashMap<String, Map>();

    public void addMap(String name) {

        Map map = new Map(SpleggOG.getPlugin(), name);
        this.MAPS.put(name, map);
        map.load();

    }

    public void deleteMap(String name) {

        Map m = this.getMap(name);
        // Stop every active game for this map -- N concurrent games allowed.
        for (Game game : new ArrayList<>(SpleggOG.getPlugin().games.gamesForMap(name)))
            SpleggOG.getPlugin().game.stopGame(game, 0);

        this.MAPS.remove(name, m);
        this.c.removeMap(name);
        m.delete();

    }

    public boolean mapExists(String name) {

        return this.MAPS.containsKey(name);

    }

    public Collection<Map> getMaps() {

        return this.MAPS.values();

    }

    public Map getMap(String name) {

        return (Map) this.MAPS.get(name);

    }

    // Returns a random usable (playable) map in LOBBY status, or null when none
    // qualify.
    // Used by /splegg random and will back in-lobby map voting once that ships.
    public Map getRandomMap() {

        // With on-demand games, any usable map is a valid candidate -- a new
        // game can always be spun up when none are joinable.
        final ArrayList<Map> candidates = new ArrayList<>();
        final Iterator<Map> mapIterator = this.MAPS.values().iterator();
        while (mapIterator.hasNext()) {

            final Map map = mapIterator.next();
            if (map.isUsable(map))
                candidates.add(map);

        }

        if (candidates.isEmpty()) {

            return null;

        }

        return candidates.get(new Random().nextInt(candidates.size()));

    }

}