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

			Player player = (Player) sender;
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

			if (args.length == 0) {

				SpleggOG.getPlugin().chat.sendMessage(player, "Plugin created by MrLuangamer, updated by Hraponssi, now maintained by NotAlexNoyle for true-og.net. For more information: /splegg help");
			}
			else if (args.length == 1) {

				if (args[0].equalsIgnoreCase("join")) {

					if (player.hasPermission("splegg.join")) {

						if (u.getGame() != null && u.isAlive()) {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are already playing.");

						}
						else if (isLobby(u)) {

							player.teleport(SpleggOG.getPlugin().config.getLobby(player));

						}
						else {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cSplegg is incorrectly setup! Ask an admin to set the lobby.");

						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("leave")) {

					if (u.getGame() != null && u.isAlive()) {

						Game game = u.getGame();
						game.leaveGame(u.getPlayer());

						SpleggOG.getPlugin().chat.sendMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.LeaveGame").replaceAll("&", "§").replaceAll("%map%", game.getMap().getName()));

					}

				}
				else if (args[0].equalsIgnoreCase("start")) {

					if (player.hasPermission("splegg.admin")) {

						if (u.getGame() == null) {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");

						}
						else if (u.getGame().getStatus() == Status.LOBBY) {

							SpleggOG.getPlugin().game.startGame(u.getGame());

							SpleggOG.getPlugin().chat.sendMessage(player, "&eGame started!");

						}
						else if (u.getGame().getStatus() == Status.INGAME) {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cGame has already begun.");

						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("stop")) {

					if (player.hasPermission("splegg.admin")) {

						if (u.getGame() == null) {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");

						}
						else if (u.getGame().getStatus() == Status.LOBBY) {

							SpleggOG.getPlugin().chat.sendMessage(player, "&cThe game has not begun yet!");

						}
						else if (u.getGame().getStatus() == Status.INGAME) {

							SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", u.getGame());

							SpleggOG.getPlugin().game.stopGame(u.getGame(), 1);

							SpleggOG.getPlugin().chat.sendMessage(player, "&eYou have stopped the game.");

						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("setlobby")) {

					if (player.hasPermission("splegg.admin")) {

						SpleggOG.getPlugin().config.setLobby(player.getLocation());

						SpleggOG.getPlugin().chat.sendMessage(player, "&aYou have set the splegg lobby.");

					}
					else {

						permissionMessage(player);

					}

				}
				else if (!args[0].equalsIgnoreCase("")) {

					SpleggOG.getPlugin().chat.sendMessage(player, "&cIncorrect Usage. &6Applicable commands are: &e/" + tag + " &6<&ejoin&6, &eleave&6, &ehelp&6>");

				}

			}
			else {

				Map map;
				String firstUserCommandArgument;
				Game game;
				if (args.length == 2 || args.length == 3) {

					if (args[0].equalsIgnoreCase("setspawn")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
								String secondUserCommandArgumentIsText;
								try {

									secondUserCommandArgumentIsText = args[2];
									if (secondUserCommandArgumentIsText.equalsIgnoreCase("next")) {

										map.addSpawn(player.getLocation(), SpleggOG.getPlugin().games.getGame(map.getName()));
										SpleggOG.getPlugin().chat.sendMessage(player, "&aSpawn &6" + map.getSpawnCount() + " &aset for map: &e" + map.getName() + "&a.");

									}
									else {

										int secondUserCommandArgumentIsInteger;
										try {

											secondUserCommandArgumentIsInteger = Integer.parseInt(args[2]);
											if (this.spawnset(secondUserCommandArgumentIsInteger, map)) {

												map.setSpawn(map, secondUserCommandArgumentIsInteger, player.getLocation());

												SpleggOG.getPlugin().chat.sendMessage(player, "&aYou have re-set the spawn point &6" + secondUserCommandArgumentIsInteger + " &afor map: &e" + firstUserCommandArgument + "&a.");

											}
											else {

												SpleggOG.getPlugin().chat.sendMessage(player, "&cThe spawn point: &6" + secondUserCommandArgumentIsInteger + " &cdoes not yet exist for map: &e" + map.getName() + "&c.");
												usageMessage(player, tag);

											}

										}
										catch(Exception error) {

											// TODO: Remove dev logger
											SpleggOG.getPlugin().getLogger().info(error.getMessage());
											usageMessage(player, tag);

										}

									}

								}
								catch(ArrayIndexOutOfBoundsException error) {

									map.addSpawn(player.getLocation(), SpleggOG.getPlugin().games.getGame(map.getName()));
									SpleggOG.getPlugin().chat.sendMessage(player, "&aSpawn &6" + map.getSpawnCount() + " &aset for map: &e" + map.getName() + "&a.");

								}

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("create")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								SpleggOG.getPlugin().chat.sendMessage(player, "&cThe map: &e" + firstUserCommandArgument + " &calready exists.");

							}
							else {

								SpleggOG.getPlugin().maps.c.addMap(firstUserCommandArgument);
								SpleggOG.getPlugin().maps.addMap(firstUserCommandArgument);

								map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
								game = new Game(SpleggOG.getPlugin(), map);
								game.setStatus(Status.DISABLED);
								SpleggOG.getPlugin().games.addGame(map.getName(), game);

								SpleggOG.getPlugin().chat.sendMessage(player, "&aThe map: &e" + firstUserCommandArgument + " &ahas been created. It will be &cDISABLED &auntil at least one spawn point and one floor have been added.");

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("delete")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								SpleggOG.getPlugin().maps.deleteMap(firstUserCommandArgument);

								SpleggOG.getPlugin().chat.sendMessage(player, "&aThe map: &e" + firstUserCommandArgument + " has been deleted.");

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if(args[0].equalsIgnoreCase("start")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
								if (game == null) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");

								}
								else if (game.getStatus() == Status.LOBBY) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&eStarting " + firstUserCommandArgument + "...");

									SpleggOG.getPlugin().game.startGame(game);

								}
								else if (game.getStatus() == Status.INGAME) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&cThe match has already begun.");

								}

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}
					}
					else if (args[0].equalsIgnoreCase("stop")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
								if (game == null) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are not in a game.");

								}
								else if (game.getStatus() == Status.LOBBY) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&cGame has not begun yet!");

								}
								else if (game.getStatus() == Status.INGAME) {

									SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", game);

									SpleggOG.getPlugin().game.stopGame(game, 1);

									SpleggOG.getPlugin().chat.sendMessage(player, "&eYou have stopped the game.");

								}

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("setlobby")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument).setLobby(player.getLocation());
								SpleggOG.getPlugin().chat.sendMessage(player, "&aLobby for map: &e" + firstUserCommandArgument + "&a has been set to: &2" + player.getLocation() + "&a.");

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("addfloor")) {

						if (player.hasPermission("splegg.admin")) {

							firstUserCommandArgument = args[1];
							if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

								map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
								WorldEditPlugin we = SpleggOG.getPlugin().getWorldEdit();
								Region sel = null;
								try {

									sel = we.getSession(player).getSelection(new BukkitWorld(player.getWorld()));

								}
								catch (IncompleteRegionException error) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&cERROR: The area you have selected is incomplete.");

								}

								if (sel == null) {

									SpleggOG.getPlugin().chat.sendMessage(player, "&5Please select an area with worldedit.");

								}
								else {

									map.addFloor(new Location(player.getWorld(), sel.getMinimumPoint().getBlockX(), sel.getMinimumPoint().getBlockY(), sel.getMinimumPoint().getBlockZ()), new Location(player.getWorld(), sel.getMaximumPoint().getBlockX(), sel.getMaximumPoint().getBlockY(), sel.getMaximumPoint().getBlockZ()), SpleggOG.getPlugin().games.getGame(map.getName()));

									SpleggOG.getPlugin().chat.sendMessage(player, "&aFloor &6" + map.getFloors() + " &aadded to map: &e" + map.getName() + "&a.");

								}

							}
							else {

								noMapMessage(player, firstUserCommandArgument);

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("join")) {

						if (player.hasPermission("splegg.join")) {

							if (u.getGame() != null && u.isAlive()) {

								SpleggOG.getPlugin().chat.sendMessage(player, "&cYou are already playing.");

							}
							else {

								firstUserCommandArgument = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

									game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
									if (game != null && SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument).isUsable(game.getMap())) {

										game.joinGame(u);

									}
									else {

										SpleggOG.getPlugin().chat.sendMessage(player, "&cThe map: &e" + firstUserCommandArgument + " &cis incorrectly set up.");

									}

								}
								else {

									noMapMessage(player, firstUserCommandArgument);

								}

							}

						}
						else {

							permissionMessage(player);

						}

					}
					else if (args[0].equalsIgnoreCase("leave")) {

						SpleggOG.getPlugin().chat.sendMessage(player, "Please use &e/" + tag + " leave");

					}
					else {

						usageMessage(player, tag);

					}

				}
				else {

					usageMessage(player, tag);

				}

			}

		}

		return false;

	}
	
	private boolean isLobby(UtilPlayer u) {
		
		if(u.getGame().getLobbyCount() > 0) {
			
			return true;
			
		}
		else {
			
			return false;
			
		}
		
	}

	void permissionMessage(Player player) {

		SpleggOG.getPlugin().chat.sendMessage(player, "&cYou do not have permission to do that.");

	}

	void usageMessage(Player player, String tag) {

		SpleggOG.getPlugin().chat.sendMessage(player, "&cUse the command: &e/splegg setspawn [MAPNAME] &cto create spawn points.");

	}

	void noMapMessage(Player player, String mapUserTriedToReferTo) {

		SpleggOG.getPlugin().chat.sendMessage(player, "&cERROR: The map: &e" + mapUserTriedToReferTo + " &cdoes not exist.");

	}

	boolean spawnset(int i, Map map) {

		return map.getConfig().isString("Spawns." + i + ".world");

	}

	public void sendUsage(Player player, String tag, String usage, String def) {

		SpleggOG.getPlugin().chat.sendMessage(player, "&c/" + tag + " &d" + usage + " &5- &b" + def);

	}

}