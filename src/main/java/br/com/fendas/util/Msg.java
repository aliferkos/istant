package br.com.fendas.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<dark_aqua>[<aqua>Fenda</aqua>]</dark_aqua> <gray>";

    private Msg() {
    }

    public static Component mm(String text) {
        return MM.deserialize(text);
    }

    /** Texto sem itálico (para nomes e lore de itens). */
    public static Component item(String text) {
        return MM.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender to, String text) {
        to.sendMessage(mm(PREFIX + text));
    }

    public static void title(Player p, String title, String subtitle) {
        p.showTitle(Title.title(mm(title), mm(subtitle),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1600), Duration.ofMillis(350))));
    }

    public static void sound(Player p, String key, float volume, float pitch) {
        p.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, volume, pitch));
    }
}
