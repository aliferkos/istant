package br.com.fendas.gen;

import br.com.fendas.util.BlockPos;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gera um andar inteiro em código, sem schematics:
 * [Sala 1] -corredor- [Sala 2] -corredor- [Sala 3] -corredor- [Tesouro]
 * Cada sala sorteia tamanho, altura e um "estilo" (pilares, fossos, ruínas ou misto).
 */
public final class RoomGenerator {

    public static final int CORRIDOR = 5;

    private static final int FREE = 0, BLOCKED = 1, HAZARD = 2;

    private final Theme t;
    private final Random rnd;
    private final Map<Long, BlockData> blocks = new LinkedHashMap<>();
    private final Map<Material, BlockData> cache = new EnumMap<>(Material.class);

    public RoomGenerator(Theme theme, Random rnd) {
        this.t = theme;
        this.rnd = rnd;
    }

    public FloorLayout generate(int x0, int y0, int zc, boolean lastFloor) {
        List<Room> rooms = new ArrayList<>();
        int cursor = x0;
        for (int i = 0; i < 4; i++) {
            boolean treasure = i == 3;
            int w = treasure ? 13 : odd(19, 27);
            int d = treasure ? 13 : odd(17, 25);
            int h = treasure ? 6 : 6 + rnd.nextInt(3);
            Room r = new Room(i, cursor, cursor + w - 1, zc - d / 2, zc + d / 2, y0, h, treasure);
            shell(r);
            rooms.add(r);
            cursor = r.maxX + 1 + CORRIDOR;
        }
        for (int i = 0; i < 3; i++) corridor(rooms.get(i), rooms.get(i + 1), zc, y0);

        int[] chest = null, exit = null, descend = null;
        for (Room r : rooms) {
            if (!r.treasure) {
                combatInterior(r);
            } else {
                int cx = (r.minX + r.maxX) / 2;
                chest = new int[]{cx, y0 + 1, r.centerZ};
                exit = new int[]{cx, y0 + 1, r.centerZ + 3};
                descend = lastFloor ? null : new int[]{cx, y0 + 1, r.centerZ - 3};
                treasureInterior(r, chest, exit, descend);
            }
        }

        int minX = rooms.get(0).minX, maxX = rooms.get(3).maxX;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, maxY = y0;
        for (Room r : rooms) {
            minZ = Math.min(minZ, r.minZ);
            maxZ = Math.max(maxZ, r.maxZ);
            maxY = Math.max(maxY, y0 + r.height + 1);
        }
        return new FloorLayout(rooms, blocks, minX, maxX, y0 - 1, maxY, minZ, maxZ, chest, exit, descend);
    }

    // ------------------------------------------------------------------ estrutura

    private void shell(Room r) {
        for (int x = r.minX; x <= r.maxX; x++) {
            for (int z = r.minZ; z <= r.maxZ; z++) {
                set(x, r.y0 - 1, z, t.sub);
                boolean edgeX = x == r.minX || x == r.maxX;
                boolean edgeZ = z == r.minZ || z == r.maxZ;
                if (edgeX || edgeZ) {
                    boolean trimCol = (edgeX && edgeZ)
                            || (edgeZ && (x - r.minX) % 6 == 0)
                            || (edgeX && (z - r.minZ) % 6 == 0);
                    for (int y = r.y0; y <= r.y0 + r.height + 1; y++) {
                        set(x, y, z, trimCol ? t.trim.pick(rnd) : t.wall.pick(rnd));
                    }
                } else {
                    set(x, r.y0, z, t.floor.pick(rnd));
                    boolean light = (x - r.minX) % 5 == 0 && (z - r.minZ) % 5 == 0;
                    set(x, r.y0 + r.height + 1, z, light ? t.light : t.ceiling);
                }
            }
        }
    }

