package config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import main.SpleggOG;

public class MapConfig {

    public FileConfiguration maps;
    public File f;

    public void setup() {

        final File dataFolder = SpleggOG.getPlugin().getDataFolder();
        if (!dataFolder.exists()) {

            dataFolder.mkdirs();

        }

        this.f = new File(dataFolder, "maps.yml");
        if (!this.f.exists()) {

            try {

                this.f.createNewFile();

            } catch (IOException error) {

                SpleggOG.getPlugin().getLogger()
                        .severe("An error occured while creating maps.yml: " + error.getMessage());

            }

        }

        this.loadMaps();
        this.saveMaps();

        SpleggOG.getPlugin().maps.MAPS.clear();

        Iterator<?> enabledMapIterator = this.getEnabledMaps().iterator();
        while (enabledMapIterator.hasNext()) {

            String maps = (String) enabledMapIterator.next();
            SpleggOG.getPlugin().maps.addMap(maps);

            // Maps are only templates now: a lobby copies one per match after the
            // vote, so no Game is tied to a map at boot.
            Map map = SpleggOG.getPlugin().maps.getMap(maps);
            map.isUsable(map); // populates the usable flag for status displays

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

    // Every key signs are stored under: lobby ids, "any", and map names left by
    // older versions (which now behave as "any").
    public List<String> getSignKeys() {

        final ConfigurationSection section = this.maps.getConfigurationSection("Signs");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));

    }

    public List<String> getSigns(String key) {

        return this.maps.getStringList("Signs." + key + ".lobby");

    }

    public void addSign(String key, String loc) {

        List<String> signs = this.maps.getStringList("Signs." + key + ".lobby");
        signs.add(loc);

        this.maps.set("Signs." + key + ".lobby", signs);
        this.saveMaps();

    }

    public void delSign(String key, String loc) {

        List<String> signs = this.maps.getStringList("Signs." + key + ".lobby");
        signs.remove(loc);

        this.maps.set("Signs." + key + ".lobby", signs.isEmpty() ? null : signs);
        if (signs.isEmpty()) {

            this.maps.set("Signs." + key, null);

        }

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
