package commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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

    private static final List<String> PLAYER_SUBS = Arrays.asList("join", "leave", "help", "list", "maps");
    private static final List<String> ADMIN_SUBS = Arrays.asList("create", "delete", "setspawn", "setlobby", "addfloor",
            "start", "stop", "info");
    private static final List<String> MAP_ARG_SUBS = Arrays.asList("create", "delete", "setspawn", "addfloor", "info");
    private static final List<String> LOBBY_ARG_SUBS = Arrays.asList("join", "start", "stop");
    // Map names become file names under the data folder, so they stay on one path
    // segment.
    private static final Pattern MAP_NAME = Pattern.compile("[A-Za-z0-9_-]{1,48}");

    private boolean ensureSpleggWorld(Player player) {

        if (SpleggOG.getPlugin().isSpleggWorld(player.getWorld())) {

            return true;

        }

        if (SpleggOG.isProtectedMainWorld(player.getWorld())) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.InProtectedMainWorld",
                    "&cERROR: Splegg cannot modify the vanilla overworld, nether, or end. Move to a Splegg lobby or in-game world first."));
            return false;

        }

        Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NotInSpleggWorld"));
        return false;

    }

    // Joins the named lobby (SP1 or 1); with no name, lists the lobbies. Shared
    // with /spjoin.
    public static void join(Player player, UtilPlayer u, String lobbyArgument) {

        if (!player.hasPermission("splegg.join")) {

            Utils.spleggOGMessage(player, SpleggOG.getPlugin().getConfig().getString("Messages.NoPermission"));
            return;

        }

        if (u.getGame() != null) {

            Utils.spleggOGMessage(player, "&cERROR: You are already in lobby &e" + u.getGame().getLobbyId() + "&c.");
            return;

        }

        // No lobby named: show the list, TheHerobrine-OG style. Each line is
        // clickable, so picking one is a single click.
        if (lobbyArgument == null || lobbyArgument.isBlank()) {

            SpleggOG.getPlugin().games.sendLobbyMessage(player);
            return;

        }

        final Game game = SpleggOG.getPlugin().games.resolveLobby(lobbyArgument);
        if (game == null) {

            Utils.spleggOGMessage(player, "&cLobby &e" + lobbyArgument + " &cdoes not exist.");
            SpleggOG.getPlugin().games.sendLobbyMessage(player);
            return;

        }

        game.joinGame(u);

    }

    public boolean onCommand(CommandSender sender, Command cmd, String tag, String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage("Only players can use this command.");
            return true;

        }

        final UtilPlayer u = SpleggOG.getPlugin().pm.track(player);

        if (args.length == 0) {

            Utils.spleggOGMessage(player,
                    "&aPlugin created by MrLuangamer, updated by Hraponssi, now maintained by NotAlexNoyle for true-og.net. &6For more information: /"
                            + tag + " help");
            return true;

        }

        final String sub = args[0].toLowerCase(Locale.ROOT);
        final String arg = args.length > 1 ? args[1] : null;

        switch (sub) {

            case "help" -> sendHelp(player, tag);
            case "list", "lobbies" -> SpleggOG.getPlugin().games.sendLobbyMessage(player);
            case "maps" -> sendMapList(player);
            case "join", "random" -> join(player, u, arg);
            case "leave" -> {

                if (u.getGame() != null && u.isAlive()) {

                    u.getGame().leaveGame(u);

                } else {

                    Utils.spleggOGMessage(player, "&cERROR: You are not in a game!");

                }

            }
            case "start" -> {

                if (!player.hasPermission("splegg.admin")) {

                    permissionMessage(player);
                    return true;

                }

                final Game game = arg == null ? u.getGame() : SpleggOG.getPlugin().games.resolveLobby(arg);
                if (game == null) {

                    Utils.spleggOGMessage(player, arg == null ? "&cERROR: You are not in a lobby!"
                            : "&cLobby &e" + arg + " &cdoes not exist.");

                } else if (game.getStatus() == Status.INGAME) {

                    Utils.spleggOGMessage(player, "&cERROR: The game has already begun!");

                } else if (game.getStatus() != Status.LOBBY) {

                    Utils.spleggOGMessage(player,
                            "&cERROR: Lobby &e" + game.getLobbyId() + " &ccannot start right now.");

                } else if (game.getPlayers().size() < 2) {

                    Utils.spleggOGMessage(player,
                            "&cERROR: There are not enough players in the lobby to start the game. &6Players required: &e2&6.");

                } else {

                    Utils.spleggOGMessage(player, "&eStarting lobby " + game.getLobbyId() + "...");
                    if (SpleggOG.getPlugin().game.startGame(game)) {

                        Utils.spleggOGMessage(player, "&aGame started!");

                    } else {

                        Utils.spleggOGMessage(player, "&cERROR: No playable map could be loaded.");

                    }

                }

            }
            case "stop" -> {

                if (!player.hasPermission("splegg.admin")) {

                    permissionMessage(player);
                    return true;

                }

                final Game game = arg == null ? u.getGame() : SpleggOG.getPlugin().games.resolveLobby(arg);
                if (game == null) {

                    Utils.spleggOGMessage(player, arg == null ? "&cERROR: You are not in a lobby!"
                            : "&cLobby &e" + arg + " &cdoes not exist.");

                } else if (game.getStatus() == Status.LOBBY) {

                    Utils.spleggOGMessage(player, "&cERROR: The game has not begun yet!");

                } else if (game.getStatus() == Status.INGAME) {

                    SpleggOG.getPlugin().chat.bc("&5" + player.getName() + "&6 has stopped the game.", game);
                    SpleggOG.getPlugin().game.stopGame(game, game.getPlayers().size());
                    Utils.spleggOGMessage(player, "&6You have stopped the game.");

                }

            }
            case "setlobby" -> {

                if (!player.hasPermission("splegg.admin")) {

                    permissionMessage(player);
                    return true;

                }

                if (arg != null) {

                    Utils.spleggOGMessage(player,
                            "&cERROR: Match lobbies are no longer per map. Stand in a hub world and run &e/" + tag
                                    + " setlobby &cwith no map name.");
                    return true;

                }

                if (!SpleggOG.getPlugin().isSpleggLobbyWorld(player.getWorld())) {

                    Utils.spleggOGMessage(player,
                            "&cERROR: The queue lobby must be set inside a lobby hub world (one of Worlds.Lobby).");
                    return true;

                }

                SpleggOG.getPlugin().config.setLobby(player.getLocation());
                Utils.spleggOGMessage(player,
                        "&aThe Splegg queue lobby has been set. Every lobby hub uses these coordinates.");

            }
            case "setspawn", "create", "delete", "addfloor", "info" -> handleMapCommand(player, tag, sub, args);
            default -> Utils.spleggOGMessage(player, "&cIncorrect Usage! &6Applicable commands are: &e/" + tag
                    + " &6<&ejoin&6, &eleave&6, &elist&6, &ehelp&6>");

        }

        return true;

    }

    private void handleMapCommand(Player player, String tag, String sub, String[] args) {

        if (!player.hasPermission("splegg.admin")) {

            permissionMessage(player);
            return;

        }

        if (args.length < 2) {

            usageMessage(player, tag);
            return;

        }

        final String mapName = args[1];

        switch (sub) {

            case "create" -> {

                if (!ensureSpleggWorld(player)) {

                    return;

                }

                if (!MAP_NAME.matcher(mapName).matches()) {

                    Utils.spleggOGMessage(player, "&cERROR: Map names may only contain letters, digits, '-' and '_'.");

                } else if (SpleggOG.getPlugin().maps.mapExists(mapName)) {

                    Utils.spleggOGMessage(player, "&cERROR: The map: &e" + mapName + " &calready exists.");

                } else {

                    SpleggOG.getPlugin().maps.c.addMap(mapName);
                    SpleggOG.getPlugin().maps.addMap(mapName);

                    Utils.spleggOGMessage(player, "&aThe map: &e" + mapName
                            + " &ahas been created. It will be &cDISABLED &auntil at least two spawn points and one floor have been added.");

                }

            }
            case "delete" -> {

                if (SpleggOG.getPlugin().maps.mapExists(mapName)) {

                    SpleggOG.getPlugin().maps.deleteMap(mapName);
                    Utils.spleggOGMessage(player, "&aThe map: &e" + mapName + " &ahas been deleted.");

                } else {

                    noMapMessage(player, mapName);

                }

            }
            case "info" -> {

                if (SpleggOG.getPlugin().maps.mapExists(mapName)) {

                    sendMapInfo(player, mapName);

                } else {

                    noMapMessage(player, mapName);

                }

            }
            case "setspawn" -> {

                if (!ensureSpleggWorld(player)) {

                    return;

                }

                if (!SpleggOG.getPlugin().maps.mapExists(mapName)) {

                    noMapMessage(player, mapName);
                    return;

                }

                final Map map = SpleggOG.getPlugin().maps.getMap(mapName);
                if (args.length < 3 || args[2].equalsIgnoreCase("next") || args[2].equalsIgnoreCase("append")) {

                    map.addSpawn(player.getLocation());
                    Utils.spleggOGMessage(player,
                            "&aSpawn &6" + map.getSpawnCount() + " &aset for map: &e" + map.getName() + "&a.");
                    return;

                }

                final int spawnId;
                try {

                    spawnId = Integer.parseInt(args[2]);

                } catch (NumberFormatException error) {

                    usageMessage(player, tag);
                    return;

                }

                if (this.spawnset(spawnId, map)) {

                    map.setSpawn(map, spawnId, player.getLocation());
                    Utils.spleggOGMessage(player,
                            "&aThe spawn point &6" + spawnId + " &afor map: &e" + mapName + "&a has been re-set.");

                } else {

                    Utils.spleggOGMessage(player, "&cERROR: The spawn point: &6" + spawnId
                            + " &cdoes not yet exist for map: &e" + map.getName() + "&c.");
                    usageMessage(player, tag);

                }

            }
            case "addfloor" -> {

                if (!ensureSpleggWorld(player)) {

                    return;

                }

                if (!SpleggOG.getPlugin().maps.mapExists(mapName)) {

                    noMapMessage(player, mapName);
                    return;

                }

                final Map map = SpleggOG.getPlugin().maps.getMap(mapName);
                final WorldEditPlugin we = SpleggOG.getPlugin().getWorldEdit();
                Region sel = null;
                try {

                    sel = we.getSession(player).getSelection(new BukkitWorld(player.getWorld()));

                } catch (IncompleteRegionException error) {

                    Utils.spleggOGMessage(player, "&cERROR: The area you have selected is incomplete.");

                }

                if (sel == null) {

                    Utils.spleggOGMessage(player, "&4Please select an area with worldedit.");
                    return;

                }

                map.addFloor(
                        new Location(player.getWorld(), sel.getMinimumPoint().getX(), sel.getMinimumPoint().getY(),
                                sel.getMinimumPoint().getZ()),
                        new Location(player.getWorld(), sel.getMaximumPoint().getX(), sel.getMaximumPoint().getY(),
                                sel.getMaximumPoint().getZ()));

                Utils.spleggOGMessage(player,
                        "&aFloor &6" + map.getFloors() + " &aadded to map: &e" + map.getName() + "&a.");

            }
            default -> usageMessage(player, tag);

        }

    }

    void permissionMessage(Player player) {

        Utils.spleggOGMessage(player, "&cERROR: You do not have permission to do that.");

    }

    void usageMessage(Player player, String tag) {

        Utils.spleggOGMessage(player,
                "&cERROR: Incorrect syntax. &6Use the command: &e/" + tag + " help &6for the command list.");

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
        sendUsage(player, tag, "join [lobby]", "Join a lobby by id (SP1 or 1); no id lists the lobbies.");
        sendUsage(player, tag, "leave", "Leave your current match or lobby.");
        sendUsage(player, tag, "list", "List every lobby and its status.");
        sendUsage(player, tag, "maps", "List every configured map and its status.");
        sendUsage(player, tag, "help", "Show this help message.");
        Utils.spleggOGMessage(player, "&c/vote &d<map number> &5- &bVote for a lobby map. Alias: &e/v");

        if (!admin) {

            return;

        }

        Utils.spleggOGMessage(player, "&cAdmin commands:");
        sendUsage(player, tag, "create <map>", "Create a new map (disabled until floor + spawns exist).");
        sendUsage(player, tag, "delete <map>", "Delete a map.");
        sendUsage(player, tag, "info <map>", "Show map setup status (spawns, floors).");
        sendUsage(player, tag, "setlobby", "Set where players wait in every lobby hub. Run inside a hub world.");
        sendUsage(player, tag, "setspawn <map> [next|append|#]", "Add or update a spawn point.");
        sendUsage(player, tag, "addfloor <map>", "Add a WorldEdit selection as a floor.");
        sendUsage(player, tag, "start [lobby]", "Start a lobby's match early.");
        sendUsage(player, tag, "stop [lobby]", "Stop a lobby's match.");
        Utils.spleggOGMessage(player, "&c/spforcestart &d[time] &5- &bStart your lobby ignoring the player minimum.");

    }

    private void sendMapList(Player player) {

        if (SpleggOG.getPlugin().maps.getMaps().isEmpty()) {

            Utils.spleggOGMessage(player, "&6No maps have been configured yet.");
            return;

        }

        Utils.spleggOGMessage(player, "&6Splegg maps:");
        for (Map map : SpleggOG.getPlugin().maps.getMaps()) {

            final List<Game> games = SpleggOG.getPlugin().games.gamesForMap(map.getName());
            final String statusLabel;
            if (!map.isUsable(map)) {

                statusLabel = "&cDISABLED";

            } else if (games.isEmpty()) {

                statusLabel = "&aREADY";

            } else {

                final List<String> lobbies = new ArrayList<>();
                for (Game g : games) {

                    lobbies.add(g.getLobbyId() + (g.getStatus() == Status.INGAME ? " &3LIVE" : " &5VOTED"));

                }

                statusLabel = "&6" + String.join("&7, &6", lobbies);

            }

            Utils.spleggOGMessage(player,
                    "&e" + map.getName() + " &7- " + statusLabel + " &7(&f" + map.getSpawnCount() + " spawns&7)");

        }

    }

    private void sendMapInfo(Player player, String mapName) {

        final Map map = SpleggOG.getPlugin().maps.getMap(mapName);
        final List<Game> games = SpleggOG.getPlugin().games.gamesForMap(mapName);

        Utils.spleggOGMessage(player, "&6Map info: &e" + map.getName());
        Utils.spleggOGMessage(player, "&6Template world: &f"
                + (map.getWorldName() != null ? map.getWorldName() : "&cunset (set a spawn first)"));
        Utils.spleggOGMessage(player, "&6Spawn points: &f" + map.getSpawnCount());
        Utils.spleggOGMessage(player, "&6Floor regions: &f" + map.getFloors());
        Utils.spleggOGMessage(player, "&6Lobbies on this map: &f" + games.size());

        final boolean playable = map.isUsable(map);
        Utils.spleggOGMessage(player, "&6Playable: " + (playable ? "&ayes" : "&cno"));

        if (!playable) {

            final List<String> missing = new ArrayList<>();
            if (map.getSpawnCount() < 2) {

                missing.add("/splegg setspawn " + map.getName() + " (at least two)");

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {

            final List<String> options = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("splegg.admin")) {

                options.addAll(ADMIN_SUBS);

            }

            return filterPrefix(options, args[0]);

        }

        if (args.length == 2 && LOBBY_ARG_SUBS.contains(args[0].toLowerCase(Locale.ROOT))) {

            return filterPrefix(SpleggOG.getPlugin().games.getLobbyIds(), args[1]);

        }

        if (args.length == 2 && MAP_ARG_SUBS.contains(args[0].toLowerCase(Locale.ROOT))) {

            final List<String> mapNames = new ArrayList<>();
            for (Map map : SpleggOG.getPlugin().maps.getMaps()) {

                mapNames.add(map.getName());

            }

            return filterPrefix(mapNames, args[1]);

        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {

            return filterPrefix(Arrays.asList("next", "append"), args[2]);

        }

        return Collections.emptyList();

    }

    private List<String> filterPrefix(List<String> source, String prefix) {

        final String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        final List<String> matched = new ArrayList<>();
        for (String candidate : source) {

            if (candidate.toLowerCase(Locale.ROOT).startsWith(needle)) {

                matched.add(candidate);

            }

        }

        Collections.sort(matched);
        return matched;

    }

}
