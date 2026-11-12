package br.com.fendas.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Coloca/remove muitos blocos espalhando o trabalho por vários ticks (evita lag). */
public final class BlockPlacer {

    private BlockPlacer() {
    }

    public static void place(Plugin plugin, World world, Map<Long, BlockData> blocks, int perTick, Runnable done) {
        final List<Map.Entry<Long, BlockData>> list = new ArrayList<>(blocks.entrySet());
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int end = Math.min(list.size(), index + perTick);
                for (; index < end; index++) {
                    long p = list.get(index).getKey();
                    world.getBlockAt(BlockPos.x(p), BlockPos.y(p), BlockPos.z(p))
                            .setBlockData(list.get(index).getValue(), false);
                }
                if (index >= list.size()) {
                    cancel();
                    if (done != null) done.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public static void clear(Plugin plugin, World world, Collection<Long> positions, int perTick, Runnable done) {
        final List<Long> list = new ArrayList<>(positions);
        final BlockData air = Material.AIR.createBlockData();
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int end = Math.min(list.size(), index + perTick);
                for (; index < end; index++) {
                    long p = list.get(index);
                    Block b = world.getBlockAt(BlockPos.x(p), BlockPos.y(p), BlockPos.z(p));
                    if (!b.getType().isAir()) b.setBlockData(air, false);
                }
                if (index >= list.size()) {
                    cancel();
                    if (done != null) done.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
