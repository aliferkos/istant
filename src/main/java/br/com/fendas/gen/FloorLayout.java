package br.com.fendas.gen;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;

/** Resultado da geração de um andar: 3 salas de combate + sala do tesouro. */
public final class FloorLayout {

    public final List<Room> rooms;
    public final Map<Long, BlockData> blocks;
    public final int minX, maxX, minY, maxY, minZ, maxZ;
    public final int[] chest;     // {x, y, z}
    public final int[] exit;      // bloco de saída
    public final int[] descend;   // bloco para descer (null no último andar)

    public FloorLayout(List<Room> rooms, Map<Long, BlockData> blocks, int minX, int maxX, int minY, int maxY,
                       int minZ, int maxZ, int[] chest, int[] exit, int[] descend) {
        this.rooms = rooms;
        this.blocks = blocks;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.chest = chest;
        this.exit = exit;
        this.descend = descend;
    }

    public BoundingBox box() {
        return new BoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static boolean is(Block b, int[] pos) {
        return pos != null && b.getX() == pos[0] && b.getY() == pos[1] && b.getZ() == pos[2];
    }

    public static Location loc(World w, int[] pos) {
        return new Location(w, pos[0] + 0.5, pos[1], pos[2] + 0.5);
    }
}
