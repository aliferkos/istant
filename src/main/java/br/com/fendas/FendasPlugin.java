package br.com.fendas;

import br.com.fendas.command.FendaCommand;
import br.com.fendas.dungeon.DungeonManager;
import br.com.fendas.item.KeyItem;
import br.com.fendas.item.LootGenerator;
import br.com.fendas.listener.DungeonListener;
import br.com.fendas.world.AltarManager;
import br.com.fendas.world.DungeonWorld;
import br.com.fendas.world.ReturnStore;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FendasPlugin extends JavaPlugin {

    private NamespacedKey keyItemTag, mobTag, lootTag;
    private DungeonWorld dungeonWorld;
    private ReturnStore returns;
    private AltarManager altars;
    private KeyItem keyItem;
    private LootGenerator loot;
    private DungeonManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyItemTag = new NamespacedKey(this, "chave");
        mobTag = new NamespacedKey(this, "instancia");
        lootTag = new NamespacedKey(this, "loot");

        dungeonWorld = new DungeonWorld(this);
        if (!dungeonWorld.prepare()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        returns = new ReturnStore(this);
        altars = new AltarManager(this);
        keyItem = new KeyItem(this);
        loot = new LootGenerator(this);
        manager = new DungeonManager(this);

        Bukkit.getPluginManager().registerEvents(new DungeonListener(this), this);
        FendaCommand command = new FendaCommand(this);
        PluginCommand pc = getCommand("fenda");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }
        Bukkit.getScheduler().runTaskTimer(this, altars::tickParticles, 40L, 20L);
        getLogger().info("Fendas pronto. Mundo: " + dungeonWorld.world().getName());
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        if (returns != null) returns.save();
    }

    public NamespacedKey keyItemTag() { return keyItemTag; }
    public NamespacedKey mobTag() { return mobTag; }
    public NamespacedKey lootTag() { return lootTag; }
    public DungeonWorld dungeonWorld() { return dungeonWorld; }
    public ReturnStore returns() { return returns; }
    public AltarManager altars() { return altars; }
    public KeyItem keyItem() { return keyItem; }
    public LootGenerator loot() { return loot; }
    public DungeonManager manager() { return manager; }
}
