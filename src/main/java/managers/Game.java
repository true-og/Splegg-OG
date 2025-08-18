package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kits;
import com.earth2me.essentials.User;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import config.Map;
import events.Listeners;
import main.SpleggOG;
import runnables.GameTime;
import runnables.LobbyCountdown;
import signs.LobbySign;
import utils.InvStore;
import utils.SpleggPlayer;
import utils.UtilPlayer;
import utils.Utils;

public class Game {

    SpleggOG splegg;
    String name;
    Map map;
    Status status;
    public HashMap<String, SpleggPlayer> players;
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

    // Enable the conversion of text from config.yml to objects.
    public FileConfiguration config = SpleggOG.getPlugin().getConfig();

    public Game(SpleggOG splegg, final Map map) {

        this.splegg = splegg;
        this.map = map;
        this.name = map.getName();
        this.status = Status.LOBBY;
        this.players = new HashMap<String, SpleggPlayer>();
        this.floor = new HashSet<Location>();
        this.data = new ArrayList<Rollback>();
        this.time = 601;
        this.lobbycount = 31;
        this.y1 = -64;
        this.y2 = -64;
        this.small = -64;

        this.setSign(new LobbySign(map, splegg));

        (new BukkitRunnable() {

            public void run() {

                Game.this.getSign().update(map, true);

            }

        }).runTaskLater(splegg, 10L);

        this.setStarting(false);

    }

