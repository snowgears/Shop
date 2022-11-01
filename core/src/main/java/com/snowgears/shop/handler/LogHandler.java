package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.OfflineTransactions;
import com.snowgears.shop.util.ShopActionType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LogHandler {

    private Shop plugin;

    private HikariDataSource dataSource;

    private boolean enabled;

    public LogHandler(Shop plugin, YamlConfiguration shopConfig){
        this.plugin = plugin;
        defineDataSource(shopConfig);

        if(!enabled)
            return;

        try {
            testDataSource();
        } catch (SQLException e){
            e.printStackTrace();
            enabled = false;
            plugin.getLogger().log(Level.WARNING, "Error establishing connection to defined database. Logging will not be used.");
            return;
        }

        try {
            initDb();
        } catch (SQLException e){
            e.printStackTrace();
            enabled = false;
            plugin.getLogger().log(Level.WARNING, "Error initializing tables in database. Logging will not be used.");
            return;
        }
    }

    public void defineDataSource(YamlConfiguration shopConfig){
        String type = shopConfig.getString("logging.type");
        String serverName = shopConfig.getString("logging.serverName");
        String databaseName = shopConfig.getString("logging.databaseName");
        int port = shopConfig.getInt("logging.port");
        String username = shopConfig.getString("logging.user");
        String password = shopConfig.getString("logging.password");

       List<String> connectionProperties = shopConfig.getStringList("logging.properties");

        this.enabled = enabled;

        if(type.equalsIgnoreCase("OFF")) {
            this.enabled = false;
            return;
        }
        if(type.equalsIgnoreCase("MYSQL")){
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
        }
        else{ //type.equalsIgnoreCase("MARIADB")
            HikariConfig config = new HikariConfig();
            config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
            config.addDataSourceProperty("serverName", serverName);
            config.addDataSourceProperty("portNumber", port);
            config.addDataSourceProperty("databaseName", databaseName);
            config.addDataSourceProperty("user", username);
            config.addDataSourceProperty("password", password);
            config.setLeakDetectionThreshold(10000);
            config.setMaximumPoolSize(10);

            dataSource = new HikariDataSource(config);
        }
        this.enabled = true;
    }

    public boolean logAction(Player player, AbstractShop shop, ShopActionType actionType){
        if(!enabled)
            return false;

        Connection conn;
        try {
            conn = dataSource.getConnection();
            try {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_action(ts, player_uuid, owner_uuid, player_action, shop_world, shop_x, shop_y, shop_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?);");
                stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
                stmt.setString(2, player.getUniqueId().toString());
                if(shop.getOwnerUUID().equals(plugin.getShopHandler().getAdminUUID()))
                    stmt.setString(3, "admin");
                else
                    stmt.setString(3, shop.getOwnerUUID().toString());
                stmt.setString(4, actionType.toString());
                stmt.setString(5, shop.getSignLocation().getWorld().getName());
                stmt.setInt(6, shop.getSignLocation().getBlockX());
                stmt.setInt(7, shop.getSignLocation().getBlockY());
                stmt.setInt(8, shop.getSignLocation().getBlockZ());
                execute(stmt);
                return true;
            } catch (SQLException e){
                plugin.getLogger().log(Level.WARNING, "SQL error occurred while trying to log player action.");
                e.printStackTrace();
                conn.close();
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,"SQL error occurred while trying to log player action.");
            e.printStackTrace();
            return false;
        }
    }

    public void logTransaction(Player player, AbstractShop shop, ShopType transactionType, double price, int amount){
        if(!enabled)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Connection conn;
                try {
                    conn = dataSource.getConnection();
                    try {
                        PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_transaction (t_type, price, amount, item, barter_item) VALUES(?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, transactionType.toString().toUpperCase());
                        stmt.setDouble(2, price);
                        stmt.setInt(3, amount);
                        stmt.setString(4, shop.getItemStack().getType().toString());
                        if (shop.getSecondaryItemStack() != null)
                            stmt.setString(5, shop.getSecondaryItemStack().getType().toString());
                        else
                            stmt.setNull(5, Types.VARCHAR);

                        stmt.execute();

                        ResultSet keys = stmt.getGeneratedKeys();
                        keys.next();
                        int transactionID = keys.getInt(1);
                        stmt.close();

                        stmt = conn.prepareStatement("INSERT INTO shop_action(ts, player_uuid, owner_uuid, player_action, transaction_id, shop_world, shop_x, shop_y, shop_z) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?);");
                        stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
                        stmt.setString(2, player.getUniqueId().toString());
                        if(shop.getOwnerUUID().equals(plugin.getShopHandler().getAdminUUID()))
                            stmt.setString(3, "admin");
                        else
                            stmt.setString(3, shop.getOwnerUUID().toString());
                        stmt.setString(4, ShopActionType.TRANSACT.toString());
                        stmt.setInt(5, transactionID);
                        stmt.setString(6, shop.getSignLocation().getWorld().getName());
                        stmt.setInt(7, shop.getSignLocation().getBlockX());
                        stmt.setInt(8, shop.getSignLocation().getBlockY());
                        stmt.setInt(9, shop.getSignLocation().getBlockZ());
                        stmt.execute();
                        conn.close();
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING,"SQL error occurred while trying to log transaction.");
                        e.printStackTrace();
                        conn.close();
                        return;
                    }
                } catch(SQLException e){
                    plugin.getLogger().log(Level.WARNING,"SQL error occurred while trying to log transaction.");
                    e.printStackTrace();
                    return;
                }
            }
        });
    }

    public void calculateOfflineTransactions(OfflineTransactions offlineTransactions){
        if(!enabled) {
            offlineTransactions.setIsCalculating(false);
            return;
        }
        offlineTransactions.setIsCalculating(true);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                String query = "SELECT * from shop_action RIGHT JOIN shop_transaction on shop_action.transaction_id = shop_transaction.id where owner_uuid=? and ts > ?;";

                Connection conn;
                try {
                    conn = dataSource.getConnection();
                    try {
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setString(1, offlineTransactions.getPlayerUUID().toString());
                        //stmt.setString(1, "admin");
                        stmt.setTimestamp(2, new Timestamp(offlineTransactions.getLastPlayed()));
                        stmt.execute();

                        ResultSet resultSet = stmt.getResultSet();
                        int size = 0;
                        if(resultSet != null){
                            while(resultSet.next()){
                                size++;
                            }
                        }

                        conn.close();
                        stmt.close();

                        //TODO also calculate profits in the future or just save whole resultset in offlinetransactions object and print
                        offlineTransactions.setNumTransactions(size);
                        offlineTransactions.setIsCalculating(false);
                        return;
                    } catch (SQLException e){
                        plugin.getLogger().log(Level.WARNING,"SQL error occurred while trying to get offline transactions.");
                        e.printStackTrace();
                        conn.close();
                        offlineTransactions.setIsCalculating(false);
                        return;
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING,"SQL error occurred while trying to get offline transactions.");
                    e.printStackTrace();
                    offlineTransactions.setIsCalculating(false);
                    return;
                }
            }
        });
    }

    private void testDataSource() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                enabled = false;
                conn.close();
                plugin.getLogger().log(Level.WARNING, "Could not establish database connection.");
                throw new SQLException("Could not establish database connection.");
            }
            else{
                enabled = true;
                plugin.getLogger().log(Level.INFO,"Established connection to database. Logging is enabled.");
                conn.close();
            }
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
            plugin.getLogger().log(Level.WARNING,"Could not read db setup file.");
            return;
        }
        // Mariadb can only handle a single query per statement. We need to split at ;.
        String[] queries = setup.split(";");

        Connection conn = dataSource.getConnection();
        // execute each query to the database.
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            //if (query.isBlank())) continue;
            if (query.isEmpty()) continue;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();
        }
        conn.close();
        plugin.getLogger().log(Level.INFO,"Successfully initialized database.");
    }

    public void execute(final PreparedStatement preparedStatement) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    preparedStatement.execute();

                    preparedStatement.getConnection().close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isEnabled() {
        return enabled;
    }
}
