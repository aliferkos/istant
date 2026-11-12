package br.com.fendas.dungeon;

import br.com.fendas.FendasPlugin;
import br.com.fendas.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Controla todas as partidas, as "vagas" no mundo vazio e quem está em qual partida. */
public final class DungeonManager {

    private final FendasPlugin plugin;
    private final Map<UUID, DungeonInstance> byPlayer = new HashMap<>();
    private final Map<UUID, DungeonInstance> byId = new HashMap<>();
    private final List<DungeonInstance> instances = new ArrayList<>();
    private final Set<Integer> usedSlots = new HashSet<>();
    private final BukkitTask ticker;

    public DungeonManager(FendasPlugin plugin) {
        this.plugin = plugin;
        this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (DungeonInstance inst : new ArrayList<>(instances)) {
                try {
                    inst.tick();
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro na fenda " + inst.id() + ": " + e);
                    e.printStackTrace();
                    inst.end("<red>A fenda colapsou por um erro. Você foi devolvido.");
                }
            }
        }, 20L, 10L);
    }

    /** Monta o grupo em volta do altar e inicia a partida. */
    public void startFromAltar(Player activator, Location altar) {
        double radius = plugin.getConfig().getDouble("grupo.raio-altar", 6);
        int max = plugin.getConfig().getInt("grupo.tamanho-maximo", 4);
        List<Player> party = new ArrayList<>();
        party.add(activator);
        altar.getNearbyPlayers(radius).stream()
                .filter(p -> !p.equals(activator))
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> !byPlayer.containsKey(p.getUniqueId()))
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(altar)))
                .limit(Math.max(0, max - 1))
                .forEach(party::add);
        start(party);
    }

    public void start(List<Player> party) {
        int slot = 0;
        while (usedSlots.contains(slot)) slot++;
        usedSlots.add(slot);

        DungeonInstance inst = new DungeonInstance(plugin, this, slot, plugin.dungeonWorld().world());
        instances.add(inst);
        byId.put(inst.id(), inst);
        for (Player p : party) byPlayer.put(p.getUniqueId(), inst);
        if (party.size() > 1) {
            String names = String.join(", ", party.stream().map(Player::getName).toList());
            party.forEach(p -> Msg.send(p, "Grupo: <white>" + names));
        }
        inst.start(party);
    }

    public DungeonInstance of(Player p) {
        return byPlayer.get(p.getUniqueId());
    }

    /** Partida dona deste mob (ou null se não for mob de fenda). */
    public DungeonInstance mobInstance(Entity e) {
        if (e == null) return null;
        String raw = e.getPersistentDataContainer().get(plugin.mobTag(), PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return byId.get(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isDungeonMob(Entity e) {
        return e != null && e.getPersistentDataContainer().has(plugin.mobTag(), PersistentDataType.STRING);
    }

    void unmap(UUID player) {
        byPlayer.remove(player);
    }

    void release(DungeonInstance inst) {
        instances.remove(inst);
        byId.remove(inst.id());
        byPlayer.values().removeIf(i -> i == inst);
        if (!plugin.isEnabled()) return; // o mundo é recriado no próximo boot
        inst.cleanup(() -> usedSlots.remove(inst.slot()));
    }

    public List<DungeonInstance> instances() {
        return List.copyOf(instances);
    }

    /** Desligando o servidor: devolve todo mundo na hora. */
    public void shutdown() {
        ticker.cancel();
        for (DungeonInstance inst : new ArrayList<>(instances)) {
            inst.end("<gray>O servidor está reiniciando; você foi devolvido.");
        }
    }
}
