package config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import main.SpleggOG;
import managers.Game;
import managers.Status;

public class MapConfig {

    public FileConfiguration maps;
    public File f;

    public void setup() {

        this.f = new File(SpleggOG.getPlugin().getDataFolder(), "maps.yml");
        try {

            this.f.createNewFile();

        } catch (IOException error) {

            SpleggOG.getPlugin().getLogger().severe("An error occured while creating maps.yml.");

        }

        this.loadMaps();
        this.saveMaps();

        SpleggOG.getPlugin().maps.MAPS.clear();

        Iterator<?> enabledMapIterator = this.getEnabledMaps().iterator();
        while (enabledMapIterator.hasNext()) {

            String maps = (String) enabledMapIterator.next();
            SpleggOG.getPlugin().maps.addMap(maps);

            Map map = SpleggOG.getPlugin().maps.getMap(maps);
            Game game = new Game(SpleggOG.getPlugin(), map);
            SpleggOG.getPlugin().games.addGame(map.getName(), game);

            if (!map.isUsable(map)) {

                game.setStatus(Status.DISABLED);

            }

        }

    }

    private void loadMaps() {

        this.maps = YamlConfiguration.loadConfiguration(this.f);

    }

    public void saveMaps() {

        try {

            this.maps.save(this.f);

        } catch (IOException error) {

            SpleggOG.getPlugin().getLogger().severe("An error occured while saving maps.yml.");

        }

    }

    public List<String> getEnabledMaps() {

        return this.maps.getStringList("maps");

    }

    public void addSign(String map, String loc) {

        List<String> signs = this.maps.getStringList("Signs." + map + ".lobby");
        signs.add(loc);

        this.maps.set("Signs." + map + ".lobby", signs);
        this.saveMaps();

    }

    public void delSign(String map, String loc) {

        List<String> signs = this.maps.getStringList("Signs." + map + ".lobby");
        signs.remove(loc);

        this.maps.set("Signs." + map + ".lobby", signs);
        this.saveMaps();

    }

    public void addMap(String name) {

        List<String> maps = this.maps.getStringList("maps");
        maps.add(name);

        this.maps.set("maps", maps);
        this.saveMaps();

    }

    public void removeMap(String name) {

        List<String> maps = this.maps.getStringList("maps");
        maps.remove(name);

        this.maps.set("maps", maps);
        this.saveMaps();

    }

}