    private void corridor(Room a, Room b, int zc, int y0) {
        int x1 = a.maxX + 1, x2 = b.minX - 1;
        int mid = (x1 + x2) / 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = zc - 2; z <= zc + 2; z++) {
                set(x, y0 - 1, z, t.sub);
                if (Math.abs(z - zc) == 2) {
                    for (int y = y0; y <= y0 + 5; y++) set(x, y, z, t.wall.pick(rnd));
                } else {
                    set(x, y0, z, t.floor.pick(rnd));
                    set(x, y0 + 5, z, (x == mid && z == zc) ? t.light : t.ceiling);
                }
            }
        }
        // aberturas nas paredes das salas
        for (int z = zc - 1; z <= zc + 1; z++) {
            for (int y = y0 + 1; y <= y0 + 4; y++) {
                set(a.maxX, y, z, Material.AIR);
                set(b.minX, y, z, Material.AIR);
                a.exitDoor.add(BlockPos.pack(a.maxX, y, z));
                b.entranceDoor.add(BlockPos.pack(b.minX, y, z));
            }
        }
        // a saída de cada sala começa fechada (abre ao limpar a sala)
        BlockData bars = doorData();
        for (long p : a.exitDoor) blocks.put(p, bars);
    }

    public static BlockData doorData() {
        BlockData bars = Material.IRON_BARS.createBlockData();
        if (bars instanceof MultipleFacing mf) {
            mf.setFace(BlockFace.NORTH, true);
            mf.setFace(BlockFace.SOUTH, true);
        }
        return bars;
    }

    // ------------------------------------------------------------------ interior de combate

    private void combatInterior(Room r) {
        int iw = r.maxX - r.minX - 1;
        int id = r.maxZ - r.minZ - 1;
        int[][] g = new int[iw][id];
        boolean[][] res = new boolean[iw][id];

        // reserva a área perto da entrada e da saída
        int czIdx = r.centerZ - r.minZ - 1;
        for (int gx = 0; gx < iw; gx++) {
            for (int gz = 0; gz < id; gz++) {
                boolean nearLane = Math.abs(gz - czIdx) <= 2;
                if (nearLane && (gx <= 3 || gx >= iw - 4)) res[gx][gz] = true;
            }
        }

        int style = rnd.nextInt(4); // 0 pilares, 1 fossos, 2 ruínas, 3 misto
        int pillars = switch (style) {
            case 0 -> 4 + rnd.nextInt(3);
            case 3 -> 2 + rnd.nextInt(2);
            case 2 -> 1 + rnd.nextInt(2);
            default -> rnd.nextInt(2);
        };
        int pits = t.hazard == null ? 0 : switch (style) {
            case 1 -> 3 + rnd.nextInt(2);
            case 3 -> 1 + rnd.nextInt(2);
            default -> 0;
        };
        if (style == 1 && t.hazard == null) pillars += 3; // tema sem fosso vira sala de pilares
        int covers = style == 2 ? 10 + rnd.nextInt(6) : 4 + rnd.nextInt(5);

        // pilares
        for (int i = 0; i < pillars; i++) {
            int size = (iw > 21 && rnd.nextBoolean()) ? 3 : 2;
            int[] at = findRect(g, res, size, size, 3);
            if (at == null) continue;
            boolean broken = rnd.nextDouble() < 0.3;
            int top = broken ? 1 + rnd.nextInt(Math.max(1, r.height - 2)) : r.height;
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    g[at[0] + dx][at[1] + dz] = BLOCKED;
                    int wx = r.minX + 1 + at[0] + dx, wz = r.minZ + 1 + at[1] + dz;
                    for (int y = 1; y <= top; y++) set(wx, r.y0 + y, wz, t.trim.pick(rnd));
                }
            }
        }

        // fossos (lava, neve fofa)
        for (int i = 0; i < pits; i++) {
            int sw = 2 + rnd.nextInt(3), sd = 2 + rnd.nextInt(3);
            int[] at = findRect(g, res, sw, sd, 3);
            if (at == null) continue;
            for (int dx = 0; dx < sw; dx++) {
                for (int dz = 0; dz < sd; dz++) {
                    g[at[0] + dx][at[1] + dz] = HAZARD;
                    set(r.minX + 1 + at[0] + dx, r.y0, r.minZ + 1 + at[1] + dz, t.hazard);
                }
            }
        }

        // coberturas baixas (1-2 blocos)
        for (int i = 0; i < covers; i++) {
            boolean alongX = rnd.nextBoolean();
            int sw = alongX ? 1 + rnd.nextInt(2) : 1, sd = alongX ? 1 : 1 + rnd.nextInt(2);
            int[] at = findRect(g, res, sw, sd, 2);
            if (at == null) continue;
            int hgt = 1 + rnd.nextInt(2);
            for (int dx = 0; dx < sw; dx++) {
                for (int dz = 0; dz < sd; dz++) {
                    g[at[0] + dx][at[1] + dz] = BLOCKED;
                    for (int y = 1; y <= hgt; y++) {
                        set(r.minX + 1 + at[0] + dx, r.y0 + y, r.minZ + 1 + at[1] + dz, t.cover.pick(rnd));
                    }
                }
            }
        }

        // manchas no piso
        if (t.patch != null) {
            int n = 3 + rnd.nextInt(4);
            for (int i = 0; i < n; i++) {
                int px = rnd.nextInt(iw), pz = rnd.nextInt(id);
                int rad = 1 + rnd.nextInt(2);
                for (int dx = -rad; dx <= rad; dx++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        int gx = px + dx, gz = pz + dz;
                        if (gx < 0 || gz < 0 || gx >= iw || gz >= id) continue;
                        if (dx * dx + dz * dz > rad * rad + 1 || g[gx][gz] == HAZARD) continue;
                        set(r.minX + 1 + gx, r.y0, r.minZ + 1 + gz, t.patch);
                    }
                }
            }
        }

        // enfeites
        if (t.decor == Material.COBWEB) {
            for (int gx = 0; gx < iw; gx++) {
                for (int gz = 0; gz < id; gz++) {
                    boolean corner = (gx == 0 || gx == iw - 1) && (gz == 0 || gz == id - 1);
                    if (g[gx][gz] == FREE && (corner || rnd.nextDouble() < 0.05)) {
                        set(r.minX + 1 + gx, r.y0 + r.height, r.minZ + 1 + gz, Material.COBWEB);
                    }
                }
            }
        } else if (t.decor != null) {
            for (int gx = 0; gx < iw; gx++) {
                for (int gz = 0; gz < id; gz++) {
                    if (g[gx][gz] == FREE && rnd.nextDouble() < 0.3) {
                        set(r.minX + 1 + gx, r.y0 + 1, r.minZ + 1 + gz, t.decor);
                    }
                }
            }
        }

        // pontos de nascimento: longe da entrada, em chão livre
        List<int[]> candidates = new ArrayList<>();
        for (int gx = 5; gx < iw; gx++) {
            for (int gz = 0; gz < id; gz++) {
                if (g[gx][gz] == FREE && !res[gx][gz]) {
                    candidates.add(new int[]{r.minX + 1 + gx, r.minZ + 1 + gz});
                }
            }
        }
        Collections.shuffle(candidates, rnd);
        r.spawnPoints.addAll(candidates.subList(0, Math.min(16, candidates.size())));
    }

    /** Procura um retângulo livre (com 1 bloco de folga em volta) a pelo menos `margin` das paredes. */
    private int[] findRect(int[][] g, boolean[][] res, int sw, int sd, int margin) {
        int iw = g.length, id = g[0].length;
        int rangeX = iw - 2 * margin - sw + 1, rangeZ = id - 2 * margin - sd + 1;
        if (rangeX <= 0 || rangeZ <= 0) return null;
        for (int attempt = 0; attempt < 30; attempt++) {
            int ax = margin + rnd.nextInt(rangeX), az = margin + rnd.nextInt(rangeZ);
            boolean ok = true;
            for (int x = ax - 1; x <= ax + sw && ok; x++) {
                for (int z = az - 1; z <= az + sd; z++) {
                    if (x < 0 || z < 0 || x >= iw || z >= id) continue;
                    if (g[x][z] != FREE || res[x][z]) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) return new int[]{ax, az};
        }
        return null;
    }

    // ------------------------------------------------------------------ sala do tesouro

    private void treasureInterior(Room r, int[] chest, int[] exit, int[] descend) {
        int cx = chest[0], cz = chest[2], y0 = r.y0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) set(cx + dx, y0, cz + dz, t.trim.pick(rnd));
            }
        }
        set(cx, y0, cz, t.light);

        // colunas nos cantos
        int[][] corners = {{r.minX + 2, r.minZ + 2}, {r.minX + 2, r.maxZ - 2}, {r.maxX - 2, r.minZ + 2}, {r.maxX - 2, r.maxZ - 2}};
        for (int[] c : corners) {
            for (int y = 1; y <= r.height; y++) set(c[0], y0 + y, c[1], y == r.height - 1 ? t.light : t.trim.pick(rnd));
        }

        BlockData chestData = Material.CHEST.createBlockData();
        if (chestData instanceof Directional dir) dir.setFacing(BlockFace.WEST);
        blocks.put(BlockPos.pack(chest[0], chest[1], chest[2]), chestData);

        set(exit[0], exit[1], exit[2], Material.CRYING_OBSIDIAN);
        if (descend != null) set(descend[0], descend[1], descend[2], Material.LODESTONE);
    }

    // ------------------------------------------------------------------ util

    private int odd(int min, int max) {
        int v = min + rnd.nextInt(max - min + 1);
        return v % 2 == 0 ? v + 1 : v;
    }

    private void set(int x, int y, int z, Material m) {
        blocks.put(BlockPos.pack(x, y, z), cache.computeIfAbsent(m, Material::createBlockData));
    }
}
