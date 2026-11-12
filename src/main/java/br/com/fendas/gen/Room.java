package br.com.fendas.gen;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Uma sala gerada. Coordenadas incluem as paredes. */
public final class Room {

    public final int index;
    public final int minX, maxX, minZ, maxZ, y0, height;
    public final int centerZ;
    public final boolean treasure;

    /** Blocos da abertura de entrada (parede oeste) e saída (parede leste). */
    public final List<Long> entranceDoor = new ArrayList<>();
    public final List<Long> exitDoor = new ArrayList<>();
    /** Pontos de nascimento de mobs: {x, z}. */
    public final List<int[]> spawnPoints = new ArrayList<>();

    public Room(int index, int minX, int maxX, int minZ, int maxZ, int y0, int height, boolean treasure) {
        this.index = index;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.y0 = y0;
        this.height = height;
        this.treasure = treasure;
        this.centerZ = (minZ + maxZ) / 2;
    }

    /** Está dentro da sala (sem contar paredes)? */
    public boolean inside(Location l) {
        int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
        return x > minX && x < maxX && z > minZ && z < maxZ && y >= y0 && y <= y0 + height + 1;
    }

    /** Ponto seguro logo após a entrada, olhando para o leste. */
    public Location entrance(World w) {
        return new Location(w, minX + 2.5, y0 + 1, centerZ + 0.5, -90f, 0f);
    }

    public Location center(World w) {
        return new Location(w, (minX + maxX) / 2.0 + 0.5, y0 + 1, centerZ + 0.5);
    }

    public Location randomSpawn(World w, Random rnd) {
        if (spawnPoints.isEmpty()) return center(w);
        int[] p = spawnPoints.get(rnd.nextInt(spawnPoints.size()));
        return new Location(w, p[0] + 0.5, y0 + 1, p[1] + 0.5, rnd.nextFloat() * 360f, 0f);
    }
}
