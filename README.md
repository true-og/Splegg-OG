# Splegg-OG BETA

![Icon](https://raw.githubusercontent.com/NotAlexNoyle/Splegg-OG/master/assets/splegg-logo.png)

**Splegg-OG** is a Splegg plugin originally made by MrLuangamer, updated for Worldedit 7.2 and Spigot 1.16.4 by Hraponssi, then updated for Purpur 1.19.4 for use at [TrueOG Network](https://true-og.net/) by [NotAlexNoyle](https://github.com/NotAlexNoyle/).

Dependencies: WorldEdit, DiamondBank-OG, Essentials-OG, and MyWorlds.

Legacy note: `My_Worlds` is still recognized as a fallback plugin name at runtime, but a MyWorlds-compatible world manager is required for Splegg-OG to start.

**To Set Up:**:

`/splegg create my-map` First, create a Splegg map with a name of your choosing.

`/splegg setspawn my-map` The amount of spawn points you set is the amount of players that will be able to join the map.

`/splegg setspawn my-map next` You can define new spawn points for a map with or without the "next" keyword.

`/splegg setspawn my-map 3` At any time, you can modify existing spawn points by using the number of the order in which you created them.

`/splegg setlobby my-map` Set a lobby area for the map during the voting/warm-up period.

`//wand` Summon a wand with WorldEdit and then use it to select two points on a one-dimensional plane. The points you select will represent parallel corners of your floor.

`/splegg addfloor my-map` Add the area you just selected with WorldEdit as a floor. You can add as many as you like.

`/splegg join my-map` You can now join the map you just created, and so can anyone else with splegg.join permission.

`/splegg start` Anyone with splegg.admin permissions can start the game even if less players join than there are spawn points.

`/splegg list` List every configured map with its status and player count.

`/splegg info my-map` Inspect a map's setup: spawns, floors, lobby, playable state, and remaining next-steps.

`/splegg random` Join a random playable map, chosen from every map currently in a LOBBY state.

`/splegg help` Show the full command reference. Tab completion is available on every subcommand and map-name argument.

[Original Bukkit Page](https://dev.bukkit.org/projects/splegg-minigame).

*The current Gradle build target is Purpur 1.19.4 on Java 17.*

**To Build:**

`./gradlew build`

The resulting .jar file will be in build/libs/

**Planned Features:**

- Database to remember shop purchases across sessions (currently in-memory only).

- Functional lobby menu items: wire up the Guide book and Cosmetics menu (items appear in lobby inventory but have no click handlers).

- Power ups.

- Spectator Mode (partial TODOs exist for spectator compass, spectator inventory, and join-in-progress).

- In-lobby map voting (the `getRandomMap` helper now exists and powers `/splegg random`; voting UI is the remaining piece).

- Leaderboards.

- PlaceholderAPI support.

- In-Game scoreboard.

**Licensed under the GPLv3.**
