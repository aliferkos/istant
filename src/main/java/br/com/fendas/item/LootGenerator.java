package br.com.fendas.item;

import br.com.fendas.FendasPlugin;
import br.com.fendas.gen.Theme;
import br.com.fendas.util.Attr;
import br.com.fendas.util.Msg;
import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Gera o conteúdo do baú do tesouro, incluindo itens de RPG com raridade. */
public final class LootGenerator {

    public enum Rarity {
        COMUM("Comum", "<white>", 0),
        RARO("Raro", "<blue>", 1),
        EPICO("Épico", "<dark_purple>", 2),
        LENDARIO("Lendário", "<gold>", 3);

        final String name, color;
        final int tier;

        Rarity(String name, String color, int tier) {
            this.name = name;
            this.color = color;
            this.tier = tier;
        }
    }

    private enum Kind { SWORD, AXE, BOW, CROSSBOW, HELMET, CHEST, LEGS, BOOTS }

    private final FendasPlugin plugin;

    public LootGenerator(FendasPlugin plugin) {
        this.plugin = plugin;
    }

    public void fill(Inventory inv, int floor, int players, double lootMult, double goldMult, Theme theme, Random rnd) {
        List<ItemStack> items = new ArrayList<>();
        double groupScale = 1 + 0.5 * (players - 1);

        int gold = (int) Math.round((4 + floor * 3 + rnd.nextInt(5)) * goldMult * groupScale);
        addSplit(items, Material.GOLD_INGOT, gold);
        int emeralds = (int) Math.round((1 + rnd.nextInt(3)) * floor * lootMult);
        addSplit(items, Material.EMERALD, emeralds);
        if (rnd.nextDouble() < 0.25 * floor * lootMult) items.add(new ItemStack(Material.DIAMOND, 1 + rnd.nextInt(2)));
        if (rnd.nextDouble() < 0.35 * lootMult) items.add(new ItemStack(Material.GOLDEN_APPLE, 1 + rnd.nextInt(2)));
        if (rnd.nextDouble() < 0.4) items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 4 + rnd.nextInt(8)));
        if (rnd.nextDouble() < plugin.getConfig().getDouble("chave.chance-no-bau", 0.15) * lootMult) {
            items.add(plugin.keyItem().create(1));
        }

        int rpg = 1 + (players > 2 ? 1 : 0) + floor / 2 + (lootMult >= 1.5 ? 1 : 0);
        for (int i = 0; i < Math.min(5, rpg); i++) items.add(rpgItem(floor, lootMult, theme, rnd));

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, rnd);
        inv.clear();
        for (int i = 0; i < items.size() && i < slots.size(); i++) inv.setItem(slots.get(i), items.get(i));
    }

    private void addSplit(List<ItemStack> items, Material mat, int amount) {
        while (amount > 0) {
            int n = Math.min(amount, 16);
            items.add(new ItemStack(mat, n));
            amount -= n;
        }
    }

    private Rarity rollRarity(int floor, double lootMult, Random rnd) {
        double common = Math.max(10, 50 - floor * 8);
        double rare = 30 * lootMult;
        double epic = (12 + floor * 3) * lootMult;
        double legendary = (3 + floor * 2) * lootMult;
        double roll = rnd.nextDouble() * (common + rare + epic + legendary);
        if ((roll -= legendary) < 0) return Rarity.LENDARIO;
        if ((roll -= epic) < 0) return Rarity.EPICO;
        if (roll - rare < 0) return Rarity.RARO;
        return Rarity.COMUM;
    }

    public ItemStack rpgItem(int floor, double lootMult, Theme theme, Random rnd) {
        Rarity rarity = rollRarity(floor, lootMult, rnd);
        Kind kind = Kind.values()[rnd.nextInt(Kind.values().length)];
        boolean diamond = rarity.tier >= 2 || (rarity.tier == 1 && rnd.nextBoolean());
        boolean netherite = rarity == Rarity.LENDARIO && rnd.nextDouble() < 0.35;

        Material mat = switch (kind) {
            case SWORD -> netherite ? Material.NETHERITE_SWORD : diamond ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
            case AXE -> netherite ? Material.NETHERITE_AXE : diamond ? Material.DIAMOND_AXE : Material.IRON_AXE;
            case BOW -> Material.BOW;
            case CROSSBOW -> Material.CROSSBOW;
            case HELMET -> netherite ? Material.NETHERITE_HELMET : diamond ? Material.DIAMOND_HELMET : Material.IRON_HELMET;
            case CHEST -> netherite ? Material.NETHERITE_CHESTPLATE : diamond ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE;
            case LEGS -> netherite ? Material.NETHERITE_LEGGINGS : diamond ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS;
            case BOOTS -> netherite ? Material.NETHERITE_BOOTS : diamond ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS;
        };
        String noun = pick(rnd, switch (kind) {
            case SWORD -> List.of("Lâmina", "Espada", "Gládio", "Presa");
            case AXE -> List.of("Machado", "Cutelo", "Talhador");
            case BOW -> List.of("Arco", "Arco Longo");
            case CROSSBOW -> List.of("Besta", "Balestra");
            case HELMET -> List.of("Elmo", "Capuz", "Coroa");
            case CHEST -> List.of("Couraça", "Peitoral", "Armadura");
            case LEGS -> List.of("Grevas", "Perneiras");
            case BOOTS -> List.of("Botas", "Passos");
        });
        String name = noun + " " + pick(rnd, theme.lootSuffixes);

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.item(rarity.color + name));
        int tier = rarity.tier;
        int scale = tier + (floor - 1) / 2;
        List<String> stats = new ArrayList<>();

        switch (kind) {
            case SWORD, AXE -> {
                ench(meta, "sharpness", Math.min(7, 1 + scale));
                if (theme == Theme.INFERNO && tier >= 1) ench(meta, "fire_aspect", Math.min(2, tier));
                if (tier >= 2) ench(meta, "looting", Math.min(3, tier));
                if (tier >= 1) {
                    double bonus = 0.5 * tier + 0.25 * floor;
                    if (attr(meta, mat, "attack_damage", bonus, EquipmentSlotGroup.MAINHAND)) {
                        stats.add("<red>+" + fmt(bonus) + " de dano extra");
                    }
                }
            }
            case BOW -> {
                ench(meta, "power", Math.min(6, 1 + scale));
                if (tier >= 1) ench(meta, "punch", 1);
                if (tier >= 2 || theme == Theme.INFERNO) ench(meta, "flame", 1);
                if (tier >= 3) ench(meta, "infinity", 1);
            }
            case CROSSBOW -> {
                ench(meta, "quick_charge", Math.min(3, 1 + tier));
                if (tier >= 1) ench(meta, "piercing", Math.min(4, tier + 1));
                if (tier >= 2) ench(meta, "multishot", 1);
            }
            default -> { // armaduras
                ench(meta, "protection", Math.min(5, 1 + scale));
                EquipmentSlot slot = mat.getEquipmentSlot();
                EquipmentSlotGroup group = slot.getGroup();
                if (tier >= 1) {
                    double hp = 2.0 * tier;
                    if (attr(meta, mat, "max_health", hp, group)) stats.add("<red>+" + (int) (hp / 2) + " ❤");
                }
                if (tier >= 2) {
                    if (attr(meta, mat, "movement_speed", 0.005 * tier, group)) {
                        stats.add("<aqua>+" + (5 * tier) + "% de velocidade");
                    }
                }
                if (kind == Kind.BOOTS && theme == Theme.GELO) ench(meta, "frost_walker", 1);
            }
        }
        ench(meta, "unbreaking", Math.min(5, 1 + tier));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(Msg.item(rarity.color + "<bold>" + rarity.name.toUpperCase()));
        lore.add(Msg.item("<dark_gray>Encontrado no andar " + floor + " • " + theme.name));
        if (!stats.isEmpty()) {
            lore.add(Msg.item(""));
            for (String s : stats) lore.add(Msg.item(s));
        }
        meta.lore(lore);
        if (tier >= 3) meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(plugin.lootTag(), PersistentDataType.STRING, rarity.name());
        item.setItemMeta(meta);
        return item;
    }

    private static void ench(ItemMeta meta, String key, int level) {
        Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (e != null && level > 0) meta.addEnchant(e, level, true);
    }

    /** Adiciona um atributo sem apagar os atributos padrão do item (dano base, armadura...). */
    private boolean attr(ItemMeta meta, Material mat, String name, double amount, EquipmentSlotGroup group) {
        Attribute attribute = Attr.get(name);
        if (attribute == null) return false;
        if (!meta.hasAttributeModifiers()) {
            try {
                Multimap<Attribute, AttributeModifier> defaults = mat.getDefaultAttributeModifiers(mat.getEquipmentSlot());
                meta.setAttributeModifiers(defaults);
            } catch (Throwable t) {
                // versão sem esse método: não adiciona o bônus para não apagar o dano/armadura base do item
                return false;
            }
        }
        try {
            NamespacedKey key = new NamespacedKey(plugin, "loot_" + name + "_" + UUID.randomUUID().toString().substring(0, 8));
            meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER, group));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v).replace('.', ',');
    }

    private static <T> T pick(Random rnd, List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }
}
