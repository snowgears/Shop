package com.snowgears.shop.handler;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

public class LogHandler {

    private Shop plugin;
    private boolean dbEnabled;
    private String dbName;
    private String dbHost;
    private int dbPort;
    private String dbUsername;
    private String dbPassword;
    private boolean dbSSL;

    private boolean historyNotifyUser;
    private int historyMaxRows;

    private HashMap<String, Boolean> logTypeMap = new HashMap<>();
    private MysqlConnectionPoolDataSource dataSource;

    public LogHandler(Shop plugin, YamlConfiguration config){
        this.plugin = plugin;

        loadSettings(config);

        if(!this.dbEnabled){
            return;
        }
        defineDataSource();
        try {
            testDataSource();
        } catch (SQLException e){
            e.printStackTrace();
            System.out.println("[Shop] Error establishing connection to defined database. Logging will not be used.");
            dbEnabled = false;
            return;
        }

        try {
            initDb();
        } catch (SQLException e){
            e.printStackTrace();
            System.out.println("[Shop] Error initializing tables in database. Logging will not be used.");
            dbEnabled = false;
            return;
        }
    }

    public void defineDataSource(){
        dataSource = new MysqlConnectionPoolDataSource();

        dataSource.setServerName(dbHost);
        dataSource.setPortNumber(dbPort);
        dataSource.setDatabaseName(dbName);
        dataSource.setUser(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setUseSSL(dbSSL);
    }

    public boolean log(Player player, AbstractShop shop, ShopType shopType, ShopActionType actionType){
        if(shouldLog(shopType, actionType)){

            try {
                Connection conn = dataSource.getConnection();
                try {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_action(ts, player_uuid, shop_location, player_action) VALUES(?, ?, ?, ?);");
                    stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
                    stmt.setString(2, player.getUniqueId().toString());
                    stmt.setString(3, UtilMethods.getCleanLocation(shop.getSignLocation(), true));
                    stmt.setString(4, actionType.toString());
                    stmt.execute();
                    stmt.close();
                    conn.close();
                    return true;
                } catch (SQLException e){
                    System.out.println("[Shop] SQL error occurred while trying to log player action.");
                    e.printStackTrace();
                    return false;
                }
            } catch (SQLException e) {
                System.out.println("[Shop] SQL error occurred while trying to log player action.");
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private boolean shouldLog(ShopType shopType, ShopActionType logType){
        if(!dbEnabled)
            return false;
        return logTypeMap.get(shopType.toString().toLowerCase()+"_"+logType.toString().toLowerCase());
    }

    private void loadSettings(YamlConfiguration config){
        this.dbEnabled = config.getBoolean("database.enabled");
        this.dbName = config.getString("database.name");
        this.dbHost = config.getString("database.host");
        this.dbPort = config.getInt("database.port");
        this.dbUsername = config.getString("database.username");
        this.dbPassword = config.getString("database.password");
        this.dbSSL = config.getBoolean("database.ssl");

        this.historyNotifyUser = config.getBoolean("transaction_history.notify_user_on_join");
        this.historyMaxRows = config.getInt("transaction_history.max_rows");

        boolean value;
        for(ShopType shopType : ShopType.values()){
            for(ShopActionType logType : ShopActionType.values()){
                value = config.getBoolean("logging."+shopType.toString()+"."+logType.toString().toLowerCase());
                logTypeMap.put(shopType.toString().toLowerCase()+"_"+logType.toString().toLowerCase(), value);
            }
        }
    }

    private void testDataSource() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
            else{
                System.out.println("[Shop] Established connection to database. Logging is enabled.");
            }
        }
    }

    private void initDb() throws SQLException {
        // first lets read our setup file.
        // This file contains statements to create our inital tables.
        // it is located in the resources.
        String setup;
        try (InputStream in = plugin.getResource("dbsetup.sql")) { //TODO this is throwing an error. cannot access class jdk.xml.internal.SecuritySupport (in module java.xml) because module java.xml does not export jdk.xml.internal to unnamed module
//            // Java 9+ way
//            setup = new String(in.readAllBytes());
            // Legacy way
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[Shop] Could not read db setup file.");
            this.dbEnabled = false;
            return;
        }
        // Mariadb can only handle a single query per statement. We need to split at ;.
        String[] queries = setup.split(";");
        // execute each query to the database.
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            //if (query.isBlank())) continue;
            if (query.isEmpty()) continue;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            }
        }
        System.out.println("[Shop] Successfully initialized database.");
    }
}
