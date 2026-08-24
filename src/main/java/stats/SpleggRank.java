package stats;

// Hive Java Splegg rank ladder; The Eggspert is reserved for the top scorer.
public enum SpleggRank {

    FARMER("&7Farmer", 0), COOK("&fCook", 5000), CHEF("&eChef", 10000), MASTER("&6Master", 100000),
    HUMPTY_DUMPTY("&bHumpty Dumpty", 250000), RAMSAY("&cRamsay", 500000), HOT_N_SPICY("&4Hot'n'Spicy", 1000000),
    SPLEGG_HEAD("&aSplegg Head", 1200000), BACON("&dBacon", 1500000), SUNNYSIDE("&eSunnyside", 2000000),
    DELICIOUS("&2Delicious", 3000000), EASTER_BUNNY("&5Easter Bunny", 4000000), SCRAMBLED("&3Scrambled", 5000000),
    OLIVER("&6&lOliver", 7500000), EGGSPERT("&4&k# &r&c&lThe Eggspert", Integer.MAX_VALUE);

    private final String display;
    private final int lowBound;

    private SpleggRank(String display, int lowBound) {

        this.display = display;
        this.lowBound = lowBound;

    }

    public String getDisplay() {

        return display;

    }

    public int getLowBound() {

        return lowBound;

    }

    // The Eggspert needs the top score and the highest regular rank, so #1 still
    // climbs the ladder.
    public static int topPlayerGate() {

        int gate = 0;
        for (SpleggRank rank : values()) {

            if (rank != EGGSPERT) {

                gate = Math.max(gate, rank.getLowBound());

            }

        }

        return gate;

    }

    // Highest ladder rank whose threshold the points meet; never the reserved one.
    public static SpleggRank findRank(int points) {

        final SpleggRank[] ranks = values();
        for (int i = ranks.length - 1; i >= 0; i--) {

            if (ranks[i] == EGGSPERT) {

                continue;

            }

            if (points >= ranks[i].getLowBound()) {

                return ranks[i];

            }

        }

        return FARMER;

    }

}