    public void startGameTimer() {

        int grace = config.getInt("Options.GraceTime");
        this.splegg.chat.bc(config.getString("Messages.GraceTimeStart").replaceAll("%grace%", String.valueOf(grace)),
                this);

        (new BukkitRunnable() {

            public void run() {

                Game.this.splegg.chat.bc(config.getString("Messages.GraceTimeFinish"), Game.this);

                Iterator<?> PlayersInGame = players.values().iterator();
                while (PlayersInGame.hasNext()) {

                    SpleggPlayer sp = (SpleggPlayer) PlayersInGame.next();
                    sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);

                    // TODO: Add all available shovels here.
                    if (!Listeners.manager.contains(sp.getPlayer().getName())) {

                        if (Listeners.goldspade.contains(sp.getPlayer().getName())) {

                            sp.getPlayer().getInventory().setItem(0, Utils.getItem(Material.GOLDEN_SHOVEL,
                                    Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Gold.Name"))
                                            .content(),
                                    Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Gold.Lore"))
                                            .content()));
                            sp.getPlayer().updateInventory();

                            Listeners.manager.remove(sp.getPlayer().getName());
                            Listeners.goldspade.remove(sp.getPlayer().getName());
                            Listeners.diamondspade.remove(sp.getPlayer().getName());
                            Listeners.shopmanager.remove(sp.getPlayer().getName());
                            Listeners.moneymanager.add(sp.getPlayer().getName());

                        }

                        if (Listeners.diamondspade.contains(sp.getPlayer().getName())) {

                            sp.getPlayer().getInventory().setItem(0, Utils.getItem(Material.DIAMOND_SHOVEL,
                                    Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Diamond.Name"))
                                            .content(),
                                    Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Diamond.Lore"))
                                            .content()));
                            sp.getPlayer().updateInventory();

                            Listeners.manager.remove(sp.getPlayer().getName());
                            Listeners.goldspade.remove(sp.getPlayer().getName());
                            Listeners.diamondspade.remove(sp.getPlayer().getName());
                            Listeners.shopmanager.remove(sp.getPlayer().getName());
                            Listeners.moneymanager.add(sp.getPlayer().getName());

                        }

                    } else {

                        sp.getPlayer().getInventory().setItem(0,
                                Utils.getItem(Material.IRON_SHOVEL,
                                        Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Iron.Name"))
                                                .content(),
                                        Utils.legacySerializerAnyCase(splegg.getConfig().getString("Shovels.Iron.Lore"))
                                                .content()));
                        sp.getPlayer().updateInventory();

                        Listeners.manager.remove(sp.getPlayer().getName());
                        Listeners.goldspade.remove(sp.getPlayer().getName());
                        Listeners.diamondspade.remove(sp.getPlayer().getName());
                        Listeners.shopmanager.remove(sp.getPlayer().getName());
                        Listeners.moneymanager.add(sp.getPlayer().getName());

                    }

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

    public HashMap<String, SpleggPlayer> getPlayers() {

        return this.players;

    }

    public ArrayList<SpleggPlayer> getSp() {

        ArrayList<SpleggPlayer> sp = new ArrayList<SpleggPlayer>();
        Iterator<?> var3 = this.players.values().iterator();
        while (var3.hasNext()) {

            SpleggPlayer sps = (SpleggPlayer) var3.next();
            sp.add(sps);

        }

        return sp;

    }

    public SpleggPlayer getPlayer(Player player) {

        return (SpleggPlayer) this.players.get(player.getName());

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

        Player player = playerWhoIsJoining.getPlayer();
        if (playerWhoIsJoining.getGame() != null) {

            Utils.spleggOGMessage(player, config.getString("Messages.AlreadyInGame"));

        } else if (this.players.containsKey(player.getName())) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.AlreadyInLobby"));

        } else if (this.status == Status.LOBBY) {

            int size = this.players.size();
            // Makes maximum players in a game the same as the amount of spawn points that
            // are set for a given map.
            int max = this.map.getSpawnCount();
            SpleggPlayer sp;
            if (max == 1) {

                sp = new SpleggPlayer(playerWhoIsJoining);
                playerWhoIsJoining.setAlive(true);
                playerWhoIsJoining.getStore().save();
                Listeners.launchEggs.add(sp.getPlayer().getName());
                saveInv(player);
                player.setHealth(20.0D);
                player.setFallDistance(1);
                player.setFoodLevel(20);
                player.setLevel(0);
                player.setExp(0.0F);
                player.setGameMode(GameMode.ADVENTURE);
                this.players.put(player.getName(), sp);
                playerWhoIsJoining.setGame(this.splegg.games.getGame(this.name));

                if (this.map.lobbySet()) {

                    player.teleport(this.map.getLobby());

                } else {

                    player.teleport(this.splegg.config.getLobby(player));

                }

                Listeners.manager.add(playerWhoIsJoining.getPlayer().getName());
                Listeners.shopmanager.add(playerWhoIsJoining.getPlayer().getName());

                setLobbyInv(player);

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
                Listeners.launchEggs.add(sp.getPlayer().getName());
                saveInv(player);
                player.setHealth(20.0D);
                player.setFoodLevel(20);
                player.setLevel(0);
                player.setExp(0.0F);
                player.setGameMode(GameMode.ADVENTURE);

                players.put(player.getName(), sp);
                playerWhoIsJoining.setGame(splegg.games.getGame(name));

                if (map.lobbySet()) {

                    player.teleport(this.map.getLobby());

                } else {

                    player.teleport(splegg.config.getLobby(player));

                }

                player.getInventory().clear();
                setLobbyInv(player);

                Listeners.manager.add(playerWhoIsJoining.getPlayer().getName());
                Listeners.shopmanager.add(playerWhoIsJoining.getPlayer().getName());

                splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("%player%", player.getName())
                        .replaceAll("%count%", String.valueOf(this.players.size()))
                        .replaceAll("%maxcount%", String.valueOf(max)), playerWhoIsJoining.getGame());

                if (players.size() >= config.getInt("Options.AutoStartPlayers") && !this.isStarting()) {

                    startCountdown();
                    setStarting(true);

                }

            }

            getSign().update(this.map, false);

        } else if (this.status == Status.DISABLED) {

            Utils.spleggOGMessage(player, splegg.getConfig().getString("Messages.Mapdisabled"));

        }

    }

    private void saveInv(Player player) {

        Essentials ess = (Essentials) SpleggOG.getPlugin().getServer().getPluginManager().getPlugin("Essentials");
        Kits preGameKit = new Kits(ess);

        ArrayList<String> itemsAsListOfStrings = new ArrayList<String>(player.getInventory().getSize());
        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null) {

                itemsAsListOfStrings.add(item.serialize().toString());

            } else {

                itemsAsListOfStrings.add("");

            }

        }

        preGameKit.addKit(player.getName(), (List<String>) itemsAsListOfStrings, 0);
        SpleggOG.getPlugin().getLogger()
                .info("The pre-game inventory: " + preGameKit.matchKit(player.getName()) + " has been saved.");

        player.closeInventory();
        player.clearActiveItem();
        player.getInventory().clear();
        player.updateInventory();
        player.setFireTicks(0);

        Iterator<?> activePotionEffects = player.getActivePotionEffects().iterator();
        while (activePotionEffects.hasNext()) {

            PotionEffect effect = (PotionEffect) activePotionEffects.next();
            player.removePotionEffect(effect.getType());

        }

    }

