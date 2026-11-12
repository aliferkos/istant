package br.com.fendas.dungeon;

import br.com.fendas.FendasPlugin;
import br.com.fendas.gen.FloorLayout;
import br.com.fendas.gen.Room;
import br.com.fendas.gen.RoomGenerator;
import br.com.fendas.gen.Theme;
import br.com.fendas.util.Attr;
import br.com.fendas.util.BlockPlacer;
import br.com.fendas.util.BlockPos;
import br.com.fendas.util.Msg;
import br.com.fendas.world.ReturnStore;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Uma partida em andamento: grupo, andar atual, salas, ondas e modificadores. */
public final class DungeonInstance {

    public enum State { BUILDING, COUNTDOWN, FIGHTING, WAITING, TREASURE, ENDED }

    public static final int FLOOR_SPACING_Z = 200;

    private final FendasPlugin plugin;
    private final DungeonManager manager;
    private final UUID id = UUID.randomUUID();
    private final int slot;
    private final World world;
    private final Random rnd = new Random();

    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> dead = new HashSet<>();
    private final Set<UUID> mobs = new HashSet<>();
    private final List<FloorLayout> builtFloors = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();

    private State state = State.BUILDING;
    private int floor;
    private FloorLayout layout;
    private Theme theme;
    private int roomIndex;
    private int wave;
    private int waveTotal;
    private long waveStartedAt;
    private int countdownTicks;
    private final long startedAt = System.currentTimeMillis();
    private final BossBar bar = BossBar.bossBar(Msg.mm("Fenda"), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);

    DungeonInstance(FendasPlugin plugin, DungeonManager manager, int slot, World world) {
        this.plugin = plugin;
        this.manager = manager;
        this.slot = slot;
        this.world = world;
    }

    // ================================================================ início / andares

    void start(List<Player> party) {
        for (Player p : party) {
            members.add(p.getUniqueId());
            plugin.returns().put(p);
            Msg.send(p, "<light_purple>A fenda se abre... prepare-se!");
            Msg.sound(p, "block.end_portal.spawn", 0.6f, 1.4f);
        }
        buildFloor(1);
    }

    private void buildFloor(int number) {
        state = State.BUILDING;
        floor = number;
        Theme previous = theme;
        do {
            theme = Theme.ALL.get(rnd.nextInt(Theme.ALL.size()));
        } while (theme == previous && Theme.ALL.size() > 1);

        modifiers.forEach(m -> forEachOnline(m::cleanup));
        modifiers.clear();
        modifiers.add(Modifier.randomCurse(rnd));
        if (rnd.nextDouble() < 0.5) modifiers.add(Modifier.randomBlessing(rnd));

        int maxFloors = plugin.getConfig().getInt("partida.andares-maximos", 3);
        int x0 = slot * plugin.getConfig().getInt("espacamento-instancias", 1000);
        int y0 = plugin.getConfig().getInt("altura-base", 100);
        int zc = (number - 1) * FLOOR_SPACING_Z;
        layout = new RoomGenerator(theme, rnd).generate(x0, y0, zc, number >= maxFloors);
        builtFloors.add(layout);

        for (int cx = layout.minX >> 4; cx <= layout.maxX >> 4; cx++) {
            for (int cz = layout.minZ >> 4; cz <= layout.maxZ >> 4; cz++) {
                world.addPluginChunkTicket(cx, cz, plugin);
            }
        }

        int perTick = plugin.getConfig().getInt("partida.blocos-por-tick", 2500);
        BlockPlacer.place(plugin, world, layout.blocks, perTick, this::onFloorBuilt);
    }

