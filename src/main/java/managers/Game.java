package managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

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

// One persistent Splegg lobby, TheHerobrine-OG style. A lobby is identified by
// its number (SP1 is lobby 1) and owns the hub world <prefix><n>-hub, where
// players wait and vote for the map. When the vote resolves, the winning map's
// template is copied to <prefix><n>-<map> and the match runs there. When the
// match ends the copy is deleted and the lobby goes back to waiting in its hub.
public class Game {

    SpleggOG splegg;
    final String gameId;
    final String hubWorldName;
    Map map;
    Status status;
    public HashMap<UUID, SpleggPlayer> players;
    private World gameWorld;
    private int lobbycount;
    int time;
    int counter;
    int timer;
    boolean starting;
    DiamondBankAPIJava diamondBankAPI;
    private LinkedHashMap<Integer, VotingMap> votingMaps;
    private HashMap<UUID, Integer> playerVotes;
    private boolean votingRunning;
    private boolean votingClosed;
    private int votingReminderTask;

    // Enable the conversion of text from config.yml to objects.
    public FileConfiguration config = SpleggOG.getPlugin().getConfig();

    public Game(SpleggOG splegg, String gameId, String hubWorldName) {

        this.splegg = splegg;
        this.gameId = gameId;
        this.hubWorldName = hubWorldName;
        this.map = null;
        this.diamondBankAPI = splegg.getDiamondBankAPI();
        this.status = Status.LOBBY;
        this.players = new HashMap<>();
        this.votingMaps = new LinkedHashMap<>();
        this.playerVotes = new HashMap<>();
        this.votingRunning = false;
        this.votingClosed = false;
        this.votingReminderTask = -1;
        this.counter = -1;
        this.timer = -1;
        this.time = 601;
        this.lobbycount = config.getInt("Options.Timer", 120);
        this.starting = false;

    }

    // The lobby number, which is also the number in every world this lobby owns.
    public String getGameId() {

        return this.gameId;

    }

    // SP1, the id players type.
    public String getLobbyId() {

        return this.splegg.getGameWorldPrefix() + this.gameId;

    }

    public String getHubWorldName() {

        return this.hubWorldName;

    }

    public World getHubWorld() {

        return Bukkit.getWorld(this.hubWorldName);

    }

    public World getGameWorld() {

        return this.gameWorld;

    }

    public void setGameWorld(World world) {

        this.gameWorld = world;

    }

    // True for this lobby's hub or arena world only. The dash matters: SP1 must
    // not claim SP10-hub.
    public boolean ownsWorld(String worldName) {

        if (worldName == null) {

            return false;

        }

        if (worldName.equalsIgnoreCase(this.hubWorldName)) {

            return true;

        }

        return this.gameWorld != null && worldName.equals(this.gameWorld.getName());

    }

    public int getMaxPlayers() {

        return Math.max(1, this.config.getInt("Options.MaxPlayers", 10));

    }

    // The map name once the vote has picked one, otherwise the lobby id.
    public String getMapDisplayName() {

        return this.map != null ? this.map.getName() : this.getLobbyId();

    }

