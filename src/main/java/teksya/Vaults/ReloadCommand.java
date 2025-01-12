package teksya.Vaults;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vaults.reload") && !sender.isOp()) {
            String var5 = String.valueOf(ChatColor.RED);
            sender.sendMessage(var5 + this.plugin.getLangMessage("vaults.no_permission"));
        } else {
            this.plugin.reloadConfig();
            this.plugin.loadLangConfig();
            String var10001 = String.valueOf(ChatColor.GREEN);
            sender.sendMessage(var10001 + this.plugin.getLangMessage("vaults.config_reloaded"));
        }

        return true;
    }
}
