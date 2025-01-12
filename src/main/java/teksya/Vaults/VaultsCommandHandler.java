package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class VaultsCommandHandler {
    private static final Gson gson = new Gson();

    public static void handleAsyncPlayerChat(AsyncPlayerChatEvent event, Main plugin, Map<UUID, String> transferVaults) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();
        if (transferVaults.containsKey(playerUUID)) {
            event.setCancelled(true);
            String vaultNumber = transferVaults.remove(playerUUID);
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.vault_transfer_cancelled"));
            } else {
                transferVault(player, vaultNumber, message, plugin);
            }
        }
    }

    public static void transferVault(Player player, String vaultNumber, String newOwnerName, Main plugin) {
        OfflinePlayer newOwner = Bukkit.getOfflinePlayer(newOwnerName);
        if (newOwner != null && newOwner.hasPlayedBefore()) {
            File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
            if (vaultFile.exists()) {
                try (FileReader reader = new FileReader(vaultFile)) {
                    Map<String, Object> vaultData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                    vaultData.put("owner", newOwner.getUniqueId().toString());

                    try (FileWriter writer = new FileWriter(vaultFile)) {
                        gson.toJson(vaultData, writer);
                    }

                    player.sendMessage(ChatColor.GREEN + "Vault ownership transferred to " + newOwnerName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(ChatColor.RED + "Vault data not found.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Player not found.");
        }
    }
}