package signs;

import org.bukkit.scheduler.BukkitRunnable;

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

    // Immediate redraw, used once the lobbies come online.
    public static void redrawNow(SpleggOG splegg) {

        LobbySign.updateAll(splegg);

    }

    @Override
    public void run() {

        LobbySign.updateAll(this.splegg);

    }

}
