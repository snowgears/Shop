package com.snowgears.shop;

import com.snowgears.shop.display.DisplayListener;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.CommandHandler;
import com.snowgears.shop.handler.EnderChestHandler;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.listener.ArmorStandListener;
import com.snowgears.shop.listener.ClearLaggListener;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.listener.TransactionListener;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.Metrics;
import com.snowgears.shop.util.PriceUtil;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

@SuppressWarnings({"unused"})
public class Shop extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft");
    private static Shop plugin;

    private ShopListener shopListener;
    private DisplayListener displayListener;
    private TransactionListener transactionListener;
    private MiscListener miscListener;
    private CreativeSelectionListener creativeSelectionListener;
    private ClearLaggListener clearLaggListener;
    private ArmorStandListener armorStandListener;
    private ShopGUIListener guiListener;

    private ShopHandler shopHandler;
    private CommandHandler commandHandler;
    private ShopGuiHandler guiHandler;
    private EnderChestHandler enderChestHandler;
    private ShopMessage shopMessage;
    private ItemNameUtil itemNameUtil;
    private PriceUtil priceUtil;
    private CreationCheck creationCheck;


    private boolean usePerms;
    private boolean enableMetrics;
    private boolean enableGUI;
    private boolean useVault;
    private boolean useTowny;
    private boolean hookWorldGuard;
    private String commandAlias;
    private DisplayType displayType;
    private boolean checkItemDurability;
    private boolean allowCreativeSelection;
    private boolean playSounds;
    private boolean playEffects;
    private ItemStack gambleDisplayItem;
    private ItemStack itemCurrency = null;
    private String itemCurrencyName = "";
    private String vaultCurrencySymbol = "";
    private String currencyFormat = "";
    private Economy econ = null;
    private boolean useEnderchests;
    private double creationCost;
    private double destructionCost;
    private double taxPercent;
    private ArrayList<String> worldBlackList;

    private YamlConfiguration config;

    public static Shop getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        File bwconfigFile = new File(getDataFolder(), "itemlist.yml");
        if (!bwconfigFile.exists()) {
        	bwconfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("itemlist.yml"), bwconfigFile);
        }
        boolean wh=false;
        if(config.get("WhitelistItems")==null)
        	wh=false;
        else if(config.getString("WhitelistItems").equalsIgnoreCase("true"))
        	wh=true;
        	
        creationCheck=new CreationCheck(YamlConfiguration.loadConfiguration(bwconfigFile),wh);        

        File chatConfigFile = new File(getDataFolder(), "chatConfig.yml");
        if (!chatConfigFile.exists()) {
            chatConfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("chatConfig.yml"), chatConfigFile);
        }

        File signConfigFile = new File(getDataFolder(), "signConfig.yml");
        if (!signConfigFile.exists()) {
            signConfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("signConfig.yml"), signConfigFile);
        }

        //TODO
//        File pricesFile = new File(getDataFolder(), "prices.tsv");
//        if (!pricesFile.exists()) {
//            pricesFile.getParentFile().mkdirs();
//            UtilMethods.copy(getResource("prices.tsv"), pricesFile);
//        }

        shopListener = new ShopListener(this);
        transactionListener = new TransactionListener(this);
        miscListener = new MiscListener(this);
        creativeSelectionListener = new CreativeSelectionListener(this);
        displayListener = new DisplayListener(this);
        armorStandListener = new ArmorStandListener(this);
        guiListener = new ShopGUIListener(this);

        if (getServer().getPluginManager().getPlugin("ClearLag") != null) {
            clearLaggListener = new ClearLaggListener(this);
            getServer().getPluginManager().registerEvents(clearLaggListener, this);
        }

        try {
            displayType = DisplayType.valueOf(config.getString("displayType"));
        } catch (Exception e) {
            displayType = DisplayType.ITEM;
        }

        shopMessage = new ShopMessage(this);
        itemNameUtil = new ItemNameUtil();
        priceUtil = new PriceUtil();

        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                getServer().getConsoleSender().sendMessage("[Shop]" + ChatColor.RED + " Data folder could not be created.");
            }
        }

        usePerms = config.getBoolean("usePermissions");
        enableMetrics = config.getBoolean("enableMetrics");
        enableGUI = config.getBoolean("enableGUI");
        hookWorldGuard = config.getBoolean("hookWorldGuard");
        commandAlias = config.getString("commandAlias");
        checkItemDurability = config.getBoolean("checkItemDurability");
        allowCreativeSelection = config.getBoolean("allowCreativeSelection");
        playSounds = config.getBoolean("playSounds");
        playEffects = config.getBoolean("playEffects");
        useVault = config.getBoolean("useVault");
        useTowny = config.getBoolean("useTowny");
        //TODO
