package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import config.Map;
import events.Listeners;
import main.SpleggOG;
import net.trueog.diamondbankog.DiamondBankException.EconomyDisabledException;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import net.trueog.utilitiesog.UtilitiesOG;
import runnables.GameTime;
import runnables.LobbyCountdown;
import signs.LobbySign;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

public class Game {

    private static final AtomicInteger GAME_ID_SEQUENCE = new AtomicInteger(1);

    SpleggOG splegg;
    String name;
    String gameId;
    Map map;
    Status status;
    public HashMap<UUID, SpleggPlayer> players;
    private World gameWorld;
    private int lobbycount;
    int time;
    int counter;
    int timer;
    boolean starting;
    LobbySign sign;
    DiamondBankAPIJava diamondBankAPI;

    // Enable the conversion of text from config.yml to objects.
    public FileConfiguration config = SpleggOG.getPlugin().getConfig();

    public Game(SpleggOG splegg, final Map map) {

        this.splegg = splegg;
        this.map = map;
        this.diamondBankAPI = splegg.getDiamondBankAPI();
        this.name = map.getName();
        this.gameId = String.valueOf(GAME_ID_SEQUENCE.getAndIncrement());
        this.status = Status.LOBBY;
        this.players = new HashMap<>();
        this.time = 601;
        this.lobbycount = 31;

        this.setSign(new LobbySign(map, splegg));

        (new BukkitRunnable() {

            @Override
            public void run() {

                Game.this.getSign().update(map, true);

            }

        }).runTaskLater(splegg, 10L);

        this.setStarting(false);

    }

    public String getGameId() {

        return this.gameId;

    }

    public World getGameWorld() {

        return this.gameWorld;

    }

    public void setGameWorld(World world) {

        this.gameWorld = world;

    }

