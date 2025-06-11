/**
 * =============================================================================
 *                                ShopLogger Class
 * =============================================================================
 *
 * The `ShopLogger` class extends the standard {@link Logger} provided by Bukkit,
 * offering a customized logging system tailored specifically for Shop
 * This enhanced logger introduces additional log levels to provide
 * server operators and developers with granular control over the verbosity and
 * detail of log outputs, facilitating effective monitoring and debugging.
 *
 * ## Logging Levels
 *
 * The logger defines the following custom log levels, in addition to the standard
 * Java logging levels (`SEVERE`, `WARNING`, `INFO`), ordered from highest to
 * lowest severity:
 *
 * 1. **SEVERE**
 *    - **Purpose**: Captures critical errors that prevent the plugin from
 *      functioning correctly.
 *    - **Use Cases**:
 *      - Plugin failed to initialize due to missing dependencies.
 *      - Critical data corruption or inability to access essential resources.
 *
 * 2. **WARNING**
 *    - **Purpose**: Highlights potential issues that may not immediately affect
 *      functionality but could lead to problems if unaddressed.
 *    - **Use Cases**:
 *      - Attempt to create a shop with invalid parameters.
 *      - Deprecated API usage or conflicts with other plugins.
 *
 * 3. **INFO**
 *    - **Purpose**: Provides general informational messages about the plugin's
 *      operations, typically logged during startup and shutdown.
 *    - **Use Cases**:
 *      - *"ChestShop plugin enabled successfully."*
 *      - *"Loaded 25 shops from the database."*
 *
 * 4. **NOTICE**
 *    - **Purpose**: Records significant but non-critical events that operators
 *      might want to monitor.
 *    - **Use Cases**:
 *      - A shop has been successfully created or deleted.
 *      - Major configuration changes applied.
 *
 * 5. **HELPFUL**
 *    - **Purpose**: Logs important in-game events related to shop operations,
 *      aiding in tracking player interactions and transactions.
 *    - **Use Cases**:
 *      - *"Transaction: Player A sold 5 diamonds to Player B for 100 coins."*
 *      - *"Player 'Steve' created a new shop at location (100, 64, 200)."*
 *
 * 6. **DEBUG**
 *    - **Purpose**: Provides detailed information useful for debugging specific
 *      components or functionalities within the plugin.
 *    - **Use Cases**:
 *      - *"Processing transaction: Player A buys 10 apples from Player B."*
 *      - *"Loading shop data for player 'Alex'."*
 *
 * 7. **TRACE**
 *    - **Purpose**: Offers an intermediary level of verbosity between `DEBUG` and
 *      `SPAM`, capturing step-by-step execution details without flooding the logs.
 *    - **Use Cases**:
 *      - *"Entering method `createShop` with parameters: player=Steve, location=(100,64,200)."*
 *      - *"Validating shop parameters for player Alex."*
 *
 * 8. **SPAM**
 *    - **Purpose**: The most verbose log level, intended for deep diagnostics
 *      and capturing every minor operation or event.
 *    - **Use Cases**:
 *      - *"Player 'Steve' accessed shop inventory at (100, 64, 200)."*
 *      - *"Detailed player command processing logs."*
 *
 * ## Configuration
 *
 * Server operators can configure the desired log level via the `config.yml` file:
 *
 * ```yaml
 * logging:
 *   level: INFO  # Options: SEVERE, WARNING, INFO, NOTICE, HELPFUL, DEBUG, TRACE, SPAM
 * ```
 *
 * Setting a higher log level (e.g., `INFO`) ensures that only the most critical
 * items are logged, while lower levels (e.g., `SPAM`) provide exhaustive
 * details for in-depth troubleshooting.
 *
 * ## Best Practices
 *
 * - **Consistent Usage**: Maintain consistency in log level assignments across
 *   the plugin to facilitate easier log management and interpretation.
 */
