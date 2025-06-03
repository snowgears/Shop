package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LogHandler {

    private Shop plugin;

    private HikariDataSource dataSource;

    private boolean enabled;

    public LogHandler(Shop plugin, YamlConfiguration shopConfig){
        this.plugin = plugin;
        // Setup database connection and check if we should enable database logging
        defineDataSource(shopConfig);

        if(!enabled)
            return;

        // Force inclusion of the H2 driver class so that it will be compiled into our jar.
        // Without this it just ignores the H2 driver and it gets removed when we minimize our jar!
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            testDataSource();
        } catch (SQLException e){
            e.printStackTrace();
            enabled = false;
            plugin.getShopLogger().log(Level.WARNING, "Error establishing connection to defined database. Logging will not be used.");
            return;
        }

        try {
            initDb();
        } catch (SQLException e){
            e.printStackTrace();
            enabled = false;
            plugin.getShopLogger().log(Level.WARNING, "Error initializing tables in database. Logging will not be used.");
            return;
        }

        plugin.getShopLogger().notice("Shop Database Logging initialized successfully!");
        plugin.getShopLogger().helpful("Offline Purchase Notifications are Enabled!");
    }

    public void defineDataSource(YamlConfiguration shopConfig){
        String type = shopConfig.getString("logging.type");
        String serverName = shopConfig.getString("logging.serverName");
        String databaseName = shopConfig.getString("logging.databaseName");
        int port = shopConfig.getInt("logging.port");
        String username = shopConfig.getString("logging.user");
        String password = shopConfig.getString("logging.password");

       List<String> connectionProperties = shopConfig.getStringList("logging.properties");

        if(type.equalsIgnoreCase("OFF")) {
            this.enabled = false;
            return;
        }

        plugin.getShopLogger().debug("Starting Database (" + type + ") connection to track purchases and Shop actions!");

        if (type.equalsIgnoreCase("MYSQL")) {
            dataSource = new HikariDataSource();

            String jdbcURL = "jdbc:mysql://"+serverName+":"+port+"/"+databaseName;
            for(String property : connectionProperties){
                jdbcURL += "?"+property;
            }

            dataSource.setJdbcUrl(jdbcURL);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setLeakDetectionThreshold(10000);
            dataSource.setMaximumPoolSize(10);
            dataSource.setMaxLifetime(600000);
        }
        else if (type.equalsIgnoreCase("MARIADB")) {
            HikariConfig config = new HikariConfig();
            config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
            config.addDataSourceProperty("serverName", serverName);
            config.addDataSourceProperty("portNumber", port);
            config.addDataSourceProperty("databaseName", databaseName);
            config.addDataSourceProperty("user", username);
            config.addDataSourceProperty("password", password);
            config.setLeakDetectionThreshold(10000);
            config.setMaximumPoolSize(10);
            config.setMaxLifetime(600000);

            dataSource = new HikariDataSource(config);
        }
        else if (type.equalsIgnoreCase("FILE")) {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.h2.Driver");
            String jdbcURL = "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/data/" + databaseName + ";MODE=MySQL";
            config.setJdbcUrl(jdbcURL);
            config.setUsername(username != null ? username : "sa");
            config.setPassword(password != null ? password : "");
            config.setLeakDetectionThreshold(10000);
            config.setMaximumPoolSize(10);
            config.setMaxLifetime(600000);

            dataSource = new HikariDataSource(config);
        }
        else {
            plugin.getShopLogger().log(Level.WARNING, "Unsupported database type! Please check your `config.yml` file! type: " + type);
            this.enabled = false;
            return;
        }

        this.enabled = true;
    }

    public void logAction(Player player, AbstractShop shop, ShopActionType actionType) {
        if (actionType == ShopActionType.INIT) {
            plugin.getShopLogger().notice(
                    player.getName() + " created a " + shop.getType().name().toUpperCase() + " shop at ("
                            + "x: " + shop.getChestLocation().getBlockX() + " y: " + shop.getChestLocation().getBlockY() + " z: " + shop.getChestLocation().getBlockZ()
                            + ") item: " + ChatColor.stripColor(new ItemNameUtil().getName(shop.getItemStack()).toPlainText())
                            + (shop.getSecondaryItemStack() != null ? " barterItem: " + ChatColor.stripColor(new ItemNameUtil().getName(shop.getSecondaryItemStack()).toPlainText()) : "")
            );
        }
        if (actionType == ShopActionType.DESTROY) {
            plugin.getShopLogger().notice(
                    player.getName() + " destroyed a " + shop.getType().name().toUpperCase() + " shop at ("
                            + "x: " + shop.getChestLocation().getBlockX() + " y: " + shop.getChestLocation().getBlockY() + " z: " + shop.getChestLocation().getBlockZ()
                            + ") item: " + ChatColor.stripColor(new ItemNameUtil().getName(shop.getItemStack()).toPlainText())
                            + (shop.getSecondaryItemStack() != null ? " barterItem: " + ChatColor.stripColor(new ItemNameUtil().getName(shop.getSecondaryItemStack()).toPlainText()) : "")
            );
        }

        if (!this.enabled) return;
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            // Connect to datasource & create statement in "try" to handle automatically closing the connection!
            try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_action(ts, player_uuid, owner_uuid, shop_uuid, player_action, shop_world, shop_x, shop_y, shop_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?);");
            ){
                stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
                stmt.setString(2, player.getUniqueId().toString());
                if (shop.getOwnerUUID().equals(plugin.getShopHandler().getAdminUUID()))
                    stmt.setString(3, "admin");
                else
                    stmt.setString(3, shop.getOwnerUUID().toString());
                String shop_uuid = "";
                if (shop.getId() != null && shop.getId().toString() != null) shop_uuid = shop.getId().toString();
                stmt.setString(4, shop_uuid);
                stmt.setString(5, actionType.toString());
                stmt.setString(6, shop.getSignLocation().getWorld().getName());
                stmt.setInt(7, shop.getSignLocation().getBlockX());
                stmt.setInt(8, shop.getSignLocation().getBlockY());
                stmt.setInt(9, shop.getSignLocation().getBlockZ());
                stmt.execute();
            } catch (SQLException e) {
                plugin.getShopLogger().log(Level.WARNING, "SQL error occurred while trying to log player action.");
                e.printStackTrace();
            }
        });
    }

    public void logTransaction(Player player, AbstractShop shop, ShopType transactionType, double price, int amount){
        plugin.getShopLogger().helpful(
            "Shop " + shop.getType().name().toUpperCase() + " from/to " + player.getName() + ": "
                + ChatColor.stripColor(new ItemNameUtil().getName(shop.getItemStack()).toPlainText()) + "(x" + amount + ")" + " for " + plugin.getPriceString(price, true)
                + " | Shop owned by " + shop.getOwnerName() + " at (x: " + shop.getChestLocation().getBlockX() + " y: " + shop.getChestLocation().getBlockY() + " z: " + shop.getChestLocation().getBlockZ() + ")"
        );

        try {
            boolean isBarterOrItemCurrency = transactionType == ShopType.BARTER || plugin.getCurrencyType() == CurrencyType.ITEM;
            txMetrics.addTransaction(amount, price, isBarterOrItemCurrency);
        } catch (Exception e) { /* Ignore errors, added for safety. */ }

        if(!enabled) return;
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            // Log the Transaction that occured
            int transactionID = 0;
            // Connect to datasource & create statement in "try" to handle automatically closing the connection!
            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement logTxStmt = conn.prepareStatement("INSERT INTO shop_transaction (t_type, price, amount, item, barter_item) VALUES(?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            ) {
                logTxStmt.setString(1, transactionType.toString().toUpperCase());
                logTxStmt.setDouble(2, price);
                logTxStmt.setInt(3, amount);
                logTxStmt.setString(4, UtilMethods.itemStackToBase64(shop.getItemStack()));
                if (shop.getSecondaryItemStack() != null)
                    logTxStmt.setString(5, UtilMethods.itemStackToBase64(shop.getSecondaryItemStack()));
                else
                    logTxStmt.setNull(5, Types.VARCHAR);

                logTxStmt.execute();
                ResultSet txRS = logTxStmt.getGeneratedKeys();
                txRS.next();
                transactionID = txRS.getInt(1);
            } catch (SQLException e) {
                plugin.getShopLogger().log(Level.WARNING,"SQL error occurred while trying to log transaction/shop action.");
                e.printStackTrace();
            } catch (IOException e) {
                plugin.getShopLogger().log(Level.WARNING,"SQL error occurred while trying to log transaction/shop action. Issue with converting itemstack to base64!");
                e.printStackTrace();
            }

            // Log the action that occured
            if (transactionID == 0) return; // Last query failed, so skip this one!
            // Connect to datasource & create statement in "try" to handle automatically closing the connection!
            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement actionStmt = conn.prepareStatement("INSERT INTO shop_action(ts, player_uuid, owner_uuid, shop_uuid, player_action, transaction_id, shop_world, shop_x, shop_y, shop_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            ) {
                actionStmt.setTimestamp(1, new Timestamp(new Date().getTime()));
                actionStmt.setString(2, player.getUniqueId().toString());
                if(shop.getOwnerUUID().equals(plugin.getShopHandler().getAdminUUID()))
                    actionStmt.setString(3, "admin");
                else
                    actionStmt.setString(3, shop.getOwnerUUID().toString());
                String shop_uuid = "";
                if (shop.getId() != null && shop.getId().toString() != null) shop_uuid = shop.getId().toString();
                actionStmt.setString(4, shop_uuid);
                actionStmt.setString(5, ShopActionType.TRANSACT.toString());
                actionStmt.setInt(6, transactionID);
                actionStmt.setString(7, shop.getSignLocation().getWorld().getName());
                actionStmt.setInt(8, shop.getSignLocation().getBlockX());
                actionStmt.setInt(9, shop.getSignLocation().getBlockY());
                actionStmt.setInt(10, shop.getSignLocation().getBlockZ());
                actionStmt.execute();
            } catch (SQLException e) {
                plugin.getShopLogger().log(Level.WARNING,"SQL error occurred while trying to log transaction/shop action.");
                e.printStackTrace();
            }
        });
    }

    public void calculateOfflineTransactions(OfflineTransactions offlineTransactions){
        if(!enabled) {
            offlineTransactions.setIsCalculating(false);
            return;
        }
        offlineTransactions.setIsCalculating(true);

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            String query = "SELECT * from shop_action RIGHT JOIN shop_transaction on shop_action.transaction_id = shop_transaction.id where owner_uuid=? and ts > ?;";
            // Connect to datasource & create statement in "try" to handle automatically closing the connection!
            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                // Initialize variables to accumulate totals
                double totalProfit = 0.0;
                double totalSpent = 0.0;
                Map<ItemStack, Integer> itemsBought = new HashMap<>();
                Map<ItemStack, Integer> itemsSold = new HashMap<>();

                // Prepare and execute the statement

                stmt.setString(1, offlineTransactions.getPlayerUUID().toString());
                stmt.setTimestamp(2, new Timestamp(offlineTransactions.getLastPlayed()));
                ResultSet resultSet = stmt.executeQuery();

                int size = 0;
                if (resultSet != null) {
                    while (resultSet.next()) {
                        size++;

                        // Extract transaction data
                        String purchaserUUID = resultSet.getString("player_uuid");
                        String tType = resultSet.getString("t_type");
                        double price = resultSet.getDouble("price");
                        int amount = resultSet.getInt("amount");
                        String item = resultSet.getString("item");
                        String barterItem = resultSet.getString("barter_item"); // May be null

                        String shopWorld = resultSet.getString("shop_world");
                        int shopX = resultSet.getInt("shop_x");
                        int shopY = resultSet.getInt("shop_y");
                        int shopZ = resultSet.getInt("shop_z");

                        Location loc = new Location(Bukkit.getWorld(shopWorld), shopX, shopY, shopZ);

                        ItemStack itemstack = UtilMethods.itemStackFromBase64(item);
                        ItemStack barterItemstack = null;

                        // Create item stack to use for the Maps (show all identical items as the same sold row)
                        ItemStack itemCheckClone = itemstack.clone();
                        itemCheckClone.setAmount(1);

                        // Process transactions based on their type
                        if (tType.equalsIgnoreCase("BUY")) {
                            // User spent money to buy items
                            totalSpent += price;
                            int itemsBoughtAmt = itemsBought.getOrDefault(itemCheckClone, 0) + amount;
                            itemsBought.put(itemCheckClone,itemsBoughtAmt);
                        } else if (tType.equalsIgnoreCase("SELL")) {
                            // User earned money by selling items
                            totalProfit += price;
                            int itemsSoldAmt = itemsSold.getOrDefault(itemCheckClone, 0) + amount;
                            itemsSold.put(itemCheckClone, itemsSoldAmt);
                        } else if (tType.equalsIgnoreCase("BARTER")) {
                            int itemsAmt = itemsSold.getOrDefault(itemCheckClone, 0) + amount;
                            itemsSold.put(itemCheckClone, itemsAmt);
                            if (barterItem != null) {
                                // Barter Item is the "currency" item being spent in the transaction
                                barterItemstack = UtilMethods.itemStackFromBase64(barterItem);
                                ItemStack barterItemCheckClone = barterItemstack.clone();
                                barterItemCheckClone.setAmount(1);
                                itemsBought.put(barterItemCheckClone, itemsBought.getOrDefault(barterItemCheckClone, 0) + (int) price);
                            }
                        }

                        OfflinePlayer purchaser = null;
                        if (purchaserUUID != null && !purchaserUUID.isEmpty()) {
                            purchaser = Bukkit.getServer().getOfflinePlayer(UUID.fromString(purchaserUUID));
                        }

                        offlineTransactions.addTx(loc, ShopType.valueOf(tType), price, purchaser, amount, itemstack, barterItemstack);
                    }
                }

                // Update offlineTransactions object with the calculated data
                offlineTransactions.setNumTransactions(size);
                offlineTransactions.setTotalProfit(totalProfit);
                offlineTransactions.setTotalSpent(totalSpent);
                offlineTransactions.setItemsBought(itemsBought);
                offlineTransactions.setItemsSold(itemsSold);
                offlineTransactions.setIsCalculating(false);
            } catch (SQLException e){
                plugin.getShopLogger().log(Level.WARNING,"SQL error occurred while trying to get offline transactions.");
                e.printStackTrace();
                offlineTransactions.setIsCalculating(false);
            } catch (IOException e) {
                plugin.getShopLogger().log(Level.WARNING,"IOException occurred while trying to get offline transactions. Unable to parse itemstack from base64!");
                e.printStackTrace();
                offlineTransactions.setIsCalculating(false);
            } catch (ClassNotFoundException e) {
                plugin.getShopLogger().log(Level.WARNING,"ClassNotFoundException occurred while trying to get offline transactions. Unable to parse itemstack from base64!");
                e.printStackTrace();
                offlineTransactions.setIsCalculating(false);
            }
        });
    }

    private void testDataSource() throws SQLException {
        // Connect to datasource & create statement in "try" to handle automatically closing the connection!
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                enabled = false;
                plugin.getShopLogger().log(Level.WARNING, "Could not establish database connection!");
                plugin.getShopLogger().log(Level.WARNING, "Purchase Database Logging and Offline Purchase notifications are disabled!");
                throw new SQLException("Could not establish database connection.");
            }
            else{
                enabled = true;
                plugin.getShopLogger().debug("Established connection to database.");
            }
        }
    }

    private final TransactionMetrics txMetrics = new TransactionMetrics();

    // Inner class to store metrics for a time bucket
    private static class TransactionMetrics {
        private int transactionCount = 0;
        private int itemVolume = 0;
        
        public void addTransaction(int itemCount, double price, boolean isBarterOrItemCurrency) {
            transactionCount++;
            itemVolume += itemCount;
            // For barter transactions or when currency is items, count the "price" as item volume too
            if (isBarterOrItemCurrency) {
                itemVolume += (int)price;
            }
        }
        
        public void resetTransactionCount() {
            transactionCount = 0;
        }

        public void resetItemVolume() {
            itemVolume = 0;
        }
        
        public int getTransactionCount() {
            return transactionCount;
        }
        
        public int getItemVolume() {
            return itemVolume;
        }
    }

    // Get the number of transactions during the last 30 minutes
    public int getRecentTransactionCount() {
        int count = txMetrics.getTransactionCount();
        txMetrics.resetTransactionCount();
        return count;
    }

    // Get the number of items bought and sold during the last 30 minutes
    public int getRecentItemVolume() {
        try {
            int volume = txMetrics.getItemVolume();
            txMetrics.resetItemVolume();
            return volume;
        } catch (Exception e) {
            plugin.getShopLogger().debug("Error calculating recent item volume");
            return 0; // Return 0 if any error occurs
        }
    }

    private void initDb() throws SQLException {
        // first lets read our setup file.
        // This file contains statements to create our inital tables.
        // it is located in the resources.
        String setup;
        try (InputStream in = plugin.getResource("dbsetup.sql")) { //TODO this is throwing an error. cannot access class jdk.xml.internal.SecuritySupport (in module java.xml) because module java.xml does not export jdk.xml.internal to unnamed module
//            setup = new String(in.readAllBytes()); // Java 9+ way
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n")); // Legacy way
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getShopLogger().log(Level.WARNING,"Could not read db setup file.");
            return;
        }
        // Mariadb can only handle a single query per statement. We need to split at ;.
        String[] queries = setup.split(";");

        // execute each query to the database.
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            //if (query.isBlank())) continue;
            if (query.isEmpty()) continue;
            // Connect to datasource & create statement in "try" to handle automatically closing the connection!
            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.execute();
            }
        }
        plugin.getShopLogger().debug("Successfully initialized database.");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
