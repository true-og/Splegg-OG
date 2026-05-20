package runnables;

import org.bukkit.Bukkit;

import managers.Game;
import managers.Status;

public class MapVotingRunnable implements Runnable {

    private int time;
    private final Game game;

    public MapVotingRunnable(Game game) {

        this.game = game;
        this.time = 0;

    }

    @Override
    public void run() {

        if (this.game.getStatus() != Status.LOBBY || !this.game.isVotingRunning()) {

            Bukkit.getScheduler().cancelTask(this.game.getVotingReminderTaskId());
            this.game.clearVotingReminderTaskId();
            return;

        }

        this.time++;
        if (this.time == this.game.getVotingReminderSeconds()) {

            this.game.sendVotingMessage(null);
            this.time = 0;

        }

    }

}
