package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
	public HashMap players;
	HashSet floor;
	ArrayList data;
	private int lobbycount;
	int time;
	int y1;
	int y2;
	int small;
	int counter = 0;
	int timer = 0;
	boolean starting;
	LobbySign sign;

	public Game(SpleggOG splegg, final Map map) {
		this.splegg = splegg;
		this.map = map;
		this.name = map.getName();
		this.status = Status.LOBBY;
		this.players = new HashMap();
		this.floor = new HashSet();
		this.data = new ArrayList();
		this.time = 601;
		this.lobbycount = 31;
		this.y1 = 0;
		this.y2 = 0;
		this.small = -1;
		this.setSign(new LobbySign(map, splegg));
		(new BukkitRunnable() {
			public void run() {
				Game.this.getSign().update(map, true);
			}
		}).runTaskLater(splegg, 10L);
		this.setStarting(false);
	}

	public void startGameTimer() {
		int grace = this.splegg.getConfig().getInt("Options.GraceTime");
		this.splegg.chat.bc(this.splegg.getConfig().getString("Messages.GraceTimeStart").replaceAll("&", "�").replaceAll("%grace%", String.valueOf(grace)), this);
		(new BukkitRunnable() {
			public void run() {
				Game.this.splegg.chat.bc(Game.this.splegg.getConfig().getString("Messages.GraceTimeFinish").replaceAll("&", "�"), Game.this);
				Iterator var2 = Game.this.players.values().iterator();

				while(var2.hasNext()) {
					SpleggPlayer sp = (SpleggPlayer)var2.next();
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
					Listeners.launchEggs.add(sp.getPlayer().getName());
				}

				Game.this.timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Game.this.splegg, new GameTime(Game.this.splegg, Game.this), 0L, 20L);
			}
		}).runTaskLater(this.splegg, (long)(20 * grace));
	}

	public void stopGameTimer() {
		Bukkit.getScheduler().cancelTask(this.timer);
	}

	public int getCounterID() {
		return this.counter;
	}

	public HashSet getFloor() {
		return this.floor;
	}

	public ArrayList getDatas() {
		return this.data;
	}

	public HashMap getPlayers() {
		return this.players;
	}

	public ArrayList getSp() {
		ArrayList sp = new ArrayList();
		Iterator var3 = this.players.values().iterator();

		while(var3.hasNext()) {
			SpleggPlayer sps = (SpleggPlayer)var3.next();
			sp.add(sps);
		}

		return sp;
	}

	public SpleggPlayer getPlayer(Player player) {
		return (SpleggPlayer)this.players.get(player.getName());
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

	public void joinGame(UtilPlayer u) {
		Player player = u.getPlayer();
		if (u.getGame() != null) {
			this.splegg.chat.sendMessage(player, this.splegg.getConfig().getString("Messages.AlreadyInGame").replaceAll("&", "�"));
		} else if (this.players.containsKey(player.getName())) {
			this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.AlreadyInLobby").replaceAll("&", "�"));
		} else if (this.status == Status.LOBBY) {
			int size = this.players.size();
			int max = this.map.getSpawnCount();
			SpleggPlayer sp;
			if (max == 1) {
				sp = new SpleggPlayer(player, u);
				u.setAlive(true);
				u.getStore().save();
				this.splegg.utils.clearInventory(player);
				player.setHealth(20.0D);
				player.setFoodLevel(20);
				player.setLevel(0);
				player.setExp(0.0F);
				player.setGameMode(GameMode.ADVENTURE);
				this.players.put(player.getName(), sp);
				u.setGame(this.splegg.games.getGame(this.name));
				if (this.map.lobbySet()) {
					player.teleport(this.map.getLobby());
				} else {
					player.teleport(this.splegg.config.getLobby());
				}

				Listeners.manager.add(u.getPlayer().getName());
				Listeners.shopmanager.add(u.getPlayer().getName());
				player.getInventory().setItem(splegg.getConfig().getInt("Shop.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Shop.Item")), 0, splegg.getConfig().getString("Shop.Name").replaceAll("&", "�"), splegg.getConfig().getString("Shop.Lore").replaceAll("&", "�")));
				this.splegg.chat.bc(this.splegg.getConfig().getString("Messages.JoinGame").replaceAll("&", "�").replaceAll("%player%", player.getName()).replaceAll("%count%", String.valueOf(this.players.size())).replaceAll("%maxcount%", String.valueOf(max)), u.getGame());
				if (this.players.size() >= this.splegg.getConfig().getInt("Options.AutoStartPlayers") && !this.isStarting()) {
					this.startCountdown();
					this.setStarting(true);
				}
			} else if (size >= max && !player.hasPermission("splegg.joinfull")) {
				this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.VIPPrivilege").replaceAll("&", "�"));
			} else {
				if (size >= max) {
					this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.VIPJoinGame").replaceAll("&", "�"));
				}

				sp = new SpleggPlayer(player, u);
				u.setAlive(true);
				u.getStore().save();
				this.splegg.utils.clearInventory(player);
				player.setHealth(20.0D);
				player.setFoodLevel(20);
				player.setLevel(0);
				player.setExp(0.0F);
				player.setGameMode(GameMode.ADVENTURE);
				this.players.put(player.getName(), sp);
				u.setGame(this.splegg.games.getGame(this.name));
				if (this.map.lobbySet()) {
					player.teleport(this.map.getLobby());
				} else {
					player.teleport(this.splegg.config.getLobby());
				}

				player.getInventory().setItem(splegg.getConfig().getInt("Shop.Slot"), Utils.getItem(Material.getMaterial(splegg.getConfig().getString("Shop.Item")), 0, splegg.getConfig().getString("Shop.Name").replaceAll("&", "�"), splegg.getConfig().getString("Shop.Lore").replaceAll("&", "�")));
				Listeners.manager.add(u.getPlayer().getName());
				Listeners.shopmanager.add(u.getPlayer().getName());
				this.splegg.chat.bc(this.splegg.getConfig().getString("Messages.JoinGame").replaceAll("&", "�").replaceAll("%player%", player.getName()).replaceAll("%count%", String.valueOf(this.players.size())).replaceAll("%maxcount%", String.valueOf(max)), u.getGame());
				if (this.players.size() >= this.splegg.getConfig().getInt("Options.AutoStartPlayers") && !this.isStarting()) {
					this.startCountdown();
					this.setStarting(true);
				}
			}

			this.getSign().update(this.map, false);
		} else if (this.status == Status.DISABLED) {
			this.splegg.chat.sendMessage(player, splegg.getConfig().getString("Messages.Mapdisabled").replaceAll("&", "�"));
		}

	}

	public void startCountdown() {
		Bukkit.getScheduler().cancelTask(this.counter);
		if (this.status == Status.LOBBY) {
			this.lobbycount = this.splegg.getConfig().getInt("Options.Timer");
			Iterator var2 = this.players.values().iterator();

			while(var2.hasNext()) {
				SpleggPlayer sp = (SpleggPlayer)var2.next();
				sp.getPlayer().setLevel(this.getLobbyCount());
			}

			this.counter = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.splegg, new LobbyCountdown(splegg, this, this.getLobbyCount()), 0L, 20L);
		}

	}

	public void leaveGame(UtilPlayer u) {
		SpleggPlayer sp = this.getPlayer(u.getPlayer());
		if (this.status == Status.ENDING || this.status == Status.INGAME) {
			this.splegg.chat.sendMessage(u.getPlayer(), splegg.getConfig().getString("Messages.Youbrokeblocks").replaceAll("&", "�").replaceAll("%broke%", String.valueOf(sp.getBroken())));
		}

		this.players.remove(u.getName());
		Listeners.launchEggs.remove(sp.getPlayer().getName());
		Listeners.shopmanager.remove(sp.getPlayer().getName());
		Listeners.manager.remove(sp.getPlayer().getName());
		Listeners.goldspade.remove(sp.getPlayer().getName());
		Listeners.diamondspade.remove(sp.getPlayer().getName());
		Listeners.moneymanager.remove(sp.getPlayer().getName());
		u.getPlayer().teleport(this.splegg.config.getLobby());
		u.setGame((Game)null);
		u.setAlive(false);
		u.getPlayer().setHealth(20.0D);
		InvStore store = u.getStore();
		store.load();
		store.reset();
		u.getPlayer().setFallDistance(0.0F);
		for(Player p : Bukkit.getOnlinePlayers()) {
			if (Listeners.moneymanager.contains(p.getName())) {
				p.sendMessage(ChatColor.GREEN + "You received " + splegg.getConfig().getInt("Money.KillPlayer") +" coins!");
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

	public int getLowestPossible() {
		return this.small;
	}

	public boolean loadFloors() {
		this.floor.clear();
		if (this.map.getFloors() <= 0) {
			return false;
		} else {
			for(int i = 1; i <= this.map.getFloors(); ++i) {
				Location l1 = this.map.getFloor(i, "1");
				Location l2 = this.map.getFloor(i, "2");
				CuboidRegion sel = new CuboidRegion(BlockVector3.at(l1.getBlockX(),l1.getBlockY(),l1.getBlockZ()), BlockVector3.at(l2.getBlockX(),l2.getBlockY(),l2.getBlockZ()));
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
				this.small = Math.min(this.y1, this.y2);

				for(int x = minX; x <= maxX; ++x) {
					for(int y = minY; y <= maxY; ++y) {
						for(int z = minZ; z <= maxZ; ++z) {
							Location l = new Location(l1.getWorld(), (double)x, (double)y, (double)z);
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
		Iterator var2 = this.data.iterator();

		while(var2.hasNext()) {
			Rollback d = (Rollback)var2.next();
			Location l = new Location(Bukkit.getWorld(d.getWorld()), (double)d.getX(), (double)d.getY(), (double)d.getZ());
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