    private void onFloorBuilt() {
        if (state == State.ENDED) return;
        spawnLabel(layout.exit, "<dark_purple>⬤ <light_purple>Sair da Fenda</light_purple>\n<gray>clique com o botão direito");
        if (layout.descend != null) {
            spawnLabel(layout.descend, "<gold>▼ <yellow>Descer ao andar " + (floor + 1) + "</yellow>\n<gray>clique com o botão direito");
        }

        roomIndex = 0;
        Room first = layout.rooms.get(0);
        forEachOnline(p -> {
            revive(p, first.entrance(world));
            p.showBossBar(bar);
        });
        forEachOnline(p -> {
            Msg.title(p, theme.color + "Andar " + floor, "<gray>" + theme.name);
            Msg.send(p, "<white>Andar " + floor + " — " + theme.color + theme.name);
            for (Modifier m : modifiers) {
                String kind = m.curse ? "<red>Maldição" : "<green>Bênção";
                String reward = m.reward.isEmpty() ? "" : " <gray>(<yellow>" + m.reward + "<gray>)";
                Msg.send(p, kind + ": " + m.label() + " <gray>— " + m.effect + reward);
            }
            Msg.sound(p, "entity.enderman.teleport", 1f, 0.8f);
        });
        beginCountdown(plugin.getConfig().getInt("partida.contagem-inicial", 5));
    }

    private void spawnLabel(int[] pos, String text) {
        Location loc = FloorLayout.loc(world, pos).add(0, 1.6, 0);
        world.spawn(loc, TextDisplay.class, td -> {
            td.text(Msg.mm(text));
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
        });
    }

    // ================================================================ tick (a cada 10 ticks)

    void tick() {
        if (state == State.ENDED || state == State.BUILDING) return;

        long limit = plugin.getConfig().getLong("partida.tempo-limite-minutos", 40) * 60_000L;
        if (System.currentTimeMillis() - startedAt > limit) {
            end("<red>O tempo acabou e a fenda se fechou.");
            return;
        }

        Room room = currentRoom();
        forEachOnline(p -> {
            if (dead.contains(p.getUniqueId())) {
                if (p.getLocation().distanceSquared(room.center(world)) > 40 * 40) p.teleport(room.center(world));
            } else {
                for (Modifier m : modifiers) m.tickPlayer(p);
            }
        });

        switch (state) {
            case COUNTDOWN -> {
                countdownTicks -= 10;
                if (countdownTicks <= 0) {
                    startRoomFight();
                } else if (countdownTicks % 20 == 0) {
                    int s = countdownTicks / 20;
                    forEachOnline(p -> {
                        Msg.title(p, "<yellow>" + s, "<gray>prepare-se");
                        Msg.sound(p, "block.note_block.hat", 1f, 1.2f);
                    });
                }
            }
            case WAITING -> {
                if (anyAliveInside(room)) {
                    if (room.treasure) {
                        state = State.TREASURE;
                        forEachOnline(p -> {
                            Msg.title(p, "<gold>Tesouro!", "<gray>o baú é de vocês");
                            Msg.sound(p, "ui.toast.challenge_complete", 0.8f, 1f);
                        });
                    } else {
                        beginCountdown(3);
                    }
                }
            }
            case FIGHTING -> tickFight(room);
            default -> {
            }
        }
        updateBar();
    }

    private void beginCountdown(int seconds) {
        state = State.COUNTDOWN;
        countdownTicks = Math.max(1, seconds) * 20;
        int total = layout.rooms.size() - 1;
        forEachOnline(p -> Msg.title(p, theme.color + "Sala " + (roomIndex + 1) + "/" + total, "<yellow>" + seconds));
    }

    private void startRoomFight() {
        Room room = currentRoom();
        // puxa quem ficou para trás e fecha a porta de entrada
        forEachOnline(p -> {
            if (!dead.contains(p.getUniqueId()) && !room.inside(p.getLocation())) p.teleport(room.entrance(world));
        });
        if (!room.entranceDoor.isEmpty()) setBlocks(room.entranceDoor, RoomGenerator.doorData());
        wave = 1;
        state = State.FIGHTING;
        spawnWave(room);
    }

