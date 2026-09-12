# Splegg-OG 0.9.8

![Icon](https://raw.githubusercontent.com/true-og/Splegg-OG/master/assets/splegg-logo.png)

**Splegg-OG** is a Splegg plugin originally made by MrLuangamer, updated for Worldedit 7.2 and Spigot 1.16.4 by Hraponssi, then updated for Purpur 1.19.4 for use at [TrueOG Network](https://true-og.net/) by [NotAlexNoyle](https://github.com/NotAlexNoyle/).

Runtime dependencies: WorldEdit, DiamondBank-OG, Utilities-OG, and MyWorlds. Chat-OG and Scoreboard-OG are optional and integrated when present.

Current feature state:

- Lobbies work like TheHerobrine-OG's. Each hub world listed under `Worlds.Lobby` is one lobby: `SP1-hub` is lobby `SP1`, joined with `/splegg join SP1` or `/splegg join 1`. Players wait and vote for the map in the hub; when the vote ends the winning map's template is copied to `SP1-<map>` and the match runs there. When it ends the copy is deleted and the lobby goes back to waiting. Add `SP2-hub` for a second concurrent lobby.
- Matches run in per-lobby MyWorlds copies of the map's template world, so two lobbies can play the same map without sharing arena blocks, scoreboards, or player state. The template copied is the world the map's **terrain** is in, taken from its spawns and then its floors.
- Splegg creates isolated MyWorlds inventory groups for configured lobby and in-game worlds. It does not manage the vanilla main-world inventory group.
- Players join a lobby with `/splegg join <lobby>` (`/sp join 1`, `/spjoin 1`), or through registered join signs, then vote for the map while waiting in the hub. `/splegg join` on its own lists every lobby with a clickable join line, like `/hbjoin`.
- Players can leave with `/splegg leave`, `/hub`, `/lobby`, `/spawn`, or the lobby slimeball. Inside Splegg territory all three commands are claimed by Splegg before any other plugin sees them, so another minigame's `/hub` or Spawn-OG's `/spawn` can never teleport a player out while Splegg still counts them as playing.
- Players who leave, relog, or reconnect inside a Splegg world are returned to where they were before they entered it. Return locations are kept on disk in `plugins/Splegg-OG/prejoin-locations.yaml` and expire after `Options.PreJoinLocationExpiryDays` days; the protected main-world spawn is the fallback.

**Protected vanilla worlds:** Splegg will *never* read, write, or configure the vanilla overworld dimensions (`world`, `world_nether`, `world_the_end`). This guard is hard-coded -- listing those names under `Worlds.Lobby` or `Worlds.InGame` in `config.yml` is rejected with startup warnings, and `/splegg create`, `/splegg setspawn`, `/splegg setlobby`, and `/splegg addfloor` all refuse to run while you are standing in one of them. Run Splegg only inside dedicated worlds that you create yourself.

## Step-by-Step Setup Guide

Follow these steps in order to get your first map running.

### 1. Choose Your Map Source
Decide how you want to create your arena:
- **Option 1A (Manual):** Create a fresh void world and build your map from scratch.
- **Option 1B (Import):** Use a pre-built world folder (e.g., from PlanetMinecraft).

#### 1A. Create Base Worlds (Manual Path)
Splegg requires dedicated worlds for lobby hubs and maps.
1. Install [MyWorlds](https://github.com/true-og/MyWorlds).
2. Create a hub world for lobby 1: `/mw create SP1-hub void`. The name must be `<GamePrefix><number>-<name>`; the number is the lobby id.
3. Create an in-game world: `/mw create splegg_ingame void`
4. Load them: `/mw load SP1-hub` and `/mw load splegg_ingame`

#### 1B. Import Pre-built Maps (Import Path)
If you have a downloaded world folder:
1. In `config.yml`, set `Worlds.MapBase` to `maps`.
2. Drop your world folder (e.g., `MyAwesomeMap`) into the `/maps/` directory at your server root, and a hub world folder named `SP1-hub`.
3. Add `MyAwesomeMap` to the `Worlds.InGame` list and `SP1-hub` to the `Worlds.Lobby` list in `config.yml`.
4. Restart the server. Splegg copies both into place on every boot.

### 2. Configure the Plugin
1. Open `plugins/Splegg-OG/config.yml`.
2. Ensure `SP1-hub` is under `Worlds.Lobby` and your in-game world (e.g., `splegg_ingame` or `MyAwesomeMap`) is under `Worlds.InGame`. Every `Worlds.Lobby` entry becomes one lobby; one that is not named `<GamePrefix><number>-<name>` is skipped with a startup warning.
3. Restart your server or run `/reload confirm`.

### 3. Initialize the Splegg Map
Teleport to your in-game world (`/mw tp <world_name>`) and run:
- `/splegg create <map-name>` (e.g., `/splegg create my-first-map`)

### 4. Define the Arena
While standing in the map:
1. **Set Spawns:** Run `/splegg setspawn <map-name>` at every player starting position. You need at least two. Use `next` or a number to modify them.
2. **Define Floors:** 
   - Use the WorldEdit wand (`//wand`) to select two parallel corners of a floor.
   - Run `/splegg addfloor <map-name>`. Repeat for as many floors as needed.

### 5. Set the Waiting Spot (optional)
Stand in `SP1-hub` where players should wait and run `/splegg setlobby`. The coordinates are reused in every lobby hub, so all hubs should be copies of the same template. Without it players land at the hub world's spawn.

### 6. Test
Run `/splegg join 1` (or `/sp join SP1`; `/splegg join` alone lists the lobbies). You are put in the hub of lobby `SP1` and asked to vote. Once enough players are waiting (`Options.AutoStartPlayers`) the countdown runs, the vote closes at `Options.EndVotingAt` seconds, and the match starts in a fresh copy of the winning map. Use `/spforcestart` to skip the player minimum while testing.

## Join Signs

Admins can place physical signs that players right-click to join a lobby. A repeating task redraws every sign each second with the lobby, its voted map once there is one, live status, and player counts, using the `Sings.Format` and `Sings.Status` templates in `config.yml`. Placeholders are `%lobby%`, `%map%` (the lobby id while the vote is open), `%status%`, `%count%` and `%maxcount%`.

To set one up:

1. Place a sign in a persistent world such as the server hub — never inside a per-match world copy, which is deleted after each game.
2. Write `[Splegg]` on the first line and a lobby id (`SP1` or `1`) on the second line, or leave it blank for "whichever lobby is best to join". Requires the `splegg.admin` permission.
3. The sign reformats itself immediately; right-clicking it joins that lobby.

Signs from older versions that name a map keep working as "any lobby" signs.

Signs are registered by location, not by their text, so reformatting cannot orphan them. Breaking a registered sign requires `splegg.admin` and unregisters it; everyone else is blocked from breaking it.

## Points, Ranks & Placeholders

Every block an egg destroys and every match win awards points (`Points.BlockBroken`
and `Points.Win` in `config.yml`). Totals persist in `plugins/Splegg-OG/stats.yml`
and map onto the Hive Java Splegg rank ladder:

| Rank | Points |
|------|--------|
| Farmer | 0 |
| Cook | 5,000 |
| Chef | 10,000 |
| Master | 100,000 |
| Humpty Dumpty | 250,000 |
| Ramsay | 500,000 |
| Hot'n'Spicy | 1,000,000 |
| Splegg Head | 1,200,000 |
| Bacon | 1,500,000 |
| Sunnyside | 2,000,000 |
| Delicious | 3,000,000 |
| Easter Bunny | 4,000,000 |
| Scrambled | 5,000,000 |
| Oliver | 7,500,000 |
| The Eggspert | Top player, once they also reach Oliver |

Registered through Utilities-OG as MiniPlaceholders, resolved for the viewing
player, and updated the moment the underlying value changes:

| Placeholder | Value |
|-------------|-------|
| `<sp_score>` | Total points |
| `<sp_rank>` | Colored rank name for that score |

## 🛠 Command Reference

### Management & Setup
- `/splegg create <map>` - Initialize a new map.
- `/splegg setspawn <map> [next|#]` - Define/modify player start points.
- `/splegg setlobby` - Set where players wait in every lobby hub. Run inside a hub world.
- `/splegg addfloor <map>` - Add selected WorldEdit region as a floor.
- `/splegg info <map>` - Check setup status (spawns, floors, etc.).

### Gameplay
- `/splegg join [lobby]` - Join a lobby by id (`SP1`, or just `1`). With no argument, lists the lobbies with clickable join lines. `/spjoin [lobby]` is the same command.
- `/splegg list` - List every lobby with a clickable join line.
- `/splegg maps` - List every map and which lobby is on it.
- `/vote [#]` or `/v [#]` - View or cast votes for the current lobby's maps.
- `/splegg leave` - Exit a lobby or match.
- `/hub`, `/lobby`, `/spawn` - Leave the lobby or match and return to where you came from, or to the main world spawn.

### Administration
- `/spforcestart [time]` - Start the match ignoring `Options.AutoStartPlayers`.
- `/splegg start [lobby]` - Start a lobby's match early. Defaults to your own lobby.
- `/splegg stop [lobby]` - End a lobby's match immediately.
- `/splegg help` - Full command reference.

*Alias: `/sp` can be used instead of `/splegg`. Splegg claims the bare `/sp` label once every plugin has enabled, so it wins over any other plugin (such as WorldGuard) that registers `/sp` first; that plugin's command stays reachable through its own namespace, for example `/worldguard:sp`.*

**Map Voting:**

When the first player joins a lobby, it picks up to `Options.VotingMaps` playable maps at random. Players get the same chat voting flow as TheHerobrine-OG: `/v #`, clickable vote lines, one active vote per player, vote changes that move the count, and reminder messages every `Options.VotingReminder` seconds.

When the start countdown reaches `Options.EndVotingAt`, voting ends and the highest-voted map wins. Splegg copies that map's template to `<GamePrefix><lobby>-<map>` while everyone keeps waiting in the hub, then teleports them onto the map's spawns when the countdown hits zero. If the countdown is abandoned for lack of players, the copy is thrown away and a fresh vote opens. A lobby holds `Options.MaxPlayers` players; extra spawn points go unused and a map with fewer spawns cycles them.

`/v` and `/vote` are claimed inside Splegg territory before any other plugin sees them, so VotingPlugin cannot take the `/vote` label away from map voting. The claim covers a player who is in a Splegg game, and anyone standing in a configured lobby or in-game world or a per-match copy. Everywhere else `/vote` behaves normally: neither label is declared in plugin.yml, so Splegg never competes for them.

**Force Starting:**

`/spforcestart [time]` restarts the lobby countdown ignoring `Options.AutoStartPlayers`, so a match can begin below the configured minimum. The countdown floor is `Options.EndVotingAt` so map voting still resolves before the match starts, and `[time]` raises it. Requires `splegg.admin`.

**Chat:**

When Chat-OG is installed, Splegg registers a chat formatter for its `Worlds.GamePrefix` key (`SP` by default), which covers both the configured lobby worlds and every per-match copy. Lobby chat shows the player count, in-game chat shows blocks broken, and eliminated players are greyed out. Chat-OG's `discord.games` must contain a matching `SP` key for Splegg worlds to get their own chat; without it they stay in global chat and the formatter never runs.

Chat-OG routes worlds named `<letters><number>-<name>`, which is also the shape every lobby hub must have, so `SP1-hub` and the match copy `SP1-<map>` land in the same lobby chat. `Worlds.InGame` entries are match templates, nobody queues or plays in one, and each per-game copy routes whatever the template is called.

**Scoreboard:**

Queued players see a sidebar with the map, the players waiting and the countdown; during the match it shows the map, the players still alive, the player's own blocks broken and the time left. The labels are the `Scoreboard.*` keys in `config.yml`. With Scoreboard-OG `1.2.0` or newer installed, the sidebar is drawn through Scoreboard-OG's sidebar API, so the network board returns on its own when the player leaves and `/togglescoreboard` is honoured. Without it, a plain Bukkit sidebar is used and the main scoreboard is restored on leave.

**Lobbies:**

A lobby is a persistent object created at startup for every hub world under `Worlds.Lobby`. Its id is the world name up to the dash: `SP1-hub` is `SP1`. A lobby is either waiting in its hub, starting (countdown running), live (match running in `SP1-<map>`), or ending. Live lobbies cannot be joined. When the match ends the arena copy is deleted and the same lobby id is ready again, so `/splegg join 1` always means the same place.

If you are upgrading from 0.9.7 or older: per-map match lobbies (`/splegg setlobby <map>`) are gone and any `Spawns.lobby` entry in a map file is ignored. Rename your lobby world to `SP1-hub` if it is not already, and set the waiting spot with `/splegg setlobby` inside it.

**Lobby Signs:**

Admins can create a join sign by placing a sign with these lines:

`[splegg]`

`SP1`

The second line is a lobby id, or blank for the best open lobby. Splegg rewrites the sign using `Sings.Format` from `config.yml`. The default layout is header, lobby (or map once voted), status, then player count. Sign clicks resolve the lobby from the saved sign location in `maps.yml`, not from the visible line text, so operators can change the displayed format without breaking joins. Admins can break registered Splegg signs to remove them; non-admin players cannot break them.

**Permissions:**

- `splegg.join` - Join Splegg lobbies with `/splegg join`, `/spjoin`, or lobby signs.
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

**Licensed under the GPLv3.**
