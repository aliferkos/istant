package br.com.fendas.listener;

import br.com.fendas.FendasPlugin;
import br.com.fendas.dungeon.DungeonInstance;
import br.com.fendas.dungeon.DungeonManager;
import br.com.fendas.gen.FloorLayout;
import br.com.fendas.util.Msg;
import br.com.fendas.world.ReturnStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public final class DungeonListener implements Listener {

    private final FendasPlugin plugin;
    private final DungeonManager manager;

    public DungeonListener(FendasPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.manager();
    }

    private boolean inDungeonWorld(Entity e) {
        return plugin.dungeonWorld().is(e.getWorld());
    }

    // ------------------------------------------------------------ altar e blocos especiais

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        Player p = e.getPlayer();
        if (block == null) return;

        // dentro da fenda: blocos de sair / descer
        DungeonInstance inst = manager.of(p);
        if (inst != null && inst.layout() != null) {
            if (FloorLayout.is(block, inst.layout().exit)) {
                e.setCancelled(true);
                inst.onExitBlock(p);
                return;
            }
            if (FloorLayout.is(block, inst.layout().descend)) {
                e.setCancelled(true);
                if (inst.state() == DungeonInstance.State.TREASURE) inst.onDescend(p);
                else Msg.send(p, "<red>Limpe as salas antes de descer.");
                return;
            }
        }

        if (!plugin.altars().isAltar(block)) return;
        e.setCancelled(true);
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (inst != null) {
            Msg.send(p, "<red>Você já está em uma fenda.");
            return;
        }
        if (!plugin.keyItem().is(hand)) {
            Msg.send(p, "Este altar reage a uma <light_purple>Pedra de Fenda</light_purple>. Segure uma e clique.");
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        manager.startFromAltar(p, block.getLocation().add(0.5, 0.5, 0.5));
    }

    // ------------------------------------------------------------ drops

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player) return;
        if (manager.isDungeonMob(dead)) {
            e.getDrops().clear();
            return;
        }
        Player killer = dead.getKiller();
        if (killer == null || !(dead instanceof Enemy) || inDungeonWorld(dead)) return;
        double chance = plugin.getConfig().getDouble("chave.chance-drop", 0.005);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            e.getDrops().add(plugin.keyItem().create(1));
            Msg.send(killer, "<light_purple>Uma Pedra de Fenda caiu do monstro!");
            Msg.sound(killer, "block.amethyst_block.resonate", 1f, 1.2f);
        }
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        if (manager.isDungeonMob(e.getEntity())) e.setCancelled(true);
    }

    // ------------------------------------------------------------ combate

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Entity shooter) damager = shooter;
        Entity victim = e.getEntity();

        boolean victimMob = manager.isDungeonMob(victim);
        boolean damagerMob = manager.isDungeonMob(damager);
        if (victimMob && damagerMob) { // mobs da fenda não brigam entre si
            e.setCancelled(true);
            return;
        }
        if (damager instanceof Player a && victim instanceof Player b) { // sem fogo amigo no grupo
            DungeonInstance ia = manager.of(a);
            if (ia != null && ia == manager.of(b)) e.setCancelled(true);
            return;
        }
        if (damagerMob && victim instanceof Player p && damager instanceof LivingEntity mob) {
            DungeonInstance inst = manager.of(p);
            if (inst != null) inst.onMobHitPlayer(p, mob, e);
        } else if (victimMob && damager instanceof Player p && victim instanceof LivingEntity mob) {
            DungeonInstance inst = manager.of(p);
            if (inst != null) inst.onPlayerHitMob(p, mob, e);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (manager.isDungeonMob(e.getEntity()) && e.getTarget() != null && !(e.getTarget() instanceof Player)) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ morte e retorno

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        DungeonInstance inst = manager.of(p);
        if (inst == null) return;
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
        inst.onDeath(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        DungeonInstance inst = manager.of(p);
        if (inst == null || !inst.isDead(p.getUniqueId())) return;
        e.setRespawnLocation(inst.respawnLocation());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (manager.of(p) == inst && inst.isDead(p.getUniqueId())) {
                p.setGameMode(GameMode.SPECTATOR);
                Msg.title(p, "<red>Você caiu", "<gray>volta quando a sala for limpa");
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        DungeonInstance inst = manager.of(e.getPlayer());
        if (inst != null) inst.onQuit(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (manager.of(p) != null) return;
        boolean hasReturn = plugin.returns().has(p.getUniqueId());
        if (!hasReturn && !inDungeonWorld(p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || manager.of(p) != null) return;
            ReturnStore.Entry entry = plugin.returns().remove(p.getUniqueId());
            p.setGameMode(entry != null ? entry.gameMode() : GameMode.SURVIVAL);
            p.setFallDistance(0);
            p.teleportAsync(entry != null ? entry.location() : Bukkit.getWorlds().get(0).getSpawnLocation());
            Msg.send(p, "Você foi trazido de volta da fenda.");
        }, 2L);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        if (!plugin.dungeonWorld().is(e.getFrom())) return;
        DungeonInstance inst = manager.of(e.getPlayer());
        if (inst != null) inst.onLeftWorld(e.getPlayer());
    }

    // ------------------------------------------------------------ proteção do mundo

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (inDungeonWorld(e.getPlayer()) && !e.getPlayer().hasPermission("fendas.admin")) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (inDungeonWorld(e.getPlayer()) && !e.getPlayer().hasPermission("fendas.admin")) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (inDungeonWorld(e.getEntity())) e.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && inDungeonWorld(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
