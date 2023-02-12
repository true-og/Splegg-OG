# Splegg-OG

![Icon](https://raw.githubusercontent.com/NotAlexNoyle/Splegg-OG/master/assets/splegg-logo.png)

**Splegg-OG** is a Splegg plugin originally made by MrLuangamer, updated for worldedit 7.2 and spigot 1.16.4 by Hraponssi, and finally updated for Purpur 1.18.2 for use by [TrueOG](https://true-og.net/) by [NotAlexNoyle](https://github.com/NotAlexNoyle/).

Dependencies: WorldEdit, Vault.

**To Set Up:**:

`/splegg create my-map` First, create a Splegg map with a name of your choosing.
`/splegg setspawn my-map next` The amount of spawn points you set is the amount of players that will be able to join the map.
`/splegg setlobby my-map` Set a lobby area for the map during the voting/warm-up period.
`//wand` Summon a wand with WorldEdit and then use it to select two points. These represent the corners of your floor.
`/splegg addfloor my-map` Add the area you just selected with WorldEdit as a floor. You can add as many as you like.
`/stop` Restart the server so that the map can be recognized correctly (this will become unnecessary in a future update).
`/splegg join my-map` You can now join the map you just created, and so can anyone else with splegg.join permission.
`/splegg start` Anyone with splegg.admin permissions can start the game even if not enough players join.

Original bukkit page: https://dev.bukkit.org/projects/splegg-minigame

Current Gradle Build Target: Purpur 1.18.2

To build:

`./gradlew build`

The resulting .jar file will be in build/libs/

**Licensed under the GPLv3.**