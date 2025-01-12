package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VaultsCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Gson gson = new Gson();

    public VaultsCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            int vaultsLimit = this.plugin.getVaultsLimit();
            int page = 0;
            boolean showOnlyOwned = false;
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("me")) {
                    showOnlyOwned = true;
                } else {
                    try {
                        page = Integer.parseInt(args[0]) - 1;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid page number.");
                        return false;
                    }
                }
            }

            this.openVault(player, vaultsLimit, page, showOnlyOwned);
            return true;
        } else {
            sender.sendMessage(this.plugin.getLangMessage("vaults.only_players"));
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = IntStream.rangeClosed(1, this.plugin.getVaultsLimit()).mapToObj(Integer::toString).collect(Collectors.toList());
            suggestions.add("me");
            return suggestions.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public void openVault(Player player, int vaultsLimit, int page, boolean showOnlyOwned) {
        int itemsPerPage = 45;
        int maxVaults = Math.min(vaultsLimit, plugin.getVaultsLimit()); // Respect the configured limit
        Inventory vault = Bukkit.createInventory(player, 54, ChatColor.GOLD + "Eldaria Vaults Page " + (page + 1));

        List<ItemStack> vaultItems = new ArrayList<>();
        for (int i = 0; i < maxVaults; i++) {
            String vaultNumber = String.valueOf(i + 1);
            File vaultFile = new File(this.plugin.getDataFolder(), "VaultsData/" + vaultNumber + ".json");
            if (vaultFile.exists()) {
                try (FileReader reader = new FileReader(vaultFile)) {
                    Map<String, Object> vaultData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                    String ownerUUID = (String) vaultData.get("owner");
                    if (!showOnlyOwned || ownerUUID.equals(player.getUniqueId().toString())) {
                        ItemStack barrel = new ItemStack(Material.BARREL);
                        ItemMeta meta = barrel.getItemMeta();
                        String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName();
                        meta.setDisplayName(ChatColor.YELLOW + "Vault #" + vaultNumber);
                        meta.setLore(Arrays.asList(
                                ChatColor.GRAY + this.plugin.getLangMessage("vaults.owner").replace("{owner}", ownerName)
                        ));
                        if (ownerUUID.equals(player.getUniqueId().toString())) {
                            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        }
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        barrel.setItemMeta(meta);
                        vaultItems.add(barrel);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!showOnlyOwned) {
                // Add vaults available for purchase
                ItemStack barrel = new ItemStack(Material.BARREL);
                ItemMeta meta = barrel.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Vault #" + vaultNumber);
                meta.setLore(Arrays.asList(ChatColor.GRAY + this.plugin.getLangMessage("vaults.price").replace("{price}", getPriceForVault(player))));
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                barrel.setItemMeta(meta);
                vaultItems.add(barrel);
            }
        }

        // Sort vaults by number
        vaultItems.sort(Comparator.comparingInt(item -> {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            String[] parts = displayName.split("#");
            return parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        }));

        // Add vaults to inventory
        for (int i = 0; i < itemsPerPage && i + page * itemsPerPage < vaultItems.size(); i++) {
            vault.setItem(i, vaultItems.get(i + page * itemsPerPage));
        }

        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack previousButton = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = previousButton.getItemMeta();
        previousMeta.setDisplayName(ChatColor.RED + "Previous");
        previousButton.setItemMeta(previousMeta);
        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.setDisplayName(ChatColor.GREEN + "Next");
        nextButton.setItemMeta(nextMeta);

        for (int i = 45; i < 54; i++) {
            vault.setItem(i, grayGlassPane);
        }

        if (page > 0) {
            vault.setItem(45, previousButton);
        }

        if ((page + 1) * itemsPerPage < vaultItems.size()) {
            vault.setItem(53, nextButton);
        }

        player.openInventory(vault);
    }

    private String getPriceForVault(Player player) {
        int ownedVaults = VaultsDataHandler.getOwnedVaults(player, plugin);
        for (Map<String, Object> priceConfig : plugin.getPrices()) {
            String[] range = ((String) priceConfig.get("range")).split("-");
            int min = Integer.parseInt(range[0]);
            int max = Integer.parseInt(range[1]);
            if (ownedVaults >= min && ownedVaults <= max) {
                return priceConfig.get("amount") + " " + priceConfig.get("item");
            }
        }
        Map<String, Object> defaultPriceConfig = plugin.getConfig().getConfigurationSection("vaults.default_price").getValues(false);
        return defaultPriceConfig.get("amount") + " " + defaultPriceConfig.get("item");
    }
}