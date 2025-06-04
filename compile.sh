#!/bin/sh

# Source SDKMAN if available
if [[ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

echo "========================================"
echo "Building Shop Plugin - Dual Build System"
echo "========================================"

export MAVEN_OPTS="-Xms2g -Xmx4g"

echo ""
echo "Building MODERN version (Java 17)..."
echo "--------------------------------------"
sdk use java 21.0.7-amzn
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

cp target/Shop-*.jar test

echo ""
echo "Building LEGACY version (Java 8)..."
echo "-------------------------------------"
sdk use java 8.0.452-amzn
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


cp target/Shop-*-legacy.jar test

echo ""
echo "========================================"
echo "Both builds completed successfully!"
echo "========================================"
echo "Available artifacts:"
ls -la test/Shop-*.jar
echo "========================================"
