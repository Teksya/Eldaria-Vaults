package teksya.Vaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class VaultsListener implements Listener {
    private final Main plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, String> openVaults = new HashMap<>();
    private final Map<UUID, String> transferVaults = new HashMap<>();
    private final Map<UUID, String> deleteVaults = new HashMap<>();

    public VaultsListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        VaultsInventoryHandler.handleInventoryClick(event, plugin, openVaults, transferVaults, deleteVaults);
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        VaultsCommandHandler.handleAsyncPlayerChat(event, plugin, transferVaults);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        VaultsInventoryHandler.handleInventoryClose(event, plugin, openVaults);
    }
}