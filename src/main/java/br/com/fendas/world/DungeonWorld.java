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
        rule(GameRule.DO_MOB_SPAWNING, false);
        rule(GameRule.DO_DAYLIGHT_CYCLE, false);
        rule(GameRule.DO_WEATHER_CYCLE, false);
        rule(GameRule.DO_FIRE_TICK, false);
        rule(GameRule.MOB_GRIEFING, false);
        rule(GameRule.KEEP_INVENTORY, true);
        rule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        rule(GameRule.DO_INSOMNIA, false);
        rule(GameRule.DO_PATROL_SPAWNING, false);
        rule(GameRule.DO_TRADER_SPAWNING, false);
        rule(GameRule.SPAWN_CHUNK_RADIUS, 0);
        return true;
    }

    private <T> void rule(GameRule<T> rule, T value) {
        try {
            world.setGameRule(rule, value);
        } catch (Throwable ignored) {
            // algumas gamerules mudaram de nome em versões novas; ignoramos as que não existem
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
