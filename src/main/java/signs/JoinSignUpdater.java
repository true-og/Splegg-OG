package signs;

import org.bukkit.scheduler.BukkitRunnable;

import config.Map;
import main.SpleggOG;

/**
 * Redraws every registered join sign once a second, mirroring TheHerobrine-OG's
 * sign updater. Event-driven updates still happen for instant feedback; this
 * pass catches everything they miss (worlds loading late, aggregate status
 * drift, signs edited externally).
 */
public class JoinSignUpdater extends BukkitRunnable {

    private final SpleggOG splegg;

    public JoinSignUpdater(SpleggOG splegg) {

        this.splegg = splegg;

    }

    @Override
    public void run() {

        if (this.splegg.maps == null || this.splegg.maps.c == null) {

            return;

        }

        for (Map map : this.splegg.maps.getMaps()) {

            new LobbySign(map, this.splegg).update(map);

        }

    }

}
