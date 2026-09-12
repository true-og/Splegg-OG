package managers;

import java.util.ArrayList;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import config.Map;
import main.SpleggOG;
import net.kyori.adventure.title.Title;
import net.trueog.diamondbankog.DiamondBankException.EconomyDisabledException;
import net.trueog.utilitiesog.UtilitiesOG;
import stats.SpleggStats;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

public class GameManager {

    SpleggOG splegg;

    public GameManager() {

        splegg = SpleggOG.getPlugin();

    }

    // Moves the lobby into its arena. Returns false, with the lobby left waiting,
    // when no map could be loaded.
    public boolean startGame(Game game) {

        if (game.getStatus() != Status.LOBBY) {

            return false;

        }

        if (!game.prepareArena()) {

            splegg.chat.bc("&cNo playable map could be loaded, so the match cannot start.", game);
            return false;

        }

        final Map map = game.getMap();
        SpleggOG.getPlugin().getLogger()
                .info("Lobby " + game.getLobbyId() + " is starting a match on map " + map.getName() + ".");
        if (game.counter != -1) {

            org.bukkit.Bukkit.getScheduler().cancelTask(game.counter);
            game.counter = -1;

        }

        game.stopVoting();
        game.status = Status.INGAME;
        game.startGameTimer();
        game.time = 901;
        game.setLobbyCount(31);
        int c = 1;
        game.loadFloors();

        final Iterator<?> playersInGame = game.players.values().iterator();
        SpleggPlayer sp;
        while (playersInGame.hasNext()) {

            sp = (SpleggPlayer) playersInGame.next();

            sp.getPlayer().setLevel(0);
            sp.getUtilPlayer().setAlive(true);

            if (c > map.getSpawnCount()) {

                c = 1;

            }

            sp.getPlayer().teleport(map.getSpawnIn(game.getGameWorld(), c));
            c++;
            sp.getPlayer().setLevel(0);
            sp.getPlayer().setExp(0.0F);
            sp.getPlayer().setGameMode(GameMode.ADVENTURE);
            sp.getPlayer().getInventory().clear();

        }

        game.updateSigns();
        LobbyScoreboard.refreshGame(game);

        splegg.chat.bc(splegg.getConfig().getString("Messages.InstructionsGame"), game);
        return true;

    }

    // Ends whatever the lobby is doing, sends everyone home, deletes the arena
    // copy and puts the lobby back to waiting in its hub.
    public void stopGame(Game game, int r) {

        SpleggOG.getPlugin().getLogger().info("Lobby " + game.getLobbyId() + " is ending"
                + (game.getMap() != null ? " its match on map " + game.getMap().getName() : "") + ".");

        game.status = Status.ENDING;
        game.stopVoting();
        game.stopGameTimer();
        game.setStarting(false);

        final Iterator<?> playersInGame = new ArrayList<>(game.players.values()).iterator();
        while (playersInGame.hasNext()) {

            final SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
            final UtilPlayer u = sp.getUtilPlayer();
            game.leaveGame(u);

        }

        game.players.clear();

        // Delete the per-game world copy. The lobby itself stays registered and
        // waits in its hub for the next match.
        SpleggOG.getPlugin().getGameWorldManager().cleanupWorld(game);
        game.resetToLobby();

        if (!splegg.disabling) {

            game.updateSigns();

        }

        SpleggOG.getPlugin().getLogger().info("Lobby " + game.getLobbyId() + " is back to waiting.");

    }

    public void finishGame(Game game, SpleggPlayer winner) {

        if (winner == null) {

            stopGame(game, 0);
            return;

        }

        final Player player = winner.getPlayer();
        final String mapName = game.getMapDisplayName();
        final String winnerBroadcast = splegg.getConfig().getString("Messages.WinnerGame",
                "&a%player% won Splegg on map &e%map%&a!");
        final String winnerTitle = splegg.getConfig().getString("Messages.WinnerTitle", "&a&lVICTORY!");
        final String winnerSubtitle = splegg.getConfig().getString("Messages.WinnerSubtitle",
                "&eYou won on &6%map%&e!");
        final String youWonMessage = splegg.getConfig().getString("Messages.YouWonGame",
                "&aYou won the match on &e%map%&a!");

        splegg.chat.bc(winnerBroadcast.replaceAll("%player%", player.getName()).replaceAll("%map%", mapName));
        player.showTitle(Title.title(
                Utils.legacySerializerAnyCase(
                        winnerTitle.replaceAll("%player%", player.getName()).replaceAll("%map%", mapName)),
                Utils.legacySerializerAnyCase(
                        winnerSubtitle.replaceAll("%player%", player.getName()).replaceAll("%map%", mapName)),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(600))));
        Utils.spleggOGMessage(player, youWonMessage.replaceAll("%map%", mapName));
        rewardWinner(player);
        SpleggStats.get().addPoints(player, SpleggStats.winPoints());

        stopGame(game, 1);

    }

    public String getDigitTime(int count) {

        final int minutes = count / 60;
        final int seconds = count % 60;

        final String disMinu = (minutes < 10 ? "0" : "") + minutes;
        final String disSec = (seconds < 10 ? "0" : "") + seconds;
        return disMinu + ":" + disSec;

    }

    public void ingameTimer(int count, HashMap<java.util.UUID, SpleggPlayer> players) {

        final Iterator<?> playersInGame = players.values().iterator();
        while (playersInGame.hasNext()) {

            final SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
            Utils.spleggOGMessage(sp.getPlayer(), ("&6Splegg is ending in... " + splegg.game.getDigitTime(count)));

        }

    }

    private void rewardWinner(Player winner) {

        if (splegg.getDiamondBankAPI() == null) {

            return;

        }

        final int rewardInDiamonds = splegg.getConfig().getInt("Money.FinishGame");
        if (rewardInDiamonds <= 0) {

            return;

        }

        final long rewardInShards = splegg.getDiamondBankAPI().diamondsToShards((float) rewardInDiamonds);

        try {

            splegg.getDiamondBankAPI().addToPlayerBankShards(winner.getUniqueId(), rewardInShards,
                    "Player " + winner.getName() + " earned Diamonds for winning Splegg.", "Plugin: Splegg-OG");

            final String rewardMessage = splegg.getConfig().getString("Messages.FinishReward",
                    "&BYou received %reward% &BDiamonds for winning!");
            winner.sendMessage(Utils
                    .legacySerializerAnyCase(rewardMessage.replaceAll("%reward%", String.valueOf(rewardInDiamonds)))
                    .content());

        } catch (EconomyDisabledException economyDisabledException) {

            UtilitiesOG.trueogMessage(winner,
                    "&cERROR: The Diamond economy is currently unavailable. Your win reward could not be paid out.");

        }

    }

}
