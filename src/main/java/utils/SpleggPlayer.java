package utils;

import org.bukkit.entity.Player;

public class SpleggPlayer {

	UtilPlayer u;
	int kills;
	int broken;

	public SpleggPlayer(UtilPlayer u) {

		this.u = u;
		this.kills = 0;
		this.broken = 0;

	}

	public UtilPlayer getUtilPlayer() {

		return this.u;

	}

	public Player getPlayer() {

		return this.u.getPlayer();

	}

	public int getKills() {

		return this.kills;

	}

	public void setKills(int i) {

		this.kills = i;

	}

	public int getBroken() {

		return this.broken;

	}

}