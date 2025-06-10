package com.snowgears.shop.util;

import org.bukkit.Bukkit;

/**
 * Utility class for checking Minecraft version compatibility.
 * Provides clean static methods to determine if the current server version
 * meets minimum version requirements for feature compatibility.
 * 
 * Usage:
 * - MCVersion.atLeast("1.12") - returns true if running 1.12+
 * - MCVersion.atLeast("1.19.4") - returns true if running 1.19.4+
 * - MCVersion.getCurrent() - returns current version string like "1.20.4"
 */
public class MCVersion {
    
    private static String currentVersion = null;
    private static int[] currentVersionParts = null;
    private static boolean initialized = false;
    
    /**
     * Checks if the current Minecraft version is at least the specified version.
     * 
     * @param requiredVersion The minimum version required (e.g., "1.12", "1.19.4")
     * @return true if the current version meets or exceeds the required version
     */
    public static boolean atLeast(String requiredVersion) {
        ensureInitialized();
        
        if (currentVersionParts == null) {
            // Fallback if version parsing failed
            return false;
        }
        
        int[] requiredParts = parseVersionString(requiredVersion);
        if (requiredParts == null) {
            // Invalid required version format
            return false;
        }
        
        boolean result = compareVersions(currentVersionParts, requiredParts) >= 0;
        return result;
    }
    
    /**
     * Gets the current Minecraft version as a clean string.
     * 
     * @return Current version string (e.g., "1.20.4") or "unknown" if parsing failed
     */
    public static String getCurrent() {
        ensureInitialized();
        return currentVersion != null ? currentVersion : "unknown";
    }
    
    /**
     * Gets the raw Bukkit version string for debugging purposes.
     * 
     * @return Raw Bukkit version string (e.g., "1.20.4-R0.1-SNAPSHOT")
     */
    public static String getRaw() {
        return Bukkit.getBukkitVersion();
    }
    
    /**
     * Forces re-initialization of version detection (mainly for testing).
     */
    public static void reinitialize() {
        initialized = false;
        currentVersion = null;
        currentVersionParts = null;
        ensureInitialized();
    }
    
    /**
     * Ensures the version detection has been initialized.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            detectCurrentVersion();
            initialized = true;
        }
    }
    
    /**
     * Detects and parses the current Minecraft version from Bukkit.
     */
    private static void detectCurrentVersion() {
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            
            // Extract version from strings like "1.20.4-R0.1-SNAPSHOT"
            String versionPart = extractVersionFromBukkitString(bukkitVersion);
            
            if (versionPart != null) {
                currentVersion = versionPart;
                currentVersionParts = parseVersionString(versionPart);
            }
            
        } catch (Exception e) {
            System.out.println("[Shop] Error detecting Minecraft version: " + e.getMessage());
        }
    }
    
    /**
     * Extracts the Minecraft version from a Bukkit version string.
     * 
     * @param bukkitVersion Raw version string from Bukkit
     * @return Clean version string or null if extraction failed
     */
    private static String extractVersionFromBukkitString(String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            return null;
        }
        
        // Handle format like "1.20.4-R0.1-SNAPSHOT"
        String[] parts = bukkitVersion.split("-");
        if (parts.length > 0) {
            String versionCandidate = parts[0];
            
            // Validate it looks like a version (e.g., "1.20.4")
            if (versionCandidate.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                return versionCandidate;
            }
        }
        
        // Try to extract version with regex as fallback
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(bukkitVersion);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Parses a version string into comparable integer parts.
     * 
     * @param version Version string like "1.20.4"
     * @return Array of version parts [major, minor, patch] or null if invalid
     */
    private static int[] parseVersionString(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = version.split("\\.");
            
            // Support major.minor or major.minor.patch
            if (parts.length < 2 || parts.length > 3) {
                return null;
            }
            
            int[] versionParts = new int[3]; // Always use 3 parts for consistency
            
            // Parse major and minor (required)
            versionParts[0] = Integer.parseInt(parts[0]);
            versionParts[1] = Integer.parseInt(parts[1]);
            
            // Parse patch if present, otherwise default to 0
            versionParts[2] = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            
            return versionParts;
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Compares two version arrays.
     * 
     * @param current Current version parts
     * @param required Required version parts
     * @return Positive if current > required, 0 if equal, negative if current < required
     */
    private static int compareVersions(int[] current, int[] required) {
        // Compare major.minor.patch in order
        for (int i = 0; i < 3; i++) {
            int currentPart = i < current.length ? current[i] : 0;
            int requiredPart = i < required.length ? required[i] : 0;
            
            if (currentPart != requiredPart) {
                return currentPart - requiredPart;
            }
        }
        
        return 0; // Versions are equal
    }
} 