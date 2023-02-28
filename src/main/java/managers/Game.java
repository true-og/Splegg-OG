package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
		this.y1 = 0;
		this.y2 = 0;

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
		this.splegg.chat.bc(config.getString("Messages.GraceTimeStart").replaceAll("&", "§").replaceAll("%grace%", String.valueOf(grace)), this);

		(new BukkitRunnable() {

			public void run() {

				Game.this.splegg.chat.bc(config.getString("Messages.GraceTimeFinish").replaceAll("&", "§"), Game.this);

				Iterator<?> PlayersInGame = players.values().iterator();
				while(PlayersInGame.hasNext()) {

					SpleggPlayer sp = (SpleggPlayer) PlayersInGame.next();
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
					Listeners.launchEggs.add(sp.getPlayer().getName());

				}

				Game.this.timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Game.this.splegg, new GameTime(Game.this.splegg, Game.this), 0L, 20L);

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
		while(var3.hasNext()) {

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

	public void joinGame(UtilPlayer playerWhoIsJoining) {

		Player player = playerWhoIsJoining.getPlayer();
		if (playerWhoIsJoining.getGame() != null) {

			this.splegg.chat.sendMessage(player, config.getString("Messages.AlreadyInGame").replaceAll("&", "§"));

		}
		else if (this.players.containsKey(player.getName())) {

			this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.AlreadyInLobby").replaceAll("&", "§"));

		}
		else if (this.status == Status.LOBBY) {

			int size = this.players.size();
			// Makes maximum players in a game the same as the amount of spawn points that are set for a given map.
			int max = this.map.getSpawnCount();
			SpleggPlayer sp;
			if (max == 1) {

				sp = new SpleggPlayer(playerWhoIsJoining);
				playerWhoIsJoining.setAlive(true);
				playerWhoIsJoining.getStore().save();
				this.splegg.utils.clearInventory(player);
				player.setHealth(20.0D);
				player.setFoodLevel(20);
				player.setLevel(0);
				player.setExp(0.0F);
				player.setGameMode(GameMode.ADVENTURE);
				this.players.put(player.getName(), sp);
				playerWhoIsJoining.setGame(this.splegg.games.getGame(this.name));

				if (this.map.lobbySet()) {

					player.teleport(this.map.getLobby());

				}
				else {

					player.teleport(this.splegg.config.getLobby(player));

				}

				Listeners.manager.add(playerWhoIsJoining.getPlayer().getName());
				Listeners.shopmanager.add(playerWhoIsJoining.getPlayer().getName());

				saveInv(player);
				setLobbyInv(player);

				this.splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("&", "§").replaceAll("%player%", player.getName()).replaceAll("%count%", String.valueOf(this.players.size())).replaceAll("%maxcount%", String.valueOf(max)), playerWhoIsJoining.getGame());

				if (this.players.size() >= config.getInt("Options.AutoStartPlayers") && ! this.isStarting()) {

					this.startCountdown();
					this.setStarting(true);

				}

			}
			else if (size >= max && ! player.hasPermission("splegg.joinfull")) {

				this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.VIPPrivilege").replaceAll("&", "§"));

			}
			else {

				if (size >= max) {

					this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.VIPJoinGame").replaceAll("&", "§"));

				}

				sp = new SpleggPlayer(playerWhoIsJoining);
				playerWhoIsJoining.setAlive(true);
				playerWhoIsJoining.getStore().save();

				splegg.utils.clearInventory(player);

				player.setHealth(20.0D);
				player.setFoodLevel(20);
				player.setLevel(0);
				player.setExp(0.0F);
				player.setGameMode(GameMode.ADVENTURE);

				players.put(player.getName(), sp);
				playerWhoIsJoining.setGame(splegg.games.getGame(name));

				if (map.lobbySet()) {

					player.teleport(this.map.getLobby());

				}
				else {

					player.teleport(splegg.config.getLobby(player));

				}

				saveInv(player);
				setLobbyInv(player);

				Listeners.manager.add(playerWhoIsJoining.getPlayer().getName());
				Listeners.shopmanager.add(playerWhoIsJoining.getPlayer().getName());

				splegg.chat.bc(config.getString("Messages.JoinGame").replaceAll("&", "§").replaceAll("%player%", player.getName()).replaceAll("%count%", String.valueOf(this.players.size())).replaceAll("%maxcount%", String.valueOf(max)), playerWhoIsJoining.getGame());

				if (players.size() >= config.getInt("Options.AutoStartPlayers") && ! this.isStarting()) {

					startCountdown();
					setStarting(true);

				}

			}

			getSign().update(this.map, false);

		}
		else if (this.status == Status.DISABLED) {

			splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.Mapdisabled").replaceAll("&", "§"));

		}

	}

	private void saveInv(Player player) {

		

	}

	private void setLobbyInv(Player player) {

		player.closeInventory();
		player.getInventory().clear();

		player.getInventory().setItem(splegg.getConfig().getInt("Shop.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Shop.Item")), splegg.getConfig().getString("Shop.Name").replaceAll("&", "§"), splegg.getConfig().getString("Shop.Lore").replaceAll("&", "§")));
		player.getInventory().setItem(splegg.getConfig().getInt("Guide.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Guide.Item")), splegg.getConfig().getString("Guide.Name").replaceAll("&", "§"), splegg.getConfig().getString("Guide.Lore").replaceAll("&", "§")));
		player.getInventory().setItem(splegg.getConfig().getInt("Cosmetics.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Cosmetics.Item")), splegg.getConfig().getString("Cosmetics.Name").replaceAll("&", "§"), splegg.getConfig().getString("Cosmetics.Lore").replaceAll("&", "§")));
		player.getInventory().setItem(splegg.getConfig().getInt("Leave.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Leave.Item")), splegg.getConfig().getString("Leave.Name").replaceAll("&", "§"), splegg.getConfig().getString("Leave.Lore").replaceAll("&", "§")));

	}

	public void startCountdown() {

		Bukkit.getScheduler().cancelTask(this.counter);
		if (this.status == Status.LOBBY) {

			this.lobbycount = config.getInt("Options.Timer");

			Iterator<?> playersInGame = this.players.values().iterator();
			while(playersInGame.hasNext()) {

				SpleggPlayer sp = (SpleggPlayer) playersInGame.next();
				sp.getPlayer().setLevel(this.getLobbyCount());

			}

			this.counter = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg, new LobbyCountdown(splegg, this, this.getLobbyCount()), 0L, 20L);

		}

	}

	public void leaveGame(HashMap<String, SpleggPlayer> playersInGame) {

		while(playersInGame.values().iterator().hasNext()) {

			SpleggPlayer sp = playersInGame.values().iterator().next();
			UtilPlayer u = sp.getUtilPlayer();

			if (this.status == Status.ENDING || this.status == Status.INGAME) {

				splegg.chat.sendMessage(u.getPlayer(), splegg.getConfig().getString("Messages.Youbrokeblocks").replaceAll("&", "§").replaceAll("%broke%", String.valueOf(sp.getBroken())));

			}

			SpleggOG.getPlugin().chat.sendMessage(sp.getPlayer(), SpleggOG.getPlugin().getConfig().getString("Messages.LeaveGame").replaceAll("&", "§").replaceAll("%map%", u.getGame().getMap().getName()));

			this.players.remove(u.getName());

			// TODO: Consider adjusting fall distance here.
			Listeners.launchEggs.remove(sp.getPlayer().getName());
			Listeners.shopmanager.remove(sp.getPlayer().getName());
			Listeners.manager.remove(sp.getPlayer().getName());
			Listeners.goldspade.remove(sp.getPlayer().getName());
			Listeners.diamondspade.remove(sp.getPlayer().getName());
			Listeners.moneymanager.remove(sp.getPlayer().getName());

			// End of game location.
			u.getPlayer().teleport(u.getGame().getMap().getLobby());
			u.setGame((Game) null);
			u.setAlive(false);
			u.getPlayer().setHealth(20.0D);

			InvStore store = u.getStore();
			store.load();
			store.reset();

		}

		for(Player p : Bukkit.getOnlinePlayers()) {

			if (Listeners.moneymanager.contains(p.getName())) {

				p.sendMessage(("&BYou received " + splegg.getConfig().getInt("Money.KillPlayer") + " &BDiamonds!").replaceAll("&", "§"));

				splegg.econ.depositPlayer(p, splegg.getConfig().getInt("Money.KillPlayer"));

			}
		}

		if (! this.splegg.disabling) {

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

		}
		else {

			for(int i = 1; i <= this.map.getFloors(); ++i) {

				Location l1 = this.map.getFloor(i, "1");
				Location l2 = this.map.getFloor(i, "2");

				CuboidRegion sel = new CuboidRegion(BlockVector3.at(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ()), BlockVector3.at(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ()));

				BlockVector3 min = sel.getMinimumPoint();
				BlockVector3 max = sel.getMaximumPoint();

				int minX = min.getBlockX();
				int minY = min.getBlockY();
				this.y1 = minY;
				int minZ = min.getBlockZ();
				int maxX = max.getBlockX();
				int maxY = max.getBlockY();
				this.y2 = maxY;
				int maxZ = max.getBlockZ();

				for(int x = minX; x <= maxX; ++x) {

					for(int y = minY; y <= maxY; ++y) {

						for(int z = minZ; z <= maxZ; ++z) {

							Location l = new Location(l1.getWorld(), (double) x, (double) y, (double) z);
							this.floor.add(l);

							Block block = l.getWorld().getBlockAt(x, y, z);
							this.data.add(new Rollback(l.getWorld().getName(), block.getType(), block.getBlockData(), x, y, z));

						}

					}

				}

			}

			return true;

		}

	}

	public void resetArena() {

		Iterator<?> var2 = this.data.iterator();
		while(var2.hasNext()) {

			Rollback d = (Rollback) var2.next();
			Location l = new Location(Bukkit.getWorld(d.getWorld()), (double) d.getX(), (double) d.getY(), (double) d.getZ());

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