package br.com.fendas.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class Effects {

    private Effects() {
    }

    /** Mantém um efeito ativo sem reiniciá-lo a todo momento. */
    public static void ensure(Player p, PotionEffectType type, int amplifier) {
        PotionEffect cur = p.getPotionEffect(type);
        if (cur != null && cur.getAmplifier() >= amplifier
                && (cur.isInfinite() || cur.getDuration() >= 40)) {
            return;
        }
        p.addPotionEffect(new PotionEffect(type, 200, amplifier, true, false, true));
    }
}
