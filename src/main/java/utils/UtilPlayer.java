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
    // Where the player stood before joining, in any non-splegg world.
    Location preJoinLocation;

    public UtilPlayer(Player player) {

        this.player = player;
        this.game = null;
        this.name = player.getName();
        this.alive = false;
        this.store = new InvStore(player);
        this.preJoinLocation = null;

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

    public Location getPreJoinLocation() {

        return this.preJoinLocation != null ? this.preJoinLocation.clone() : null;

    }

    public void setPreJoinLocation(Location location) {

        this.preJoinLocation = location != null ? location.clone() : null;

    }

}