    private void tickFight(Room room) {
        mobs.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead() || !e.isValid();
        });
        boolean slow = System.currentTimeMillis() - waveStartedAt > 60_000;
        for (UUID uuid : mobs) {
            Entity e = Bukkit.getEntity(uuid);
            if (e == null) continue;
            if (!room.inside(e.getLocation())) e.teleport(room.randomSpawn(world, rnd));
            if (slow && !e.isGlowing()) e.setGlowing(true); // ajuda a achar os últimos
            if (e instanceof Mob mob) {
                LivingEntity target = mob.getTarget();
                if (target == null || target.isDead() || (target instanceof Player tp && !isAlive(tp))) {
                    Player nearest = nearestAlive(mob.getLocation());
                    if (nearest != null) mob.setTarget(nearest);
                }
            }
        }
        if (!mobs.isEmpty()) return;

        int waves = plugin.getConfig().getInt("partida.ondas-por-sala", 3);
        if (wave < waves) {
            wave++;
            spawnWave(room);
        } else {
            roomCleared(room);
        }
    }

    // ================================================================ ondas

    private void spawnWave(Room room) {
        int alive = aliveCount();
        var cfg = plugin.getConfig();
        double countMult = modifiers.stream().mapToDouble(m -> m.mobCountMult).reduce(1, (a, b) -> a * b);
        double raw = (cfg.getInt("ondas.base", 3) + cfg.getInt("ondas.por-onda", 2) * (wave - 1)
                + cfg.getInt("ondas.por-jogador", 2) * (alive - 1)) * countMult * (1 + 0.2 * (floor - 1));
        int count = Math.max(1, Math.min(cfg.getInt("ondas.maximo", 24), (int) Math.round(raw)));

        for (int i = 0; i < count; i++) spawnMob(room, theme.mobs.pick(rnd), false);
        int waves = cfg.getInt("partida.ondas-por-sala", 3);
        boolean bossWave = room.index == 2 && wave == waves;
        if (bossWave) spawnMob(room, theme.elite, true);

        waveTotal = mobs.size();
        waveStartedAt = System.currentTimeMillis();
        forEachOnline(p -> {
            Msg.title(p, "<red>Onda " + wave + "/" + waves, bossWave ? theme.eliteName : "<gray>" + waveTotal + " inimigos");
            Msg.sound(p, bossWave ? "entity.wither.spawn" : "event.raid.horn", bossWave ? 0.5f : 0.7f, 1f);
        });
    }

    private void spawnMob(Room room, EntityType type, boolean elite) {
        Location loc = room.randomSpawn(world, rnd);
        world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.05);
        Entity e = world.spawnEntity(loc, type);
        if (!(e instanceof LivingEntity le)) {
            e.remove();
            return;
        }
        le.getPersistentDataContainer().set(plugin.mobTag(), PersistentDataType.STRING, id.toString());
        le.setRemoveWhenFarAway(false);

        double hpMult = modifiers.stream().mapToDouble(m -> m.mobHealthMult).reduce(1, (a, b) -> a * b)
                * (1 + 0.25 * (floor - 1)) * (1 + 0.15 * (aliveCount() - 1));
        if (elite) {
            hpMult *= 5;
            le.customName(Msg.mm(theme.eliteName));
            le.setCustomNameVisible(true);
            le.setGlowing(true);
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                eq.setHelmetDropChance(0f);
                eq.setChestplateDropChance(0f);
            }
        }
        Attr.scaleMaxHealth(le, hpMult);
        if (le instanceof PiglinAbstract pa) pa.setImmuneToZombification(true);
        if (le instanceof Slime slime) slime.setSize(elite ? 4 : 2);
        for (Modifier m : modifiers) m.onMobSpawn(le);
        if (le instanceof Mob mob) {
            Player target = nearestAlive(loc);
            if (target != null) mob.setTarget(target);
        }
        mobs.add(le.getUniqueId());
    }

    private void roomCleared(Room room) {
        setBlocks(room.exitDoor, Material.AIR.createBlockData());
        forEachOnline(p -> {
            Msg.title(p, "<green>Sala limpa!", "<gray>a passagem se abriu");
            Msg.sound(p, "block.iron_door.open", 1f, 0.8f);
        });
        // revive quem caiu
        Location back = room.entrance(world);
        for (UUID u : new ArrayList<>(dead)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) revive(p, back);
        }
        roomIndex++;
        if (currentRoom().treasure) fillChest();
        state = State.WAITING;
    }

    private void fillChest() {
        Block b = world.getBlockAt(layout.chest[0], layout.chest[1], layout.chest[2]);
        if (!(b.getState() instanceof Chest chest)) return;
        double loot = modifiers.stream().mapToDouble(m -> m.lootMult).reduce(1, (a, c) -> a * c);
        double gold = modifiers.stream().mapToDouble(m -> m.goldMult).reduce(1, (a, c) -> a * c);
        plugin.loot().fill(chest.getBlockInventory(), floor, Math.max(1, members.size()), loot, gold, theme, rnd);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 1, 0.5), 40, 0.4, 0.4, 0.4, 0.3);
    }

    // ================================================================ interações

    public void onDescend(Player p) {
        int max = plugin.getConfig().getInt("partida.andares-maximos", 3);
        if (state != State.TREASURE || floor >= max) return;
        forEachOnline(o -> Msg.send(o, "<yellow>" + p.getName() + "<gray> abriu a passagem para o andar " + (floor + 1) + "..."));
        // remove rótulos e itens soltos do andar atual
        clearEntities(layout, true);
        buildFloor(floor + 1);
    }

    public void onExitBlock(Player p) {
        leave(p, "<gray>Você saiu da fenda.");
    }

    public void onMobHitPlayer(Player p, LivingEntity mob, EntityDamageByEntityEvent e) {
        for (Modifier m : modifiers) m.onMobHitPlayer(p, mob, e);
    }

    public void onPlayerHitMob(Player p, LivingEntity mob, EntityDamageByEntityEvent e) {
        for (Modifier m : modifiers) m.onPlayerHitMob(p, mob, e);
    }

    public void onDeath(Player p) {
        dead.add(p.getUniqueId());
        forEachOnline(o -> Msg.send(o, "<red>" + p.getName() + " caiu! <gray>Volta quando a sala for limpa."));
        if (aliveCount() == 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> end("<red>Todo o grupo caiu. A fenda se fechou."), 40L);
        }
    }

    /** Local de renascimento de quem morreu (fica como espectador). */
    public Location respawnLocation() {
        return currentRoom().center(world);
    }

    public void onQuit(Player p) {
        UUID u = p.getUniqueId();
        if (!members.remove(u)) return;
        dead.remove(u);
        manager.unmap(u);
        p.hideBossBar(bar);
        modifiers.forEach(m -> m.cleanup(p));
        // o retorno fica salvo: ele é devolvido quando entrar de novo
        if (members.isEmpty()) end(null);
        else if (aliveCount() == 0) end("<red>Todo o grupo caiu. A fenda se fechou.");
    }

    /** Jogador saiu do mundo das fendas por outro meio (/spawn, /home...). */
    public void onLeftWorld(Player p) {
        UUID u = p.getUniqueId();
        if (!members.remove(u)) return;
        dead.remove(u);
        manager.unmap(u);
        p.hideBossBar(bar);
        modifiers.forEach(m -> m.cleanup(p));
        ReturnStore.Entry entry = plugin.returns().remove(u);
        if (p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(entry != null ? entry.gameMode() : GameMode.SURVIVAL);
        if (members.isEmpty()) end(null);
    }

    public void leave(Player p, String message) {
        UUID u = p.getUniqueId();
        if (!members.remove(u)) return;
        dead.remove(u);
        manager.unmap(u);
        p.hideBossBar(bar);
        modifiers.forEach(m -> m.cleanup(p));
        sendBack(p);
        if (message != null) Msg.send(p, message);
        if (members.isEmpty()) end(null);
    }

    void sendBack(Player p) {
        ReturnStore.Entry entry = plugin.returns().remove(p.getUniqueId());
        Location to = entry != null ? entry.location() : Bukkit.getWorlds().get(0).getSpawnLocation();
        p.setGameMode(entry != null ? entry.gameMode() : GameMode.SURVIVAL);
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setFallDistance(0);
        if (plugin.isEnabled()) p.teleportAsync(to);
        else p.teleport(to);
    }

    public void end(String message) {
        if (state == State.ENDED) return;
        state = State.ENDED;
        for (UUID u : new ArrayList<>(members)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) leave(p, message);
        }
        members.clear();
        manager.release(this);
    }

    // ================================================================ limpeza

    void cleanup(Runnable done) {
        int perTick = plugin.getConfig().getInt("partida.blocos-por-tick", 2500);
        List<Long> positions = new ArrayList<>();
        for (FloorLayout fl : builtFloors) {
            clearEntities(fl, true);
            positions.addAll(fl.blocks.keySet());
        }
        BlockPlacer.clear(plugin, world, positions, perTick, () -> {
            for (FloorLayout fl : builtFloors) {
                for (int cx = fl.minX >> 4; cx <= fl.maxX >> 4; cx++) {
                    for (int cz = fl.minZ >> 4; cz <= fl.maxZ >> 4; cz++) {
                        world.removePluginChunkTicket(cx, cz, plugin);
                    }
                }
            }
            done.run();
        });
    }

    private void clearEntities(FloorLayout fl, boolean includeLabels) {
        for (Entity e : world.getNearbyEntities(fl.box().expand(4))) {
            if (e instanceof Player) continue;
            if (!includeLabels && e instanceof TextDisplay) continue;
            e.remove();
        }
    }

    // ================================================================ util

    private Room currentRoom() {
        return layout.rooms.get(Math.min(roomIndex, layout.rooms.size() - 1));
    }

    private void revive(Player p, Location at) {
        boolean wasDead = dead.remove(p.getUniqueId());
        if (wasDead || p.getGameMode() == GameMode.SPECTATOR) {
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(Attr.maxHealth(p));
            p.setFoodLevel(20);
        }
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setFallDistance(0);
        p.teleport(at);
    }

    private void setBlocks(List<Long> positions, BlockData data) {
        for (long pos : positions) {
            world.getBlockAt(BlockPos.x(pos), BlockPos.y(pos), BlockPos.z(pos)).setBlockData(data, false);
        }
    }

    private boolean anyAliveInside(Room room) {
        for (UUID u : members) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && !dead.contains(u) && room.inside(p.getLocation())) return true;
        }
        return false;
    }

    private boolean isAlive(Player p) {
        return members.contains(p.getUniqueId()) && !dead.contains(p.getUniqueId()) && !p.isDead();
    }

    private int aliveCount() {
        int n = 0;
        for (UUID u : members) if (!dead.contains(u) && Bukkit.getPlayer(u) != null) n++;
        return Math.max(n, 0);
    }

    private Player nearestAlive(Location from) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID u : members) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || dead.contains(u) || !p.getWorld().equals(from.getWorld())) continue;
            double d = p.getLocation().distanceSquared(from);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private void forEachOnline(java.util.function.Consumer<Player> action) {
        for (UUID u : new ArrayList<>(members)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) action.accept(p);
        }
    }

    private void updateBar() {
        String mods = String.join(" ", modifiers.stream().map(Modifier::label).toList());
        int total = layout.rooms.size() - 1;
        String text;
        float progress = 1f;
        switch (state) {
            case FIGHTING -> {
                int waves = plugin.getConfig().getInt("partida.ondas-por-sala", 3);
                text = theme.color + "Andar " + floor + " <gray>• <white>Sala " + (roomIndex + 1) + "/" + total
                        + " <gray>• <white>Onda " + wave + "/" + waves + " <gray>• <red>" + mobs.size() + " inimigos <gray>| " + mods;
                progress = waveTotal == 0 ? 0f : Math.max(0f, Math.min(1f, mobs.size() / (float) waveTotal));
            }
            case TREASURE -> text = "<gold>Tesouro do andar " + floor + " <gray>| " + mods;
            case WAITING -> text = theme.color + "Andar " + floor + " <gray>• <green>Siga em frente <gray>| " + mods;
            default -> text = theme.color + "Andar " + floor + " <gray>| " + mods;
        }
        bar.name(Msg.mm(text));
        bar.progress(progress);
    }

    // ================================================================ getters

    public UUID id() { return id; }
    public int slot() { return slot; }
    public State state() { return state; }
    public FloorLayout layout() { return layout; }
    public boolean isMember(UUID u) { return members.contains(u); }
    public boolean isDead(UUID u) { return dead.contains(u); }
    public int floor() { return floor; }
    public Theme theme() { return theme; }
    public List<Modifier> modifiers() { return modifiers; }
    public int roomIndex() { return roomIndex; }
    public int wave() { return wave; }
    public int mobsLeft() { return mobs.size(); }
    public int memberCount() { return members.size(); }
}
