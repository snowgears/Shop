#!/bin/sh

echo "========================================"
echo "Running Shop Plugin Compatibility Tests"
echo "========================================"

# Build both versions of the plugin
./compile.sh

echo ""
echo "Setting up compatibility tests..."
echo "--------------------------------------"

# Clean up old plugin jars from test directory
rm -f test/Shop-*.jar

# For legacy Minecraft versions (1.8-1.16), use the legacy build
# For modern Minecraft versions (1.17+), use the modern build
echo "Copying legacy build for old Minecraft version testing..."
if [ -f "target/Shop-*-legacy.jar" ]; then
    cp target/Shop-*-legacy.jar test/Shop-legacy.jar
    echo "✓ Legacy build copied to test directory"
else
    echo "✗ Legacy build not found!"
    exit 1
fi

echo "Copying modern build for current Minecraft version testing..."
if [ -f target/Shop-*.jar ] && [ ! -f target/Shop-*-legacy.jar ]; then
    # This will match the modern build (without -legacy suffix)
    cp target/Shop-1*.jar test/Shop-modern.jar 2>/dev/null || cp target/Shop-*.jar test/Shop-modern.jar
    echo "✓ Modern build copied to test directory"
else
    # Find modern build (non-legacy)
    modern_jar=$(ls target/Shop-*.jar | grep -v legacy | head -1)
    if [ -n "$modern_jar" ]; then
        cp "$modern_jar" test/Shop-modern.jar
        echo "✓ Modern build copied to test directory"
    else
        echo "✗ Modern build not found!"
        exit 1
    fi
fi

# Download the supported versions
echo ""
echo "Downloading supported Minecraft versions..."
echo "--------------------------------------"
./scripts/downloadSupportedVersions.sh

# Run the compatibility tests
echo ""
echo "Running compatibility tests..."
echo "--------------------------------------"
./scripts/test-compatibility-simple.sh