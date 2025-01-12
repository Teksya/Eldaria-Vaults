package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
    private static Main plugin;

    public ItemUtils() {
    }

    public static void initialize(Main mainPlugin) {
        plugin = mainPlugin;
    }

    public static String serializeItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("Type", item.getType().name());
        itemInfo.put("Amount", item.getAmount());
        if (meta != null) {
            if (meta.hasDisplayName()) {
                itemInfo.put("Name", meta.getDisplayName());
            }

            if (meta.hasLore()) {
                itemInfo.put("Lore", meta.getLore());
            }

            if (meta.hasEnchants()) {
                Map<String, Integer> enchants = new HashMap<>();
                meta.getEnchants().forEach((enchantment, level) -> enchants.put(enchantment.getKey().getKey(), level));
                itemInfo.put("Enchantments", enchants);
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (!container.getKeys().isEmpty()) {
                Map<String, String> nbtData = new HashMap<>();

                for (NamespacedKey key : container.getKeys()) {
                    nbtData.put(key.toString(), container.get(key, PersistentDataType.STRING));
                }

                itemInfo.put("NBT Data", nbtData);
            }
        }

        return gson.toJson(itemInfo);
    }

    public static ItemStack deserializeItem(String json) {
        Map<String, Object> itemInfo = gson.fromJson(json, HashMap.class);
        Material material = Material.valueOf((String) itemInfo.get("Type"));
        int amount = ((Double) itemInfo.get("Amount")).intValue();
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (itemInfo.containsKey("Name")) {
            meta.setDisplayName((String) itemInfo.get("Name"));
        }

        if (itemInfo.containsKey("Lore")) {
            meta.setLore((List<String>) itemInfo.get("Lore"));
        }

        if (itemInfo.containsKey("Enchantments")) {
            Map<String, Double> enchants = (Map<String, Double>) itemInfo.get("Enchantments");
            enchants.forEach((enchant, level) -> {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchant.toLowerCase()));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level.intValue(), true);
                }
            });
        }

        if (itemInfo.containsKey("NBT Data")) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Map<String, String> nbtData = (Map<String, String>) itemInfo.get("NBT Data");
            nbtData.forEach((key, value) -> {
                String[] keyParts = key.split(":");
                NamespacedKey namespacedKey = new NamespacedKey(keyParts[0], keyParts[1]);
                container.set(namespacedKey, PersistentDataType.STRING, value);
            });
        }

        item.setItemMeta(meta);
        return item;
    }
}