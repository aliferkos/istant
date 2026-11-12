package br.com.fendas.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Busca atributos pelo nome, funcionando tanto em 1.21.1 ("generic.max_health")
 * quanto em 1.21.3+ ("max_health").
 */
public final class Attr {

    private Attr() {
    }

    public static Attribute get(String name) {
        Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
        if (a == null) {
            a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + name));
        }
        return a;
    }

    public static void scaleMaxHealth(LivingEntity e, double multiplier) {
        Attribute attr = get("max_health");
        if (attr == null) return;
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.setBaseValue(Math.max(1.0, inst.getBaseValue() * multiplier));
        e.setHealth(inst.getValue());
    }

    public static double maxHealth(LivingEntity e) {
        Attribute attr = get("max_health");
        if (attr == null) return 20.0;
        AttributeInstance inst = e.getAttribute(attr);
        return inst == null ? 20.0 : inst.getValue();
    }
}
