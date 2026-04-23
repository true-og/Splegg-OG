package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

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

    SpleggOG splegg;
    String name;
    Map map;
    Status status;
    public HashMap<UUID, SpleggPlayer> players;
    HashSet<Location> floor;
    ArrayList<Rollback> data;
    private int lobbycount;
    int time;
    int y1;
    int y2;
    int small;
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
        this.status = Status.LOBBY;
        this.players = new HashMap<>();
        this.floor = new HashSet<>();
        this.data = new ArrayList<>();
        this.time = 601;
        this.lobbycount = 31;
        this.y1 = -64;
        this.y2 = -64;
        this.small = -64;

        this.setSign(new LobbySign(map, splegg));

        (new BukkitRunnable() {

            @Override
            public void run() {

                Game.this.getSign().update(map, true);

            }

        }).runTaskLater(splegg, 10L);

        this.setStarting(false);

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

    public HashSet<Location> getFloor() {

        return this.floor;

    }

    public ArrayList<Rollback> getDatas() {

        return this.data;

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

        return this.small;

    }

    public void joinGame(UtilPlayer playerWhoIsJoining) {

        final Player player = playerWhoIsJoining.getPlayer();
        if (splegg.isMainWorld(player.getWorld())) {

            playerWhoIsJoining.setLastMainSmpLocation(player.getLocation());

        }

        if (playerWhoIsJoining.getGame() != null) {

            Utils.spleggOGMessage(player, config.getString("Messages.AlreadyInGame"));

        } else if (splegg.isMainWorld(this.map.getWorldName()) || !splegg.isSpleggWorld(this.map.getWorldName())) {

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
                playerWhoIsJoining.setGame(this.splegg.games.getGame(this.name));
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
                playerWhoIsJoining.setGame(splegg.games.getGame(name));
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

            final Location mapLobby = this.map.getLobby();
            if (mapLobby != null && mapLobby.getWorld() != null) {

                return mapLobby;

            }

        }

        final Location globalQueueLobby = this.splegg.config.getLobby(null);
        if (globalQueueLobby != null && globalQueueLobby.getWorld() != null) {

            return globalQueueLobby;

        }

        if (this.map.getSpawnCount() > 0) {

            final Location firstSpawn = this.map.getSpawn(1);
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

    public boolean loadFloors() {

        this.floor.clear();
        this.data.clear();

        if (this.map.getFloors() <= 0) {

            return false;

        } else {

            for (int i = 1; i <= this.map.getFloors(); ++i) {

                final Location l1 = this.map.getFloor(i, "1");
                final Location l2 = this.map.getFloor(i, "2");

                final CuboidRegion sel = new CuboidRegion(
                        BlockVector3.at(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ()),
                        BlockVector3.at(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ()));

                final BlockVector3 min = sel.getMinimumPoint();
                final BlockVector3 max = sel.getMaximumPoint();

                final int minX = min.x();
                final int minY = min.y();
                this.y1 = minY;
                final int minZ = min.z();
                final int maxX = max.x();
                final int maxY = max.y();
                this.y2 = maxY;
                final int maxZ = max.z();
                this.small = Math.min(this.y1, this.y2);

                for (int x = minX; x <= maxX; ++x) {

                    for (int y = minY; y <= maxY; ++y) {

                        for (int z = minZ; z <= maxZ; ++z) {

                            final Location l = new Location(l1.getWorld(), (double) x, (double) y, (double) z);
                            this.floor.add(l);

                            final Block block = l.getWorld().getBlockAt(x, y, z);
                            this.data.add(new Rollback(l.getWorld().getName(), block.getType(), block.getBlockData(), x,
                                    y, z));

                        }

                    }

                }

            }

            return true;

        }

    }

    public void resetArena() {

        final Iterator<?> var2 = this.data.iterator();
        while (var2.hasNext()) {

            final Rollback d = (Rollback) var2.next();
            final Location l = new Location(Bukkit.getWorld(d.getWorld()), (double) d.getX(), (double) d.getY(),
                    (double) d.getZ());

            l.getBlock().setType(d.getPrevid());
            l.getBlock().setBlockData(d.getPrevdata());
            l.getBlock().getState().update();

        }

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