    private void setLobbyInv(Player player) {

        int[] slotsDeclaredInConfigFile = new int[42];
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
        if (this.status == Status.LOBBY) {

            this.lobbycount = config.getInt("Options.Timer");

            Iterator<?> playersInGame = this.players.values().iterator();
            while (playersInGame.hasNext()) {

                SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
                sp.getPlayer().setLevel(this.getLobbyCount());

            }

            counter = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg,
                    new LobbyCountdown(splegg, this, this.getLobbyCount()), 0L, 20L);

        }

    }

    public void leaveGame(UtilPlayer u) {

        Player player = u.getPlayer();
        Game game = u.getGame();
        if (game != null) {

            // Tell player that left about their current state.
            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.IndividualLeaveGame")
                    .replaceAll("%map%", u.getGame().getMap().getName()));

            this.players.remove(player.getName());
            Listeners.manager.remove(u.getName());
            Listeners.shopmanager.remove(u.getName());
            Listeners.goldspade.remove(u.getName());
            Listeners.diamondspade.remove(u.getName());
            Listeners.launchEggs.remove(u.getName());
            Listeners.moneymanager.remove(u.getName());

            player.teleport(u.getGame().getMap().getLobby());
            u.setGame((Game) null);
            u.setAlive(false);
            player.setHealth(20.0D);
            player.setFallDistance(1);

        }

        String playerWhoOnlyNeedsIndividualLeaveGameMessage = new String();

        Essentials ess = (Essentials) SpleggOG.getPlugin().getServer().getPluginManager().getPlugin("Essentials");
        User user = new User(player, ess);

        playerWhoOnlyNeedsIndividualLeaveGameMessage = player.getName();

        Kits essentialsKitList = new Kits(ess);
        if (essentialsKitList != null) {

            try {

                essentialsKitList.getKit(user.getName());

            } catch (Exception error) {

                SpleggOG.getPlugin().getLogger().severe(error.getMessage());

            }

        }

        InvStore store = u.getStore();
        store.load();
        store.reset();

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (p.getName() != playerWhoOnlyNeedsIndividualLeaveGameMessage) {

                UtilPlayer playerRemainingInGame = SpleggOG.getPlugin().pm.getPlayer(p);
                if (playerRemainingInGame.getGame() != null) {

                    // Tell the rest of the people in the lobby that the player has left.
                    Utils.spleggOGMessage(p,
                            config.getString("Messages.LeaveGame").replaceAll("%player%", p.getName())
                                    .replaceAll("%count%", String.valueOf(this.players.size()))
                                    .replaceAll("%maxcount%", String.valueOf(this.map.getSpawnCount())));

                }

            }

            if (Listeners.moneymanager.contains(p.getName())) {

                p.sendMessage(Utils
                        .legacySerializerAnyCase(
                                "&BYou received " + splegg.getConfig().getInt("Money.KillPlayer") + " &BDiamonds!")
                        .content());
                splegg.econ.depositPlayer(p, splegg.getConfig().getInt("Money.KillPlayer"));

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

    public boolean loadFloors() {

        this.floor.clear();

        if (this.map.getFloors() <= 0) {

            return false;

        } else {

            for (int i = 1; i <= this.map.getFloors(); ++i) {

                Location l1 = this.map.getFloor(i, "1");
                Location l2 = this.map.getFloor(i, "2");

                CuboidRegion sel = new CuboidRegion(BlockVector3.at(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ()),
                        BlockVector3.at(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ()));

                BlockVector3 min = sel.getMinimumPoint();
                BlockVector3 max = sel.getMaximumPoint();

                int minX = min.x();
                int minY = min.y();
                this.y1 = minY;
                int minZ = min.z();
                int maxX = max.x();
                int maxY = max.y();
                this.y2 = maxY;
                int maxZ = max.z();
                this.small = Math.min(this.y2, this.y2);

                for (int x = minX; x <= maxX; ++x) {

                    for (int y = minY; y <= maxY; ++y) {

                        for (int z = minZ; z <= maxZ; ++z) {

                            Location l = new Location(l1.getWorld(), (double) x, (double) y, (double) z);
                            this.floor.add(l);

                            Block block = l.getWorld().getBlockAt(x, y, z);
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

        Iterator<?> var2 = this.data.iterator();
        while (var2.hasNext()) {

            Rollback d = (Rollback) var2.next();
            Location l = new Location(Bukkit.getWorld(d.getWorld()), (double) d.getX(), (double) d.getY(),
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