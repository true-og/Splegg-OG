package managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.trueog.diamondbankog.DiamondBankException.EconomyDisabledException;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import net.trueog.utilitiesog.UtilitiesOG;
import runnables.GameTime;
import runnables.LobbyCountdown;
import runnables.MapVotingRunnable;
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
    private LinkedHashMap<Integer, VotingMap> votingMaps;
    private HashMap<UUID, Integer> playerVotes;
    private boolean votingRunning;
    private boolean votingClosed;
    private int votingReminderTask;

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
        this.votingMaps = new LinkedHashMap<>();
        this.playerVotes = new HashMap<>();
        this.votingRunning = false;
        this.votingClosed = false;
        this.votingReminderTask = -1;
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
        this.name = map != null ? map.getName() : null;
        this.setSign(map != null ? new LobbySign(map, this.splegg) : null);

    }

    public Map getMap() {

        return this.map;

    }

    public java.util.Map<Integer, VotingMap> getVotingMaps() {

        return Collections.unmodifiableMap(this.votingMaps);

    }

    public boolean isVotingRunning() {

        return this.votingRunning;

    }

    public int getVotingReminderTaskId() {

        return this.votingReminderTask;

    }

    public void clearVotingReminderTaskId() {

        this.votingReminderTask = -1;

    }

    public int getVotingReminderSeconds() {

        return Math.max(1, this.config.getInt("Options.VotingReminder", 30));

    }

    private int getVotingMapCount() {

        return Math.max(1, this.config.getInt("Options.VotingMaps", 2));

    }

    private int getEndVotingAt() {

        return Math.max(0, this.config.getInt("Options.EndVotingAt", 10));

    }

    private void ensureVotingReady() {

        if (this.status != Status.LOBBY) {

            return;

        }

        if (this.votingClosed) {

            return;

        }

        if (this.votingMaps.isEmpty()) {

            pickVotingMaps();

        }

        if (!this.votingMaps.isEmpty()) {

            this.votingRunning = true;
            startVotingReminder();

        }

    }

    private void pickVotingMaps() {

        this.votingMaps.clear();
        this.playerVotes.clear();
        this.votingClosed = false;

        final List<Map> candidates = new ArrayList<>();
        if (this.map != null && this.map.isUsable(this.map)) {

            candidates.add(this.map);

        }

        final List<Map> remaining = new ArrayList<>();
        for (Map candidate : this.splegg.maps.getMaps()) {

            if (candidate == null || !candidate.isUsable(candidate)) {

                continue;

            }

            if (this.map != null && candidate.getName().equals(this.map.getName())) {

                continue;

            }

            remaining.add(candidate);

        }

        Collections.shuffle(remaining);
        for (Map candidate : remaining) {

            if (candidates.size() >= getVotingMapCount()) {

                break;

            }

            candidates.add(candidate);

        }

        int id = 1;
        for (Map candidate : candidates) {

            this.votingMaps.put(id, new VotingMap(id, candidate));
            id++;

        }

        this.votingRunning = !this.votingMaps.isEmpty();

    }

    private void startVotingReminder() {

        if (this.votingReminderTask != -1) {

            return;

        }

        this.votingReminderTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg,
                new MapVotingRunnable(this), 0L, 20L);

    }

    public void stopVoting() {

        this.votingRunning = false;
        if (this.votingReminderTask != -1) {

            Bukkit.getScheduler().cancelTask(this.votingReminderTask);
            this.votingReminderTask = -1;

        }

    }

    public void resetVoting() {

        stopVoting();
        this.votingClosed = false;
        this.votingMaps.clear();
        this.playerVotes.clear();

        if (this.players.isEmpty()) {

            return;

        }

        ensureVotingReady();
        for (SpleggPlayer spleggPlayer : this.players.values()) {

            final Player player = spleggPlayer.getPlayer();
            this.playerVotes.put(player.getUniqueId(), 0);
            sendVotingMessage(player);

        }

    }

    private void registerVotingPlayer(Player player) {

        if (this.votingClosed) {

            return;

        }

        ensureVotingReady();
        if (this.votingMaps.isEmpty()) {

            return;

        }

        this.playerVotes.put(player.getUniqueId(), 0);
        sendVotingMessage(player);

    }

    private void removeVotingPlayer(Player player) {

        final Integer previousVote = this.playerVotes.remove(player.getUniqueId());
        if (previousVote != null && previousVote != 0) {

            final VotingMap previousMap = this.votingMaps.get(previousVote);
            if (previousMap != null) {

                previousMap.decrementVotes();

            }

        }

        if (this.players.isEmpty() && this.votingReminderTask != -1) {

            Bukkit.getScheduler().cancelTask(this.votingReminderTask);
            this.votingReminderTask = -1;

        }

    }

    public void sendVotingMessage(Player player) {

        final ArrayList<Player> toSend = new ArrayList<>();
        if (player == null) {

            for (SpleggPlayer spleggPlayer : this.players.values()) {

                toSend.add(spleggPlayer.getPlayer());

            }

        } else {

            toSend.add(player);

        }

        for (Player recipient : toSend) {

            Utils.spleggOGMessage(recipient, "&6Vote for a map with /v #.");
            Utils.spleggOGMessage(recipient, "&6Map choices up for voting:");
            for (java.util.Map.Entry<Integer, VotingMap> entry : this.votingMaps.entrySet()) {

                final int id = entry.getKey();
                final VotingMap votingMap = entry.getValue();
                final String mapName = votingMap.getMap().getName();
                final TextComponent textComponent = Utils
                        .legacySerializerAnyCase(Utils.prefix + "&6&l" + id + ". &6" + mapName + " (&b"
                                + votingMap.getVotes() + "&6 votes)")
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Utils.legacySerializerAnyCase("&6Click here to vote for &b" + mapName)))
                        .clickEvent(ClickEvent.runCommand("/v " + id));
                recipient.sendMessage(textComponent);

            }

            recipient.sendMessage(" ");

        }

    }

    public void vote(Player player, int mapId) {

        if (!this.votingMaps.containsKey(mapId)) {

            Utils.spleggOGMessage(player, "&cInvalid map!");
            sendVotingMessage(player);
            return;

        }

        final UUID playerId = player.getUniqueId();
        final int previousVote = this.playerVotes.getOrDefault(playerId, 0);
        if (previousVote == mapId) {

            Utils.spleggOGMessage(player, "&cYou have already voted for this map!");
            return;

        }

        if (previousVote != 0) {

            final VotingMap previousMap = this.votingMaps.get(previousVote);
            if (previousMap != null) {

                previousMap.decrementVotes();

            }

        }

        this.playerVotes.put(playerId, mapId);
        final VotingMap selectedMap = this.votingMaps.get(mapId);
        selectedMap.incrementVotes();
        Utils.spleggOGMessage(player, "&6Vote received. &b" + selectedMap.getMap().getName() + "&6 now has &b"
                + selectedMap.getVotes() + "&6 votes.");

    }

    public boolean selectMapFromVote() {

        if (!this.votingRunning || this.votingMaps.isEmpty()) {

            return true;

        }

        VotingMap highest = null;
        int highestVotes = -1;
        for (VotingMap candidate : this.votingMaps.values()) {

            if (candidate.getVotes() > highestVotes) {

                highest = candidate;
                highestVotes = candidate.getVotes();

            }

        }

        if (highest == null) {

            this.votingRunning = false;
            return false;

        }

        this.splegg.chat.bc("&6Voting has ended! The map &b" + highest.getMap().getName() + "&6 has won!", this);
        this.votingRunning = false;
        this.votingClosed = true;
        if (this.votingReminderTask != -1) {

            Bukkit.getScheduler().cancelTask(this.votingReminderTask);
            this.votingReminderTask = -1;

        }

        return switchToVotedMap(highest.getMap());

    }

    public void selectMapFromVoteIfDue(int secondsRemaining) {

        if (this.votingRunning && secondsRemaining <= getEndVotingAt()) {

            selectMapFromVote();

        }

    }

    private boolean switchToVotedMap(Map selectedMap) {

        if (selectedMap == null || !selectedMap.isUsable(selectedMap)) {

            return false;

        }

        if (this.map != null && this.map.getName().equals(selectedMap.getName())) {

            LobbyScoreboard.refreshGame(this);
            return true;

        }

        final Map previousMap = this.map;
        final World previousWorld = this.gameWorld;
        setMap(selectedMap);

        final World selectedWorld = this.splegg.getGameWorldManager().prepareWorld(this);
        if (selectedWorld == null) {

            setMap(previousMap);
            this.gameWorld = previousWorld;
            this.splegg.chat.bc("&cVoting winner could not be loaded. Staying on &e" + previousMap.getName() + "&c.",
                    this);
            return false;

        }

        this.gameWorld = selectedWorld;
        for (SpleggPlayer spleggPlayer : this.players.values()) {

            final Player player = spleggPlayer.getPlayer();
            if (teleportToQueueLobby(player)) {

                preparePlayerForLobby(player);

            } else {

                // Old world cleanup below evicts them to the main world spawn.
                Utils.spleggOGMessage(player, "&cUnable to move you to the voted map.");

            }

        }

        if (previousWorld != null && !previousWorld.getName().equals(selectedWorld.getName())) {

            this.splegg.getGameWorldManager().cleanupWorld(previousWorld);

        }

        if (previousMap != null) {

            new LobbySign(previousMap, this.splegg).update(previousMap, false);

        }

        this.getSign().update(this.map, false);
        LobbyScoreboard.refreshGame(this);
        return true;

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
        final World joinWorld = player.getWorld();
        // Any world outside splegg territory is a valid return spot, not just the
        // vanilla overworld set -- leaveGame sends the player back here.
        if (joinWorld != null && !splegg.isSpleggWorld(joinWorld)
                && !splegg.getGameWorldManager().isGameCopyName(joinWorld.getName()))
        {

            playerWhoIsJoining.setPreJoinLocation(player.getLocation());

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

                // Stats are captured before the teleport so MyWorlds' own per-bundle
                // exp/health swap cannot pollute the snapshot.
                playerWhoIsJoining.getStore().save();
                if (!teleportToQueueLobby(player)) {

                    Utils.spleggOGMessage(player, "&cUnable to teleport you to the game lobby.");
                    return;

                }

                sp = new SpleggPlayer(playerWhoIsJoining);
                playerWhoIsJoining.setAlive(true);
                Listeners.launchEggs.add(player.getUniqueId());
                this.players.put(player.getUniqueId(), sp);
                playerWhoIsJoining.setGame(this);

                preparePlayerForLobby(player);
                registerVotingPlayer(player);
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

                // Stats are captured before the teleport so MyWorlds' own per-bundle
                // exp/health swap cannot pollute the snapshot.
                playerWhoIsJoining.getStore().save();
                if (!teleportToQueueLobby(player)) {

                    Utils.spleggOGMessage(player, "&cUnable to teleport you to the game lobby.");
                    return;

                }

                sp = new SpleggPlayer(playerWhoIsJoining);
                playerWhoIsJoining.setAlive(true);
                Listeners.launchEggs.add(player.getUniqueId());

                players.put(player.getUniqueId(), sp);
                playerWhoIsJoining.setGame(this);

                preparePlayerForLobby(player);
                registerVotingPlayer(player);

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

    // Returns false when no queue lobby resolves or the teleport is cancelled;
    // callers must not touch the player's inventory in that case, or they would
    // clear the main-world inventory the player is still standing in.
    private boolean teleportToQueueLobby(Player player) {

        final Location queueLobby = getQueueLobbyLocation();
        if (queueLobby == null || queueLobby.getWorld() == null) {

            return false;

        }

        return player.teleport(queueLobby);

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
            removeVotingPlayer(player);
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

            final Location returnLocation = u.getPreJoinLocation();
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
