**0.9.0:**

- Leaving Splegg now returns players to the exact main-SMP location they were at before joining, preserving overworld/nether/end context while still crossing back out of Splegg inventory isolation.

**0.8.9:**

- Re-keyed all per-player runtime state (match roster, shovel selections, money tracking, egg-launch flags) from player names to UUIDs so name changes, duplicate names, and offline-player lookups no longer corrupt match state.

- Fixed a NullPointerException in the egg-landing handler that could fire when the ray trace produced no candidate block.

- Fixed the lobby-entry and match-exit flows that reset `FallDistance` to 1 instead of 0, leaving players with a one-block fall penalty after Splegg transitions.

- Replaced the try/catch NPE swallow in the plugin shutdown path with a direct null-check so unrelated bugs no longer silently print stack traces.

- Removed the noisy try/catch around `PlayerInteractEvent.getHand()` and the misleading `HandlerList.unregisterAll(new X())` no-ops from `onDisable`.

- Removed the legacy `My_Worlds` softdepend and plugin-id fallback; `MyWorlds` is the only recognized world-manager plugin name.

**0.8.8:**

- Tightened playable-map validation so a map now requires at least 2 spawn points, at least 1 floor, and valid loaded world references before it can enter a playable lobby state.

- Fixed join flow so players can now join Splegg lobbies from outside Splegg worlds, including via `/splegg join`, `/splegg random`, and registered join signs.

- Fixed lobby countdown recovery so a failed countdown now returns to a waiting state instead of immediately restarting itself.

- Fixed kill reward payouts so eliminations only reward surviving players in the same match instead of players from other matches.

- Reworked the Splegg shop onto the local GxUI-OG GUI layer and added live affordability feedback: affordable shovels glow, unaffordable ones do not, and shop lore now reflects the current affordability state.

**0.8.7:**

- Fixed the in-game round timer so matches now count down correctly and end on time limit instead of stopping immediately.

- Added proper last-player-standing match completion, including server-wide winner chat, loser messaging, and winner payout handling.

- Added a winner title that is shown only to the winning player, while the winner chat announcement remains server-wide.

- Added broken-block tracking to match exits so players now see their block total when they leave a round.

- Fixed leave messaging to name the player who actually left and cleaned up shovel-selection state more completely on exit.

**0.8.6:**

- Added `/splegg random` to join a random playable map and a `getRandomMap()` utility that lays the groundwork for in-lobby map voting.

- Added `/splegg help`, `/splegg list`, and `/splegg info <map>` to surface configured maps, their status, and setup next-steps.

- Added tab completion for every `/splegg` subcommand and map-name argument.

- Added a per-player lobby scoreboard driven by the existing `Scoreboard.*` config keys, refreshed on join, leave, and each countdown tick.

- Fixed lobby signs rendering raw `&` color codes by serializing line content through the Adventure legacy serializer.

- Fixed right-click-to-join and sign-break handling, which previously compared Adventure Component `toString()` output to raw config strings and never matched.

- Added a distinct `[Starting]` status for lobby signs during the start countdown (`Sings.Status.Starting`) so operators can see at a glance that a match is about to begin.

- Improved the sign player count line with color accents.

**0.8.5:**

- Fixed plugin startup with players already online by initializing tracked player storage before repopulating it.

- Stopped using Essentials kits to save and restore Splegg inventories, so MyWorlds now remains the only inventory boundary between main and Splegg worlds.

- Fixed in-match protection listeners to use the tracked `UtilPlayer` state instead of creating fresh session objects.

- Fixed the knockout floor height calculation so eliminations happen below the actual arena floor.

- Fixed admin `/splegg start <map>` and `/splegg stop <map>` targeting so they operate on the specified game instead of the command sender's current game.

- Tightened Splegg chat isolation so joined players only talk to players in the same match, while non-participants in Splegg worlds stay scoped to their current world.

**0.8:**

- Added permission metadata for `splegg.join`, `splegg.joinfull`, and `splegg.admin`.

- Expanded the shop to include every shovel available in Minecraft 1.19.4.

- Added support for selecting wood, stone, iron, gold, diamond, and netherite shovels before a match.

- Updated the README and plugin dependency metadata to reflect the current DiamondBank-OG, Essentials-OG, and MyWorlds requirements.

- Fixed the incorrect startup error message shown when Essentials-OG is missing.

**0.7:**

- 1.19.4 Support.

**0.6:**

- The splegg shop is fixed.

- The winner is now removed from the game.

- The floor now resets correctly after a match.

**0.5:**

- Fixed dust particles coming off of egg instead of the block that was hit.

- Fixed match not ending when players get below the minimum level.

**0.4:**

- It is no longer possible to start a match with only 1 player in the lobby.

- Match startup is fixed.

- The formatting of the shop items is fixed.

- Right clicking items in the lobby inventory is fixed.

- The shovel is now given only after the grace period is over.

- Players pre-game inventory is now saved and restored using EssentialsX kits.

- Inventory saving now still works upon leaving the server during a match.

- The game leave message now displays slightly differently to the individual who left.

- The game leave message is now only displayed to players that are in the game.

- The game leave message now displays when someone quits the server completely.

- Leaving the server in the lobby now properly removes players from the game.

- Restored lobby inventory layout to be like it was on Hive Java.

- The lobby items now properly go away when a game starts.

**0.3**

- Fixed the shovel being given in the lobby.

- Fixed leaving a game or dying teleporting players to the wrong point.

- Fixed "you are already playing" message being displayed even after leaving a game.

- Fixed grace period timer.

- Right clicking blocks now only shoots one egg.

- The shooting sound has been restored from Hive Java's Splegg.

- Fixed a few NullPointerException errors.

**0.2**

- In-game players can no longer be hit with eggs.

- Shovels are now given out when the game starts, not in the lobby.

- The server no longer needs to be restarted to set up a map.

- The "next" keyword for adding spawn points is now optional.

- The TUI design has been revamped.

- Improved sounds and particle effects.

- Fixed advancements being triggered by lobby items.

- Fixed lots of runtime errors and crashes.

- Fixed an infinite loop upon a player dying.

**0.1**

- Initial Release of Splegg-OG.
