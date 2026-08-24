package stats;

import org.bukkit.entity.Player;

import net.trueog.utilitiesog.UtilitiesOG;

// Registers the sp_score and sp_rank MiniPlaceholders through Utilities-OG.
public final class SpleggPlaceholders {

    private SpleggPlaceholders() {

    }

    public static void register() {

        UtilitiesOG.registerAudiencePlaceholder("sp_score",
                (Player player) -> String.valueOf(SpleggStats.get().getPoints(player.getUniqueId())));
        UtilitiesOG.registerAudiencePlaceholder("sp_rank",
                (Player player) -> SpleggStats.get().getRank(player.getUniqueId()).getDisplay());

    }

}
