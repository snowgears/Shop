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
import com.snowgears.shop.util.Metrics;
import com.snowgears.shop.util.Metrics.*;
import java.util.concurrent.Callable;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.tcoded.folialib.FoliaLib;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shop extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Shop plugin;
    private ShopLogger logger = new ShopLogger(this, true);
    private FoliaLib foliaLib;

    private ShopListener shopListener;
    private DisplayListener displayListener;
    private TransactionHandler transactionHandler;
    private MiscListener miscListener;
    private CreativeSelectionListener creativeSelectionListener;
    private ShopGUIListener guiListener;
    private Boolean worldGuardExists;
    private LWCHookListener lwcHookListener;
    private DynmapHookListener dynmapHookListener;
    private BluemapHookListener bluemapHookListener;
    private boolean bluemapEnabled;
    private boolean dynmapEnabled;
    private BentoBoxHookListener bentoBoxHookListener;
    private ARMHookListener armHookListener;
    private PlotSquaredHookListener plotSquaredHookListener;

    private ShopHandler shopHandler;
    private CommandHandler commandHandler;
    private ShopGuiHandler guiHandler;
    private EnderChestHandler enderChestHandler;
    private ShopMessage shopMessage;
    private ItemNameUtil itemNameUtil;
    private PriceUtil priceUtil;
    private ShopCreationUtil shopCreationUtil;

    private NMSBullshitHandler nmsBullshitHandler;
    private NBTAdapter nbtAdapter;

    private boolean usePerms;
    private boolean checkUpdates;
    private boolean enableGUI;
    private boolean hookWorldGuard;
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
    public static Shop getPlugin() {
        return plugin;
    }

    public static boolean loggedDisplayDisabledWarning = false;

    // // Return the custom ShopLogger so that we can log at higher levels.
    // @Override
    // public ShopLogger getLogger() { return logger; }

    /**
     * Returns the custom ShopLogger with legacy compatibility handling.
     * This method provides access to all custom log levels (NOTICE, HELPFUL, DEBUG, TRACE, SPAM, HYPER)
     * while gracefully handling legacy Minecraft versions that don't support logger overrides.
     * 
     * In legacy builds, the ShopLogger class itself contains built-in compatibility handling,
     * so this method can simply return the logger instance directly.
     * 
     * Use this method instead of getLogger() for maximum compatibility across all Minecraft versions.
     * 
     * @return ShopLogger instance with built-in legacy compatibility
     */
    public ShopLogger getShopLogger() {
        return logger;
    }

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
        this.getShopLogger().setLogLevel(config.getString("logLevel"));

        // look for the worldguard boolean, as it needs a flag registered before worldguard is enabled
        hookWorldGuard = config.getBoolean("hookWorldGuard");
        // Check if WorldGuard exists
        // Note: If WorldGuard exists we will check to verify a user can build in the region
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.getShopLogger().notice("WorldGuard detected, Shop will respect `passthrough`, `build`, and `chest-access` region flags during shop creation!");
            // Store for later
            this.worldGuardExists = true;
            // Check if we want to require `allow-shop: true` to exist on regions
            if(hookWorldGuard){
                this.getShopLogger().notice("Registering WorldGuard `allow-shop` flag...");
                // Register flag for WorldGuard if we are hooking into the flag system
                WorldGuardHook.registerAllowShopFlag();
                this.getShopLogger().notice("WorldGuard `allow-shop` flag restriction enabled, Shops can only be created in regions with the `allow-shop` flag set!");
            } else {
                this.getShopLogger().notice("WorldGuard `allow-shop` flag restriction is disabled, if you want to only allow shops in regions with the `allow-shop` flag, please set `hookWorldGuard` to `true` in `config.yml`");
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
            // Check if offlinePurchaseNotifications.enabled is a new value
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
            // One time update if the Offline Purchase Notifications feature is being started up for the very first time
            // Previous default OFF, new default FILE
            if (oldConfig.get("offlinePurchaseNotifications") == null && oldConfig.getString("logging.type").equals("OFF")) {
                logger.info("Config default update: v1.10.0(+) is being run for the first time, setting logging type to FILE from old default OFF");
                oldConfig.set("logging.type", "FILE");
                oldConfig.save(configFile);
            }

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

        //Legacy versions don't support NamespacedKey/PersistentDataContainer
        //signLocationNameSpacedKey = CompatibilityUtil.createNamespacedKey(this, "signLocation");
        //playerUUIDNameSpacedKey = CompatibilityUtil.createNamespacedKey(this, "playerUUID");
        config = YamlConfiguration.loadConfiguration(configFile);
        // Load logger values again in case the log level was changed on a reload
        this.getShopLogger().setLogLevel(config.getString("logLevel"));
        this.getShopLogger().enableColor(config.getBoolean("enableLogColor"));

        nmsBullshitHandler = new NMSBullshitHandler(this);
        nbtAdapter = new NBTAdapter(this);
        
        shopCreationUtil = new ShopCreationUtil(this);

        //removed item names file after item ids are no longer used. may revisit later with new materials
//        File itemNameFile = new File(getDataFolder(), "items.tsv");
//        if (!itemNameFile.exists()) {
//            itemNameFile.getParentFile().mkdirs();
//            UtilMethods.copy(getResource("items.tsv"), itemNameFile);
//        }

        //TODO
//        File pricesFile = new File(getDataFolder(), "prices.tsv");
//        if (!pricesFile.exists()) {
//            pricesFile.getParentFile().mkdirs();
//            UtilMethods.copy(getResource("prices.tsv"), pricesFile);
//        }

        shopListener = new ShopListener(this);
        transactionHandler = new TransactionHandler(this);
        miscListener = new MiscListener(this);
        creativeSelectionListener = new CreativeSelectionListener(this);
        displayListener = new DisplayListener(this);
        guiListener = new ShopGUIListener(this);

        //TODO set all config defaults here
        //config.setDefaults();

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

        //TODO in the future read the Skull Texture for display item directly from a value in the config file instead of its own serialized item file
//        ItemStack gambleDisplayItem = new ItemStack(Material.PLAYER_HEAD);
//        SkullMeta gambleDisplayItemMeta = (SkullMeta) gambleDisplayItem.getItemMeta();
//        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
//
//        profile.getProperties().put("textures", new Property("texture", headTextureID));
//        try {
//            Field profileField = gambleDisplayItemMeta.getClass().getDeclaredField("profile");
//            profileField.setAccessible(true);
//            profileField.set(gambleDisplayItemMeta, profile);
//        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
//            e.printStackTrace();
//        }
//        gambleDisplayItem.setItemMeta(gambleDisplayItemMeta);


        shopMessage = new ShopMessage(this);
        itemNameUtil = new ItemNameUtil();
        priceUtil = new PriceUtil();

        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                this.getShopLogger().severe("[Shop] Data folder could not be created!");
            }
        }

        allowCreateMethodSign = config.getBoolean("creationMethod.placeSign");
        allowCreateMethodChest = config.getBoolean("creationMethod.hitChest");

        usePerms = config.getBoolean("usePermissions");
        if (usePerms) {
            this.getShopLogger().info("Permissions enabled, Shop will respect player permissions");
        } else {
            this.getShopLogger().info("Permissions disabled, everyone will be able to create/use shops by default");
        }
        checkUpdates = config.getBoolean("checkUpdates");
        enableGUI = config.getBoolean("enableGUI");
        hookWorldGuard = config.getBoolean("hookWorldGuard");
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
            this.getShopLogger().warning("Offline purchase notifications are enabled in `config.yml` but DB logging is set to `OFF`. Offline purchase notifications will be disabled.");
            this.getShopLogger().warning("Please set `logging.type` to `FILE` or setup a database in `config.yml` to enable offline purchase notifications.");
            offlinePurchaseNotificationsEnabled = false;
        }

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
            this.getShopLogger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        } catch (Exception e) {
            this.getShopLogger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        } catch (Error e) {
            this.getShopLogger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
            gambleDisplayItem = new ItemStack(Material.DIAMOND);
        }

        if (gambleDisplayItem == null) {
            this.getShopLogger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
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
                this.getShopLogger().info("Shops will use the Vault economy (" + currencyName + ") as currency on the server.");
            } else {
                this.getShopLogger().severe("Unable to connect to Vault Economy! Are both Vault AND an Economy plugin installed?");
                this.getShopLogger().severe("Plugin Disabled: Invalid configuration value `economy.type` config.yml. If you do not wish to use Vault with Shop, make sure to set `economy.type` in the config file to `ITEM`.");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        } else {
            if (itemCurrency == null) {
                this.getShopLogger().severe("Plugin Disabled: Invalid value for `itemCurrencyID` in `config.yml`");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
            this.getShopLogger().info("Shops will use " + itemNameUtil.getName(itemCurrency).toPlainText() + "(s) as the currency on the server.");
        }

        commandHandler = new CommandHandler(this, null, commandAlias, "Base command for the Shop plugin", "/shop", new ArrayList(Arrays.asList(commandAlias)));
        //this.getCommand(commandAlias).setExecutor(new CommandHandler(this));
        //this.getCommand(commandAlias).setTabCompleter(new CommandTabCompleter());
        //this.getCommand(commandAlias).setAliases(new ArrayList<>())

        //check for unregistered enchantments when new MC updates come out
        /*for (Enchantment enchantment : Enchantment.values()){
            if(UtilMethods.getEnchantmentName(enchantment).equals("Unknown")){
                System.out.println("[Shop] warning: unregistered enchantment: "+enchantment.getName());
            }
        }*/

        guiHandler = new ShopGuiHandler(plugin);
        shopHandler = new ShopHandler(plugin);
        if(enableGUI){
            guiHandler.loadIconsAndTitles();
        }
        enderChestHandler = new EnderChestHandler(plugin);
        logHandler = new LogHandler(plugin, config);

        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(shopListener, this);
        getServer().getPluginManager().registerEvents(miscListener, this);
        getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        //only define different listener hooks if the plugins are present on the server
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.getShopLogger().notice("WorldGuard is installed, creating WorldGuard listener");
            this.getShopLogger().helpful("Shop will respect WorldGuard `passthrough`, `build`, and `chest-access` region flags during shop creation!");
            // Store for later
            this.worldGuardExists = true;
            // Check if we want to require `allow-shop: true` to exist on regions
            if(hookWorldGuard){
                this.getShopLogger().helpful("WorldGuard `allow-shop` flag restriction enabled, Shops can only be created in regions with the `allow-shop` flag set!");
                // Register flag for WorldGuard if we are hooking into the flag system
                // Only log, don't re-register flags, we do that in the onLoad function.
//                WorldGuardHook.registerAllowShopFlag();
            } else {
                this.getShopLogger().helpful("WorldGuard `allow-shop` flag restriction is disabled, if you want to only allow shops in regions with the `allow-shop` flag, please set `hookWorldGuard` to `true` in `config.yml`");
            }
        } else {
            this.worldGuardExists = false;
        }

        if(getServer().getPluginManager().getPlugin("Towny") != null && this.hookTowny){
            this.getShopLogger().notice("Towny is installed, Shop will respect Towny!");
        }

        if(getServer().getPluginManager().getPlugin("LWC") != null){
            lwcHookListener = new LWCHookListener(this);
            getServer().getPluginManager().registerEvents(lwcHookListener, this);
            this.getShopLogger().notice("LWC is installed, creating LWC listener");
        }

        if(getServer().getPluginManager().getPlugin("dynmap") != null && dynmapEnabled){
            dynmapHookListener = new DynmapHookListener(this);
            getServer().getPluginManager().registerEvents(dynmapHookListener, this);
            this.getShopLogger().notice("Dynmap is installed, creating Dynmap listener");
        }

        if(getServer().getPluginManager().getPlugin("BlueMap") != null && bluemapEnabled){
            plugin.getShopLogger().notice("BlueMap is installed, starting BlueMap integration");
            // Wait for 2 minutes for BlueMap to become available/boot up, then initialize listener.
            foliaLib.getScheduler().runTimer(task -> {
                // Check if BlueMapAPI classes are available before using them
                if (CompatibilityUtil.hasBlueMapAPI()) {
                    try {
                        // Use reflection to avoid import issues in legacy builds
                        Class<?> blueMapAPIClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
                        java.lang.reflect.Method getInstanceMethod = blueMapAPIClass.getMethod("getInstance");
                        Object apiOptional = getInstanceMethod.invoke(null);
                        
                        // Check if Optional.isPresent()
                        java.lang.reflect.Method isPresentMethod = apiOptional.getClass().getMethod("isPresent");
                        boolean isPresent = (Boolean) isPresentMethod.invoke(apiOptional);
                        
                        if (isPresent) {
                            plugin.getShopLogger().debug("BlueMap is ready, creating BlueMap listener");
                            bluemapHookListener = new BluemapHookListener(plugin);
                            getServer().getPluginManager().registerEvents(bluemapHookListener, plugin);
                            // Make sure we load the markers in case there are shops that BlueMap doesn't know about
                            bluemapHookListener.reloadMarkers(shopHandler);
                            // Mark the task as complete and cancel the timer
                            foliaLib.getScheduler().cancelTask(task);
                        }
                    } catch (Exception e) {
                        plugin.getShopLogger().warning("Failed to initialize BlueMap integration: " + e.getMessage());
                        foliaLib.getScheduler().cancelTask(task);
                    }
                } else {
                    plugin.getShopLogger().warning("BlueMapAPI not available in this Minecraft version - BlueMap integration disabled");
                    foliaLib.getScheduler().cancelTask(task);
                }
            }, 20, 20); // Check every second (20 ticks) until BlueMap is booted
        }

        if(getServer().getPluginManager().getPlugin("BentoBox") != null){
            bentoBoxHookListener = new BentoBoxHookListener(this);
            getServer().getPluginManager().registerEvents(bentoBoxHookListener, this);
            this.getShopLogger().notice("BentoBox is installed, creating BentoBox listener");
        }

        if(getServer().getPluginManager().getPlugin("AdvancedRegionMarket") != null){
            armHookListener = new ARMHookListener(this);
            getServer().getPluginManager().registerEvents(armHookListener, this);
            this.getShopLogger().notice("AdvancedRegionMarket is installed, creating AdvancedRegionMarket listener");
        }

        if(getServer().getPluginManager().getPlugin("PlotSquared") != null){
            plotSquaredHookListener = new PlotSquaredHookListener(this);
            getServer().getPluginManager().registerEvents(plotSquaredHookListener, this);
            this.getShopLogger().notice("PlotSquared is installed, creating PlotSquared listener");
        }

        int bstatsPluginId = 25211;
        Metrics metrics = new Metrics(plugin, bstatsPluginId);
        // transactions would be cool
        // It would also be cool to see the number of items transacted (bought/sold & item currency)
        // I don't think showing vault currency is worth it, since people have vastly different economy scaling
        // It would be worth it to show a pie chart of what economy type is being used!
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
        
        metrics.addCustomChart(new SimplePie("worldguard_enabled", () -> { return String.valueOf(hookWorldGuard); }));
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
            
            // Track ender chests
            boolean hasEnderChest = enabledContainers.contains(Material.ENDER_CHEST);
            valueMap.put("Ender Chests Allowed", hasEnderChest ? 1 : 0);
            valueMap.put("Ender Chests Disabled", hasEnderChest ? 0 : 1);
            
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

        this.getShopLogger().info("Enabled Shop " + this.getDescription().getVersion());

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

        this.getShopLogger().info("Disabled Shop " + this.getDescription().getVersion());
    }

    public void reload(){
        this.getShopLogger().info("Reloading Shop " + this.getDescription().getVersion());

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

        plugin.getShopHandler().removeAllDisplays(null);

        onDisable();
        onEnable();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        this.getShopLogger().notice("Vault is installed, creating Vault integration for Economy support");
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

    public EnderChestHandler getEnderChestHandler(){
        return enderChestHandler;
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

    public boolean hookWorldGuard(){
        return hookWorldGuard;
    }

    public boolean worldGuardExists() { return worldGuardExists; }

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

    public boolean useEnderChests(){
        return enabledContainers.contains(Material.ENDER_CHEST);
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

    public NBTAdapter getNBTAdapter() {
        return nbtAdapter;
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
