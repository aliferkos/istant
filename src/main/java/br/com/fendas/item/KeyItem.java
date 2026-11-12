package br.com.fendas.item;

import br.com.fendas.FendasPlugin;
import br.com.fendas.util.Msg;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** A "Pedra de Fenda": item com marca invisível (não dá para falsificar renomeando). */
public final class KeyItem {

    private final FendasPlugin plugin;

    public KeyItem(FendasPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(int amount) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("chave.material", "ECHO_SHARD"));
        if (mat == null || !mat.isItem()) mat = Material.ECHO_SHARD;
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.item("<gradient:#7fdbff:#b10dc9><bold>Pedra de Fenda</bold></gradient>"));
        meta.lore(List.of(
                Msg.item("<gray>Um fragmento que vibra com energia instável."),
                Msg.item(""),
                Msg.item("<dark_aqua>Use em um <aqua>Altar de Fenda</aqua> para abrir"),
                Msg.item("<dark_aqua>uma masmorra para você e seu grupo.")));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(plugin.keyItemTag(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean is(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.keyItemTag(), PersistentDataType.BYTE);
    }
}
