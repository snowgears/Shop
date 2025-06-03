#!/bin/bash

# Script to download supported Paper versions for testing
# This ensures all contributors can reproduce the same test environment

# Create the supported_versions directory if it doesn't exist
mkdir -p test/supported_versions

# Change to the supported_versions directory
cd test/supported_versions

# Array of versions and their corresponding build numbers
# Format: "version:build"
versions=(
    "1.8.8:445"
    "1.9.4:775"
    "1.10.2:918"
    "1.11.2:1106"
    "1.12.2:1620"
    "1.13.2:657"
    "1.14.4:245"
    "1.15.2:393"
    "1.16.5:794"
    "1.17.1:411"
    "1.18.2:388"
    "1.19.4:550"
    "1.20.6:151"
    "1.21.4:231"
    "1.21.5:103"
)

echo "Downloading supported Paper versions..."
echo "Target directory: $(pwd)"
echo

# Download each version
for version_build in "${versions[@]}"; do
    # Split version and build
    IFS=':' read -r version build <<< "$version_build"
    
    # Construct filename and URL
    filename="paper-${version}-${build}.jar"
    url="https://api.papermc.io/v2/projects/paper/versions/${version}/builds/${build}/downloads/${filename}"
    
    # Check if file already exists
    if [ -f "$filename" ]; then
        echo "✓ $filename already exists, skipping..."
    else
        echo "⬇ Downloading $filename..."
        if curl -L -o "$filename" "$url"; then
            echo "✓ Successfully downloaded $filename"
        else
            echo "✗ Failed to download $filename"
            exit 1
        fi
    fi
done

echo
echo "All supported Paper versions are now available!"
echo "Downloaded jars are located in: test/supported_versions/" 