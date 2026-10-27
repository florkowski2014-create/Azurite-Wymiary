package pl.azurite.wymiar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class AzuriteWymiar extends JavaPlugin implements Listener, TabExecutor {

    private static final String BYPASS = "azurite.wymiar.bypass";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        var cmd = getCommand("azurite-wymiar");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    /** true = wymiar otwarty. Overworld zawsze otwarty. */
    private boolean isOpen(World world) {
        if (world == null) return true;
        return switch (world.getEnvironment()) {
            case NETHER -> getConfig().getBoolean("nether", true);
            case THE_END -> getConfig().getBoolean("end", true);
            default -> true;
        };
    }

    private void denyMessage(Player player) {
        player.sendActionBar(Component.text("Ten wymiar jest aktualnie wyłączony!", NamedTextColor.RED));
    }

    // ---------- Blokada wejścia ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent e) {
        Location to = e.getTo();
        if (to == null) return;
        if (!isOpen(to.getWorld()) && !e.getPlayer().hasPermission(BYPASS)) {
            e.setCancelled(true);
            denyMessage(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Location to = e.getTo();
        if (to == null) return;
        World from = e.getFrom().getWorld();
        World dest = to.getWorld();
        if (dest == null || dest.equals(from)) return;
        if (!isOpen(dest) && !e.getPlayer().hasPermission(BYPASS)) {
            e.setCancelled(true);
            denyMessage(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) {
        Location to = e.getTo();
        if (to == null) return;
        if (!isOpen(to.getWorld())) {
            e.setCancelled(true);
        }
    }

    // ---------- Komenda ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("azurite.wymiar.admin")) {
            sender.sendMessage(Component.text("Brak uprawnień.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(Component.text("Użycie: /azurite-wymiar <on|off> <end|nether>", NamedTextColor.RED));
            return true;
        }

        String state = args[0].toLowerCase(Locale.ROOT);
        String dim = args[1].toLowerCase(Locale.ROOT);

        boolean value;
        if (state.equals("on")) value = true;
        else if (state.equals("off")) value = false;
        else {
            sender.sendMessage(Component.text("Pierwszy argument: on lub off.", NamedTextColor.RED));
            return true;
        }

        String name;
        switch (dim) {
            case "nether" -> {
                getConfig().set("nether", value);
                name = "Nether";
            }
            case "end" -> {
                getConfig().set("end", value);
                name = "End";
            }
            default -> {
                sender.sendMessage(Component.text("Drugi argument: end lub nether.", NamedTextColor.RED));
                return true;
            }
        }
        saveConfig();

        sender.sendMessage(Component.text(name + " został " + (value ? "włączony" : "wyłączony") + ".",
                value ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("azurite.wymiar.admin")) return List.of();
        if (args.length == 1) return filter(Stream.of("on", "off"), args[0]);
        if (args.length == 2) return filter(Stream.of("end", "nether"), args[1]);
        return List.of();
    }

    private List<String> filter(Stream<String> options, String prefix) {
        return options.filter(s -> s.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
}
