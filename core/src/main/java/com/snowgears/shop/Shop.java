package com.snowgears.shop;

import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.*;
import com.snowgears.shop.hook.*;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.DisplayListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import com.snowgears.shop.hook.WorldGuardHook.WorldGuardConfig;
import com.snowgears.shop.util.Metrics;
import com.snowgears.shop.util.Metrics.*;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.tcoded.folialib.FoliaLib;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Shop extends JavaPlugin {

    private static Shop plugin;
    private ShopLogger logger = new ShopLogger(this, true);
    private FoliaLib foliaLib;

    private static final String CONFIG_PATH_WORLD_GUARD_ENABLED = "worldGuard.enabled";
    private static final String CONFIG_PATH_LWC_ENABLED = "lwc.enabled";
    private static final String CONFIG_PATH_BENTO_BOX_ENABLED = "bentoBox.enabled";
    private static final String CONFIG_PATH_ADVANCED_REGION_MARKET_ENABLED = "advancedRegionMarket.enabled";
    private static final String CONFIG_PATH_PLOT_SQUARED_ENABLED = "plotSquared.enabled";
    private static final String CONFIG_PATH_BOLT_TRUST_INTEGRATION_ENABLED = "bolt.trustIntegration.enabled";
    private static final String CONFIG_PATH_BLOCK_PROT_TRUST_INTEGRATION_ENABLED = "blockProt.trustIntegration.enabled";
    private static final String CONFIG_PATH_GRIEF_PREVENTION_ENABLED = "griefprevention.enabled";


    private ShopListener shopListener;
    private DisplayListener displayListener;
    private TransactionHandler transactionHandler;
    private MiscListener miscListener;
    private CreativeSelectionListener creativeSelectionListener;
    private ShopGUIListener guiListener;
    private Boolean worldGuardExists = false;
    private LWCHookListener lwcHookListener;
    private DynmapHookListener dynmapHookListener;
    private BluemapHookListener bluemapHookListener;
    private boolean bluemapEnabled;
    private boolean dynmapEnabled;
    private BentoBoxHookListener bentoBoxHookListener;
    private ARMHookListener armHookListener;
    private PlotSquaredHookListener plotSquaredHookListener;
    private BoltTrustListener boltTrustListener;
    private BlockProtTrustListener blockProtTrustListener;
    private GriefPreventionHookListener griefPreventionHookListener;

    private boolean worldGuardIntegrationEnabled;
    private boolean lwcIntegrationEnabled;
    private boolean bentoBoxIntegrationEnabled;
    private boolean advancedRegionMarketIntegrationEnabled;
    private boolean plotSquaredIntegrationEnabled;
    private boolean boltTrustIntegrationEnabled;
    private boolean blockProtTrustIntegrationEnabled;
    private boolean griefPreventionIntegrationEnabled;

    private ShopHandler shopHandler;
    private ShopGuiHandler guiHandler;
    private ItemNameUtil itemNameUtil;
    private ShopCreationUtil shopCreationUtil;

    private NMSBullshitHandler nmsBullshitHandler;

    private boolean usePerms;
    private boolean checkUpdates;
    private boolean enableGUI;
    
    // Simplified WorldGuard configuration
    private WorldGuardConfig worldGuardConfig;
    
    private boolean hookTowny;
    private String commandAlias;
    private DisplayType displayType;
    private DisplayTagOption displayTagOption;
    private DisplayType[] displayCycle;
    private boolean checkItemDurability;
    private boolean ignoreItemRepairCost;
    private boolean allowCreativeSelection;
    private boolean forceDisplayToNoneIfBlocked;
    private int displayLightLevel;
    private boolean setGlowingItemFrame;
    private boolean setGlowingSignText;
    private NavigableMap<Double, String> priceSuffixes;
    private Double priceSuffixMinimumValue;
    private boolean destroyShopRequiresSneak;
    private int hoursOfflineToRemoveShops;
    private boolean playSounds;
    private boolean playEffects;
    private boolean allowCreateMethodSign;
    private boolean allowCreateMethodChest;
    private ItemStack gambleDisplayItem;
    private ItemStack itemCurrency = null;
    private CurrencyType currencyType;
    private String currencyName = "";
    private String currencyFormat = "";
    private boolean allowFractionalCurrency = false;
    private Economy econ = null;
    private List<Material> enabledContainers;
    private boolean inverseComboShops;
    private double creationCost;
    private double destructionCost;
    private double teleportCost;
    private double teleportCooldown;
    private boolean returnCreationCost;
    private boolean allowPartialSales;
    private double taxPercent;
    private boolean offlinePurchaseNotificationsEnabled;
    private ItemListType itemListType;
    private List<String> worldBlackList;
    private HashMap<ShopClickType, ShopAction> clickTypeActionMap;
    private NamespacedKey signLocationNameSpacedKey;
    private NamespacedKey playerUUIDNameSpacedKey;
    private LogHandler logHandler;
    
    // Shop display optimization settings
    private double displayProcessInterval;
    private double displayMovementThreshold;
    private double maxShopDisplayDistance;
    private int shopSearchRadius;
    private int displayBatchSize;
    private int displayBatchDelay;

    private YamlConfiguration config;

    private boolean debug_allowUseOwnShop;
    private boolean debug_transactionDebugLogs;
    private int debug_shopCreateCooldown;
    private boolean debug_forceResaveAll;

    private Metrics metrics;

    public static Shop getPlugin() {
        return plugin;
    }

    public static boolean loggedDisplayDisabledWarning = false;

    // Return the custom ShopLogger so that we can log at higher levels.
    @Override
    public ShopLogger getLogger() { return logger; }

    @Override
    public void onLoad(){
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        // Load logger
        logger = new ShopLogger(this, config.getBoolean("enableLogColor"));
        this.getLogger().setLogLevel(config.getString("logLevel"));
        
        // Check if WorldGuard exists
        // Note: If WorldGuard exists we will check to verify a user can build in the region
        boolean worldGuardDetected = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        this.worldGuardIntegrationEnabled = config.getBoolean(CONFIG_PATH_WORLD_GUARD_ENABLED, true);
        if (worldGuardDetected) {
            if (worldGuardIntegrationEnabled) {
                this.getLogger().notice("WorldGuard detected, Shop will respect WorldGuard region flags during shop creation!");
                // Store for later
                this.worldGuardExists = true;
                // Load WorldGuard configuration (needed for flag registration)
                this.worldGuardConfig = new WorldGuardConfig(config);
                // Check if we want to require `allow-shop: true` to exist on regions
                if(worldGuardConfig.requireAllowShopFlag){
                    this.getLogger().notice("Registering WorldGuard `allow-shop` flag...");
                    // Register flag for WorldGuard if we are hooking into the flag system
                    WorldGuardHook.registerAllowShopFlag();
                    this.getLogger().notice("WorldGuard `allow-shop` flag restriction enabled, Shops can only be created in regions with the `allow-shop` flag set!");
                } else {
                    this.getLogger().notice("WorldGuard `allow-shop` flag restriction is disabled, if you want to only allow shops in regions with the `allow-shop` flag, please set `worldGuard.requireAllowShopFlag` to `true` in `config.yml`");
                }
                this.getLogger().notice("Loaded WorldGuard Config: " + worldGuardConfig.toString());
            } else {
                this.worldGuardExists = false;
                this.getLogger().notice("WorldGuard detected, but Shop WorldGuard integration is disabled via `" + CONFIG_PATH_WORLD_GUARD_ENABLED + ": false`");
            }
        } else {
            this.worldGuardExists = false;
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Initialize FoliaLib
        foliaLib = new FoliaLib(this);

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("config.yml"), configFile);
        }

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

        File displayConfigFile = new File(getDataFolder(), "displayConfig.yml");
        if (!displayConfigFile.exists()) {
            displayConfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("displayConfig.yml"), displayConfigFile);
        }

        try {
            // Check if we need to update any legacy config values

            // v1.10.0
            // Check if offlinePurchaseNotifications.enabled is a new value
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
            // One time update if the Offline Purchase Notifications feature is being started up for the very first time
            // Previous default OFF, new default FILE
            if (oldConfig.get("offlinePurchaseNotifications") == null && oldConfig.getString("logging.type").equals("OFF")) {
                logger.info("Config default update: v1.10.0(+) is being run for the first time, setting logging type to FILE from old default OFF");
                oldConfig.set("logging.type", "FILE");
                oldConfig.save(configFile);
            }

            // v1.10.2
            // Migrate old hookWorldGuard to new worldGuard.requireAllowShopFlag structure
            if (oldConfig.get("hookWorldGuard") != null && oldConfig.get("worldGuard.requireAllowShopFlag") == null) {
                boolean oldValue = oldConfig.getBoolean("hookWorldGuard");
                logger.info("Config migration: moving 'hookWorldGuard' to 'worldGuard.requireAllowShopFlag'");
                oldConfig.set("worldGuard.requireAllowShopFlag", oldValue);
                oldConfig.set("hookWorldGuard", null); // remove old key
                oldConfig.save(configFile);
            }

            // Next time we add a migration lets move it to a util class to keep things clean.

            ConfigUpdater.update(plugin, "config.yml", configFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ConfigUpdater.update(plugin, "chatConfig.yml", chatConfigFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ConfigUpdater.update(plugin, "signConfig.yml", signConfigFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ConfigUpdater.update(plugin, "displayConfig.yml", displayConfigFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }

        reloadConfig();
        signLocationNameSpacedKey = new NamespacedKey(this, "signLocation");
        playerUUIDNameSpacedKey = new NamespacedKey(this, "playerUUID");
        config = YamlConfiguration.loadConfiguration(configFile);
        // Load logger values again in case the log level was changed on a reload
        this.getLogger().setLogLevel(config.getString("logLevel"));
        this.getLogger().enableColor(config.getBoolean("enableLogColor"));

        nmsBullshitHandler = new NMSBullshitHandler(this);
        
        shopCreationUtil = new ShopCreationUtil(this);

        shopListener = new ShopListener(this);
        transactionHandler = new TransactionHandler(this);
        miscListener = new MiscListener(this);
        creativeSelectionListener = new CreativeSelectionListener(this);
        displayListener = new DisplayListener(this);
        guiListener = new ShopGUIListener(this);

        try {
            displayType = DisplayType.valueOf(config.getString("displayType"));
        } catch (Exception e){ displayType = DisplayType.ITEM; }

        try {
            displayTagOption = DisplayTagOption.valueOf(config.getString("displayNameTags"));
        } catch (Exception e){ displayTagOption = DisplayTagOption.NONE; }

        try {
            List<String> cycle = config.getStringList("displayCycle");
            if(cycle.isEmpty()){
                for(DisplayType dt : DisplayType.values()){
                    cycle.add(dt.name());
                }
            }

            displayCycle = new DisplayType[cycle.size()];
            for(int i=0; i < cycle.size(); i++){
                displayCycle[i] = DisplayType.valueOf(cycle.get(i));
            }
        } catch (Exception e){ e.printStackTrace(); }

        // Load ShopMessage by initializing it once
        new ShopMessage(this);
        itemNameUtil = new ItemNameUtil();

        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                this.getLogger().severe("[Shop] Data folder could not be created!");
            }
        }

        allowCreateMethodSign = config.getBoolean("creationMethod.placeSign");
        allowCreateMethodChest = config.getBoolean("creationMethod.hitChest");

        usePerms = config.getBoolean("usePermissions");
        if (usePerms) {
            this.getLogger().info("Permissions enabled, Shop will respect player permissions");
        } else {
            this.getLogger().info("Permissions disabled, everyone will be able to create/use shops by default");
        }
        checkUpdates = config.getBoolean("checkUpdates");
        enableGUI = config.getBoolean("enableGUI");
        
        worldGuardIntegrationEnabled = config.getBoolean(CONFIG_PATH_WORLD_GUARD_ENABLED, true);
        lwcIntegrationEnabled = config.getBoolean(CONFIG_PATH_LWC_ENABLED, true);
        bentoBoxIntegrationEnabled = config.getBoolean(CONFIG_PATH_BENTO_BOX_ENABLED, true);
        advancedRegionMarketIntegrationEnabled = config.getBoolean(CONFIG_PATH_ADVANCED_REGION_MARKET_ENABLED, true);
        plotSquaredIntegrationEnabled = config.getBoolean(CONFIG_PATH_PLOT_SQUARED_ENABLED, true);
        boltTrustIntegrationEnabled = config.getBoolean(CONFIG_PATH_BOLT_TRUST_INTEGRATION_ENABLED, true);
        blockProtTrustIntegrationEnabled = config.getBoolean(CONFIG_PATH_BLOCK_PROT_TRUST_INTEGRATION_ENABLED, true);
        griefPreventionIntegrationEnabled = config.getBoolean(CONFIG_PATH_GRIEF_PREVENTION_ENABLED, true);

        // WorldGuard integration is only active if the plugin is installed AND the integration toggle is enabled.
        boolean worldGuardDetected = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        worldGuardExists = worldGuardDetected && worldGuardIntegrationEnabled;
        if (worldGuardExists) {
            worldGuardConfig = new WorldGuardConfig(config);
        } else {
            worldGuardConfig = null;
        }
        hookTowny = config.getBoolean("hookTowny");
        bluemapEnabled = config.getBoolean("bluemap-marker.enabled");
        dynmapEnabled = config.getBoolean("dynmap-marker.enabled");
        commandAlias = config.getString("commandAlias");
        checkItemDurability = config.getBoolean("checkItemDurability");
        ignoreItemRepairCost = config.getBoolean("ignoreItemRepairCost");
        allowCreativeSelection = config.getBoolean("allowCreativeSelection");
        forceDisplayToNoneIfBlocked = config.getBoolean("forceDisplayToNoneIfBlocked");
        displayLightLevel = config.getInt("displayLightLevel");
        setGlowingItemFrame = config.getBoolean("setGlowingItemFrame");
        hoursOfflineToRemoveShops = config.getInt("deletePlayerShopsAfterXHoursOffline");
        playSounds = config.getBoolean("playSounds");
        playEffects = config.getBoolean("playEffects");
        setGlowingSignText = config.getBoolean("setGlowingSignText");
        priceSuffixes = new TreeMap<>();
        for(String suffixKey : config.getConfigurationSection("priceSuffixes").getKeys(false)){
            if(suffixKey.equals("minimumValue")){
                priceSuffixMinimumValue = config.getDouble("priceSuffixes.minimumValue");
            }
            else {
                boolean enabled = config.getBoolean("priceSuffixes." + suffixKey + ".enabled");
                if (enabled) {
                    Double suffixValue = config.getDouble("priceSuffixes." + suffixKey + ".value");
                    priceSuffixes.put(suffixValue, suffixKey);
                }
            }
        }

        destroyShopRequiresSneak = config.getBoolean("destroyShopRequiresSneak");

        try {
            currencyType = CurrencyType.valueOf(config.getString("currency.type"));
        } catch(Exception e){
            currencyType = CurrencyType.ITEM;
        }

        if (currencyType == CurrencyType.VAULT) {
            allowFractionalCurrency = config.getBoolean("allowFractionalCurrency");
        }

        offlinePurchaseNotificationsEnabled = config.getBoolean("offlinePurchaseNotifications.enabled");

        if (offlinePurchaseNotificationsEnabled && config.getString("logging.type").toUpperCase().equals("OFF")) {
            this.getLogger().warning("Offline purchase notifications are enabled in `config.yml` but DB logging is set to `OFF`. Offline purchase notifications will be disabled.");
            this.getLogger().warning("Please set `logging.type` to `FILE` or setup a database in `config.yml` to enable offline purchase notifications.");
            offlinePurchaseNotificationsEnabled = false;
        }

        //Loading the itemCurrency from a file makes it easier to allow servers to use detailed itemstacks as the server's economy item
        File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
        if(itemCurrencyFile.exists()){
            YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
            itemCurrency = currencyConfig.getItemStack("item");
            itemCurrency.setAmount(1);
        }
        else{
            try {
                itemCurrency = new ItemStack(Material.EMERALD);
                itemCurrencyFile.createNewFile();

                YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
                currencyConfig.set("item", itemCurrency);
                currencyConfig.save(itemCurrencyFile);
            } catch (Exception e) {}
        }

        //load the gamble display item from it's file
        File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
        if (!gambleDisplayFile.exists()) {
            gambleDisplayFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("GAMBLE_DISPLAY.yml"), gambleDisplayFile);
        }
        try {
            YamlConfiguration gambleItemConfig = YamlConfiguration.loadConfiguration(gambleDisplayFile);
            gambleDisplayItem = gambleItemConfig.getItemStack("GAMBLE_DISPLAY");
        } catch (IllegalArgumentException e) {
            this.getLogger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        } catch (Exception e) {
            this.getLogger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        } catch (Error e) {
            this.getLogger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        }

        if (gambleDisplayItem == null) {
            this.getLogger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        }

        currencyName = config.getString("currency.name");
        currencyFormat = config.getString("currency.format");

        enabledContainers = new ArrayList<>();
        for(String materialString : config.getStringList("enabledContainers")){
            try{
                enabledContainers.add(Material.valueOf(materialString));
            } catch(IllegalArgumentException e) {}
        }

        inverseComboShops = config.getBoolean("inverseComboShops");

        creationCost = config.getDouble("creationCost");
        destructionCost = config.getDouble("destructionCost");
        teleportCost = config.getDouble("teleportCost");
        teleportCooldown = config.getDouble("teleportCooldown");
        returnCreationCost = config.getBoolean("returnCreationCost");
        allowPartialSales = config.getBoolean("allowPartialSales");

        try {
            itemListType = ItemListType.valueOf(config.getString("itemList"));
        } catch(Exception e){
            itemListType = ItemListType.NONE;
        }

        worldBlackList = config.getStringList("worldBlacklist");
        for(String world : config.getStringList("worldBlacklist")){
            worldBlackList.add(world);
        }

        clickTypeActionMap = new HashMap<>();
        clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.transactWithShop")), ShopAction.TRANSACT);
        clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.transactWithShopFullStack")), ShopAction.TRANSACT_FULLSTACK);
        clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.viewShopDetails")), ShopAction.VIEW_DETAILS);
        clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.cycleShopDisplay")), ShopAction.CYCLE_DISPLAY);
        
        // Load shop display optimization settings
        displayProcessInterval = config.getDouble("displayProcessInterval");
        displayMovementThreshold = config.getDouble("displayMovementThreshold");
        maxShopDisplayDistance = config.getDouble("maxShopDisplayDistance");
        shopSearchRadius = config.getInt("shopSearchRadius");
        displayBatchSize = config.getInt("displayBatchSize", 10);
        displayBatchDelay = config.getInt("displayBatchDelay", 2);

        // Check if we should load VAULT economy
        if (currencyType == CurrencyType.VAULT) {
            if (setupEconomy()) {
                this.getLogger().info("Shops will use the Vault economy (" + currencyName + ") as currency on the server.");
            } else {
                this.getLogger().severe("Unable to connect to Vault Economy! Are both Vault AND an Economy plugin installed?");
                this.getLogger().severe("Plugin Disabled: Invalid configuration value `economy.type` config.yml. If you do not wish to use Vault with Shop, make sure to set `economy.type` in the config file to `ITEM`.");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        } else {
            if (itemCurrency == null) {
                this.getLogger().severe("Plugin Disabled: Invalid value for `itemCurrencyID` in `config.yml`");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
            this.getLogger().info("Shops will use " + itemNameUtil.getName(itemCurrency).toPlainText() + "(s) as the currency on the server.");
        }

        // Load CommandHandler by initializing it once
        new CommandHandler(this, null, commandAlias, "Base command for the Shop plugin", "/shop", new ArrayList(Arrays.asList(commandAlias)));

        guiHandler = new ShopGuiHandler(plugin);
        shopHandler = new ShopHandler(plugin);
        guiHandler.loadIconsAndTitles();
        logHandler = new LogHandler(plugin, config);

        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(shopListener, this);
        getServer().getPluginManager().registerEvents(miscListener, this);
        getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        //only define different listener hooks if the plugins are present on the server
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            if (worldGuardExists) {
                this.getLogger().notice("WorldGuard is installed, WorldGuard integration is enabled");
                if(worldGuardConfig.requireAllowShopFlag){
                    this.getLogger().helpful("WorldGuard `allow-shop` flag restriction enabled, Shops can only be created in regions with the `allow-shop` flag set!");
                } else {
                    this.getLogger().helpful("WorldGuard `allow-shop` flag restriction is disabled, if you want to only allow shops in regions with the `allow-shop` flag, please set `worldGuard.requireAllowShopFlag` to `true` in `config.yml`");
                }
                this.getLogger().helpful("WorldGuard flags that will be checked for shop creation: " + worldGuardConfig.createShopFlags.toString());
                this.getLogger().helpful("WorldGuard flags that will be checked for shop use: " + worldGuardConfig.useShopFlags.toString());
            } else {
                this.getLogger().notice("WorldGuard detected, but Shop WorldGuard integration is disabled via `" + CONFIG_PATH_WORLD_GUARD_ENABLED + ": false`");
            }
        }

        if(getServer().getPluginManager().getPlugin("Towny") != null && this.hookTowny){
            this.getLogger().notice("Towny is installed, Shop will respect Towny!");
        }

        if(getServer().getPluginManager().getPlugin("LWC") != null){
            if (lwcIntegrationEnabled) {
                lwcHookListener = new LWCHookListener(this);
                getServer().getPluginManager().registerEvents(lwcHookListener, this);
                this.getLogger().notice("LWC is installed, enabling LWC integration");
            } else {
                this.getLogger().notice("LWC detected, but Shop LWC integration is disabled via `" + CONFIG_PATH_LWC_ENABLED + ": false`");
            }
        }

        if(getServer().getPluginManager().getPlugin("dynmap") != null && dynmapEnabled){
            dynmapHookListener = new DynmapHookListener(this);
            getServer().getPluginManager().registerEvents(dynmapHookListener, this);
            this.getLogger().notice("Dynmap is installed, creating Dynmap listener");
        }

        if(getServer().getPluginManager().getPlugin("BlueMap") != null && bluemapEnabled){
            plugin.getLogger().notice("BlueMap is installed, starting BlueMap integration");
            // Wait for 2 minutes for BlueMap to become available/boot up, then initialize listener.
            foliaLib.getScheduler().runTimer(task -> {
                BlueMapAPI.getInstance().ifPresent(api -> {
                    plugin.getLogger().debug("BlueMap is ready, creating BlueMap listener");
                    bluemapHookListener = new BluemapHookListener(plugin);
                    getServer().getPluginManager().registerEvents(bluemapHookListener, plugin);
                    // Make sure we load the markers in case there are shops that BlueMap doesn't know about
                    bluemapHookListener.reloadMarkers(shopHandler);
                    // Mark the task as complete and cancel the timer
                    foliaLib.getScheduler().cancelTask(task);
                });
            }, 20, 20); // Check every second (20 ticks) until BlueMap is booted
        }

        if(getServer().getPluginManager().getPlugin("BentoBox") != null){
            if (bentoBoxIntegrationEnabled) {
                bentoBoxHookListener = new BentoBoxHookListener(this);
                getServer().getPluginManager().registerEvents(bentoBoxHookListener, this);
                this.getLogger().notice("BentoBox is installed, enabling BentoBox integration");
            } else {
                this.getLogger().notice("BentoBox detected, but Shop BentoBox integration is disabled via `" + CONFIG_PATH_BENTO_BOX_ENABLED + ": false`");
            }
        }

        if(getServer().getPluginManager().getPlugin("AdvancedRegionMarket") != null){
            if (advancedRegionMarketIntegrationEnabled) {
                armHookListener = new ARMHookListener(this);
                getServer().getPluginManager().registerEvents(armHookListener, this);
                this.getLogger().notice("AdvancedRegionMarket is installed, enabling AdvancedRegionMarket integration");
            } else {
                this.getLogger().notice("AdvancedRegionMarket detected, but Shop AdvancedRegionMarket integration is disabled via `" + CONFIG_PATH_ADVANCED_REGION_MARKET_ENABLED + ": false`");
            }
        }

        if(getServer().getPluginManager().getPlugin("PlotSquared") != null){
            if (plotSquaredIntegrationEnabled) {
                plotSquaredHookListener = new PlotSquaredHookListener(this);
                getServer().getPluginManager().registerEvents(plotSquaredHookListener, this);
                this.getLogger().notice("PlotSquared is installed, enabling PlotSquared integration");
            } else {
                this.getLogger().notice("PlotSquared detected, but Shop PlotSquared integration is disabled via `" + CONFIG_PATH_PLOT_SQUARED_ENABLED + ": false`");
            }
        }

        // Register trust integration listeners for protection plugins
        if(getServer().getPluginManager().getPlugin("Bolt") != null){
            if (boltTrustIntegrationEnabled) {
                try {
                    boltTrustListener = new BoltTrustListener();
                    getServer().getPluginManager().registerEvents(boltTrustListener, this);
                    this.getLogger().notice("Bolt is installed, enabling trust integration for opening shop containers");
                } catch (Exception e) {
                    this.getLogger().warning("Bolt detected but could not enable trust integration: " + e.getMessage());
                }
            } else {
                this.getLogger().notice("Bolt detected, but Shop Bolt trust integration is disabled via `" + CONFIG_PATH_BOLT_TRUST_INTEGRATION_ENABLED + ": false`");
            }
        }
        if(getServer().getPluginManager().getPlugin("BlockProt") != null){
            if (blockProtTrustIntegrationEnabled) {
                try {
                    blockProtTrustListener = new BlockProtTrustListener();
                    getServer().getPluginManager().registerEvents(blockProtTrustListener, this);
                    this.getLogger().notice("BlockProt is installed, enabling trust integration for opening shop containers");
                } catch (Exception e) {
                    this.getLogger().warning("BlockProt detected but could not enable trust integration: " + e.getMessage());
                }
            } else {
                this.getLogger().notice("BlockProt detected, but Shop BlockProt trust integration is disabled via `" + CONFIG_PATH_BLOCK_PROT_TRUST_INTEGRATION_ENABLED + ": false`");
            }
        }
        if(getServer().getPluginManager().getPlugin("GriefPrevention") != null){
            if (griefPreventionIntegrationEnabled) {
                try {
                    griefPreventionHookListener = new GriefPreventionHookListener();
                    getServer().getPluginManager().registerEvents(griefPreventionHookListener, this);
                    this.getLogger().notice("GriefPrevention is installed, enabling GriefPrevention integration");
                } catch (Exception e) {
                    this.getLogger().warning("GriefPrevention detected but could not enable GriefPrevention integration: " + e.getMessage());
                }
            } else {
                this.getLogger().notice("GriefPrevention detected, but Shop GriefPrevention integration is disabled via `" + CONFIG_PATH_GRIEF_PREVENTION_ENABLED + ": false`");
            }
        }

        int bstatsPluginId = 25211;
        metrics = new Metrics(plugin, bstatsPluginId);
        metrics.addCustomChart(new SingleLineChart("transactions", () -> logHandler.getRecentTransactionCount()));
        metrics.addCustomChart(new SingleLineChart("item_volume", () -> logHandler.getRecentItemVolume()));
        metrics.addCustomChart(new SingleLineChart("shops", () -> shopHandler.getNumberOfShops()));
        metrics.addCustomChart(new AdvancedPie("shop_types", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Buy", shopHandler.getNumberOfShops(ShopType.BUY));
            valueMap.put("Sell", shopHandler.getNumberOfShops(ShopType.SELL));
            valueMap.put("Barter", shopHandler.getNumberOfShops(ShopType.BARTER));
            valueMap.put("Combo", shopHandler.getNumberOfShops(ShopType.COMBO));
            valueMap.put("Gamble", shopHandler.getNumberOfShops(ShopType.GAMBLE));
            return valueMap;
        }));
        metrics.addCustomChart(new AdvancedPie("shop_display_types", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Floating Item", shopHandler.getNumberOfShopDisplayTypes(DisplayType.ITEM));
            valueMap.put("Large Item", shopHandler.getNumberOfShopDisplayTypes(DisplayType.LARGE_ITEM));
            valueMap.put("Item Frame", shopHandler.getNumberOfShopDisplayTypes(DisplayType.ITEM_FRAME));
            valueMap.put("Glass Case", shopHandler.getNumberOfShopDisplayTypes(DisplayType.GLASS_CASE));
            valueMap.put("None", shopHandler.getNumberOfShopDisplayTypes(DisplayType.NONE));
            return valueMap;
        }));
        metrics.addCustomChart(new AdvancedPie("shop_containers", () -> shopHandler.getShopContainerCounts()));
        metrics.addCustomChart(new SimplePie("economy_type", () -> { return currencyType.toString(); }));
        metrics.addCustomChart(new SimplePie("fractional_currency", () -> { return String.valueOf(allowFractionalCurrency); }));
        // Add metrics for more configuration options
        metrics.addCustomChart(new SimplePie("use_permissions", () -> String.valueOf(usePerms)));
        metrics.addCustomChart(new SimplePie("allow_partial_sales", () -> String.valueOf(allowPartialSales)));

        // Group these into an advanced pie
        metrics.addCustomChart(new AdvancedPie("shop_creation_methods", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Sign Creation", allowCreateMethodSign ? 1 : 0);
            valueMap.put("Chest Creation", allowCreateMethodChest ? 1 : 0);
            valueMap.put("Signs Disabled", allowCreateMethodSign ? 0 : 1);
            valueMap.put("Chests Disabled", allowCreateMethodChest ? 0 : 1);
            return valueMap;
        }));

        metrics.addCustomChart(new SimplePie("offline_purchase_notifications", () -> String.valueOf(offlinePurchaseNotificationsEnabled)));
        metrics.addCustomChart(new SimplePie("shop_gui_enabled", () -> { return String.valueOf(enableGUI); }));
        metrics.addCustomChart(new SimplePie("allow_searching_items", () -> String.valueOf(allowCreativeSelection)));
        metrics.addCustomChart(new SimplePie("check_item_durability", () -> String.valueOf(checkItemDurability)));
        metrics.addCustomChart(new SimplePie("ignore_item_repair_cost", () -> String.valueOf(ignoreItemRepairCost)));
        metrics.addCustomChart(new AdvancedPie("sounds_and_effects", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Sounds Enabled", playSounds ? 1 : 0);
            valueMap.put("Effects Enabled", playEffects ? 1 : 0);
            valueMap.put("Sounds Disabled", playSounds ? 0 : 1);
            valueMap.put("Effects Disabled", playEffects ? 0 : 1);
            return valueMap;
        }));
        
        metrics.addCustomChart(new SimplePie("worldguard_enabled", () -> { return String.valueOf(worldGuardExists); }));
        metrics.addCustomChart(new SimplePie("towny_enabled", () -> { return String.valueOf(hookTowny); }));
        metrics.addCustomChart(new SimplePie("dynmap_enabled", () -> String.valueOf(dynmapEnabled)));
        metrics.addCustomChart(new SimplePie("bluemap_enabled", () -> String.valueOf(bluemapEnabled)));
        metrics.addCustomChart(new SimplePie("database_type", () -> String.valueOf(config.getString("logging.type"))));
        
        // Track display type preferences
        metrics.addCustomChart(new SimplePie("item_hover_display_type", () -> displayType.toString()));
        metrics.addCustomChart(new SimplePie("hover_text_activation_type", () -> displayTagOption.toString()));
        
        // Track if shop auto-deletion is enabled
        metrics.addCustomChart(new SimplePie("auto_cleanup_dead_shops", () -> String.valueOf(hoursOfflineToRemoveShops > 0)));
        // Track if destroying shops requires sneaking
        metrics.addCustomChart(new SimplePie("destroy_requires_sneak", () -> String.valueOf(destroyShopRequiresSneak)));
        // Track if combo shops are inverted
        metrics.addCustomChart(new SimplePie("inverse_combo_shops", () -> String.valueOf(inverseComboShops)));

        // Add container types tracking - group by container categories
        metrics.addCustomChart(new AdvancedPie("enabled_containers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            // Track basic chest types
            boolean hasChests = enabledContainers.contains(Material.CHEST) || 
                                enabledContainers.contains(Material.TRAPPED_CHEST);
            valueMap.put("Chests Allowed", hasChests ? 1 : 0);
            valueMap.put("Chests Disabled", hasChests ? 0 : 1);
            
            // Track barrels
            boolean hasBarrel = enabledContainers.contains(Material.BARREL);
            valueMap.put("Barrels Allowed", hasBarrel ? 1 : 0);
            valueMap.put("Barrels Disabled", hasBarrel ? 0 : 1);
            
            // Track if any shulker box is enabled
            boolean hasShulker = enabledContainers.stream()
                    .anyMatch(m -> m.name().endsWith("SHULKER_BOX"));
            valueMap.put("Shulker Boxes Allowed", hasShulker ? 1 : 0);
            valueMap.put("Shulker Boxes Disabled", hasShulker ? 0 : 1);
            
            return valueMap;
        }));
        
        // Track economic barriers (costs)
        metrics.addCustomChart(new AdvancedPie("economic_barriers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Creation Cost", creationCost > 0 ? 1 : 0);
            valueMap.put("No Creation Cost", creationCost > 0 ? 0 : 1);
            valueMap.put("Destruction Cost", destructionCost > 0 ? 1 : 0);
            valueMap.put("No Destruction Cost", destructionCost > 0 ? 0 : 1);
            valueMap.put("Teleport Cost", teleportCost > 0 ? 1 : 0);
            valueMap.put("No Teleport Cost", teleportCost > 0 ? 0 : 1);
            valueMap.put("Return Creation Cost", returnCreationCost ? 1 : 0);
            valueMap.put("Do not Return Creation Cost", returnCreationCost ? 0 : 1);
            return valueMap;
        }));
        
        // Track display enhancement features (1.17+)
        metrics.addCustomChart(new AdvancedPie("display_enhancements", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("Custom Light Level", displayLightLevel > 0 ? 1 : 0);
            valueMap.put("Normal Light Level", displayLightLevel > 0 ? 0 : 1);
            valueMap.put("Glowing Item Frames", setGlowingItemFrame ? 1 : 0);
            valueMap.put("Normal Item Frames", setGlowingItemFrame ? 0 : 1);
            valueMap.put("Glowing Sign Text", setGlowingSignText ? 1 : 0);
            valueMap.put("Normal Sign Text", setGlowingSignText ? 0 : 1);
            return valueMap;
        }));
        
        // Track which click types are used for each action
        // Find which click type is assigned to TRANSACT
        metrics.addCustomChart(new SimplePie("transaction_action_mapping", () -> {
            for (Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()) {
                if (entry.getValue() == ShopAction.TRANSACT) return entry.getKey().toString();
            }
            return "NOT_SET";
        }));
        // Find which click type is assigned to TRANSACT_FULLSTACK
        metrics.addCustomChart(new SimplePie("full_stack_transaction_action_mapping", () -> {
            for (Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()) {
                if (entry.getValue() == ShopAction.TRANSACT_FULLSTACK) return entry.getKey().toString();
            }
            return "NOT_SET";
        }));
        // Find which click type is assigned to VIEW_DETAILS
        metrics.addCustomChart(new SimplePie("view_details_action_mapping", () -> {
            for (Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()) {
                if (entry.getValue() == ShopAction.VIEW_DETAILS) return entry.getKey().toString();
            }
            return "NOT_SET";
        }));
        // Find which click type is assigned to CYCLE_DISPLAY
        metrics.addCustomChart(new SimplePie("cycle_display_action_mapping", () -> {
            for (Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()) {
                if (entry.getValue() == ShopAction.CYCLE_DISPLAY) return entry.getKey().toString();
            }
            return "NOT_SET";
        }));

        metrics.addCustomChart(new SimplePie("display_processing_interval", () -> String.valueOf(displayProcessInterval)));
        metrics.addCustomChart(new SimplePie("display_movement_threshold", () -> String.valueOf(displayMovementThreshold)));
        metrics.addCustomChart(new SimplePie("display_max_shop_distance", () -> String.valueOf(maxShopDisplayDistance)));
        metrics.addCustomChart(new SimplePie("display_shop_search_radius", () -> String.valueOf(shopSearchRadius)));
        metrics.addCustomChart(new SimplePie("display_batch_size", () -> String.valueOf(displayBatchSize)));
        metrics.addCustomChart(new SimplePie("display_batch_delay", () -> String.valueOf(displayBatchDelay)));

        debug_allowUseOwnShop = config.getBoolean("debug.allowUseOwnShop");
        debug_transactionDebugLogs = config.getBoolean("debug.transactionDebugLogs");
        debug_shopCreateCooldown = config.getInt("debug.shopCreateCooldown");
        debug_forceResaveAll = config.getBoolean("debug.forceResaveAll");

        displayListener.startRepeatingDisplayViewTask();

        this.getLogger().info("Enabled Shop " + this.getDescription().getVersion());

        if(checkUpdates){
            new UpdateChecker(this).checkForUpdate();
        }
    }

    @Override
    public void onDisable(){
        // Cancel all FoliaLib scheduled tasks
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
        
        //save any remaining shops (usually not required but just in case)
        if (shopHandler != null) shopHandler.saveAllShops();

        // Save player name cache to ensure no data loss
        PlayerNameCache.saveToFile();

        // shutdown the database
        if (logHandler != null) logHandler.shutdown();
        if (metrics != null) metrics.shutdown();

        this.getLogger().info("Disabled Shop " + this.getDescription().getVersion());
    }

    public void reload(){
        this.getLogger().info("Reloading Shop " + this.getDescription().getVersion());

        HandlerList.unregisterAll(displayListener);
        HandlerList.unregisterAll(shopListener);
        HandlerList.unregisterAll(miscListener);
        HandlerList.unregisterAll(creativeSelectionListener);
        HandlerList.unregisterAll(guiListener);
        if(lwcHookListener != null){
            HandlerList.unregisterAll(lwcHookListener);
        }
        if(dynmapHookListener != null){
            dynmapHookListener.deleteMarkers();
            HandlerList.unregisterAll(dynmapHookListener);
        }
        if(bluemapHookListener != null){
            //bluemapHookListener.deleteMarkers();
            HandlerList.unregisterAll(bluemapHookListener);
        }
        if(bentoBoxHookListener != null){
            HandlerList.unregisterAll(bentoBoxHookListener);
        }
        if(armHookListener != null){
            HandlerList.unregisterAll(armHookListener);
        }
        if(plotSquaredHookListener != null){
            HandlerList.unregisterAll(plotSquaredHookListener);
        }
        if(boltTrustListener != null){
            HandlerList.unregisterAll(boltTrustListener);
        }
        if(blockProtTrustListener != null){
            HandlerList.unregisterAll(blockProtTrustListener);
        }

        plugin.getShopHandler().removeAllDisplays(null);

        onDisable();
        onEnable();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        this.getLogger().notice("Vault is installed, creating Vault integration for Economy support");
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

    public MiscListener getMiscListener(){
        return miscListener;
    }

    public CreativeSelectionListener getCreativeSelectionListener() {
        return creativeSelectionListener;
    }

    public BluemapHookListener getBluemapHookListener() {
        return bluemapHookListener;
    }

    public TransactionHandler getTransactionHelper() {
        return transactionHandler;
    }

    public ShopHandler getShopHandler() {
        return shopHandler;
    }

    public ShopGuiHandler getGuiHandler(){
        return guiHandler;
    }

    public boolean usePerms() {
        return usePerms;
    }

    public boolean getAllowCreationMethodSign(){
        return allowCreateMethodSign;
    }

    public boolean getAllowCreationMethodChest(){
        return allowCreateMethodChest;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    // Simplified WorldGuard configuration getter
    public WorldGuardConfig getWorldGuardConfig() {
        return worldGuardConfig;
    }

    public boolean worldGuardExists() { return worldGuardExists; }

    public boolean isWorldGuardIntegrationEnabled() { return worldGuardExists(); }
    public boolean isLwcIntegrationEnabled() { return lwcIntegrationEnabled; }
    public boolean isBentoBoxIntegrationEnabled() { return bentoBoxIntegrationEnabled; }
    public boolean isAdvancedRegionMarketIntegrationEnabled() { return advancedRegionMarketIntegrationEnabled; }
    public boolean isPlotSquaredIntegrationEnabled() { return plotSquaredIntegrationEnabled; }
    public boolean isBoltTrustIntegrationEnabled() { return boltTrustIntegrationEnabled; }
    public boolean isBlockProtTrustIntegrationEnabled() { return blockProtTrustIntegrationEnabled; }

    public boolean hookTowny(){
        return hookTowny;
    }

    public DisplayType getDisplayType(){
        return displayType;
    }

    public DisplayTagOption getDisplayTagOption(){
        return displayTagOption;
    }

    public DisplayType[] getDisplayCycle(){
        return displayCycle;
    }

    public boolean checkItemDurability(){
        return checkItemDurability;
    }
    public boolean ignoreItemRepairCost(){
        return ignoreItemRepairCost;
    }

    public boolean allowCreativeSelection(){
        return allowCreativeSelection;
    }

    public boolean forceDisplayToNoneIfBlocked() {
        return forceDisplayToNoneIfBlocked;
    }

    public int getDisplayLightLevel(){
        return displayLightLevel;
    }

    public boolean getGlowingItemFrame(){
        return setGlowingItemFrame;
    }

    public int getHoursOfflineToRemoveShops(){
        return hoursOfflineToRemoveShops;
    }

    public boolean playSounds(){
        return playSounds;
    }

    public boolean playEffects(){
        return playEffects;
    }

    public boolean getGlowingSignText(){
        return setGlowingSignText;
    }

    public NavigableMap<Double, String> getPriceSuffixes(){
        return priceSuffixes;
    }

    public Double getPriceSuffixMinimumValue(){
        return priceSuffixMinimumValue;
    }

    public boolean useGUI(){
        return enableGUI;
    }

    public ItemStack getGambleDisplayItem(){
        return gambleDisplayItem;
    }

    public ItemStack getItemCurrency() {
        return itemCurrency;
    }

    public boolean offlinePurchaseNotificationsEnabled() {
        return offlinePurchaseNotificationsEnabled;
    }

    private Boolean isMockBukkit = null;
    public boolean isMockBukkit() { 
        if (this.isMockBukkit == null) {
            this.isMockBukkit = plugin.getServer().getClass().getPackage().getName().contains("mockbukkit");
        }
        return this.isMockBukkit;
    }
    public boolean getDebug_allowUseOwnShop() { return debug_allowUseOwnShop; }
    public boolean getDebug_transactionDebugLogs() { return debug_transactionDebugLogs; }
    public int getDebug_shopCreateCooldown() { return debug_shopCreateCooldown; }
    public boolean getDebug_forceResaveAll() { return debug_forceResaveAll; }

    public void setItemCurrency(ItemStack itemCurrency){
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

    public void setGambleDisplayItem(ItemStack is){
        this.gambleDisplayItem = is;

        try{
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

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCommandAlias(){
        return commandAlias;
    }

    public String getPriceString(double price, boolean pricePer){
        if(price == 0){
            return ShopMessage.getFreePriceWord();
        }

        String format = currencyFormat;

        if(format.contains("[name]")){
            format = format.replace("[name]", currencyName);
        }
        if(format.contains("[price]")){
            if(currencyType == CurrencyType.VAULT) {
                return format.replace("[price]", UtilMethods.formatLongToKString(price, true));
                //return format.replace("[price]", new DecimalFormat("0.00").format(price).toString());
            }
            else if(pricePer) {
                return format.replace("[price]", UtilMethods.formatLongToKString(price, false));
                //return format.replace("[price]", new DecimalFormat("#.##").format(price).toString());
            }
            else
                return format.replace("[price]", ""+(int)price);
        }
        return format;
    }

    public String getPriceComboString(double price, double priceSell, boolean pricePer){
        if(price == 0){
            return ShopMessage.getFreePriceWord();
        }

        String format = currencyFormat;

        if(format.contains("[name]")){
            format = format.replace("[name]", currencyName);
        }
        if(format.contains("[price]")){
            if(currencyType == CurrencyType.VAULT)
                //return format.replace("[price]", new DecimalFormat("0.00").format(price)+"/"+new DecimalFormat("0.00").format(priceSell).toString());
            return format.replace("[price]", UtilMethods.formatLongToKString(price, true)+"/"+UtilMethods.formatLongToKString(priceSell, true));
            else if(pricePer)
                //return format.replace("[price]", new DecimalFormat("#.##").format(price).toString()+"/"+new DecimalFormat("0.00").format(priceSell).toString());
                return format.replace("[price]", UtilMethods.formatLongToKString(price, false)+"/"+UtilMethods.formatLongToKString(priceSell, true));
            else
                return format.replace("[price]", ""+(int)price+"/"+(int)priceSell);
        }
        return format;
    }

    public double getTaxPercent(){
        return taxPercent;
    }

    public Economy getEconomy() {

        if (econ == null) {
            setupEconomy();
        }

        return econ;
    }

    public boolean getAllowFractionalCurrency() { 
        return allowFractionalCurrency;
    }

    public List<Material> getEnabledContainers(){
        return enabledContainers;
    }

    public boolean inverseComboShops(){
        return inverseComboShops;
    }

    public double getCreationCost(){
        return creationCost;
    }

    public double getDestructionCost(){
        return destructionCost;
    }

    public boolean getDestroyShopRequiresSneak(){
        return destroyShopRequiresSneak;
    }

    public double getTeleportCost(){
        return teleportCost;
    }

    public double getTeleportCooldown(){
        return teleportCooldown;
    }

    public boolean returnCreationCost(){
        return returnCreationCost;
    }

    public boolean getAllowPartialSales(){
        return allowPartialSales;

    }

    public ItemNameUtil getItemNameUtil(){
        return itemNameUtil;
    }

    public ShopCreationUtil getShopCreationUtil(){
        return shopCreationUtil;
    }

    public ItemListType getItemListType(){
        return itemListType;
    }

    public List<String> getWorldBlacklist(){
        return worldBlackList;
    }

    public ShopAction getShopAction(ShopClickType shopClickType){
        return clickTypeActionMap.get(shopClickType);
    }

    public NMSBullshitHandler getNmsBullshitHandler() {
        return nmsBullshitHandler;
    }

    public NamespacedKey getSignLocationNameSpacedKey(){
        return signLocationNameSpacedKey;
    }

    public NamespacedKey getPlayerUUIDNameSpacedKey(){
        return playerUUIDNameSpacedKey;
    }

    public LogHandler getLogHandler(){
        return logHandler;
    }

    // Getter for FoliaLib
    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public double getDisplayProcessInterval() {
        return displayProcessInterval;
    }
    
    public double getDisplayMovementThreshold() {
        return displayMovementThreshold;
    }

    /**
     * Gets the maximum distance at which shop displays will be shown to players.
     * Higher values will show shops from further away but may cause client lag.
     * 
     * @return The maximum display distance in blocks
     */
    public double getMaxShopDisplayDistance() {
        return maxShopDisplayDistance;
    }

    /**
     * Gets the radius (in chunks) around a player to search for shops.
     * Each increment searches exponentially more chunks (1=3x3 area, 2=5x5 area, 3=7x7 area).
     * 
     * @return The shop search radius in chunks
     */
    public int getShopSearchRadius() {
        return shopSearchRadius;
    }

    /**
     * Gets the number of shop displays to process in a single batch when sending to a player.
     * This controls how many displays are sent at once to create a smoother appearance.
     * 
     * @return The batch size for shop display processing
     */
    public int getDisplayBatchSize() {
        return displayBatchSize;
    }

    /**
     * Gets the delay between batches of shop displays in server ticks (20 ticks = 1 second).
     * Higher values create a smoother appearance but take longer to show all displays.
     * 
     * @return The delay in ticks between display batches
     */
    public int getDisplayBatchDelay() {
        return displayBatchDelay;
    }
}
