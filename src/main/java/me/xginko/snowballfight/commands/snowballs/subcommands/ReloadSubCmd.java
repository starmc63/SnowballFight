package me.xginko.snowballfight.commands.snowballs.subcommands;

import me.xginko.snowballfight.SnowballFight;
import me.xginko.snowballfight.commands.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCmd extends SubCommand {

    @Override
    public String getLabel() {
        return "reload";
    }

    @Override
    public TextComponent getDescription() {
        return Component.text("Reload the plugin configuration.").color(NamedTextColor.GRAY);
    }

    @Override
    public TextComponent getSyntax() {
        return Component.text("/snowballs reload").color(NamedTextColor.WHITE);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snowballfight.cmd.reload")) return;
        sender.sendMessage(Component.text("Reloading SnowballFight...").color(NamedTextColor.WHITE));
        SnowballFight plugin = SnowballFight.getInstance();
        plugin.getServer().getAsyncScheduler().runNow(plugin, reload -> {
            plugin.reloadConfiguration();
            sender.sendMessage(Component.text("Reload complete.").color(NamedTextColor.GREEN));
        });
    }
}