package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopLogger extends Logger {

    Plugin plugin;

    // Custom log levels - Note: must be greater than INFO (800) to show up!
    public static final Level NOTICE = new Level("NOTICE", 700) {};
    public static final Level HELPFUL = new Level("HELPFUL", 600) {};
    public static final Level DEBUG = new Level("DEBUG", 500) {};
    public static final Level TRACE = new Level("TRACE", 400) {};
    public static final Level SPAM = new Level("SPAM", 300) {};
    public static final Level HYPER = new Level("HYPER", 100) {};

    // ANSI color codes for console output
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String INTENSE_RED    = "\u001B[91m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN  = "\u001B[32m";
    private static final String BLUE   = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN   = "\u001B[36m";
    private static final String SOFT_PURPLE   = "\u001B[38;5;141m";
    private static final String CASH_GREEN   = "\u001B[38;5;40m";
    private static final String CASH_GREEN_BACKGROUND   = "\u001B[48;5;22m";
    private static final String INTENSE_YELLOW   = "\u001B[93m"; // Intense Yellow
    private static final String INTENSE_CYAN   = "\u001B[96m"; // Intense Yellow
    private static final String WHITE  = "\u001B[37m";
    private static final String INTENSE_WHITE  = "\u001B[97m";

    private static final String GREY  = "\u001B[38;5;250m";
    private static final String DIM_GREY  = "\u001B[38;5;242m";
    private static final String VERY_DIM_GREY  = "\u001B[38;5;237m";
    private static final String VERY_VERY_DIM_GREY  = "\u001B[38;5;235m";
    private static final String ALMOST_BLACK  = "\u001B[38;5;233m";

    private boolean enableColor = true;

    public ShopLogger(@NotNull Plugin context, boolean colorEnabled) {
        super(!colorEnabled ? context.getDescription().getName() : INTENSE_WHITE + context.getDescription().getName() + RESET, null);
        plugin = context;
        enableColor = colorEnabled;
        
        setParent(context.getLogger());
        setLevel(Level.ALL);
    }

    public boolean isLevelEnabled(Level level) {
        return this.getLogLevel().intValue() <= level.intValue();
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message) {

        if (this.getLogLevel().intValue() > level.intValue()) { return; }
        // Add color formatting & reroute messages
        if (level == Level.SEVERE) { severe(message); }
        else if (level == Level.WARNING) { warning(message); }
        else if (level == Level.INFO) { info(message); }
        else if (level == NOTICE) { notice(message); }
        else if (level == HELPFUL) { helpful(message); }
        else if (level == DEBUG) { debug(message); }
        else if (level == TRACE) { trace(message); }
        else if (level == SPAM) { spam(message); }
        else if (level == HYPER) { hyper(message); }
        // Catch some custom unknown log level
        else super.getParent().log(level, message);
    }

    public void logFilterLevel(Level level, String message) {
        if (this.getLogLevel().intValue() > level.intValue()) { return; }
        super.getParent().log(Level.INFO, message);
    }

    public String addColor(String color, String message) {
        if (!enableColor) return message;
        return ChatColor.translateAlternateColorCodes('&',color + message + RESET);
    }

    // Normal Logging functions
    public void severe(String message) { super.getParent().log(Level.SEVERE, addColor(BOLD + INTENSE_RED, message)); }
    public void warning(String message) { super.getParent().log(Level.WARNING, addColor(BOLD + INTENSE_YELLOW, message)); }
    public void info(String message) { super.getParent().log(Level.INFO, addColor(YELLOW, message)); }
    // Additional Log Levels
    public void notice(String message) { 
        logFilterLevel(NOTICE, addColor(BLUE, "[Notice] " + message)); 
    }
    
    public void helpful(String message) { 
        logFilterLevel(HELPFUL, addColor(CYAN, "[Helpful] " + message)); 
    }
    
    public void debug(String message) { 
        logFilterLevel(DEBUG, addColor(DIM_GREY, "[Debug] " + message)); 
    }
    
    public void debug(String message, boolean withChatColors) {
        if (this.getLogLevel().intValue() > DEBUG.intValue()) { return; }
        Bukkit.getConsoleSender().sendMessage(INTENSE_WHITE + "[" + plugin.getDescription().getName() + "] " + DIM_GREY + "[Debug] " + message + RESET);
    }
    
    public void trace(String message) { 
        logFilterLevel(TRACE, addColor(VERY_DIM_GREY, "[Trace] " + message)); 
    }
    
    public void spam(String message) { 
        logFilterLevel(SPAM, addColor(VERY_VERY_DIM_GREY, "[Spam] " + message)); 
    }
    
    public void spam(String message, boolean withChatColors) {
        if (this.getLogLevel().intValue() > SPAM.intValue()) { return; }
        Bukkit.getConsoleSender().sendMessage(INTENSE_WHITE + "[" + plugin.getDescription().getName() + "] " + DIM_GREY + "[Spam] " + message + RESET);
    }
    
    public void hyper(String message) { 
        logFilterLevel(SPAM, addColor(ALMOST_BLACK, "[Hyper] " + message)); 
    }

    public void setLogLevel(String level) {
        if (level == null) { setLevel(Level.INFO); return; }
        if (level.equalsIgnoreCase("info") || level.equalsIgnoreCase("normal")) { setLevel(Level.INFO); }
        else if (level.equalsIgnoreCase("notice")) { setLevel(NOTICE); }
        else if (level.equalsIgnoreCase("helpful")) { setLevel(HELPFUL); }
        else if (level.equalsIgnoreCase("debug")) { setLevel(DEBUG); }
        else if (level.equalsIgnoreCase("trace")) { setLevel(TRACE); }
        else if (level.equalsIgnoreCase("spam")) { setLevel(SPAM); }
        else if (level.equalsIgnoreCase("hyper")) { setLevel(HYPER); }
    }

    public Level getLogLevel() {
        return getLevel();
    }

    public void enableColor(boolean enabled) {
        enableColor = enabled;
    }
}
