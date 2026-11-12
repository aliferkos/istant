package br.com.fendas.world;

import br.com.fendas.FendasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Cria (e recria a cada boot) o mundo vazio das fendas. */
public final class DungeonWorld {

    private static final String MARKER = ".fendas-marker";
    private final FendasPlugin plugin;
    private World world;

    public DungeonWorld(FendasPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean prepare() {
        String name = plugin.getConfig().getString("mundo", "fendas");
        World main = Bukkit.getWorlds().get(0);
        if (main.getName().equalsIgnoreCase(name)) {
            plugin.getLogger().severe("O mundo das fendas não pode ser o mundo principal! Mude 'mundo' na config.");
            return false;
        }

        File folder = new File(Bukkit.getWorldContainer(), name);
        World existing = Bukkit.getWorld(name);
        boolean ours = new File(folder, MARKER).exists();

        if (ours) {
            if (existing != null) {
                for (Player p : existing.getPlayers()) p.teleport(main.getSpawnLocation());
                Bukkit.unloadWorld(existing, false);
            }
            deleteFolder(folder.toPath());
        } else if (folder.isDirectory()) {
            plugin.getLogger().warning("A pasta '" + name + "' já existe e não foi criada pelo Fendas. "
                    + "Ela NÃO será apagada. Recomendo usar outro nome em 'mundo'.");
        }

        world = Bukkit.getWorld(name);
        if (world == null) {
            world = new WorldCreator(name)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Não foi possível criar o mundo das fendas.");
            return false;
        }

        try {
            new File(world.getWorldFolder(), MARKER).createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui criar o marcador do mundo: " + e.getMessage());
        }

        world.setDifficulty(Difficulty.HARD);
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        // Os nomes das gamerules mudaram entre versões (1.21 -> 26.x).
        // Procuramos por nome e ignoramos as que não existirem na versão do servidor.
        rule(false, "DO_MOB_SPAWNING", "SPAWN_MOBS");
        rule(false, "DO_DAYLIGHT_CYCLE", "ADVANCE_TIME");
        rule(false, "DO_WEATHER_CYCLE", "ADVANCE_WEATHER");
        rule(false, "DO_FIRE_TICK");
        rule(0, "FIRE_SPREAD_RADIUS_AROUND_PLAYER");
        rule(false, "MOB_GRIEFING");
        rule(true, "KEEP_INVENTORY");
        rule(false, "ANNOUNCE_ADVANCEMENTS", "SHOW_ADVANCEMENT_MESSAGES");
        rule(false, "DO_INSOMNIA", "SPAWN_PHANTOMS");
        rule(false, "DO_PATROL_SPAWNING", "SPAWN_PATROLS");
        rule(false, "DO_TRADER_SPAWNING", "SPAWN_WANDERING_TRADERS");
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void rule(Object value, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Object field = GameRule.class.getField(name).get(null);
                if (field instanceof GameRule rule) {
                    world.setGameRule(rule, value);
                    return;
                }
            } catch (Throwable ignored) {
                // não existe nesta versão: tenta o próximo nome
            }
        }
    }

    private void deleteFolder(Path path) {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao limpar o mundo das fendas: " + e.getMessage());
        }
    }

    public World world() {
        return world;
    }

    public boolean is(World w) {
        return world != null && w != null && w.getUID().equals(world.getUID());
    }
}
