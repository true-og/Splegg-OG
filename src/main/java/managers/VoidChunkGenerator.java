package managers;

import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

// Pure-void generator for Splegg lobby and arena worlds.
// Saved chunks (from the .mca region files of the copied world) still load from
// disk. Anything outside the saved area generates as void instead of the
// level.dat default, which is what admins asked MyWorlds for when they ran
// /mw create <world> void -- a plain directory copy loses that setting.
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {

        return false;

    }

    @Override
    public boolean shouldGenerateSurface() {

        return false;

    }

    @Override
    public boolean shouldGenerateCaves() {

        return false;

    }

    @Override
    public boolean shouldGenerateDecorations() {

        return false;

    }

    @Override
    public boolean shouldGenerateMobs() {

        return false;

    }

    @Override
    public boolean shouldGenerateStructures() {

        return false;

    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {

        return Collections.emptyList();

    }

}
