package commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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

public class SpleggCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBS = Arrays.asList("join", "leave", "help", "list", "random");
    private static final List<String> ADMIN_SUBS = Arrays.asList("create", "delete", "setspawn", "setlobby", "addfloor",
            "start", "stop", "info");
    private static final List<String> MAP_ARG_SUBS = Arrays.asList("join", "create", "delete", "setspawn", "setlobby",
            "addfloor", "start", "stop", "info");

    private boolean ensureSpleggWorld(Player player) {

        if (SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return true;

        }

        Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotInSpleggWorld"));
        return false;

    }

    public boolean onCommand(CommandSender sender, Command cmd, String tag, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            UtilPlayer u = SpleggOG.getPlugin().pm.getPlayer(player);

            if (args.length == 0) {

                Utils.spleggOGMessage(player,
                        "&aPlugin created by MrLuangamer, updated by Hraponssi, now maintained by NotAlexNoyle for true-og.net. &6For more information: /splegg help");

            } else if (args.length == 1) {

                if (args[0].equalsIgnoreCase("help")) {

                    sendHelp(player, tag);

                } else if (args[0].equalsIgnoreCase("list")) {

                    sendMapList(player);

                } else if (args[0].equalsIgnoreCase("random")) {

                    if (player.hasPermission("splegg.join")) {

                        joinRandomMap(player, u);

                    } else {

                        permissionMessage(player);

                    }

                } else if (args[0].equalsIgnoreCase("join")) {

                    if (player.hasPermission("splegg.join")) {

                        if (u.getGame() != null && u.isAlive() && u.getGame().getLobbyCount() > 0) {

                            player.teleport(u.getGame().getMap().getLobby());

                        } else {

                            // Test to see if the user entered any text or not. If they didn't, an
                            // ArrayIndexOutOfBoundsException is thrown.
                            try {

                                String gameThatWasNotFound = args[1];

                                Utils.spleggOGMessage(player, "&cERROR: Failed to start game: &6" + gameThatWasNotFound
                                        + "&c! &eSyntax: /splegg join &6mapname.");

                            } catch (ArrayIndexOutOfBoundsException error) {

                                Utils.spleggOGMessage(player,
                                        "&cERROR: The game to join was unspecified! &6Syntax: &e/splegg join mapname.");

                            }

                        }

                    } else {

                        permissionMessage(player);

                    }

                } else if (args[0].equalsIgnoreCase("leave")) {

                    if (u.getGame() != null && u.isAlive()) {

                        Game game = u.getGame();
                        game.leaveGame(u);

                    } else {

                        Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                    }

                } else if (args[0].equalsIgnoreCase("start")) {

                    if (player.hasPermission("splegg.admin")) {

                        if (u.getGame() == null) {

                            Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                        } else if (u.getGame().getStatus() == Status.LOBBY) {

                            if (u.getGame().getPlayers().size() >= 2) {

                                SpleggOG.getPlugin().game.startGame(u.getGame());

                                Utils.spleggOGMessage(player, "&aGame started!");

                            } else {

                                Utils.spleggOGMessage(player,
                                        "&CERROR: There are not enough players in the lobby to start the game. &6Players required: &e"
                                                + u.getGame().getMap().getSpawnCount() + "&6.");

                            }

                        } else if (u.getGame().getStatus() == Status.INGAME) {

                            Utils.spleggOGMessage(player, "&cERROR: The game has already begun!");

                        }

                    } else {

                        permissionMessage(player);

                    }

                } else if (args[0].equalsIgnoreCase("stop")) {

                    if (player.hasPermission("splegg.admin")) {

                        if (u.getGame() == null) {

                            Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                        } else if (u.getGame().getStatus() == Status.LOBBY) {

                            Utils.spleggOGMessage(player, "&cERROR: The game has not begun yet!");

                        } else if (u.getGame().getStatus() == Status.INGAME) {

                            SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.",
                                    u.getGame());

                            SpleggOG.getPlugin().game.stopGame(u.getGame(), u.getGame().players.size());

                            Utils.spleggOGMessage(player, "&6You have stopped the game.");

                        }

                    } else {

                        permissionMessage(player);

                    }

                } else if (args[0].equalsIgnoreCase("setlobby")) {

                    if (player.hasPermission("splegg.admin")) {

                        if (!ensureSpleggWorld(player)) {

                            return false;

                        }

                        SpleggOG.getPlugin().config.setLobby(player.getLocation());

                        Utils.spleggOGMessage(player, "&aThe lobby has been set.");

                    } else {

                        permissionMessage(player);

                    }

                } else if (!args[0].equalsIgnoreCase("")) {

                    Utils.spleggOGMessage(player, "&cIncorrect Usage! &6Applicable commands are: &e/" + tag
                            + " &6<&ejoin&6, &eleave&6, &ehelp&6>");

                }

            } else {

                Map map;
                String firstUserCommandArgument;
                Game game;
                if (args.length == 2 || args.length == 3) {

                    if (args[0].equalsIgnoreCase("setspawn")) {

                        if (player.hasPermission("splegg.admin")) {

                            if (!ensureSpleggWorld(player)) {

                                return false;

                            }

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
                                String secondUserCommandArgumentIsText;
                                try {

                                    secondUserCommandArgumentIsText = args[2];
                                    if (secondUserCommandArgumentIsText.equalsIgnoreCase("next")) {

                                        map.addSpawn(player.getLocation(),
                                                SpleggOG.getPlugin().games.getGame(map.getName()));
                                        Utils.spleggOGMessage(player, "&aSpawn &6" + map.getSpawnCount()
                                                + " &aset for map: &e" + map.getName() + "&a.");

                                    } else {

                                        int secondUserCommandArgumentIsInteger;
                                        try {

                                            secondUserCommandArgumentIsInteger = Integer.parseInt(args[2]);
                                            if (this.spawnset(secondUserCommandArgumentIsInteger, map)) {

                                                map.setSpawn(map, secondUserCommandArgumentIsInteger,
                                                        player.getLocation());

                                                Utils.spleggOGMessage(player,
                                                        "&aThe spawn point &6" + secondUserCommandArgumentIsInteger
                                                                + " &afor map: &e" + firstUserCommandArgument
                                                                + "&a has been re-set.");

                                            } else {

                                                Utils.spleggOGMessage(player, "&ERROR: The spawn point: &6"
                                                        + secondUserCommandArgumentIsInteger
                                                        + " &cdoes not yet exist for map: &e" + map.getName() + "&c.");
                                                usageMessage(player, tag);

                                            }

                                        } catch (Exception error) {

                                            usageMessage(player, tag);

                                        }

                                    }

                                } catch (ArrayIndexOutOfBoundsException error) {

                                    map.addSpawn(player.getLocation(),
                                            SpleggOG.getPlugin().games.getGame(map.getName()));
                                    Utils.spleggOGMessage(player, "&aThe spawn: &6" + map.getSpawnCount()
                                            + " &ahas been set for the map: &e" + map.getName() + "&a.");

                                }

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("create")) {

                        if (player.hasPermission("splegg.admin")) {

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                Utils.spleggOGMessage(player,
                                        "&cERROR: The map: &e" + firstUserCommandArgument + " &calready exists.");

                            } else {

                                SpleggOG.getPlugin().maps.c.addMap(firstUserCommandArgument);
                                SpleggOG.getPlugin().maps.addMap(firstUserCommandArgument);

                                map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
                                game = new Game(SpleggOG.getPlugin(), map);
                                game.setStatus(Status.DISABLED);
                                SpleggOG.getPlugin().games.addGame(map.getName(), game);

                                Utils.spleggOGMessage(player, "&aThe map: &e" + firstUserCommandArgument
                                        + " &ahas been created. It will be &cDISABLED &auntil at least one spawn point and one floor have been added.");

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("delete")) {

                        if (player.hasPermission("splegg.admin")) {

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                SpleggOG.getPlugin().maps.deleteMap(firstUserCommandArgument);

                                Utils.spleggOGMessage(player,
                                        "&aThe map: &e" + firstUserCommandArgument + " &ahas been deleted.");

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("start")) {

                        if (player.hasPermission("splegg.admin")) {

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
                                if (game == null) {

                                    Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                                } else if (game.getStatus() == Status.LOBBY) {

                                    if (game.getPlayers().size() >= 2) {

                                        Utils.spleggOGMessage(player,
                                                "&eStarting Game " + firstUserCommandArgument + "...");

                                        SpleggOG.getPlugin().game.startGame(game);

                                    } else {

                                        Utils.spleggOGMessage(player,
                                                "&cERROR: There are not enough players in the lobby to start the game.");

                                    }

                                } else if (game.getStatus() == Status.INGAME) {

                                    // TODO: Change this for spectator mode when it is implemented.
                                    Utils.spleggOGMessage(player, "&cERROR: The game has already begun.");

                                }

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("stop")) {

                        if (player.hasPermission("splegg.admin")) {

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
                                if (game == null) {

                                    Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                                } else if (game.getStatus() == Status.LOBBY) {

                                    Utils.spleggOGMessage(player, "&cERROR: The game has not begun yet!");

                                } else if (game.getStatus() == Status.INGAME) {

                                    SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.",
                                            game);

                                    SpleggOG.getPlugin().game.stopGame(game, game.getPlayers().size());

                                    Utils.spleggOGMessage(player, "&6You have stopped the game.");

                                }

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("setlobby")) {

                        if (player.hasPermission("splegg.admin")) {

                            if (!ensureSpleggWorld(player)) {

                                return false;

                            }

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument)
                                        .setLobby(player.getLocation());
                                Utils.spleggOGMessage(player, "&aThe lobby for map: &e" + firstUserCommandArgument
                                        + "&a has been set to: &2" + player.getLocation() + "&a.");

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("addfloor")) {

                        if (player.hasPermission("splegg.admin")) {

                            if (!ensureSpleggWorld(player)) {

                                return false;

                            }

                            firstUserCommandArgument = args[1];
                            if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                map = SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument);
                                WorldEditPlugin we = SpleggOG.getPlugin().getWorldEdit();
                                Region sel = null;
                                try {

                                    sel = we.getSession(player).getSelection(new BukkitWorld(player.getWorld()));

                                } catch (IncompleteRegionException error) {

                                    Utils.spleggOGMessage(player, "&cERROR: The area you have selected is incomplete.");

                                }

                                if (sel == null) {

                                    Utils.spleggOGMessage(player, "&4Please select an area with worldedit.");

                                } else {

                                    map.addFloor(
                                            new Location(player.getWorld(), sel.getMinimumPoint().x(),
                                                    sel.getMinimumPoint().y(), sel.getMinimumPoint().z()),
                                            new Location(player.getWorld(), sel.getMaximumPoint().x(),
                                                    sel.getMaximumPoint().y(), sel.getMaximumPoint().z()),
                                            SpleggOG.getPlugin().games.getGame(map.getName()));

                                    Utils.spleggOGMessage(player, "&aFloor &6" + map.getFloors() + " &aadded to map: &e"
                                            + map.getName() + "&a.");

                                }

                            } else {

                                noMapMessage(player, firstUserCommandArgument);

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("join")) {

                        if (player.hasPermission("splegg.join")) {

                            if (u.getGame() != null) {

                                Utils.spleggOGMessage(player, "&cERROR: You are already playing.");

                            } else {

                                firstUserCommandArgument = args[1];
                                if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                                    game = SpleggOG.getPlugin().games.getGame(firstUserCommandArgument);
                                    if (game != null && SpleggOG.getPlugin().maps.getMap(firstUserCommandArgument)
                                            .isUsable(game.getMap()))
                                    {

                                        game.joinGame(u);

                                    } else {

                                        Utils.spleggOGMessage(player, "&cERROR: The map: &e" + firstUserCommandArgument
                                                + " &cis incorrectly set up. &6Please read the README.md file for instructions.");

                                    }

                                } else {

                                    noMapMessage(player, firstUserCommandArgument);

                                }

                            }

                        } else {

                            permissionMessage(player);

                        }

                    } else if (args[0].equalsIgnoreCase("info")) {

                        if (!player.hasPermission("splegg.admin")) {

                            permissionMessage(player);
                            return false;

                        }

                        firstUserCommandArgument = args[1];
                        if (SpleggOG.getPlugin().maps.mapExists(firstUserCommandArgument)) {

                            sendMapInfo(player, firstUserCommandArgument);

                        } else {

                            noMapMessage(player, firstUserCommandArgument);

                        }

                    } else if (args[0].equalsIgnoreCase("leave")) {

                        Utils.spleggOGMessage(player, "&4Please use &e/" + tag + " leave");

                    } else {

                        usageMessage(player, tag);

                    }

                } else {

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

        Utils.spleggOGMessage(player,
                "&cERROR: Incorrect syntax. &6Use the command: &e/splegg setspawn [MAPNAME] &6to create spawn points.");

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

    private void sendHelp(Player player, String tag) {

        final boolean admin = player.hasPermission("splegg.admin");
        Utils.spleggOGMessage(player, "&6Splegg-OG commands:");
        sendUsage(player, tag, "join <map>", "Join a specific map lobby.");
        sendUsage(player, tag, "random", "Join a random playable map.");
        sendUsage(player, tag, "leave", "Leave your current match or lobby.");
        sendUsage(player, tag, "list", "List every configured map and its status.");
        sendUsage(player, tag, "help", "Show this help message.");

        if (!admin) {

            return;

        }

        Utils.spleggOGMessage(player, "&cAdmin commands:");
        sendUsage(player, tag, "create <map>", "Create a new map (disabled until floor + spawn exist).");
        sendUsage(player, tag, "delete <map>", "Delete a map.");
        sendUsage(player, tag, "info <map>", "Show map setup status (spawns, floors, lobby).");
        sendUsage(player, tag, "setspawn <map> [next|#]", "Add or update a spawn point.");
        sendUsage(player, tag, "setlobby <map>", "Set the lobby teleport point.");
        sendUsage(player, tag, "addfloor <map>", "Add a WorldEdit selection as a floor.");
        sendUsage(player, tag, "start [map]", "Start a map early.");
        sendUsage(player, tag, "stop [map]", "Stop an in-progress match.");

    }

    private void sendMapList(Player player) {

        if (SpleggOG.getPlugin().maps.getMaps().isEmpty()) {

            Utils.spleggOGMessage(player, "&6No maps have been configured yet.");
            return;

        }

        Utils.spleggOGMessage(player, "&6Splegg maps:");
        for (Map map : SpleggOG.getPlugin().maps.getMaps()) {

            final Game game = SpleggOG.getPlugin().games.getGame(map.getName());
            final String statusLabel;
            final int playerCount;
            if (game == null) {

                statusLabel = "&8UNKNOWN";
                playerCount = 0;

            } else {

                switch (game.getStatus()) {

                    case LOBBY -> statusLabel = game.isStarting() ? "&eSTARTING" : "&aLOBBY";
                    case INGAME -> statusLabel = "&6INGAME";
                    case DISABLED -> statusLabel = "&cDISABLED";
                    default -> statusLabel = "&7" + game.getStatus();

                }

                playerCount = game.getPlayers().size();

            }

            Utils.spleggOGMessage(player, "&e" + map.getName() + " &7- " + statusLabel + " &7(&f" + playerCount
                    + "&7/&f" + map.getSpawnCount() + "&7)");

        }

    }

    private void sendMapInfo(Player player, String mapName) {

        final Map map = SpleggOG.getPlugin().maps.getMap(mapName);
        final Game game = SpleggOG.getPlugin().games.getGame(mapName);

        Utils.spleggOGMessage(player, "&6Map info: &e" + map.getName());
        Utils.spleggOGMessage(player,
                "&6World: &f" + (map.getWorldName() != null ? map.getWorldName() : "&cunset (set a spawn first)"));
        Utils.spleggOGMessage(player, "&6Spawn points: &f" + map.getSpawnCount());
        Utils.spleggOGMessage(player, "&6Floor regions: &f" + map.getFloors());
        Utils.spleggOGMessage(player,
                "&6Match lobby: " + (map.lobbySet() ? "&aset" : "&cunset (falls back to global lobby)"));

        final boolean playable = game != null && map.isUsable(map);
        Utils.spleggOGMessage(player, "&6Playable: " + (playable ? "&ayes" : "&cno"));

        if (!playable) {

            final List<String> missing = new ArrayList<>();
            if (map.getSpawnCount() <= 0) {

                missing.add("/splegg setspawn " + map.getName());

            }

            if (map.getFloors() <= 0) {

                missing.add("/splegg addfloor " + map.getName());

            }

            if (!missing.isEmpty()) {

                Utils.spleggOGMessage(player, "&6Next steps:");
                for (String step : missing) {

                    Utils.spleggOGMessage(player, "  &e" + step);

                }

            }

        }

    }

    private void joinRandomMap(Player player, UtilPlayer u) {

        if (u.getGame() != null) {

            Utils.spleggOGMessage(player, "&cERROR: You are already playing.");
            return;

        }

        final Map chosen = SpleggOG.getPlugin().maps.getRandomMap();
        if (chosen == null) {

            Utils.spleggOGMessage(player, "&cERROR: No playable maps are currently available.");
            return;

        }

        final Game game = SpleggOG.getPlugin().games.getGame(chosen.getName());
        Utils.spleggOGMessage(player, "&aJoining random map: &e" + chosen.getName() + "&a.");
        game.joinGame(u);

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {

            final List<String> options = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("splegg.admin")) {

                options.addAll(ADMIN_SUBS);

            }

            return filterPrefix(options, args[0]);

        }

        if (args.length == 2 && MAP_ARG_SUBS.contains(args[0].toLowerCase())) {

            final List<String> mapNames = new ArrayList<>();
            for (Map map : SpleggOG.getPlugin().maps.getMaps()) {

                mapNames.add(map.getName());

            }

            return filterPrefix(mapNames, args[1]);

        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {

            return filterPrefix(Collections.singletonList("next"), args[2]);

        }

        return Collections.emptyList();

    }

    private List<String> filterPrefix(List<String> source, String prefix) {

        final String needle = prefix == null ? "" : prefix.toLowerCase();
        final List<String> matched = new ArrayList<>();
        for (String candidate : source) {

            if (candidate.toLowerCase().startsWith(needle)) {

                matched.add(candidate);

            }

        }

        Collections.sort(matched);
        return matched;

    }

}