    public void startGameTimer() {

        final int grace = config.getInt("Options.GraceTime");
        this.splegg.chat.bc(config.getString("Messages.GraceTimeStart").replaceAll("%grace%", String.valueOf(grace)),
                this);

        Bukkit.getScheduler().runTaskLater(this.splegg, () -> {

            if (Game.this.status != Status.INGAME) {

                return;

            }

            Game.this.splegg.chat.bc(config.getString("Messages.GraceTimeFinish"), Game.this);

            final Iterator<?> PlayersInGame = players.values().iterator();
            while (PlayersInGame.hasNext()) {

                final SpleggPlayer sp = (SpleggPlayer) PlayersInGame.next();
                final UUID playerId = sp.getPlayer().getUniqueId();
                sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);

                final Material selectedShovel = Listeners.getSelectedShovelMaterial(playerId);
                final String selectedShovelConfigPath = Listeners.getSelectedShovelConfigPath(playerId);

                sp.getPlayer().getInventory().setItem(0, Utils.getItem(selectedShovel,
                        Utils.legacySerializerAnyCase(splegg.getConfig().getString(selectedShovelConfigPath + ".Name"))
                                .content(),
                        Utils.legacySerializerAnyCase(splegg.getConfig().getString(selectedShovelConfigPath + ".Lore"))
                                .content()));
                sp.getPlayer().updateInventory();

                Listeners.finalizePreGameShovelState(playerId);

            }

            Game.this.timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Game.this.splegg,
                    new GameTime(Game.this.splegg, Game.this), 0L, 20L);

        }, (long) (20 * grace));

    }

    public void stopGameTimer() {

        if (this.timer != -1) {

            Bukkit.getScheduler().cancelTask(this.timer);
            this.timer = -1;

        }

    }

    public int getCounterID() {

        return this.counter;

    }

    public HashMap<UUID, SpleggPlayer> getPlayers() {

        return this.players;

    }

    public ArrayList<SpleggPlayer> getSp() {

        return new ArrayList<>(this.players.values());

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

    // The map this lobby is playing or has voted for. Null while the vote is open.
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

        if (this.status != Status.LOBBY || this.votingClosed) {

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

    // Up to Options.VotingMaps playable maps, drawn at random from every map.
    private void pickVotingMaps() {

        this.votingMaps.clear();
        this.playerVotes.clear();
        this.votingClosed = false;

        final List<Map> candidates = new ArrayList<>();
        for (Map candidate : this.splegg.maps.getMaps()) {

            if (candidate != null && candidate.isUsable(candidate)) {

                candidates.add(candidate);

            }

        }

        Collections.shuffle(candidates);

        int id = 1;
        for (Map candidate : candidates) {

            if (id > getVotingMapCount()) {

                break;

            }

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

    // Reopens the vote. Any arena already prepared for the previous winner is
    // thrown away, since the next vote may pick a different map.
    public void resetVoting() {

        stopVoting();
        discardArena();
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

    private Map highestVotedMap() {

        VotingMap highest = null;
        int highestVotes = -1;
        for (VotingMap candidate : this.votingMaps.values()) {

            if (candidate.getVotes() > highestVotes) {

                highest = candidate;
                highestVotes = candidate.getVotes();

            }

        }

        return highest == null ? null : highest.getMap();

    }

    // Closes the vote and prepares the arena for the winner. Players stay in the
    // hub until the countdown ends.
    public boolean selectMapFromVote() {

        if (!this.votingRunning || this.votingMaps.isEmpty()) {

            return this.map != null;

        }

        final Map winner = highestVotedMap();
        this.stopVoting();
        this.votingClosed = true;

        if (winner == null) {

            return false;

        }

        this.splegg.chat.bc("&6Voting has ended! The map &b" + winner.getName() + "&6 has won!", this);
        if (!prepareArena(winner)) {

            this.splegg.chat.bc("&cThe voted map could not be loaded. Another map will be picked at the start.", this);
            return false;

        }

        updateSigns();
        LobbyScoreboard.refreshGame(this);
        return true;

    }

    public void selectMapFromVoteIfDue(int secondsRemaining) {

        if (this.votingRunning && secondsRemaining <= getEndVotingAt()) {

            selectMapFromVote();

        }

    }

    // Makes sure a map is chosen and its arena copy is loaded. The preferred map
    // is tried first, then the vote winner, then every other playable map, so one
    // broken template does not stop the lobby from ever starting.
    public boolean prepareArena(Map preferred) {

        if (this.map != null && this.gameWorld != null) {

            return true;

        }

        final List<Map> candidates = new ArrayList<>();
        if (preferred != null) {

            candidates.add(preferred);

        }

        if (this.map != null) {

            candidates.add(this.map);

        }

        final Map voted = highestVotedMap();
        if (voted != null) {

            candidates.add(voted);

        }

        final List<Map> rest = new ArrayList<>();
        for (Map candidate : this.splegg.maps.getMaps()) {

            if (candidate != null && candidate.isUsable(candidate)) {

                rest.add(candidate);

            }

        }

        Collections.shuffle(rest);
        candidates.addAll(rest);

        final List<String> tried = new ArrayList<>();
        for (Map candidate : candidates) {

            if (candidate == null || tried.contains(candidate.getName()) || !candidate.isUsable(candidate)) {

                continue;

            }

            tried.add(candidate.getName());
            this.map = candidate;
            final World world = this.splegg.getGameWorldManager().prepareWorld(this);
            if (world != null) {

                this.gameWorld = world;
                return true;

            }

        }

        this.map = null;
        this.gameWorld = null;
        return false;

    }

    public boolean prepareArena() {

        return prepareArena(null);

    }

    // Drops a prepared arena that will not be played. The reference is cleared
    // before the unload so the world-unload listener does not mistake our own
    // cleanup for the arena vanishing under a live lobby.
    public void discardArena() {

        final World arena = this.gameWorld;
        this.gameWorld = null;
        this.map = null;
        if (arena != null) {

            this.splegg.getGameWorldManager().cleanupWorld(arena);

        }

    }

    // Back to an empty, waiting lobby. The arena copy must already be gone.
    public void resetToLobby() {

        if (this.counter != -1) {

            Bukkit.getScheduler().cancelTask(this.counter);
            this.counter = -1;

        }

        stopGameTimer();
        stopVoting();
        this.players.clear();
        this.playerVotes.clear();
        this.votingMaps.clear();
        this.votingClosed = false;
        this.map = null;
        this.gameWorld = null;
        this.starting = false;
        this.time = 601;
        this.lobbycount = config.getInt("Options.Timer", 120);
        this.status = Status.LOBBY;

    }

    public int getLowestPossible() {

        // The lowest Y the floor extends to. Computed once from the map config.
        // No mutable state needed -- the per-game world is reset by being
        // deleted between games.
        // Floors live in the per-map file, not in config.yml.
        if (this.map == null) {

            return -64;

        }

        final FileConfiguration mapConfig = this.map.getConfig();
        int small = Integer.MAX_VALUE;
        for (int i = 1; i <= map.getFloors(); i++) {

            small = Math.min(small, Math.min(mapConfig.getInt("Floors." + i + ".p1.y", small),
                    mapConfig.getInt("Floors." + i + ".p2.y", small)));

        }

        return small == Integer.MAX_VALUE ? -64 : small;

    }

    public void joinGame(UtilPlayer playerWhoIsJoining) {

        final Player player = playerWhoIsJoining.getPlayer();
        // Any world outside Splegg territory is a valid return spot; the store refuses
        // the rest.
        splegg.savePreJoinLocation(player.getUniqueId(), player.getLocation());

        if (playerWhoIsJoining.getGame() != null) {

            Utils.spleggOGMessage(player, config.getString("Messages.AlreadyInGame"));
            return;

        }

        if (this.players.containsKey(player.getUniqueId())) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.AlreadyInLobby"));
            return;

        }

        if (this.status == Status.DISABLED) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.Mapdisabled"));
            return;

        }

        if (this.status != Status.LOBBY) {

            Utils.spleggOGMessage(player, "&cLobby &e" + getLobbyId() + " &cis in a match right now.");
            return;

        }

        if (this.getHubWorld() == null) {

            Utils.spleggOGMessage(player, "&cLobby &e" + getLobbyId() + " &cis unavailable: its hub world &e"
                    + hubWorldName + " &cis not loaded.");
            return;

        }

        final int size = this.players.size();
        final int max = this.getMaxPlayers();
        if (size >= max) {

            if (!player.hasPermission("splegg.joinfull")) {

                Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.VIPPrivilege"));
                return;

            }

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.VIPJoinGame"));

        }

        // Stats are captured before the teleport so MyWorlds' own per-bundle
        // exp/health swap cannot pollute the snapshot.
        playerWhoIsJoining.getStore().save();
        if (!teleportToQueueLobby(player)) {

            Utils.spleggOGMessage(player, "&cUnable to teleport you to the game lobby.");
            return;

        }

        final SpleggPlayer sp = new SpleggPlayer(playerWhoIsJoining);
        playerWhoIsJoining.setAlive(true);
        Listeners.launchEggs.add(player.getUniqueId());

        players.put(player.getUniqueId(), sp);
        playerWhoIsJoining.setGame(this);

        preparePlayerForLobby(player);
        registerVotingPlayer(player);

        Listeners.manager.add(player.getUniqueId());
        Listeners.shopmanager.add(player.getUniqueId());

        Utils.spleggOGMessage(player, "&aJoined lobby &e" + getLobbyId() + "&a.");
        splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("%player%", player.getName())
                .replaceAll("%count%", String.valueOf(this.players.size()))
                .replaceAll("%maxcount%", String.valueOf(max)), this);

        if (players.size() >= config.getInt("Options.AutoStartPlayers") && !this.isStarting()) {

            startCountdown();
            setStarting(true);

        }

        updateSigns();
        LobbyScoreboard.refreshGame(this);

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

    // Where a joining player lands: the global lobby point rebased into this
    // lobby's hub (every hub is a copy of the same template), else the hub's
    // own spawn.
    public Location getQueueLobbyLocation() {

        final World hub = this.getHubWorld();
        if (hub == null) {

            return null;

        }

        final Location globalQueueLobby = this.splegg.config.getLobby(null);
        if (globalQueueLobby != null) {

            return new Location(hub, globalQueueLobby.getX(), globalQueueLobby.getY(), globalQueueLobby.getZ(),
                    globalQueueLobby.getYaw(), globalQueueLobby.getPitch());

        }

        return hub.getSpawnLocation();

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

        if (this.counter != -1) {

            Bukkit.getScheduler().cancelTask(counter);
            this.counter = -1;

        }

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

    // Restarts the lobby countdown ignoring Options.AutoStartPlayers. The floor is
    // EndVotingAt so map voting still resolves before the game starts.
    // Returns the countdown actually used, or -1 when the game is not in a lobby.
    public int forceStartCountdown(int seconds) {

        if (this.status != Status.LOBBY) {

            return -1;

        }

        if (this.counter != -1) {

            Bukkit.getScheduler().cancelTask(counter);
            this.counter = -1;

        }

        this.lobbycount = Math.max(seconds, getEndVotingAt());
        this.setStarting(true);

        final Iterator<?> playersInGame = this.players.values().iterator();
        while (playersInGame.hasNext()) {

            final SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
            sp.getPlayer().setLevel(this.getLobbyCount());

        }

        counter = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg,
                new LobbyCountdown(splegg, this, this.getLobbyCount(), true), 0L, 20L);

        return this.lobbycount;

    }

    // Stops the countdown and reopens the vote when everybody has left.
    private void abandonCountdownIfEmpty() {

        if (!this.players.isEmpty() || this.status != Status.LOBBY) {

            return;

        }

        if (this.counter != -1) {

            Bukkit.getScheduler().cancelTask(this.counter);
            this.counter = -1;

        }

        this.setStarting(false);
        this.lobbycount = config.getInt("Options.Timer", 120);
        this.resetVoting();

    }

    public void leaveGame(UtilPlayer u) {

        final Player player = u.getPlayer();
        final Game game = u.getGame();
        final UUID playerId = player.getUniqueId();
        final SpleggPlayer spleggPlayer = this.players.get(playerId);
        final int brokenBlocks = spleggPlayer != null ? spleggPlayer.getBroken() : 0;
        if (game != null) {

            // Tell player that left about their current state.
            Utils.spleggOGMessage(player,
                    SpleggOG.getPlugin().getConfig()
                            .getString("Messages.IndividualLeaveGame", "&6You have left lobby &a%lobby%&6.")
                            .replaceAll("%map%", getMapDisplayName()).replaceAll("%lobby%", getLobbyId()));
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

            if (!this.splegg.returnPlayer(player, true)) {

                SpleggOG.getPlugin().getLogger()
                        .warning("Could not return " + player.getName() + " out of lobby " + getLobbyId() + ".");

            }

            u.setGame((Game) null);
            u.setAlive(false);

        }

        final String playerWhoOnlyNeedsIndividualLeaveGameMessage = player.getName();
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
                                    .replaceAll("%maxcount%", String.valueOf(this.getMaxPlayers())));

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

            abandonCountdownIfEmpty();

        }

        if (!this.splegg.disabling) {

            updateSigns();

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

        return this.map != null && this.map.getFloors() > 0;

    }

    /**
     * Returns true when the supplied {@link Location} lies inside any of this
     * game's floor cuboids (per the map config), rebased onto the per-game world
     * copy. Used by the egg-impact handler to decide if an egg should vaporize a
     * block.
     */
    public boolean isInsideFloor(Location target) {

        if (target == null || this.map == null)
            return false;
        if (this.gameWorld == null)
            return false;
        if (target.getWorld() == null || !this.gameWorld.equals(target.getWorld()))
            return false;
        int tx = target.getBlockX();
        int ty = target.getBlockY();
        int tz = target.getBlockZ();
        // Floors live in the per-map file, not in config.yml.
        final FileConfiguration mapConfig = this.map.getConfig();
        for (int i = 1; i <= map.getFloors(); i++) {

            int p1x = mapConfig.getInt("Floors." + i + ".p1.x");
            int p1y = mapConfig.getInt("Floors." + i + ".p1.y");
            int p1z = mapConfig.getInt("Floors." + i + ".p1.z");
            int p2x = mapConfig.getInt("Floors." + i + ".p2.x");
            int p2y = mapConfig.getInt("Floors." + i + ".p2.y");
            int p2z = mapConfig.getInt("Floors." + i + ".p2.z");
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

    // Every join sign shows lobby state, so redraw them all.
    public void updateSigns() {

        LobbySign.updateAll(this.splegg);

    }

    public void setLobbyCount(int lobbycount) {

        this.lobbycount = lobbycount;

    }

}