    public void startGameTimer() {

        final int grace = config.getInt("Options.GraceTime");
        this.splegg.chat.bc(config.getString("Messages.GraceTimeStart").replaceAll("%grace%", String.valueOf(grace)),
                this);

        (new BukkitRunnable() {

            @Override
            public void run() {

                Game.this.splegg.chat.bc(config.getString("Messages.GraceTimeFinish"), Game.this);

                final Iterator<?> PlayersInGame = players.values().iterator();
                while (PlayersInGame.hasNext()) {

                    final SpleggPlayer sp = (SpleggPlayer) PlayersInGame.next();
                    final UUID playerId = sp.getPlayer().getUniqueId();
                    sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);

                    final Material selectedShovel = Listeners.getSelectedShovelMaterial(playerId);
                    final String selectedShovelConfigPath = Listeners.getSelectedShovelConfigPath(playerId);

                    sp.getPlayer().getInventory().setItem(0, Utils.getItem(selectedShovel,
                            Utils.legacySerializerAnyCase(
                                    splegg.getConfig().getString(selectedShovelConfigPath + ".Name")).content(),
                            Utils.legacySerializerAnyCase(
                                    splegg.getConfig().getString(selectedShovelConfigPath + ".Lore")).content()));
                    sp.getPlayer().updateInventory();

                    Listeners.finalizePreGameShovelState(playerId);

                }

                Game.this.timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Game.this.splegg,
                        new GameTime(Game.this.splegg, Game.this), 0L, 20L);

            }

        }).runTaskLater(this.splegg, (long) (20 * grace));

    }

    public void stopGameTimer() {

        Bukkit.getScheduler().cancelTask(this.timer);

    }

    public int getCounterID() {

        return this.counter;

    }

    public HashMap<UUID, SpleggPlayer> getPlayers() {

        return this.players;

    }

    public ArrayList<SpleggPlayer> getSp() {

        final ArrayList<SpleggPlayer> sp = new ArrayList<>();
        final Iterator<?> var3 = this.players.values().iterator();
        while (var3.hasNext()) {

            final SpleggPlayer sps = (SpleggPlayer) var3.next();
            sp.add(sps);

        }

        return sp;

    }

    public SpleggPlayer getPlayer(Player player) {

        return this.players.get(player.getUniqueId());

    }

    public Status getStatus() {

        return this.status;

    }

    public void setStatus(Status status) {

        this.status = status;

    }

    public void setMap(Map map) {

        this.map = map;

    }

    public Map getMap() {

        return this.map;

    }

    public int getLowestPossible() {

        // The lowest Y the floor extends to. Computed once from the map config.
        // No mutable state needed -- the per-game world is reset by being
        // deleted between games.
        int small = Integer.MAX_VALUE;
        for (int i = 1; i <= map.getFloors(); i++) {

            small = Math.min(small, Math.min(config.getInt("Floors." + i + ".p1.y", small),
                    config.getInt("Floors." + i + ".p2.y", small)));

        }

        return small == Integer.MAX_VALUE ? -64 : small;

    }

    public void joinGame(UtilPlayer playerWhoIsJoining) {

        final Player player = playerWhoIsJoining.getPlayer();
        if (splegg.isMainWorld(player.getWorld())) {

            playerWhoIsJoining.setLastMainSmpLocation(player.getLocation());

        }

        if (playerWhoIsJoining.getGame() != null) {

            Utils.spleggOGMessage(player, config.getString("Messages.AlreadyInGame"));

        } else if (this.gameWorld == null || splegg.isMainWorld(this.gameWorld)
                || !splegg.isSpleggWorld(this.gameWorld))
        {

            Utils.spleggOGMessage(player, config.getString("Messages.NotInSpleggWorld"));

        } else if (this.players.containsKey(player.getUniqueId())) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.AlreadyInLobby"));

        } else if (this.status == Status.LOBBY) {

            final int size = this.players.size();
            // Makes maximum players in a game the same as the amount of spawn points that
            // are set for a given map.
            final int max = this.map.getSpawnCount();
            final SpleggPlayer sp;
            if (max == 1) {

                sp = new SpleggPlayer(playerWhoIsJoining);
                playerWhoIsJoining.setAlive(true);
                playerWhoIsJoining.getStore().save();
                Listeners.launchEggs.add(player.getUniqueId());
                this.players.put(player.getUniqueId(), sp);
                playerWhoIsJoining.setGame(this);
                teleportToQueueLobby(player);

                preparePlayerForLobby(player);
                Listeners.manager.add(player.getUniqueId());
                Listeners.shopmanager.add(player.getUniqueId());

                this.splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("%player%", player.getName())
                        .replaceAll("%count%", String.valueOf(this.players.size()))
                        .replaceAll("%maxcount%", String.valueOf(max)), playerWhoIsJoining.getGame());

                if (this.players.size() >= config.getInt("Options.AutoStartPlayers") && !this.isStarting()) {

                    this.startCountdown();
                    this.setStarting(true);

                }

            } else if (size >= max && !player.hasPermission("splegg.joinfull")) {

                Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.VIPPrivilege"));

            } else {

                if (size >= max) {

                    Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.VIPJoinGame"));

                }

                sp = new SpleggPlayer(playerWhoIsJoining);
                playerWhoIsJoining.setAlive(true);
                playerWhoIsJoining.getStore().save();
                Listeners.launchEggs.add(player.getUniqueId());

                players.put(player.getUniqueId(), sp);
                playerWhoIsJoining.setGame(this);
                teleportToQueueLobby(player);

                preparePlayerForLobby(player);

                Listeners.manager.add(player.getUniqueId());
                Listeners.shopmanager.add(player.getUniqueId());

                splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("%player%", player.getName())
                        .replaceAll("%count%", String.valueOf(this.players.size()))
                        .replaceAll("%maxcount%", String.valueOf(max)), playerWhoIsJoining.getGame());

                if (players.size() >= config.getInt("Options.AutoStartPlayers") && !this.isStarting()) {

                    startCountdown();
                    setStarting(true);

                }

            }

            getSign().update(this.map, false);
            LobbyScoreboard.refreshGame(this);

        } else if (this.status == Status.DISABLED) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.Mapdisabled"));

        }

    }

    private void preparePlayerForLobby(Player player) {

        player.closeInventory();
        player.clearActiveItem();
        player.getInventory().clear();
        player.updateInventory();
        player.setFireTicks(0);
        player.setHealth(20.0D);
        player.setFallDistance(0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setGameMode(GameMode.ADVENTURE);

        final Iterator<?> activePotionEffects = player.getActivePotionEffects().iterator();
        while (activePotionEffects.hasNext()) {

            final PotionEffect effect = (PotionEffect) activePotionEffects.next();
            player.removePotionEffect(effect.getType());

        }

        setLobbyInv(player);
        LobbyScoreboard.attach(player, this);

    }

    public Location getQueueLobbyLocation() {

        if (this.map.lobbySet()) {

            final Location mapLobby = this.map.getLobbyIn(this.gameWorld);
            if (mapLobby != null && mapLobby.getWorld() != null) {

                return mapLobby;

            }

        }

        final Location globalQueueLobby = this.splegg.config.getLobby(null);
        if (globalQueueLobby != null && globalQueueLobby.getWorld() != null) {

            return globalQueueLobby;

        }

        if (this.map.getSpawnCount() > 0) {

            final Location firstSpawn = this.map.getSpawnIn(this.gameWorld, 1);
            if (firstSpawn != null && firstSpawn.getWorld() != null) {

                return firstSpawn;

            }

        }

        return null;

    }

    private void teleportToQueueLobby(Player player) {

        final Location queueLobby = getQueueLobbyLocation();
        if (queueLobby != null && queueLobby.getWorld() != null) {

            player.teleport(queueLobby);

        }

    }

    private void setLobbyInv(Player player) {

        final int[] slotsDeclaredInConfigFile = new int[42];
        for (int i = 0; i < slotsDeclaredInConfigFile.length; i++) {

            setInventorySlotItem(player, i);

        }

    }

    private void setInventorySlotItem(Player player, int slotNumber) {

        if (slotNumber == splegg.getConfig().getInt("Shop.Slot")) {

            player.getInventory().setItem(slotNumber,
                    Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Shop.Item")),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shop.Name")).content(),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shop.Lore")).content()));

        } else if (slotNumber == splegg.getConfig().getInt("Guide.Slot")) {

            player.getInventory().setItem(slotNumber,
                    Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Guide.Item")),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Guide.Name")).content(),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Guide.Lore")).content()));

        } else if (slotNumber == splegg.getConfig().getInt("Cosmetics.Slot")) {

            player.getInventory().setItem(slotNumber,
                    Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Cosmetics.Item")),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Cosmetics.Name")).content(),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Cosmetics.Lore")).content()));

        } else if (slotNumber == splegg.getConfig().getInt("Leave.Slot")) {

            player.getInventory().setItem(slotNumber,
                    Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Leave.Item")),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Leave.Name")).content(),
                            Utils.legacySerializerAnyCase(splegg.getConfig().getString("Leave.Lore")).content()));

        } else {

            player.getInventory().clear(slotNumber);

        }

    }

    public void startCountdown() {

        Bukkit.getScheduler().cancelTask(counter);
        if (this.status != Status.LOBBY) {

            return;

        }

        this.lobbycount = config.getInt("Options.Timer");
        final Iterator<?> playersInGame = this.players.values().iterator();
        while (playersInGame.hasNext()) {

            final SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
            sp.getPlayer().setLevel(this.getLobbyCount());

        }

        counter = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg,
                new LobbyCountdown(splegg, this, this.getLobbyCount()), 0L, 20L);

    }

    public void leaveGame(UtilPlayer u) {

        final Player player = u.getPlayer();
        final Game game = u.getGame();
        final UUID playerId = player.getUniqueId();
        final SpleggPlayer spleggPlayer = this.players.get(playerId);
        final int brokenBlocks = spleggPlayer != null ? spleggPlayer.getBroken() : 0;
        if (game != null) {

            // Tell player that left about their current state.
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.IndividualLeaveGame")
                    .replaceAll("%map%", u.getGame().getMap().getName()));
            Utils.spleggOGMessage(player,
                    config.getString("Messages.Youbrokeblocks").replaceAll("%broke%", String.valueOf(brokenBlocks)));

            this.players.remove(playerId);
            Listeners.manager.remove(playerId);
            Listeners.shopmanager.remove(playerId);
            Listeners.woodspade.remove(playerId);
            Listeners.stonespade.remove(playerId);
            Listeners.goldspade.remove(playerId);
            Listeners.diamondspade.remove(playerId);
            Listeners.netheritespade.remove(playerId);
            Listeners.launchEggs.remove(playerId);
            Listeners.moneymanager.remove(playerId);

            LobbyScoreboard.detach(player);
            LobbyScoreboard.refreshGame(game);

            final Location returnLocation = u.getLastMainSmpLocation();
            if (returnLocation != null && returnLocation.getWorld() != null) {

                player.teleport(returnLocation);

            } else {

                final List<String> mainWorlds = this.splegg.getMainWorlds();
                if (!mainWorlds.isEmpty()) {

                    final org.bukkit.World mainWorld = Bukkit.getWorld(mainWorlds.get(0));
                    if (mainWorld != null) {

                        player.teleport(mainWorld.getSpawnLocation());

                    }

                }

            }

            u.setGame((Game) null);
            u.setAlive(false);
            player.setHealth(20.0D);
            player.setFallDistance(0);

        }

        String playerWhoOnlyNeedsIndividualLeaveGameMessage = "";

        playerWhoOnlyNeedsIndividualLeaveGameMessage = player.getName();
        u.getStore().load();
        u.getStore().reset();

        if (game != null) {

            for (SpleggPlayer remainingPlayer : this.players.values()) {

                final Player remaining = remainingPlayer.getPlayer();
                if (!remaining.getName().equals(playerWhoOnlyNeedsIndividualLeaveGameMessage)) {

                    // Tell the rest of the people in the lobby that the player has left.
                    Utils.spleggOGMessage(remaining,
                            config.getString("Messages.LeaveGame").replaceAll("%player%", player.getName())
                                    .replaceAll("%count%", String.valueOf(this.players.size()))
                                    .replaceAll("%maxcount%", String.valueOf(this.map.getSpawnCount())));

                }

                if (game.getStatus() == Status.INGAME && Listeners.moneymanager.contains(remaining.getUniqueId())
                        && diamondBankAPI != null)
                {

                    final int rewardInDiamonds = splegg.getConfig().getInt("Money.KillPlayer");
                    if (rewardInDiamonds <= 0) {

                        continue;

                    }

                    final long rewardInShards = diamondBankAPI.diamondsToShards((float) rewardInDiamonds);

                    try {

                        diamondBankAPI.addToPlayerBankShards(remaining.getUniqueId(), rewardInShards,
                                "Player " + remaining.getName() + " earned Diamonds for a kill in Splegg.",
                                "Plugin: Splegg-OG");

                        remaining.sendMessage(
                                Utils.legacySerializerAnyCase("&BYou received " + rewardInDiamonds + " &BDiamonds!")
                                        .content());

                    } catch (EconomyDisabledException economyDisabledException) {

                        UtilitiesOG.trueogMessage(remaining,
                                "&cERROR: The Diamond economy is currently unavailable. Your kill reward could not be paid out.");

                    }

                }

            }

        }

        if (!this.splegg.disabling) {

            this.getSign().update(this.map, false);

        }

    }

    public int getLobbyCount() {

        return this.lobbycount;

    }

    public int getCount() {

        return this.time;

    }

    public int tickTime() {

        if (this.time > 0) {

            this.time--;

        }

        return this.time;

    }

    /**
     * Pre-game world-state setup. No-op now that each game owns a fresh world copy
     * from {@link GameWorldManager} -- the world starts in template state and is
     * deleted when the game ends, so block-level rollback is not needed.
     */
    public boolean loadFloors() {

        return this.map.getFloors() > 0;

    }

    /**
     * Returns true when the supplied {@link Location} lies inside any of this
     * game's floor cuboids (per the map config), rebased onto the per-game world
     * copy. Used by the egg-impact handler to decide if an egg should vaporize a
     * block.
     */
    public boolean isInsideFloor(Location target) {

        if (target == null)
            return false;
        if (this.gameWorld == null)
            return false;
        if (target.getWorld() == null || !this.gameWorld.equals(target.getWorld()))
            return false;
        int tx = target.getBlockX();
        int ty = target.getBlockY();
        int tz = target.getBlockZ();
        for (int i = 1; i <= map.getFloors(); i++) {

            int p1x = config.getInt("Floors." + i + ".p1.x");
            int p1y = config.getInt("Floors." + i + ".p1.y");
            int p1z = config.getInt("Floors." + i + ".p1.z");
            int p2x = config.getInt("Floors." + i + ".p2.x");
            int p2y = config.getInt("Floors." + i + ".p2.y");
            int p2z = config.getInt("Floors." + i + ".p2.z");
            int minX = Math.min(p1x, p2x), maxX = Math.max(p1x, p2x);
            int minY = Math.min(p1y, p2y), maxY = Math.max(p1y, p2y);
            int minZ = Math.min(p1z, p2z), maxZ = Math.max(p1z, p2z);
            if (tx >= minX && tx <= maxX && ty >= minY && ty <= maxY && tz >= minZ && tz <= maxZ)
                return true;

        }

        return false;

    }

    /**
     * Post-game world-state reset. World is deleted by {@link GameManager#stopGame}
     * via {@link GameWorldManager#cleanupWorld}, so no block-level reset runs.
     */
    public void resetArena() {

        // intentional: world deletion handles state reset

    }

    public boolean isStarting() {

        return this.starting;

    }

    public void setStarting(boolean starting) {

        this.starting = starting;

    }

    public LobbySign getSign() {

        return this.sign;

    }

    public void setSign(LobbySign sign) {

        this.sign = sign;

    }

    public void setLobbyCount(int lobbycount) {

        this.lobbycount = lobbycount;

    }

}
