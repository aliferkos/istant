package br.com.fendas.command;

import br.com.fendas.FendasPlugin;
import br.com.fendas.dungeon.DungeonInstance;
import br.com.fendas.dungeon.Modifier;
import br.com.fendas.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class FendaCommand implements TabExecutor {

    private final FendasPlugin plugin;

    public FendaCommand(FendasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "ajuda" : args[0].toLowerCase();
        boolean admin = sender.hasPermission("fendas.admin");

        switch (sub) {
            case "sair" -> {
                if (!(sender instanceof Player p)) return true;
                DungeonInstance inst = plugin.manager().of(p);
                if (inst == null) Msg.send(p, "<red>Você não está em uma fenda.");
                else inst.leave(p, "<gray>Você abandonou a fenda.");
            }
            case "info" -> {
                if (!(sender instanceof Player p)) return true;
                DungeonInstance inst = plugin.manager().of(p);
                if (inst == null || inst.theme() == null) {
                    Msg.send(p, "<red>Você não está em uma fenda.");
                    return true;
                }
                Msg.send(p, "<white>Andar " + inst.floor() + " — " + inst.theme().color + inst.theme().name);
                Msg.send(p, "Sala " + Math.min(3, inst.roomIndex() + 1) + "/3 • Onda " + inst.wave()
                        + " • " + inst.mobsLeft() + " inimigos • " + inst.memberCount() + " jogador(es)");
                for (Modifier m : inst.modifiers()) {
                    Msg.send(p, (m.curse ? "<red>Maldição: " : "<green>Bênção: ") + m.label() + " <gray>— " + m.effect);
                }
            }
            case "chave" -> {
                if (!admin) return noPerm(sender);
                Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player p ? p : null);
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (target == null) {
                    Msg.send(sender, "<red>Jogador não encontrado.");
                    return true;
                }
                target.getInventory().addItem(plugin.keyItem().create(amount))
                        .values().forEach(left -> target.getWorld().dropItem(target.getLocation(), left));
                Msg.send(sender, "Entregue " + amount + "x Pedra de Fenda para <white>" + target.getName());
            }
            case "altar" -> {
                if (!admin) return noPerm(sender);
                if (!(sender instanceof Player p)) return true;
                String action = args.length >= 2 ? args[1].toLowerCase() : "";
                if (action.equals("listar")) {
                    List<String> list = plugin.altars().list();
                    Msg.send(p, list.isEmpty() ? "Nenhum altar." : "Altares: <white>" + String.join(" | ", list));
                    return true;
                }
                Block b = p.getTargetBlockExact(6);
                if (b == null) {
                    Msg.send(p, "<red>Olhe para um bloco (até 6 blocos de distância).");
                    return true;
                }
                if (action.equals("adicionar")) {
                    Msg.send(p, plugin.altars().add(b) ? "<green>Altar criado em " + b.getType().name().toLowerCase() + "." : "<red>Esse bloco já é um altar.");
                } else if (action.equals("remover")) {
                    Msg.send(p, plugin.altars().remove(b) ? "<green>Altar removido." : "<red>Esse bloco não é um altar.");
                } else {
                    Msg.send(p, "Uso: /fenda altar <adicionar|remover|listar>");
                }
            }
            case "iniciar" -> {
                if (!admin) return noPerm(sender);
                if (!(sender instanceof Player p)) return true;
                if (plugin.manager().of(p) != null) {
                    Msg.send(p, "<red>Você já está em uma fenda.");
                    return true;
                }
                plugin.manager().startFromAltar(p, p.getLocation());
            }
            case "recarregar" -> {
                if (!admin) return noPerm(sender);
                plugin.reloadConfig();
                plugin.altars().load();
                Msg.send(sender, "<green>Configuração recarregada.");
            }
            default -> {
                Msg.send(sender, "<white>/fenda sair <gray>— abandona a fenda");
                Msg.send(sender, "<white>/fenda info <gray>— andar, sala e modificadores");
                if (admin) {
                    Msg.send(sender, "<white>/fenda chave [jogador] [qtd] <gray>— dá Pedras de Fenda");
                    Msg.send(sender, "<white>/fenda altar <adicionar|remover|listar> <gray>— olhando para o bloco");
                    Msg.send(sender, "<white>/fenda iniciar <gray>— abre uma fenda sem chave (teste)");
                    Msg.send(sender, "<white>/fenda recarregar");
                }
            }
        }
        return true;
    }

    private boolean noPerm(CommandSender s) {
        Msg.send(s, "<red>Sem permissão.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission("fendas.admin");
        if (args.length == 1) {
            options.add("sair");
            options.add("info");
            if (admin) options.addAll(List.of("chave", "altar", "iniciar", "recarregar"));
        } else if (args.length == 2 && admin && args[0].equalsIgnoreCase("altar")) {
            options.addAll(List.of("adicionar", "remover", "listar"));
        } else if (args.length == 2 && admin && args[0].equalsIgnoreCase("chave")) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String last = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(last)).collect(Collectors.toList());
    }
}
