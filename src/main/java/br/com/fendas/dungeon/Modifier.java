package br.com.fendas.dungeon;

import br.com.fendas.util.Attr;
import br.com.fendas.util.Effects;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Maldições (deixam o andar mais difícil, mas aumentam o loot) e Bênçãos (ajudam o grupo).
 */
public enum Modifier {

    // ----------------------------------------------------------------- MALDIÇÕES
    INFERNO(true, "Inferno", "<red>", "Monstros incendeiam quem acertam", "Ouro em dobro",
            1.0, 1.0, 1.0, 2.0),
    ENXAME(true, "Enxame", "<gold>", "Quase o dobro de monstros, mais frágeis", "+50% de loot",
            1.8, 0.6, 1.5, 1.0),
    NEVASCA(true, "Nevasca", "<aqua>", "Golpes dos monstros congelam", "+50% de loot",
            1.0, 1.0, 1.5, 1.2),
    ESCURIDAO(true, "Escuridão", "<dark_gray>", "A luz pulsa e some", "+60% de loot",
            1.0, 1.0, 1.6, 1.0),
    FURIA(true, "Fúria", "<dark_red>", "Monstros mais rápidos e fortes", "Loot em dobro",
            1.0, 1.0, 2.0, 1.5),
    FRAGILIDADE(true, "Fragilidade", "<light_purple>", "Vocês recebem +30% de dano", "+70% de loot",
            1.0, 1.0, 1.7, 1.0),

    // ----------------------------------------------------------------- BÊNÇÃOS
    VAMPIRISMO(false, "Vampirismo", "<dark_green>", "Golpes recuperam vida", "",
            1.0, 1.0, 1.0, 1.0),
    FORTUNA(false, "Fortuna", "<yellow>", "Mais loot no baú", "",
            1.0, 1.0, 1.5, 1.5),
    VIGOR(false, "Vigor", "<green>", "Regeneração constante", "",
            1.0, 1.0, 1.0, 1.0),
    FORCA(false, "Força Ancestral", "<gold>", "Vocês causam mais dano", "",
            1.0, 1.0, 1.0, 1.0),
    PELE_DE_PEDRA(false, "Pele de Pedra", "<gray>", "Resistência a dano", "",
            1.0, 1.0, 1.0, 1.0);

    public final boolean curse;
    public final String displayName;
    public final String color;
    public final String effect;
    public final String reward;
    public final double mobCountMult, mobHealthMult, lootMult, goldMult;

    Modifier(boolean curse, String displayName, String color, String effect, String reward,
             double mobCountMult, double mobHealthMult, double lootMult, double goldMult) {
        this.curse = curse;
        this.displayName = displayName;
        this.color = color;
        this.effect = effect;
        this.reward = reward;
        this.mobCountMult = mobCountMult;
        this.mobHealthMult = mobHealthMult;
        this.lootMult = lootMult;
        this.goldMult = goldMult;
    }

    public String label() {
        return color + displayName + "</" + color.substring(1);
    }

    public static Modifier randomCurse(Random rnd) {
        List<Modifier> list = Arrays.stream(values()).filter(m -> m.curse).toList();
        return list.get(rnd.nextInt(list.size()));
    }

    public static Modifier randomBlessing(Random rnd) {
        List<Modifier> list = Arrays.stream(values()).filter(m -> !m.curse).toList();
        return list.get(rnd.nextInt(list.size()));
    }

    // ----------------------------------------------------------------- ganchos

    public void onMobSpawn(LivingEntity mob) {
        switch (this) {
            case INFERNO -> mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                    PotionEffect.INFINITE_DURATION, 0, false, false));
            case FURIA -> {
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, false));
            }
            default -> {
            }
        }
    }

    public void onMobHitPlayer(Player p, LivingEntity mob, EntityDamageByEntityEvent e) {
        switch (this) {
            case INFERNO -> {
                p.setFireTicks(Math.max(p.getFireTicks(), 80));
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 12, .3, .5, .3, .02);
            }
            case NEVASCA -> {
                p.setFreezeTicks(Math.min(p.getMaxFreezeTicks() + 40, p.getFreezeTicks() + 70));
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 15, .3, .5, .3, .02);
            }
            case FRAGILIDADE -> e.setDamage(e.getDamage() * 1.3);
            default -> {
            }
        }
    }

    public void onPlayerHitMob(Player p, LivingEntity mob, EntityDamageByEntityEvent e) {
        if (this == VAMPIRISMO) {
            double heal = Math.max(0.5, e.getFinalDamage() * 0.15);
            p.setHealth(Math.min(Attr.maxHealth(p), p.getHealth() + heal));
        }
    }

    /** Chamado a cada 10 ticks para cada jogador vivo no andar. */
    public void tickPlayer(Player p) {
        switch (this) {
            case ESCURIDAO -> Effects.ensure(p, PotionEffectType.DARKNESS, 0);
            case VIGOR -> Effects.ensure(p, PotionEffectType.REGENERATION, 0);
            case FORCA -> Effects.ensure(p, PotionEffectType.STRENGTH, 0);
            case PELE_DE_PEDRA -> Effects.ensure(p, PotionEffectType.RESISTANCE, 0);
            default -> {
            }
        }
    }

    /** Remove o que o modificador colocou no jogador. */
    public void cleanup(Player p) {
        switch (this) {
            case ESCURIDAO -> p.removePotionEffect(PotionEffectType.DARKNESS);
            case VIGOR -> p.removePotionEffect(PotionEffectType.REGENERATION);
            case FORCA -> p.removePotionEffect(PotionEffectType.STRENGTH);
            case PELE_DE_PEDRA -> p.removePotionEffect(PotionEffectType.RESISTANCE);
            default -> {
            }
        }
    }
}
