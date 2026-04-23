package utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import managers.Game;

public class UtilPlayer {

    Player player;
    String name;
    boolean alive;
    Game game;
    InvStore store;
    Location lastMainSmpLocation;

    public UtilPlayer(Player player) {

        this.player = player;
        this.game = null;
        this.name = player.getName();
        this.alive = false;
        this.store = new InvStore(player);
        this.lastMainSmpLocation = null;

    }

    public InvStore getStore() {

        return this.store;

    }

    public Player getPlayer() {

        return this.player;

    }

    public String getName() {

        return this.name;

    }

    public boolean isAlive() {

        return this.alive;

    }

    public void setAlive(boolean a) {

        this.alive = a;

    }

    public Game getGame() {

        return this.game;

    }

    public void setGame(Game game) {

        this.game = game;

    }

    public Location getLastMainSmpLocation() {

        return this.lastMainSmpLocation != null ? this.lastMainSmpLocation.clone() : null;

    }

    public void setLastMainSmpLocation(Location location) {

        this.lastMainSmpLocation = location != null ? location.clone() : null;

    }

}
