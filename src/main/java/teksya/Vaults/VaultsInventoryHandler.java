package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VaultsInventoryHandler implements Listener {

    private final Main plugin;

    public VaultsInventoryHandler(Main plugin) {
        this.plugin = plugin;
    }

    public static void handleInventoryClick(InventoryClickEvent event, Main plugin, Map<UUID, String> openVaults, Map<UUID, String> transferVaults, Map<UUID, String> deleteVaults) {
        Inventory inventory = event.getInventory();
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            String title = event.getView().getTitle();
            if (title.startsWith(String.valueOf(ChatColor.GOLD) + "Eldaria Vaults")) {
                event.setCancelled(true);
                int currentPage = getCurrentPage(title);
                if (event.getCurrentItem() != null) {
                    if (event.getCurrentItem().getType() == Material.ARROW) {
                        VaultsCommand vaultsCommand = new VaultsCommand(plugin);
                        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(String.valueOf(ChatColor.RED) + "Previous")) {
                            vaultsCommand.openVault(player, plugin.getVaultsLimit(), currentPage - 1, false);
                        } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(String.valueOf(ChatColor.GREEN) + "Next")) {
                            vaultsCommand.openVault(player, plugin.getVaultsLimit(), currentPage + 1, false);
                        }
                    } else if (event.getCurrentItem().getType() == Material.BARREL) {
                        ItemMeta meta = event.getCurrentItem().getItemMeta();
                        if (meta != null && meta.getLore() != null) {
                            if (meta.getLore().get(0).startsWith(String.valueOf(ChatColor.GRAY) + "Price:")) {
                                openConfirmationInventory(player, event.getCurrentItem(), plugin);
                            } else {
                                if (event.isRightClick()) {
                                    openManagementMenu(player, event.getCurrentItem(), plugin, transferVaults, deleteVaults);
                                } else if (event.isLeftClick()) {
                                    openPlayerVault(player, event.getCurrentItem(), plugin, openVaults);
                                }
                            }
                        }
                    }
                }
            } else if (title.equals(String.valueOf(ChatColor.RED) + "Confirm Purchase")) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
                        ItemStack vaultItem = event.getInventory().getItem(11);
                        if (vaultItem != null) {
                            String vaultNumber = ChatColor.stripColor(vaultItem.getItemMeta().getDisplayName()).split("#")[1];
                            File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
                            if (vaultFile.exists()) {
                                player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.vault_already_taken"));
                            } else {
                                int priceAmount = 0;
                                Material priceItem = Material.AIR;
                                int ownedVaults = VaultsDataHandler.getOwnedVaults(player, plugin);
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
                                if (player.getInventory().containsAtLeast(new ItemStack(priceItem), priceAmount)) {
                                    player.getInventory().removeItem(new ItemStack(priceItem, priceAmount));
                                    VaultsDataHandler.saveVaultData(player, vaultItem, plugin);
                                    player.sendMessage(ChatColor.GREEN + plugin.getLangMessage("vaults.vault_purchased"));
                                    player.closeInventory();
                                } else {
                                    player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.not_enough_resources"));
                                }
                            }
                        }
                    } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
                        player.closeInventory();
                    }
                }
            } else if (title.equals(String.valueOf(ChatColor.BLUE) + "Vault Management")) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    ItemStack vaultItem = event.getInventory().getItem(0);
                    if (vaultItem != null) {
                        String vaultNumber = ChatColor.stripColor(vaultItem.getItemMeta().getDisplayName()).split("#")[1];
                        if (event.getCurrentItem().getType() == Material.PAPER) {
                            transferVaults.put(player.getUniqueId(), vaultNumber);
                            player.closeInventory();
                            player.sendMessage(String.valueOf(ChatColor.YELLOW) + plugin.getLangMessage("vaults.enter_new_owner"));
                        } else if (event.getCurrentItem().getType() == Material.BARRIER) {
                            openDeleteConfirmation(player, vaultNumber, plugin, deleteVaults);
                        }
                    }
                }
            } else if (title.equals(String.valueOf(ChatColor.RED) + "Confirm Deletion")) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
                        String vaultNumber = deleteVaults.remove(player.getUniqueId());
                        VaultsDataHandler.deleteVault(player, vaultNumber, plugin);
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + plugin.getLangMessage("vaults.vault_deleted"));
                        player.closeInventory();
                    } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
                        deleteVaults.remove(player.getUniqueId());
                        player.sendMessage(String.valueOf(ChatColor.RED) + plugin.getLangMessage("vaults.vault_deletion_cancelled"));
                        player.closeInventory();
                    }
                }
            }
        }
    }

    public static void handleInventoryClose(InventoryCloseEvent event, Main plugin, Map<UUID, String> openVaults) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        Bukkit.getLogger().info("InventoryCloseEvent triggered for player: " + player.getName());
        Bukkit.getLogger().info("Closed inventory title: " + title);
        if (title.contains("Vault #")) {
            String vaultNumber = title.split("#")[1];
            Bukkit.getLogger().info("Closing vault #" + vaultNumber + " for player: " + player.getName());
            VaultsDataHandler.saveVaultContents(player, vaultNumber, event.getInventory(), plugin);
            openVaults.remove(player.getUniqueId());
        } else {
            Bukkit.getLogger().info("Inventory closed is not a vault.");
        }
    }

    private static void openConfirmationInventory(Player player, ItemStack vaultItem, Main plugin) {
        Inventory confirmation = Bukkit.createInventory(player, 27, String.valueOf(ChatColor.RED) + "Confirm Purchase");
        ItemStack confirmButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        confirmMeta.setDisplayName(String.valueOf(ChatColor.GREEN) + plugin.getLangMessage("vaults.confirm"));
        confirmButton.setItemMeta(confirmMeta);
        ItemStack cancelButton = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(String.valueOf(ChatColor.RED) + plugin.getLangMessage("vaults.cancel"));
        cancelButton.setItemMeta(cancelMeta);
        confirmation.setItem(11, vaultItem);
        confirmation.setItem(13, confirmButton);
        confirmation.setItem(15, cancelButton);
        player.openInventory(confirmation);
    }

    private static void openDeleteConfirmation(Player player, String vaultNumber, Main plugin, Map<UUID, String> deleteVaults) {
        Inventory confirmation = Bukkit.createInventory(player, 27, String.valueOf(ChatColor.RED) + "Confirm Deletion");
        ItemStack confirmButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        confirmMeta.setDisplayName(String.valueOf(ChatColor.GREEN) + plugin.getLangMessage("vaults.confirm"));
        confirmButton.setItemMeta(confirmMeta);
        ItemStack cancelButton = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(String.valueOf(ChatColor.RED) + plugin.getLangMessage("vaults.cancel"));
        cancelButton.setItemMeta(cancelMeta);
        confirmation.setItem(11, confirmButton);
        confirmation.setItem(15, cancelButton);
        deleteVaults.put(player.getUniqueId(), vaultNumber);
        player.openInventory(confirmation);
    }

    private static int getCurrentPage(String title) {
        String[] parts = title.split(" ");
        return Integer.parseInt(parts[parts.length - 1]) - 1;
    }

    private static void openPlayerVault(Player player, ItemStack vaultItem, Main plugin, Map<UUID, String> openVaults) {
        String displayName = ChatColor.stripColor(vaultItem.getItemMeta().getDisplayName());
        String[] parts = displayName.split("#");
        if (parts.length > 1) {
            String vaultNumber = parts[1].trim();
            File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
            if (vaultFile.exists()) {
                try (FileReader reader = new FileReader(vaultFile)) {
                    Map<String, Object> vaultData = new Gson().fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                    String ownerUUID = (String) vaultData.get("owner");
                    if (!ownerUUID.equals(player.getUniqueId().toString())) {
                        player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.not_owner"));
                        return;
                    }
                    Inventory vaultInventory = Bukkit.createInventory(player, 54, ChatColor.GOLD + "Vault #" + vaultNumber);

                    for (Map<String, Object> itemData : (List<Map<String, Object>>) vaultData.get("items")) {
                        String json = new Gson().toJson(itemData);
                        ItemStack item = ItemUtils.deserializeItem(json);
                        int position = ((Double) itemData.get("position")).intValue();
                        vaultInventory.setItem(position, item);
                    }

                    player.openInventory(vaultInventory);
                    openVaults.put(player.getUniqueId(), vaultNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.vault_data_not_found"));
            }
        } else {
            player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.invalid_vault_number"));
        }
    }

    private static void openManagementMenu(Player player, ItemStack vaultItem, Main plugin, Map<UUID, String> transferVaults, Map<UUID, String> deleteVaults) {
        String displayName = ChatColor.stripColor(vaultItem.getItemMeta().getDisplayName());
        String[] parts = displayName.split("#");
        if (parts.length > 1) {
            String vaultNumber = parts[1];
            File vaultFile = new File(plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
            if (vaultFile.exists()) {
                try (FileReader reader = new FileReader(vaultFile)) {
                    Map<String, Object> vaultData = new Gson().fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                    String ownerUUID = (String) vaultData.get("owner");
                    if (!ownerUUID.equals(player.getUniqueId().toString())) {
                        player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.not_owner"));
                        return;
                    }
                    Inventory managementMenu = Bukkit.createInventory(player, 27, String.valueOf(ChatColor.BLUE) + "Vault Management");
                    ItemStack transferButton = new ItemStack(Material.PAPER);
                    ItemMeta transferMeta = transferButton.getItemMeta();
                    transferMeta.setDisplayName(String.valueOf(ChatColor.YELLOW) + plugin.getLangMessage("vaults.transfer_ownership"));
                    transferButton.setItemMeta(transferMeta);
                    ItemStack deleteButton = new ItemStack(Material.BARRIER);
                    ItemMeta deleteMeta = deleteButton.getItemMeta();
                    deleteMeta.setDisplayName(String.valueOf(ChatColor.RED) + plugin.getLangMessage("vaults.delete_vault"));
                    deleteButton.setItemMeta(deleteMeta);
                    managementMenu.setItem(0, vaultItem);
                    managementMenu.setItem(13, transferButton);
                    managementMenu.setItem(15, deleteButton);
                    player.openInventory(managementMenu);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(String.valueOf(ChatColor.RED) + plugin.getLangMessage("vaults.vault_data_not_found"));
            }
        } else {
            player.sendMessage(ChatColor.RED + plugin.getLangMessage("vaults.invalid_vault_number"));
        }
    }
}