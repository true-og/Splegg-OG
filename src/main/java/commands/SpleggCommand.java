package commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

import config.Map;
import main.SpleggOG;
import managers.Game;
import managers.Status;
import utils.UtilPlayer;

public class SpleggCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String tag, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);
			if (args.length == 0) {
				SpleggOG.getPlugin().chat.sendMessage(player, "Plugin created by MrLuangamer, > Theluangamer9416 for more information /splegg help");
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("join")) {
					if (player.hasPermission("splegg.join")) {
						if (u.getGame() != null && u.isAlive()) {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are already playing.");
						} else if (this.lobbyset()) {
							player.teleport(SpleggOG.getPlugin().config.getLobby());
						} else {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cSplegg is incorrectly setup! Ask an admin to set the lobby.");
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
					}
				} else if (args[0].equalsIgnoreCase("leave")) {
					if (u.getGame() != null && u.isAlive()) {
						Game game = u.getGame();
						game.leaveGame(u);
						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.LeaveGame").replaceAll("&", "�").replaceAll("%map%", game.getMap().getName()));
					}
				} else if (args[0].equalsIgnoreCase("start")) {
					if (player.hasPermission("splegg.admin")) {
						if (u.getGame() == null) {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");
						} else if (u.getGame().getStatus() == Status.LOBBY) {
							SpleggOG.getPlugin().game.startGame(u.getGame());
							SpleggOG.getPlugin().chat.sendMessage(player, "&eGame started!");
						} else if (u.getGame().getStatus() == Status.INGAME) {
							SpleggOG.getPlugin().chat.sendMessage(player, "�cGame has already begun.");
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
					}
				} else if (args[0].equalsIgnoreCase("stop")) {
					if (player.hasPermission("splegg.admin")) {
						if (u.getGame() == null) {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");
						} else if (u.getGame().getStatus() == Status.LOBBY) {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cGame has not begun yet!");
						} else if (u.getGame().getStatus() == Status.INGAME) {
							SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", u.getGame());
							SpleggOG.getPlugin().game.stopGame(u.getGame(), 1);
							SpleggOG.getPlugin().chat.sendMessage(player, "�eYou have stopped the game.");
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
					}
				} else if (args[0].equalsIgnoreCase("setlobby")) {
					if (player.hasPermission("splegg.admin")) {
						SpleggOG.getPlugin().config.setLobby(player.getLocation());
						SpleggOG.getPlugin().chat.sendMessage(player, "You have set splegg lobby.");
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
					}
				} else if (!args[0].equalsIgnoreCase("")) {
					SpleggOG.getPlugin().chat.sendMessage(player, "&cIncorrect Usage: &6/" + tag + " <join,leave,stop,start,setlobby,help>");
				}
			} else {
				Map map;
				String name;
				if (args.length == 2) {
					if (args[0].equalsIgnoreCase("create")) {
						if (player.hasPermission("splegg.admin")) {
							name = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(name)) {
								SpleggOG.getPlugin().chat.sendMessage(player, "Map already exists.");
							} else {
								SpleggOG.getPlugin().maps.c.addMap(name);
								SpleggOG.getPlugin().maps.addMap(name);
								map = SpleggOG.getPlugin().maps.getMap(name);
								Game game = new Game(SpleggOG.getPlugin(), map);
								SpleggOG.getPlugin().games.addGame(map.getName(), game);
								if (!map.isUsable()) {
									game.setStatus(Status.DISABLED);
								}

								SpleggOG.getPlugin().chat.sendMessage(player, "Map has been added and created &c" + name + "&6.");
							}
						} else {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
						}
					} else if (args[0].equalsIgnoreCase("delete")) {
						if (player.hasPermission("splegg.admin")) {
							name = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(name)) {
								SpleggOG.getPlugin().maps.deleteMap(name);
								SpleggOG.getPlugin().chat.sendMessage(player, "Map has been deleted.");
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
							}
						} else {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
						}
					} else {
						Game game;
						if (args[0].equalsIgnoreCase("start")) {
							if (player.hasPermission("splegg.admin")) {
								name = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(name)) {
									game = SpleggOG.getPlugin().games.getGame(name);
									if (game == null) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");
									} else if (game.getStatus() == Status.LOBBY) {
										SpleggOG.getPlugin().game.startGame(game);
										SpleggOG.getPlugin().chat.sendMessage(player, "&eStarting " + name + ".");
									} else if (game.getStatus() == Status.INGAME) {
										SpleggOG.getPlugin().chat.sendMessage(player, "�cGame has already begun.");
									}
								} else {
									SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
							}
						} else if (args[0].equalsIgnoreCase("stop")) {
							if (player.hasPermission("splegg.admin")) {
								name = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(name)) {
									game = SpleggOG.getPlugin().games.getGame(name);
									if (game == null) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");
									} else if (game.getStatus() == Status.LOBBY) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cGame has not begun yet!");
									} else if (game.getStatus() == Status.INGAME) {
										SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", game);
										SpleggOG.getPlugin().game.stopGame(game, 1);
										SpleggOG.getPlugin().chat.sendMessage(player, "�eYou have stopped the game.");
									}
								} else {
									SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
							}
						} else if (args[0].equalsIgnoreCase("setlobby")) {
							if (player.hasPermission("splegg.admin")) {
								name = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(name)) {
									SpleggOG.getPlugin().maps.getMap(name).setLobby(player.getLocation());
									SpleggOG.getPlugin().chat.sendMessage(player, "Lobby for map &e" + name + "&6 set.");
								} else {
									SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
							}
						} else if (args[0].equalsIgnoreCase("addfloor")) {
							if (player.hasPermission("splegg.admin")) {
								name = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(name)) {
									map = SpleggOG.getPlugin().maps.getMap(name);
									WorldEditPlugin we = SpleggOG.getPlugin().getWorldEdit();
									Region sel = null;
									try {
										sel = we.getSession(player).getSelection(new BukkitWorld(player.getWorld()));
									}
									catch (IncompleteRegionException e) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cERROR: The area you have selected is incomplete.");
									}
									if (sel == null) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&5Please select an area with worldedit.");
									} else {
										map.addFloor(new Location(player.getWorld(), sel.getMinimumPoint().getBlockX(), sel.getMinimumPoint().getBlockY(), sel.getMinimumPoint().getBlockZ()), new Location(player.getWorld(), sel.getMaximumPoint().getBlockX(), sel.getMaximumPoint().getBlockY(), sel.getMaximumPoint().getBlockZ()));
										SpleggOG.getPlugin().chat.sendMessage(player, "Floor " + map.getFloors() + " added to map " + map.getName() + ".");
									}
								} else {
									SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
							}
						} else if (args[0].equalsIgnoreCase("join")) {
							if (player.hasPermission("splegg.join")) {
								if (u.getGame() != null && u.isAlive()) {
									SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are already playing.");
								} else {
									name = args[1];
									if (SpleggOG.getPlugin().maps.mapExists(name)) {
										game = SpleggOG.getPlugin().games.getGame(name);
										if (game != null && SpleggOG.getPlugin().maps.getMap(name).isUsable()) {
											game.joinGame(u);
										} else {
											SpleggOG.getPlugin().chat.sendMessage(player, "This map is incorrectly setup - See console for detailed output.");
										}
									} else {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist.");
									}
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
							}
						} else if (args[0].equalsIgnoreCase("leave")) {
							SpleggOG.getPlugin().chat.sendMessage(player, "Please use &e/" + tag + " leave");
						} else if (args[0].equalsIgnoreCase("") && !args[1].equalsIgnoreCase("") && !args[1].equalsIgnoreCase("") && !args[1].equalsIgnoreCase("")) {
							SpleggOG.getPlugin().chat.sendMessage(player, "Usage: /" + tag + " help <player|mod|admin>");
						}
					}
				} else if (args.length == 3) {
					if (args[0].equalsIgnoreCase("setspawn")) {
						if (player.hasPermission("splegg.admin")) {
							name = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(name)) {
								map = SpleggOG.getPlugin().maps.getMap(name);
								if (args[2].equalsIgnoreCase("next")) {
									map.addSpawn(player.getLocation());
									SpleggOG.getPlugin().chat.sendMessage(player, "Spawn &a" + map.getSpawnCount() + "&6 set for map &c" + map.getName() + "&6.");
								} else {
									try {
										int id = Integer.parseInt(args[2]);
										if (this.spawnset(id, map)) {
											map.setSpawn(id, player.getLocation());
											SpleggOG.getPlugin().chat.sendMessage(player, "You have re-set the spawn " + id + " for map " + name + ".");
										} else {
											SpleggOG.getPlugin().chat.sendMessage(player, "Please set the spawn using &e/" + tag + " setspawn <mapname> next &6then try this command again.");
										}
									} catch (NumberFormatException var11) {
										SpleggOG.getPlugin().chat.sendMessage(player, "&cPlease type a number.");
									}
								}
							} else {
								SpleggOG.getPlugin().chat.sendMessage(player, "&cMap does not exist!");
							}
						} else {
							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission.");
						}
					} else {
						SpleggOG.getPlugin().chat.sendMessage(player, "Usage: &a/" + tag + " setspawn <mapname> <next|spawnid>");
					}
				} else {
					SpleggOG.getPlugin().chat.sendMessage(player, "Incorrect Usage!");
				}
			}
		}

		return false;
	}

	boolean lobbyset() {
		try {
			SpleggOG.getPlugin().config.getLobby();
			return true;
		} catch (Exception var2) {
			return false;
		}
	}

	boolean spawnset(int i, Map map) {
		return map.getConfig().isString("Spawns." + i + ".world");
	}

	public void sendUsage(Player player, String tag, String usage, String def) {
		SpleggOG.getPlugin().chat.sendMessage(player, "&c/" + tag + " &d" + usage + " &5- &b" + def);
	}
}