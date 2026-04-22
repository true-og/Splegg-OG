package config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import main.SpleggOG;
import managers.Game;
import managers.Status;

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
        Game game = SpleggOG.getPlugin().games.getGame(name);
        if (game.getStatus() == Status.INGAME) {

            SpleggOG.getPlugin().game.stopGame(game, 0);

        }

        SpleggOG.getPlugin().games.GAMES.remove(m.getName());

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

        final ArrayList<Map> candidates = new ArrayList<>();
        final Iterator<Map> mapIterator = this.MAPS.values().iterator();
        while (mapIterator.hasNext()) {

            final Map map = mapIterator.next();
            final Game game = SpleggOG.getPlugin().games.getGame(map.getName());
            if (game == null) {

                continue;

            }

            if (!map.isUsable(map)) {

                continue;

            }

            if (game.getStatus() != Status.LOBBY) {

                continue;

            }

            candidates.add(map);

        }

        if (candidates.isEmpty()) {

            return null;

        }

        return candidates.get(new Random().nextInt(candidates.size()));

    }

}