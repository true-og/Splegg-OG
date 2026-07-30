# Splegg-OG 0.9.2 BETA

![Icon](https://raw.githubusercontent.com/true-og/Splegg-OG/master/assets/splegg-logo.png)

**Splegg-OG** is a Splegg plugin originally made by MrLuangamer, updated for Worldedit 7.2 and Spigot 1.16.4 by Hraponssi, then updated for Purpur 1.19.4 for use at [TrueOG Network](https://true-og.net/) by [NotAlexNoyle](https://github.com/NotAlexNoyle/).

Runtime dependencies: WorldEdit, DiamondBank-OG, Essentials-OG, and MyWorlds.

Current feature state:

- Matches run in per-game MyWorlds copies of the configured template world, so multiple games can use the same map without sharing arena blocks, scoreboards, or player state.
- Splegg creates isolated MyWorlds inventory groups for configured lobby and in-game worlds. It does not manage the vanilla main-world inventory group.
- Players can join by command, random queue, or registered lobby signs, then vote for the map while waiting in the lobby.
- Players can leave with `/splegg leave`, `/hub`, or the lobby slimeball.
- Players who reconnect inside a Splegg world are returned to a protected main-world spawn instead of remaining in an abandoned match world.

**Protected vanilla worlds:** Splegg will *never* read, write, or configure the vanilla overworld dimensions (`world`, `world_nether`, `world_the_end`). This guard is hard-coded -- listing those names under `Worlds.Lobby` or `Worlds.InGame` in `config.yml` is rejected with startup warnings, and `/splegg create`, `/splegg setspawn`, `/splegg setlobby`, and `/splegg addfloor` all refuse to run while you are standing in one of them. Run Splegg only inside dedicated worlds that you create yourself.

## Step-by-Step Setup Guide

Follow these steps in order to get your first map running.

### 1. Choose Your Map Source
Decide how you want to create your arena:
- **Option 1A (Manual):** Create a fresh void world and build your map from scratch.
- **Option 1B (Import):** Use a pre-built world folder (e.g., from PlanetMinecraft).

#### 1A. Create Base Worlds (Manual Path)
Splegg requires dedicated worlds for lobbies and games.
1. Install [MyWorlds](https://github.com/true-og/MyWorlds).
2. Create a lobby world: `/mw create splegg_lobby void`
3. Create an in-game world: `/mw create splegg_ingame void`
4. Load them: `/mw load splegg_lobby` and `/mw load splegg_ingame`

#### 1B. Import Pre-built Maps (Import Path)
If you have a downloaded world folder:
1. In `config.yml`, set `Worlds.MapBase` to `maps`.
2. Drop your world folder (e.g., `MyAwesomeMap`) into the `/maps/` directory at your server root.
3. Add `MyAwesomeMap` to the `Worlds.InGame` list in `config.yml`.
4. Restart the server. Splegg will automatically deploy the map.

### 2. Configure the Plugin
1. Open `plugins/Splegg-OG/config.yml`.
2. Ensure `splegg_lobby` is under `Worlds.Lobby` and your in-game world (e.g., `splegg_ingame` or `MyAwesomeMap`) is under `Worlds.InGame`.
3. Restart your server or run `/reload confirm`.

### 3. Initialize the Splegg Map
Teleport to your in-game world (`/mw tp <world_name>`) and run:
- `/splegg create <map-name>` (e.g., `/splegg create my-first-map`)

### 4. Define the Arena
While standing in the map:
1. **Set Spawns:** Run `/splegg setspawn <map-name>` at every player starting position. You need at least two. Use `next` or a number to modify them.
2. **Set Lobby:** Run `/splegg setlobby <map-name>` where players should wait before the game starts.
3. **Define Floors:** 
   - Use the WorldEdit wand (`//wand`) to select two parallel corners of a floor.
   - Run `/splegg addfloor <map-name>`. Repeat for as many floors as needed.

### 5. Test Your Map
Run `/splegg join <map-name>` to ensure everything is working correctly.

## 🛠 Command Reference

### Management & Setup
- `/splegg create <map>` - Initialize a new map.
- `/splegg setspawn <map> [next|#]` - Define/modify player start points.
- `/splegg setlobby <map>` - Set map-specific warm-up area.
- `/splegg setlobby` - Set the global fallback lobby.
- `/splegg addfloor <map>` - Add selected WorldEdit region as a floor.
- `/splegg info <map>` - Check setup status (spawns, floors, etc.).

### Gameplay
- `/splegg join <map>` - Join a specific map.
- `/splegg random` - Join a random available map.
- `/vote [#]` - View or cast votes for the current lobby's maps.
- `/splegg leave` - Exit a lobby or match.
- `/hub` - Return to the main world spawn.

### Administration
- `/splegg start [map]` - Force start a match.
- `/splegg stop [map]` - End a match immediately.
- `/splegg list` - List all maps, their status, and player counts.
- `/splegg help` - Full command reference.

*Alias: `/sp` can be used instead of `/splegg`.*

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
