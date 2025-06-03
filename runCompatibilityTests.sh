#!/bin/sh

# Build the plugin
./compile.sh
# Copy the plugin to the test directory
rm test/Shop-*.jar
cp target/Shop-*.jar test/
# Download the supported versions
./scripts/downloadSupportedVersions.sh
# Run the compatibility tests
./scripts/test-compatibility-simple.sh