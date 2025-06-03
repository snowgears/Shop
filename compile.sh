#!/bin/sh

echo "========================================"
echo "Building Shop Plugin - Dual Build System"
echo "========================================"

export MAVEN_OPTS="-Xms2g -Xmx4g"

echo ""
echo "Building MODERN version (Java 17)..."
echo "--------------------------------------"
mvn clean compile package -T 2C -P modern

if [ $? -eq 0 ]; then
    echo "✓ Modern build completed successfully!"
    if [ -f "target/Shop-*.jar" ]; then
        echo "✓ Modern artifact: $(ls target/Shop-*.jar | grep -v legacy)"
    fi
else
    echo "✗ Modern build failed!"
    exit 1
fi

echo ""
echo "Building LEGACY version (Java 8)..."
echo "-------------------------------------"
mvn clean compile package -T 2C -P legacy

if [ $? -eq 0 ]; then
    echo "✓ Legacy build completed successfully!"
    if [ -f "target/Shop-*-legacy.jar" ]; then
        echo "✓ Legacy artifact: $(ls target/Shop-*-legacy.jar)"
    fi
else
    echo "✗ Legacy build failed!"
    exit 1
fi

echo ""
echo "========================================"
echo "Both builds completed successfully!"
echo "========================================"
echo "Available artifacts:"
ls -la target/Shop-*.jar
echo "========================================"
