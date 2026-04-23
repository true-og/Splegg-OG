package managers;

import java.util.ArrayList;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import config.Map;
import main.SpleggOG;
import net.kyori.adventure.title.Title;
import net.trueog.diamondbankog.DiamondBankException.EconomyDisabledException;
import net.trueog.utilitiesog.UtilitiesOG;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

public class GameManager {

    SpleggOG splegg;

    public GameManager() {

        splegg = SpleggOG.getPlugin();

    }

    public void startGame(Game game) {

        final Map map = game.getMap();
        SpleggOG.getPlugin().getLogger().info("New game commencing in map: " + map);
        game.startGameTimer();
        Bukkit.getScheduler().cancelTask(game.counter);
        game.status = Status.INGAME;
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

            sp.getPlayer().teleport(map.getSpawn(c));
            c++;
            sp.getPlayer().setLevel(0);
            sp.getPlayer().setExp(0.0F);
            sp.getPlayer().setGameMode(GameMode.ADVENTURE);
            sp.getPlayer().getInventory().clear();

        }

        game.getSign().update(map, false);

        splegg.chat.bc(splegg.getConfig().getString("Messages.InstructionsGame"), game);

    }

    public void stopGame(Game game, int r) {

        SpleggOG.getPlugin().getLogger().info("Commencing shutdown of: " + game.getMap().getName() + ".");

        game.status = Status.ENDING;
        game.stopGameTimer();
        game.time = 601;
        game.setStatus(Status.LOBBY);
        game.resetArena();
        game.data.clear();
        game.floor.clear();
        game.setStarting(false);

        final Iterator<?> playersInGame = new ArrayList<>(game.players.values()).iterator();
        while (playersInGame.hasNext()) {

            final SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
            final UtilPlayer u = sp.getUtilPlayer();
            game.leaveGame(u);

        }

        if (!splegg.disabling) {

            game.getSign().update(game.map, true);

        }

        game.players.clear();

        SpleggOG.getPlugin().getLogger().info("Map " + game.map.getName() + " was reset.");

    }

    public void finishGame(Game game, SpleggPlayer winner) {

        if (winner == null) {

            stopGame(game, 0);
            return;

        }

        final Player player = winner.getPlayer();
        final String mapName = game.getMap().getName();
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
