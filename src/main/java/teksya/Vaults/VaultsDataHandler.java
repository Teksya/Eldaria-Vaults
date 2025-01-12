package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VaultsDataHandler {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static int getOwnedVaults(Player player, Main plugin) {
        File vaultsDataFolder = new File(plugin.getDataFolder(), "VaultsData");
        File[] vaultFiles = vaultsDataFolder.listFiles();
        int ownedVaults = 0;
        if (vaultFiles != null) {
            for (File vaultFile : vaultFiles) {
                try (FileReader reader = new FileReader(vaultFile)) {
                    Map<String, Object> vaultData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                    String ownerUUID = (String) vaultData.get("owner");
                    if (ownerUUID.equals(player.getUniqueId().toString())) {
                        ownedVaults++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ownedVaults;
    }

    public static boolean hasReachedVaultLimit(Player player, Main plugin) {
        int ownedVaults = getOwnedVaults(player, plugin);
        int vaultLimit = plugin.getLimitPerPlayer();
        return ownedVaults >= vaultLimit;
    }

    public static void saveVaultContents(Player player, String vaultNumber, Inventory inventory, Main plugin) {
        File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
        Map<String, Object> vaultData = new HashMap<>();
        vaultData.put("owner", player.getUniqueId().toString());
        vaultData.put("name", cleanColorCodes(player.getOpenInventory().getTitle()));

        try (FileReader reader = new FileReader(vaultFile)) {
            Map<String, Object> existingData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
            if (existingData.containsKey("priceAmount")) {
                vaultData.put("priceAmount", existingData.get("priceAmount"));
            }
            if (existingData.containsKey("priceItem")) {
                vaultData.put("priceItem", existingData.get("priceItem"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                String json = ItemUtils.serializeItem(item);
                Map<String, Object> itemData = gson.fromJson(json, new HashMap<>().getClass());
                itemData.put("position", i);
                items.add(itemData);
            }
        }
        vaultData.put("items", items);

        try (FileWriter writer = new FileWriter(vaultFile)) {
            gson.toJson(vaultData, writer);
            plugin.getLogger().info("Vault contents saved for vault #" + vaultNumber + " owned by " + player.getName());
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to save vault contents for vault #" + vaultNumber + " owned by " + player.getName());
        }
    }

    public static void saveVaultData(Player player, ItemStack vaultItem, Main plugin) {
        if (hasReachedVaultLimit(player, plugin)) {
            player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.limit_reached"));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        String vaultNumber = cleanColorCodes(vaultItem.getItemMeta().getDisplayName()).split("#")[1];
        File vaultsDataFolder = new File(plugin.getDataFolder(), "VaultsData");
        if (!vaultsDataFolder.exists()) {
            vaultsDataFolder.mkdirs();
        }

        File vaultFile = new File(vaultsDataFolder, vaultNumber + ".json");
        int ownedVaults = getOwnedVaults(player, plugin);
        int priceAmount = 0;
        Material priceItem = Material.AIR;

        for (Map<String, Object> priceConfig : plugin.getPrices()) {
            String[] range = ((String) priceConfig.get("range")).split("-");
            int min = Integer.parseInt(range[0]);
            int max = Integer.parseInt(range[1]);
            if (ownedVaults >= min && ownedVaults <= max) {
                priceAmount = (Integer) priceConfig.get("amount");
                priceItem = Material.valueOf((String) priceConfig.get("item"));
                break;
            }
        }

        Map<String, Object> vaultData = new HashMap<>();
        vaultData.put("owner", playerUUID.toString());
        vaultData.put("name", cleanColorCodes(vaultItem.getItemMeta().getDisplayName()));
        vaultData.put("priceAmount", priceAmount);
        vaultData.put("priceItem", priceItem.name());
        vaultData.put("items", new ArrayList<>());

        try (FileWriter writer = new FileWriter(vaultFile)) {
            gson.toJson(vaultData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteVault(Player player, String vaultNumber, Main plugin) {
        File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
        if (vaultFile.exists()) {
            try (FileReader reader = new FileReader(vaultFile)) {
                Map<String, Object> vaultData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());

                for (Map<String, Object> itemData : (List<Map<String, Object>>) vaultData.get("items")) {
                    String json = gson.toJson(itemData);
                    ItemStack item = ItemUtils.deserializeItem(json);
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }

                if (vaultData.containsKey("priceAmount") && vaultData.containsKey("priceItem")) {
                    int priceAmount = ((Double) vaultData.get("priceAmount")).intValue();
                    Material priceItem = Material.valueOf((String) vaultData.get("priceItem"));
                    player.getInventory().addItem(new ItemStack(priceItem, priceAmount));
                    player.sendMessage(ChatColor.GREEN + plugin.getLangMessage("vaults.refunded")
                            .replace("{price_amount}", String.valueOf(priceAmount))
                            .replace("{price_item}", priceItem.name()));
                } else {
                    player.sendMessage(ChatColor.RED + "Price information not found in vault data.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (vaultFile.delete()) {
                player.sendMessage(ChatColor.GREEN + plugin.getLangMessage("vaults.vault_deleted"));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.vault_deletion_failed"));
            }
        } else {
            player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.vault_data_not_found"));
        }
    }

    private static String cleanColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
}