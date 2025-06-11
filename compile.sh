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
echo "Building using Java 21 (Java 8 compatibility)..."
echo "--------------------------------------"
sdk use java 21.0.7-amzn
mvn clean compile package -T 2C

if [ $? -eq 0 ]; then
    echo "Build done!"
else
    echo "Build failed!"
    exit 1
fi

rm test/Shop-*.jar
cp target/Shop-*.jar test

echo ""
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo "Available artifacts:"
ls -la test/Shop-*.jar
echo "========================================"
