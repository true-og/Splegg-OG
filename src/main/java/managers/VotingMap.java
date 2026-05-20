package managers;

import config.Map;

public class VotingMap {

    private final int id;
    private final Map map;
    private int votes;

    public VotingMap(int id, Map map) {

        this.id = id;
        this.map = map;
        this.votes = 0;

    }

    public int getId() {

        return this.id;

    }

    public Map getMap() {

        return this.map;

    }

    public int getVotes() {

        return this.votes;

    }

    public void incrementVotes() {

        this.votes++;

    }

    public void decrementVotes() {

        if (this.votes > 0) {

            this.votes--;

        }

    }

}
