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
import utils.Utils;

public class SpleggCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String tag, String[] args) {

		if (sender instanceof Player) {

			Player player = (Player) sender;
			UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

			if (args.length == 0) {

				Utils.spleggOGMessage(player, "&aPlugin created by MrLuangamer, updated by Hraponssi, now maintained by NotAlexNoyle for true-og.net. &6For more information: /splegg help");

			}
			else if (args.length == 1) {

				if (args[0].equalsIgnoreCase("join")) {

					if (player.hasPermission("splegg.join")) {

						if (u.getGame() != null && u.isAlive() && u.getGame().getLobbyCount() > 0) {

							player.teleport(u.getGame().getMap().getLobby());

						}
						else {

							// Test to see if the user entered any text or not. If they didn't, an ArrayIndexOutOfBoundsException is thrown.
							try {

								String gameThatWasNotFound = args[1];

								Utils.spleggOGMessage(player, "&cERROR: Failed to start game: &6" + gameThatWasNotFound + "&c! &eSyntax: /splegg join &6mapname.");

							}
							catch(ArrayIndexOutOfBoundsException error) {

								Utils.spleggOGMessage(player, "&cERROR: The game to join was unspecified! &6Syntax: &e/splegg join mapname.");

							}


						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("leave")) {

					if (u.getGame() != null && u.isAlive()) {

						Game game = u.getGame();
						game.leaveGame(u);

					}
					else {

						Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

					}

				}
				else if (args[0].equalsIgnoreCase("start")) {

					if (player.hasPermission("splegg.admin")) {

						if (u.getGame() == null) {

							Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");
						}
						else if (u.getGame().getStatus() == Status.LOBBY) {

							if(u.getGame().getPlayers().size() >= 2) {

								SpleggOG.getPlugin().game.startGame(u.getGame());

								Utils.spleggOGMessage(player, "&aGame started!");

							}
							else {

								Utils.spleggOGMessage(player, "&CERROR: There are not enough players in the lobby to start the game. &6Players required: &e" + u.getGame().getMap().getSpawnCount() + "&6.");

							}

						}
						else if (u.getGame().getStatus() == Status.INGAME) {

							Utils.spleggOGMessage(player, "&cERROR: The game has already begun!");

						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("stop")) {

					if (player.hasPermission("splegg.admin")) {

						if (u.getGame() == null) {

							Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

						}
						else if (u.getGame().getStatus() == Status.LOBBY) {

							Utils.spleggOGMessage(player, "&cERROR: The game has not begun yet!");

						}
						else if (u.getGame().getStatus() == Status.INGAME) {

							SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", u.getGame());

							SpleggOG.getPlugin().game.stopGame(u.getGame(), u.getGame().players.size());

							Utils.spleggOGMessage(player, "&6You have stopped the game.");

						}

					}
					else {

						permissionMessage(player);

					}

				}
				else if (args[0].equalsIgnoreCase("setlobby")) {

					if (player.hasPermission("splegg.admin")) {

						SpleggOG.getPlugin().config.setLobby(player.getLocation());

						Utils.spleggOGMessage(player, "&aThe lobby has been set.");

					}
					else {

						permissionMessage(player);

					}

				}
				else if (!args[0].equalsIgnoreCase("")) {

					Utils.spleggOGMessage(player, "&cIncorrect Usage! &6Applicable commands are: &e/" + tag + " &6<&ejoin&6, &eleave&6, &ehelp&6>");

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
										Utils.spleggOGMessage(player, "&aSpawn &6" + map.getSpawnCount() + " &aset for map: &e" + map.getName() + "&a.");

									}
									else {

										int secondUserCommandArgumentIsInteger;
										try {

											secondUserCommandArgumentIsInteger = Integer.parseInt(args[2]);
											if (this.spawnset(secondUserCommandArgumentIsInteger, map)) {

												map.setSpawn(map, secondUserCommandArgumentIsInteger, player.getLocation());

												Utils.spleggOGMessage(player, "&aThe spawn point &6" + secondUserCommandArgumentIsInteger + " &afor map: &e" + firstUserCommandArgument + "&a has been re-set.");

											}
											else {

												Utils.spleggOGMessage(player, "&ERROR: The spawn point: &6" + secondUserCommandArgumentIsInteger + " &cdoes not yet exist for map: &e" + map.getName() + "&c.");
												usageMessage(player, tag);

											}

										}
										catch(Exception error) {

											usageMessage(player, tag);

										}

									}

								}
								catch(ArrayIndexOutOfBoundsException error) {

									map.addSpawn(player.getLocation(), SpleggOG.getPlugin().games.getGame(map.getName()));
									Utils.spleggOGMessage(player, "&aThe spawn: &6" + map.getSpawnCount() + " &ahas been set for the map: &e" + map.getName() + "&a.");

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

								Utils.spleggOGMessage(player, "&cERROR: The map: &e" + firstUserCommandArgument + " &calready exists.");

							}
							else {

								SpleggOG.getPlugin().maps.c.addMap(firstUserCommandArgument);
								SpleggOG.getPlugin().maps.addMap(firstUserCommandArgument);

								map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
								game = new Game(SpleggOG.getPlugin(), map);
								game.setStatus(Status.DISABLED);
								SpleggOG.getPlugin().games.addGame(map.getName(), game);

								Utils.spleggOGMessage(player, "&aThe map: &e" + firstUserCommandArgument + " &ahas been created. It will be &cDISABLED &auntil at least one spawn point and one floor have been added.");

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

								Utils.spleggOGMessage(player, "&aThe map: &e" + firstUserCommandArgument + " &ahas been deleted.");

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

									Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

								}
								else if (game.getStatus() == Status.LOBBY) {

									if(u.getGame().getPlayers().size() >= 2) {

										Utils.spleggOGMessage(player, "&eStarting Game " + firstUserCommandArgument + "...");

										SpleggOG.getPlugin().game.startGame(game);

									}

								}
								else if (game.getStatus() == Status.INGAME) {

									// TODO: Change this for spectator mode when it is implemented.
									Utils.spleggOGMessage(player, "&cERROR: The game has already begun.");

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

									Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

								}
								else if (game.getStatus() == Status.LOBBY) {

									Utils.spleggOGMessage(player, "&cERROR: The game has not begun yet!");

								}
								else if (game.getStatus() == Status.INGAME) {

									SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", game);

									SpleggOG.getPlugin().game.stopGame(game, u.getGame().players.size());

									Utils.spleggOGMessage(player, "&6You have stopped the game.");

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
								Utils.spleggOGMessage(player, "&aThe lobby for map: &e" + firstUserCommandArgument + "&a has been set to: &2" + player.getLocation() + "&a.");

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

									Utils.spleggOGMessage(player, "&cERROR: The area you have selected is incomplete.");

								}

								if (sel == null) {

									Utils.spleggOGMessage(player, "&4Please select an area with worldedit.");

								}
								else {

									map.addFloor(new Location(player.getWorld(), sel.getMinimumPoint().x(), sel.getMinimumPoint().y(), sel.getMinimumPoint().z()), new Location(player.getWorld(), sel.getMaximumPoint().x(), sel.getMaximumPoint().y(), sel.getMaximumPoint().z()), SpleggOG.getPlugin().games.getGame(map.getName()));

									Utils.spleggOGMessage(player, "&aFloor &6" + map.getFloors() + " &aadded to map: &e" + map.getName() + "&a.");

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

							if (u.getGame() != null) {

								Utils.spleggOGMessage(player, "&cERROR: You are already playing.");

							}
							else {

								firstUserCommandArgument = args[1];
								if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

									game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
									if (game != null && SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument).isUsable(game.getMap())) {

										game.joinGame(u);

									}
									else {

										Utils.spleggOGMessage(player, "&cERROR: The map: &e" + firstUserCommandArgument + " &cis incorrectly set up. &6Please read the README.md file for instructions.");

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

						Utils.spleggOGMessage(player, "&4Please use &e/" + tag + " leave");

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

	void permissionMessage(Player player) {

		Utils.spleggOGMessage(player, "&cERROR: You do not have permission to do that.");

	}

	void usageMessage(Player player, String tag) {

		Utils.spleggOGMessage(player, "&cERROR: Incorrect syntax. &6Use the command: &e/splegg setspawn [MAPNAME] &6to create spawn points.");

	}

	void noMapMessage(Player player, String mapUserTriedToReferTo) {

		Utils.spleggOGMessage(player, "&cERROR: The map: &e" + mapUserTriedToReferTo + " &cdoes not exist.");

	}

	boolean spawnset(int i, Map map) {

		return map.getConfig().isString("Spawns." + i + ".world");

	}

	public void sendUsage(Player player, String tag, String usage, String def) {

		Utils.spleggOGMessage(player, "&c/" + tag + " &d" + usage + " &5- &b" + def);

	}

}