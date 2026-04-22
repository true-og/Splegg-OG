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
