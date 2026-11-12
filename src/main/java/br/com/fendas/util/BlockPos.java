package br.com.fendas.util;

/** Empacota coordenadas de bloco em um long (mesmo formato do Minecraft). */
public final class BlockPos {

    private BlockPos() {
    }

    public static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    public static int x(long p) {
        return (int) (p >> 38);
    }

    public static int y(long p) {
        return (int) (p << 52 >> 52);
    }

    public static int z(long p) {
        return (int) (p << 26 >> 38);
    }
}
