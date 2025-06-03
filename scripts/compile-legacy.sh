#!/bin/bash

# compile-legacy.sh - Safe wrapper for legacy Maven compilation
# This script manages file movement and ensures files are always restored

set -e  # Exit on error (except where we handle it)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create unique backup directory using process ID
BACKUP_DIR="/tmp/shop-legacy-backup-$$"
CORE_SRC_DIR="core/src/"

echo -e "${BLUE}=== Safe Legacy Compilation Wrapper ===${NC}"
echo -e "${BLUE}This script manages file movement and ensures restoration${NC}"
echo ""

# Files that need to be moved during legacy compilation
# These files have legacy-specific versions in core/src/legacy/java
PROBLEMATIC_FILES=(
    "main/java/com/snowgears/shop/hook/PlotSquaredHookListener.java"
    "main/java/com/snowgears/shop/hook/BluemapHookListener.java"
    "main/java/com/snowgears/shop/util/NMSBullshitHandler.java"
    "main/java/com/snowgears/shop/hook/BentoBoxHookListener.java"
    "main/java/com/snowgears/shop/display/Display.java"
    "main/java/com/snowgears/shop/listener/CreativeSelectionListener.java"
    "test/java/com/snowgears/shop/util/PriceNegotiatorTest.java"
    "test/java/com/snowgears/shop/util/UtilMethodsTest.java"
    "test/java/com/snowgears/shop/util/ShopMessageTest.java"
)

# Function to move files to backup
backup_files() {
    echo -e "${YELLOW}[BACKUP] Moving problematic files to backup directory...${NC}"
    
    # Create backup directory
    mkdir -p "$BACKUP_DIR"
    
    local moved_count=0
    for file in "${PROBLEMATIC_FILES[@]}"; do
        local src_path="$CORE_SRC_DIR/$file"
        if [ -f "$src_path" ]; then
            local backup_path="$BACKUP_DIR/$file"
            # Create directory structure in backup
            mkdir -p "$(dirname "$backup_path")"
            # Move file to backup
            mv "$src_path" "$backup_path"
            moved_count=$((moved_count + 1))
            echo -e "${YELLOW}  Moved: $file${NC}"
        fi
    done
    
    echo -e "${YELLOW}[BACKUP] Moved $moved_count files to backup${NC}"
}

# Function to restore files from backup
restore_files() {
    echo -e "${YELLOW}[RESTORE] Restoring files from backup...${NC}"
    
    if [ ! -d "$BACKUP_DIR" ]; then
        echo -e "${YELLOW}[RESTORE] No backup directory found (this is normal)${NC}"
        return 0
    fi
    
    local restored_count=0
    for file in "${PROBLEMATIC_FILES[@]}"; do
        local backup_path="$BACKUP_DIR/$file"
        if [ -f "$backup_path" ]; then
            local src_path="$CORE_SRC_DIR/$file"
            # Create directory structure if needed
            mkdir -p "$(dirname "$src_path")"
            # Move file back
            mv "$backup_path" "$src_path"
            restored_count=$((restored_count + 1))
            echo -e "${YELLOW}  Restored: $file${NC}"
        fi
    done
    
    # Clean up backup directory
    rm -rf "$BACKUP_DIR"
    echo -e "${YELLOW}[RESTORE] Restored $restored_count files and cleaned up backup${NC}"
}

# Function to cleanup on exit (success or failure)
cleanup() {
    local exit_code=$?
    echo ""
    echo -e "${BLUE}=== Cleanup Phase ===${NC}"
    restore_files
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ Build completed successfully and files restored${NC}"
    else
        echo -e "${RED}❌ Build failed but files have been safely restored${NC}"
        echo -e "${YELLOW}💡 You can now retry the build - working directory is clean${NC}"
    fi
    
    exit $exit_code
}

# Set up trap to always restore files on exit
trap cleanup EXIT

# Step 1: Backup problematic files
backup_files

echo ""
echo -e "${BLUE}[BUILD] Starting Maven legacy compilation...${NC}"
echo ""

# Step 2: Run Maven compilation (without AntRun file management)
mvn clean compile package -T 2C -P legacy

echo ""
echo -e "${GREEN}✅ Compilation completed successfully!${NC}" 