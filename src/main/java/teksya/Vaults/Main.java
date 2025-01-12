package teksya.Vaults;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private int vaultsLimit;
    private int limitPerPlayer;
    private List<Map<String, Object>> prices;
    private FileConfiguration langConfig;

    public Main() {
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.vaultsLimit = this.getConfig().getInt("vaults.limit");
        this.limitPerPlayer = this.getConfig().getInt("vaults.limit_per_player");
        this.prices = (List<Map<String, Object>>) (List<?>) this.getConfig().getList("vaults.prices");
        this.loadLangConfig();
        VaultsCommand vaultsCommand = new VaultsCommand(this);
        this.getCommand("vaults").setExecutor(vaultsCommand);
        this.getCommand("vaults").setTabCompleter(vaultsCommand);
        ReloadCommand reloadCommand = new ReloadCommand(this);
        this.getCommand("vaults-reload").setExecutor(reloadCommand);
        this.getServer().getPluginManager().registerEvents(new VaultsListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    public int getVaultsLimit() {
        return this.vaultsLimit;
    }

    public int getLimitPerPlayer() {
        return this.limitPerPlayer;
    }

    public List<Map<String, Object>> getPrices() {
        return this.prices;
    }

    public String getLangMessage(String key) {
        return this.langConfig.getString(key, "Message not found: " + key);
    }

    public void loadLangConfig() {
        File langFile = new File(this.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            this.saveResource("lang.yml", false);
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
}