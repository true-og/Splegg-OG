# Splegg-OG 0.9.2 BETA

![Icon](https://raw.githubusercontent.com/NotAlexNoyle/Splegg-OG/master/assets/splegg-logo.png)

**Splegg-OG** is a Splegg plugin originally made by MrLuangamer, updated for Worldedit 7.2 and Spigot 1.16.4 by Hraponssi, then updated for Purpur 1.19.4 for use at [TrueOG Network](https://true-og.net/) by [NotAlexNoyle](https://github.com/NotAlexNoyle/).

Runtime dependencies: WorldEdit, DiamondBank-OG, Essentials-OG, and MyWorlds.

Current feature state:

- Matches run in per-game MyWorlds copies of the configured template world, so multiple games can use the same map without sharing arena blocks, scoreboards, or player state.
- Splegg creates isolated MyWorlds inventory groups for configured lobby and in-game worlds. It does not manage the vanilla main-world inventory group.
- Players can join by command, random queue, or registered lobby signs, then vote for the map while waiting in the lobby.
- Players can leave with `/splegg leave`, `/hub`, or the lobby slimeball.
- Players who reconnect inside a Splegg world are returned to a protected main-world spawn instead of remaining in an abandoned match world.

**Protected vanilla worlds:** Splegg will *never* read, write, or configure the vanilla overworld dimensions (`world`, `world_nether`, `world_the_end`). This guard is hard-coded -- listing those names under `Worlds.Lobby` or `Worlds.InGame` in `config.yml` is rejected with startup warnings, and `/splegg create`, `/splegg setspawn`, `/splegg setlobby`, and `/splegg addfloor` all refuse to run while you are standing in one of them. Run Splegg only inside dedicated worlds that you create yourself.

**World Setup (do this first):**

Splegg requires at least one dedicated *lobby* world (where queued players wait before a match) and one dedicated *in-game* world (where matches play out). Both must exist as real MyWorlds worlds before any map can be created.

1. Install [MyWorlds](https://www.spigotmc.org/resources/my-worlds.39252/) and start the server once so its config is generated.
2. Create the worlds with MyWorlds. The defaults assume `splegg_lobby` and `splegg_ingame`:

    `/mw create splegg_lobby void` -- creates a void-generator lobby world.

    `/mw create splegg_ingame void` -- creates a void-generator world for matches. Build your map terrain here (or paste it in with WorldEdit).

    `/mw load splegg_lobby` / `/mw load splegg_ingame` if either is not auto-loaded.

3. Open `plugins/Splegg-OG/config.yml` and confirm the two world names appear under `Worlds.Lobby` and `Worlds.InGame`. Add additional worlds to either list if you want more than one of each. Vanilla names (`world`, `world_nether`, `world_the_end`) are rejected -- do not put them here.
4. Reload the server (or `/reload confirm`). The startup log prints a `Created MyWorlds inventory group for ...` line for each list; inventories are then isolated so that match items cannot leak back to the SMP overworld.
5. Teleport into your in-game world with `/mw tp splegg_ingame` (or `/mvtp` if you also use Multiverse aliases). All `/splegg` setup commands must be issued from inside one of the configured Splegg worlds.

**Map Setup:**

`/splegg create my-map` First, create a Splegg map with a name of your choosing. You must be standing inside a configured Splegg lobby or in-game world for this to succeed.

`/splegg setspawn my-map` The amount of spawn points you set is the amount of players that will be able to join the map. A playable map needs at least two spawn points.

`/splegg setspawn my-map next` You can define new spawn points for a map with or without the "next" keyword.

`/splegg setspawn my-map 3` At any time, you can modify existing spawn points by using the number of the order in which you created them.

`/splegg setlobby my-map` Set a lobby area for the map during the voting/warm-up period.

`/splegg setlobby` Set the global Splegg queue-lobby fallback used when a map-specific lobby is not configured.

`//wand` Summon a wand with WorldEdit and then use it to select two points on a one-dimensional plane. The points you select will represent parallel corners of your floor.

`/splegg addfloor my-map` Add the area you just selected with WorldEdit as a floor. You can add as many as you like.

`/splegg join my-map` You can now join the map you just created, and so can anyone else with splegg.join permission.

`/splegg random` Join a random playable map, chosen from every map currently in a LOBBY state.

`/vote` Show the current lobby's map choices.

`/vote 1` Vote for a map by number. `/v 1` is also supported, and the vote list is clickable in chat.

`/splegg leave` Leave your current Splegg lobby or match.

`/hub` Leave your active Splegg game, or return to a protected main-world spawn if you are not currently in a game.

`/splegg start [my-map]` Anyone with splegg.admin permission can start a game early once at least two players have joined.

`/splegg stop [my-map]` Stop an in-progress match.

`/splegg list` List every configured map with its status and player count.

`/splegg info my-map` Inspect a map's setup: spawns, floors, lobby, playable state, and remaining next-steps.

`/splegg help` Show the full command reference. Tab completion is available on every subcommand and map-name argument.

The main command alias is `/sp`.

**Map Voting:**

When a player joins a Splegg lobby, the lobby picks up to `Options.VotingMaps` playable maps. The joined map is kept as one of the choices and the remaining slots are filled randomly. Players get the same chat voting flow as TheHerobrine-OG: `/v #`, clickable vote lines, one active vote per player, vote changes that move the count, and reminder messages every `Options.VotingReminder` seconds.

When the start countdown reaches `Options.EndVotingAt`, voting ends and the highest-voted map wins. If the winning map is different from the joined map, Splegg prepares a new per-game world copy for the winner, moves lobby players to that map's queue lobby, updates signs and scoreboards, and starts the match on the winning map.

**Lobby Signs:**

Admins can create a join sign by placing a sign with these lines:

`[splegg]`

`join`

`my-map`

Splegg rewrites the sign using `Sings.Format` from `config.yml`. The default layout is header, map, status, then player count. Sign clicks resolve the map from the saved sign location in `maps.yml`, not from the visible line text, so operators can change the displayed format without breaking joins. Admins can break registered Splegg signs to remove them; non-admin players cannot break them.

**Permissions:**

- `splegg.join` - Join Splegg games with `/splegg join`, `/splegg random`, or lobby signs.
- `splegg.joinfull` - Join a full lobby.
- `splegg.admin` - Manage maps, signs, and matches. Includes the join permissions.
- `splegg.use` - Metadata permission that includes `splegg.join`.

[Original Bukkit Page](https://dev.bukkit.org/projects/splegg-minigame).

*The current Gradle build target is Purpur 1.19.4 on GraalVM CE 17 / Java 17.*

**To Build:**

`./bootstrap.sh`

`export JAVA_HOME=/path/to/java-17`

`./gradlew build`

Gradle must run on Java 17 or newer, and the git submodules under `libs/` must be initialized before the build.

The resulting .jar file will be in build/libs/

**Planned Features:**

- Database to remember shop purchases across sessions (currently in-memory only).

- Functional lobby menu items: wire up the Guide book and Cosmetics menu. The shop item and leave slimeball are functional.

- Power ups.

- Spectator Mode (partial TODOs exist for spectator compass, spectator inventory, and join-in-progress).

- Leaderboards.

- PlaceholderAPI support.

- In-Game scoreboard.

**Licensed under the GPLv3.**
