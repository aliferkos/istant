package br.com.fendas.world;

import br.com.fendas.FendasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Guarda em disco de onde cada jogador saiu, para devolvê-lo mesmo se o servidor
 * reiniciar ou ele desconectar no meio da fenda.
 */
public final class ReturnStore {

    public record Entry(Location location, GameMode gameMode) {
    }

    private final FendasPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    public ReturnStore(FendasPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "retornos.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public void put(Player p) {
        String key = p.getUniqueId().toString();
        if (yml.isConfigurationSection(key)) return; // não sobrescreve o local original
        Location l = p.getLocation();
        ConfigurationSection s = yml.createSection(key);
        s.set("mundo", l.getWorld().getName());
        s.set("x", l.getX());
        s.set("y", l.getY());
        s.set("z", l.getZ());
        s.set("yaw", (double) l.getYaw());
        s.set("pitch", (double) l.getPitch());
        s.set("modo", p.getGameMode().name());
        save();
    }

    public boolean has(UUID id) {
        return yml.isConfigurationSection(id.toString());
    }

    /** Remove e devolve o registro (ou null). */
    public Entry remove(UUID id) {
        ConfigurationSection s = yml.getConfigurationSection(id.toString());
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("mundo", ""));
        Location loc = w == null
                ? Bukkit.getWorlds().get(0).getSpawnLocation()
                : new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
        GameMode mode;
        try {
            mode = GameMode.valueOf(s.getString("modo", "SURVIVAL"));
        } catch (IllegalArgumentException e) {
            mode = GameMode.SURVIVAL;
        }
        yml.set(id.toString(), null);
        save();
        return new Entry(loc, mode);
    }

    public void save() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao salvar retornos.yml: " + e.getMessage());
        }
    }
}
