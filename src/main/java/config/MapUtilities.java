package config;

import java.util.Collection;
import java.util.HashMap;

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

    // TODO: Use getRandomMap in map voting as an option.
    /*
     * private Map getRandomMap() {
     * 
     * ArrayList<Map> all = new ArrayList<Map>(); Iterator<?> var3 =
     * this.MAPS.values().iterator();
     * 
     * while(var3.hasNext()) {
     * 
     * Map map = (Map)var3.next();
     * 
     * if (map.isUsable()) {
     * 
     * all.add(map);
     * 
     * }
     * 
     * }
     * 
     * Object[] mapsa = all.toArray(); Map randomMap = (Map) mapsa[(new
     * Random()).nextInt(mapsa.length)];
     * 
     * return randomMap;
     * 
     * }
     */

}