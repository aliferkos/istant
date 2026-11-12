package br.com.fendas.world;

import br.com.fendas.FendasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Altares onde a Pedra de Fenda é usada. Salvos na config como "mundo;x;y;z". */
public final class AltarManager {

    private final FendasPlugin plugin;
    private final List<String> altars = new ArrayList<>();

    public AltarManager(FendasPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        altars.clear();
        altars.addAll(plugin.getConfig().getStringList("altares"));
    }

    private static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    public boolean isAltar(Block b) {
        return b != null && altars.contains(key(b));
    }

    public boolean add(Block b) {
        if (isAltar(b)) return false;
        altars.add(key(b));
        persist();
        return true;
    }

    public boolean remove(Block b) {
        boolean removed = altars.remove(key(b));
        if (removed) persist();
        return removed;
    }

    public List<String> list() {
        return List.copyOf(altars);
    }

    private void persist() {
        plugin.getConfig().set("altares", altars);
        plugin.saveConfig();
    }

    /** Partículas nos altares que têm jogadores por perto. */
    public void tickParticles() {
        for (String s : altars) {
            String[] p = s.split(";");
            if (p.length != 4) continue;
            World w = Bukkit.getWorld(p[0]);
            if (w == null) continue;
            int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Location loc = new Location(w, x + 0.5, y + 1.3, z + 0.5);
            if (loc.getNearbyPlayers(16).isEmpty()) continue;
            w.spawnParticle(Particle.PORTAL, loc, 25, 0.3, 0.4, 0.3, 0.3);
            w.spawnParticle(Particle.ENCHANT, loc, 8, 0.4, 0.4, 0.4, 0.5);
        }
    }
}