//        taxPercent = config.getDouble("taxPercent");

//        String itemCurrencyIDString = config.getString("itemCurrencyID");
//        int itemCurrencyId;
//        int itemCurrencyData = 0;
//        if (itemCurrencyIDString.contains(";")) {
//            itemCurrencyId = Integer.parseInt(itemCurrencyIDString.substring(0, itemCurrencyIDString.indexOf(";")));
//            itemCurrencyData = Integer.parseInt(itemCurrencyIDString.substring(itemCurrencyIDString.indexOf(";") + 1, itemCurrencyIDString.length()));
//        } else {
//            itemCurrencyId = Integer.parseInt(itemCurrencyIDString.substring(0, itemCurrencyIDString.length()));
//        }
//
//        itemCurrency = new ItemStack(itemCurrencyId);
//        itemCurrency.setData(new MaterialData(itemCurrencyId, (byte) itemCurrencyData));

        if (enableMetrics) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                // Failed to submit the stats
            }
        }

        //Loading the itemCurrency from a file makes it easier to allow servers to use detailed itemstacks as the server's economy item
        File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
        if (itemCurrencyFile.exists()) {
            YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
            itemCurrency = currencyConfig.getItemStack("item");
            if (itemCurrency != null) {
                itemCurrency.setAmount(1);
            }
        } else {
            try {
                itemCurrency = new ItemStack(Material.EMERALD);
                itemCurrencyFile.createNewFile();

                YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
                currencyConfig.set("item", itemCurrency);
                currencyConfig.save(itemCurrencyFile);
            } catch (Exception ignore) {
            }
        }

        //load the gamble display item from it's file
        File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
        if (!gambleDisplayFile.exists()) {
            gambleDisplayFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("GAMBLE_DISPLAY.yml"), gambleDisplayFile);
        }
        YamlConfiguration gambleItemConfig = YamlConfiguration.loadConfiguration(gambleDisplayFile);
        gambleDisplayItem = gambleItemConfig.getItemStack("GAMBLE_DISPLAY");

        itemCurrencyName = config.getString("itemCurrencyName");
        vaultCurrencySymbol = config.getString("vaultCurrencyName");
        currencyFormat = config.getString("currencyFormat");

        useEnderchests = config.getBoolean("enableEnderChests");

        creationCost = config.getDouble("creationCost");
        destructionCost = config.getDouble("destructionCost");

        worldBlackList = new ArrayList<>();
        worldBlackList.addAll(config.getConfigurationSection("worldBlacklist").getKeys(true));

        if (useVault) {
            if (!setupEconomy()) {
                log.severe("[Shop] PLUGIN DISABLED DUE TO NO VAULT DEPENDENCY FOUND ON SERVER!");
                log.info("[Shop] If you do not wish to use Vault with Shop, make sure to set 'useVault' in the config file to false.");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            } else {
                log.info("[Shop] Vault dependency found. Using the Vault economy (" + vaultCurrencySymbol + ") for currency on the server.");
            }
        } else {
            if (itemCurrency == null) {
                log.severe("[Shop] PLUGIN DISABLED DUE TO INVALID VALUE IN CONFIGURATION SECTION: \"itemCurrencyID\"");
                getServer().getPluginManager().disablePlugin(plugin);
            } else
                log.info("[Shop] Shops will use " + itemCurrency.getType().name().replace("_", " ").toLowerCase() + " as the currency on the server.");
        }

        commandHandler = new CommandHandler(this, "shop.use", commandAlias, "Base command for the Shop plugin", "/shop", Collections.singletonList(commandAlias));
        shopHandler = new ShopHandler(plugin);
        guiHandler = new ShopGuiHandler(plugin);
        enderChestHandler = new EnderChestHandler(plugin);

        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(shopListener, this);
        getServer().getPluginManager().registerEvents(transactionListener, this);
        getServer().getPluginManager().registerEvents(miscListener, this);
        getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(armorStandListener, this);
    }

    @Override
    public void onDisable() {
//        if(useEnderChests())
//            enderChestHandler.saveEnderChests();
        //shopHandler.saveAllShops();
    }

    public void reload() {
        HandlerList.unregisterAll(displayListener);
        HandlerList.unregisterAll(shopListener);
        HandlerList.unregisterAll(transactionListener);
        HandlerList.unregisterAll(miscListener);
        HandlerList.unregisterAll(creativeSelectionListener);
        HandlerList.unregisterAll(guiListener);
        if (clearLaggListener != null)
            HandlerList.unregisterAll(clearLaggListener);

        onEnable();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ShopListener getShopListener() {
        return shopListener;
    }

    public DisplayListener getDisplayListener() {
        return displayListener;
    }

    public CreativeSelectionListener getCreativeSelectionListener() {
        return creativeSelectionListener;
    }

    public TransactionListener getTransactionListener() {
        return transactionListener;
    }

    public ShopHandler getShopHandler() {
        return shopHandler;
    }

    public CreationCheck getCreationCheck() {
        return creationCheck;
    }

    public ShopGuiHandler getGuiHandler() {
        return guiHandler;
    }

    public EnderChestHandler getEnderChestHandler() {
        return enderChestHandler;
    }

    public boolean usePerms() {
        return usePerms;
    }

    public boolean useVault() {
        return useVault;
    }

    public boolean hookWorldGuard() {
        return hookWorldGuard;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }
    public boolean towny() {
        return useTowny;
    }

    public boolean checkItemDurability() {
        return checkItemDurability;
    }

    public boolean allowCreativeSelection() {
        return allowCreativeSelection;
    }

    public boolean playSounds() {
        return playSounds;
    }

    public boolean playEffects() {
        return playEffects;
    }

    public boolean useGUI() {
        return enableGUI;
    }

    public ItemStack getGambleDisplayItem() {
        return gambleDisplayItem;
    }

    public ItemStack getItemCurrency() {
        return itemCurrency;
    }

    public void setItemCurrency(ItemStack itemCurrency) {
        this.itemCurrency = itemCurrency;

        try {
            File fileDirectory = new File(getDataFolder(), "Data");
            File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
            YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
            currencyConfig.set("item", plugin.getItemCurrency());
            currencyConfig.save(itemCurrencyFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGambleDisplayItem(ItemStack is) {
        this.gambleDisplayItem = is;

        try {
            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
            if (!gambleDisplayFile.exists()) {
                gambleDisplayFile.getParentFile().mkdirs();
                gambleDisplayFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(gambleDisplayFile);

            config.set("GAMBLE_DISPLAY", is);
            config.save(gambleDisplayFile);

            plugin.reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getItemCurrencyName() {
        return itemCurrencyName;
    }

    public String getVaultCurrencySymbol() {
        return vaultCurrencySymbol;
    }

    public String getCommandAlias() {
        return commandAlias;
    }

    public String getPriceString(double price, boolean pricePer) {
        if (price == 0) {
            return ShopMessage.getFreePriceWord();
        }

        String format = currencyFormat;

        if (format.contains("[name]")) {
            if (useVault())
                format = format.replace("[name]", vaultCurrencySymbol);
            else
                format = format.replace("[name]", itemCurrencyName);
        }
        if (format.contains("[price]")) {
            if (useVault())
                return format.replace("[price]", new DecimalFormat("0.00").format(price));
            else if (pricePer)
                return format.replace("[price]", new DecimalFormat("#.##").format(price));
            else
                return format.replace("[price]", "" + (int) price);
        }
        return format;
    }

    public String getPriceComboString(double price, double priceSell, boolean pricePer) {
        if (price == 0) {
            return ShopMessage.getFreePriceWord();
        }

        String format = currencyFormat;

        if (format.contains("[name]")) {
            if (useVault())
                format = format.replace("[name]", vaultCurrencySymbol);
            else
                format = format.replace("[name]", itemCurrencyName);
        }
        if (format.contains("[price]")) {
            if (useVault())
                return format.replace("[price]", new DecimalFormat("0.00").format(price) + "/" + new DecimalFormat("0.00").format(priceSell));
            else if (pricePer)
                return format.replace("[price]", new DecimalFormat("#.##").format(price) + "/" + new DecimalFormat("0.00").format(priceSell));
            else
                return format.replace("[price]", "" + (int) price + "/" + (int) priceSell);
        }
        return format;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public Economy getEconomy() {
        return econ;
    }

    public boolean useEnderChests() {
        return useEnderchests;
    }

    public double getCreationCost() {
        return creationCost;
    }

    public double getDestructionCost() {
        return destructionCost;
    }

    public ItemNameUtil getItemNameUtil() {
        return itemNameUtil;
    }

    public ArrayList<String> getWorldBlacklist() {
        return worldBlackList;
    }
}