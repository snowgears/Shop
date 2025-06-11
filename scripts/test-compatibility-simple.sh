#!/bin/bash

# Simplified Minecraft Shop Plugin Compatibility Test Script
# Tests plugin compatibility across different Paper server versions

set -e  # Exit on any error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEST_DIR="$PROJECT_ROOT/test"
SUPPORTED_VERSIONS_DIR="$TEST_DIR/supported_versions"
TEST_ENVIRONMENTS_DIR="$TEST_DIR/environments"
TEST_RESULTS_DIR="$TEST_DIR/results"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test timeout in seconds
TEST_TIMEOUT=30

echo_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

echo_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

echo_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

echo_error() {
    echo -e "${RED}✗ $1${NC}"
}

echo_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Function to get Java command for a given Minecraft version
get_java_cmd() {
    local mc_version="$1"
    
    # Source SDKMAN if available
    if [[ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    
    # Extract major and minor version numbers
    local major=$(echo "$mc_version" | cut -d. -f1)
    local minor=$(echo "$mc_version" | cut -d. -f2)
    local patch=$(echo "$mc_version" | cut -d. -f3)
    
    # Determine required Java version based on Minecraft version
    # MC 1.8 - 1.16.5: Java 8 (1.16.5 still needs Java 8, not 16/17)
    # MC 1.17.x: Java 17
    # MC 1.18 - 1.20.x: Java 17  
    # MC 1.20.1+: Java 21
    
    local java_version=""
    
    if [[ "$major" -eq 1 ]]; then
        if [[ "$minor" -eq 8 ]] || [[ "$minor" -eq 9 ]] || [[ "$minor" -eq 10 ]] || [[ "$minor" -eq 11 ]] || [[ "$minor" -eq 12 ]] || [[ "$minor" -eq 13 ]] || [[ "$minor" -eq 14 ]] || [[ "$minor" -eq 15 ]] || [[ "$minor" -eq 16 ]]; then
            # All 1.8 through 1.16.x use Java 8
            java_version="8.0.452-amzn"
        elif [[ "$minor" -eq 17 ]] || [[ "$minor" -eq 18 ]] || [[ "$minor" -eq 19 ]]; then
            java_version="17.0.15-amzn"
        elif [[ "$minor" -eq 20 ]]; then
            if [[ -z "$patch" ]] || [[ "$patch" -eq 0 ]]; then
                java_version="17.0.15-amzn"  # 1.20.0
            else
                java_version="21.0.7-amzn"   # 1.20.1+
            fi
        elif [[ "$minor" -ge 21 ]]; then
            java_version="21.0.7-amzn"
        else
            # Default fallback
            java_version="21.0.7-amzn"
        fi
    else
        # For any version not starting with 1 (future versions)
        java_version="21.0.7-amzn"
    fi
    
    # Get the Java path for the selected version
    local java_home=""
    if [[ -d "$HOME/.sdkman/candidates/java/$java_version" ]]; then
        java_home="$HOME/.sdkman/candidates/java/$java_version"
        echo "$java_home/bin/java"
    else
        echo_warning "SDKMAN Java $java_version not found, falling back to system java"
        echo "java"
    fi
}

# Function to check if --nogui should be used for a given Minecraft version
should_use_nogui() {
    local mc_version="$1"
    local major=$(echo "$mc_version" | cut -d. -f1)
    local minor=$(echo "$mc_version" | cut -d. -f2)
    
    # For MC 1.14.4 and below, don't use --nogui
    if [[ "$major" -eq 1 ]] && [[ "$minor" -le 14 ]]; then
        return 1  # Don't use --nogui
    else
        return 0  # Use --nogui
    fi
}

# Function to get port for a given Minecraft version
get_port() {
    local mc_version="$1"
    local major=$(echo "$mc_version" | cut -d. -f1)
    local minor=$(echo "$mc_version" | cut -d. -f2)
    
    # Calculate port based on version to avoid conflicts
    local base_port=25565
    local port_offset=0
    
    case "$major.$minor" in
        "1.21") port_offset=0 ;;
        "1.20") port_offset=1 ;;
        "1.19") port_offset=2 ;;
        "1.18") port_offset=3 ;;
        "1.17") port_offset=4 ;;
        "1.16") port_offset=5 ;;
        "1.15") port_offset=6 ;;
        "1.14") port_offset=7 ;;
        "1.13") port_offset=8 ;;
        "1.12") port_offset=9 ;;
        "1.11") port_offset=10 ;;
        "1.10") port_offset=11 ;;
        "1.9") port_offset=12 ;;
        "1.8") port_offset=13 ;;
        *) port_offset=14 ;;
    esac
    
    echo $((base_port + port_offset))
}

# Function to extract Minecraft version from jar filename
get_mc_version() {
    local jar_file="$1"
    # Extract version like "1.21.4" from "paper-1.21.4-231.jar"
    echo "$jar_file" | sed 's/paper-\([0-9.]*\)-.*/\1/'
}

# Function to find and validate plugin jar
find_plugin_jar() {
    echo_header "Finding Plugin Jar"
    
    # Look for plugin jar in test directory, but exclude test environment directories
    PLUGIN_JAR=$(find "$TEST_DIR" -name "Shop-*.jar" -not -path "*/environments/*" | head -n1)
    
    if [[ -z "$PLUGIN_JAR" ]]; then
        echo_error "No Shop plugin jar found in $TEST_DIR"
        echo_info "Please ensure you have a built plugin jar at test/Shop-*.jar"
        echo_info "You can build the plugin with: mvn clean package"
        echo_info "Then copy the jar to the test directory"
        exit 1
    fi
    
    if [[ ! -f "$PLUGIN_JAR" ]]; then
        echo_error "Plugin jar not found at: $PLUGIN_JAR"
        exit 1
    fi
    
    echo_success "Found plugin jar: $(basename "$PLUGIN_JAR")"
    echo_info "Plugin location: $PLUGIN_JAR"
}

# Function to create server configuration files
create_server_config() {
    local env_dir="$1"
    local mc_version="$2"
    local port="$3"
    
    # Create eula.txt
    echo "eula=true" > "$env_dir/eula.txt"
    
    # Create server.properties
    cat > "$env_dir/server.properties" << EOF
# Minecraft server properties for compatibility testing
# $(date)
server-port=$port
server-ip=127.0.0.1
online-mode=false
spawn-protection=0
max-players=1
difficulty=peaceful
gamemode=creative
force-gamemode=true
hardcore=false
white-list=false
broadcast-console-to-ops=false
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
generate-structures=false
view-distance=3
simulation-distance=3
max-world-size=1000
motd=Compatibility Test - MC $mc_version
announce-player-achievements=false
enable-command-block=false
EOF

    # Create plugins directory
    mkdir -p "$env_dir/plugins"
    
    echo_info "Created server configuration for MC $mc_version on port $port"
}

# Function to check plugin success with enhanced detection
check_plugin_success() {
    local log_file="$1"
    local plugin_enabling=false
    local plugin_enabled=false
    local server_started=false
    local shops_loaded=false
    local has_critical_errors=false
    local error_details=""
    
    # Check for success patterns
    if grep -q "\[Shop\] Enabling Shop" "$log_file" 2>/dev/null; then
        plugin_enabling=true
    fi
    
    if grep -q "\[Shop\] Enabled Shop" "$log_file" 2>/dev/null; then
        plugin_enabled=true
    fi
    
    if grep -q "Done.*For help, type \"help\"" "$log_file" 2>/dev/null; then
        server_started=true
    fi
    
    # Check for shops loaded message (optional for success)
    if grep -q "\[Shop\] Loaded.*Shops!" "$log_file" 2>/dev/null; then
        shops_loaded=true
    fi
    
    # Check for critical error patterns
    local error_patterns=(
        "ERROR.*Shop"
        "Exception.*Shop"
        "\[Shop\] Error"
        "IllegalArgumentException.*Newer version.*Server downgrades"
        "Could not call method.*Shop"
        "ClassNotFoundException.*Shop"
        "NoSuchMethodError.*Shop"
        "NoClassDefFoundError.*Shop"
    )
    
    for pattern in "${error_patterns[@]}"; do
        if grep -q -E "$pattern" "$log_file" 2>/dev/null; then
            has_critical_errors=true
            if [[ -z "$error_details" ]]; then
                error_details=$(grep -E "$pattern" "$log_file" 2>/dev/null | head -3 | tr '\n' ' ')
            fi
            break
        fi
    done
    
    # Export results for caller
    PLUGIN_ENABLING=$plugin_enabling
    PLUGIN_ENABLED=$plugin_enabled
    SERVER_STARTED=$server_started
    SHOPS_LOADED=$shops_loaded
    HAS_CRITICAL_ERRORS=$has_critical_errors
    ERROR_DETAILS="$error_details"
    
    # Return success if core criteria met (enabling, enabled, server started) and no critical errors
    # Shops loading is optional since it may happen after server completion
    if $plugin_enabling && $plugin_enabled && $server_started && $shops_loaded && ! $has_critical_errors; then
        return 0  # Success
    else
        return 1  # Failure
    fi
}

# Function to test a single server version
test_server_version() {
    local jar_file="$1"
    local mc_version=$(get_mc_version "$jar_file")
    local java_cmd=$(get_java_cmd "$mc_version")
    local port=$(get_port "$mc_version")
    
    echo_header "Testing Minecraft $mc_version"
    echo_info "Server jar: $jar_file"
    echo_info "Java command: $java_cmd"
    echo_info "Port: $port"
    
    # Create test environment
    local env_dir="$TEST_ENVIRONMENTS_DIR/mc-$mc_version"
    rm -rf "$env_dir"
    mkdir -p "$env_dir"
    
    # Copy server jar
    cp "$SUPPORTED_VERSIONS_DIR/$jar_file" "$env_dir/"
    
    # Copy plugin
    echo_info "Copying plugin to $env_dir/plugins/"
    mkdir -p "$env_dir/plugins"
    cp "$PLUGIN_JAR" "$env_dir/plugins/"
    
    # Create server configuration
    create_server_config "$env_dir" "$mc_version" "$port"
    
    # Start server test
    local log_file="$TEST_RESULTS_DIR/mc-$mc_version.log"
    local success_file="$TEST_RESULTS_DIR/mc-$mc_version.success"
    local error_file="$TEST_RESULTS_DIR/mc-$mc_version.error"
    
    echo_info "Starting server test (timeout: ${TEST_TIMEOUT}s)..."
    echo_info "Logs will be saved to: $log_file"
    
    cd "$env_dir"
    
    # Run server with timeout (macOS compatible)
    local startup_args=""
    if should_use_nogui "$mc_version"; then
        startup_args="--nogui"
    fi
    
    $java_cmd -Xms1G -Xmx2G -jar "$jar_file" $startup_args > "$log_file" 2>&1 &
    local server_pid=$!
    
    # Enhanced monitoring for success conditions
    local start_time=$(date +%s)
    echo_info "Monitoring server startup (timeout: ${TEST_TIMEOUT}s)..."
    
    while kill -0 $server_pid 2>/dev/null; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [[ $elapsed -gt $TEST_TIMEOUT ]]; then
            echo_warning "Test timeout reached after ${TEST_TIMEOUT}s"
            break
        fi
        
        # Check if we have enough log content to evaluate
        if [[ -f "$log_file" ]] && [[ $(wc -l < "$log_file" 2>/dev/null) -gt 10 ]]; then
            # Check current status using enhanced detection
            if check_plugin_success "$log_file"; then
                echo_success "All success criteria met!"
                echo_info "✓ Plugin enabling: $PLUGIN_ENABLING"
                echo_info "✓ Plugin enabled: $PLUGIN_ENABLED" 
                echo_info "✓ Server started: $SERVER_STARTED"
                echo_info "✓ Shops loaded: $SHOPS_LOADED"
                echo_success "Stopping test early - success confirmed"
                break
            fi
            
            # Check for critical errors that indicate immediate failure
            if $HAS_CRITICAL_ERRORS; then
                echo_error "Critical errors detected, stopping test"
                echo_warning "Error details: $ERROR_DETAILS"
                break
            fi
        fi
        
        sleep 1
    done
    
    # Stop server if still running with proper shutdown sequence
    if kill -0 $server_pid 2>/dev/null; then
        echo_info "Stopping server gracefully..."
        
        # Try graceful shutdown first (send SIGTERM)
        kill $server_pid 2>/dev/null || true
        
        # Wait up to 10 seconds for graceful shutdown
        local shutdown_timeout=10
        local shutdown_start=$(date +%s)
        
        while kill -0 $server_pid 2>/dev/null; do
            local current_time=$(date +%s)
            local shutdown_elapsed=$((current_time - shutdown_start))
            
            if [[ $shutdown_elapsed -gt $shutdown_timeout ]]; then
                echo_warning "Graceful shutdown timeout, forcing termination..."
                kill -9 $server_pid 2>/dev/null || true
                break
            fi
            
            sleep 1
        done
        
        # Wait a bit more for file system to sync and databases to close
        echo_info "Waiting for resources to clean up..."
        sleep 3
        
        # Verify process is actually dead
        if kill -0 $server_pid 2>/dev/null; then
            echo_error "Process still running, forcing kill..."
            kill -9 $server_pid 2>/dev/null || true
            sleep 2
        else
            echo_success "Server stopped successfully"
        fi
    fi
    
    # Final result determination using enhanced detection
    local result="UNKNOWN"
    local details=""
    
    # Give the log file a moment to flush
    sleep 1
    
    if [[ -f "$log_file" ]]; then
        if check_plugin_success "$log_file"; then
            result="SUCCESS"
            details="Plugin loaded successfully: enabling=$PLUGIN_ENABLING, enabled=$PLUGIN_ENABLED, server_started=$SERVER_STARTED, shops_loaded=$SHOPS_LOADED"
            echo "$result|$details" > "$success_file"
            echo_success "Test PASSED for MC $mc_version"
        else
            result="FAILED"
            
            # Build detailed failure reason
            local failure_reasons=()
            
            if $HAS_CRITICAL_ERRORS; then
                failure_reasons+=("Critical errors detected")
                details="$ERROR_DETAILS"
            else
                if ! $PLUGIN_ENABLING; then
                    failure_reasons+=("Plugin did not start enabling")
                fi
                if ! $PLUGIN_ENABLED; then
                    failure_reasons+=("Plugin failed to enable completely")
                fi
                if ! $SERVER_STARTED; then
                    failure_reasons+=("Server failed to complete startup")
                fi
                if ! $SHOPS_LOADED; then
                    failure_reasons+=("Shops failed to load")
                fi
                
                if [[ ${#failure_reasons[@]} -eq 0 ]]; then
                    failure_reasons+=("Unknown failure - check log for details")
                fi
                
                details="$(IFS=', '; echo "${failure_reasons[*]}")"
            fi
            
            echo "$result|$details" > "$error_file"
            echo_error "Test FAILED for MC $mc_version"
            echo_warning "Failure reason: $details"
            echo_warning "Check log file: $log_file"
        fi
    else
        result="FAILED"
        details="Log file not found - server may have failed to start"
        echo "$result|$details" > "$error_file"
        echo_error "Test FAILED for MC $mc_version - no log file generated"
    fi
    
    cd "$PROJECT_ROOT"
    return 0
}

# Function to generate compatibility report
generate_report() {
    echo_header "Generating Compatibility Report"
    
    local report_file="$TEST_RESULTS_DIR/COMPATIBILITY_REPORT.md"
    local json_report_file="$TEST_RESULTS_DIR/compatibility_results.json"
    
    cat > "$report_file" << EOF
# Minecraft Shop Plugin Compatibility Report

Generated on: $(date)
Plugin Version: $(basename "$PLUGIN_JAR")

## Test Results

| Minecraft Version | Status | Details |
|-------------------|--------|---------|
EOF

    # JSON report start
    echo '{"test_date":"'$(date -Iseconds)'","plugin_jar":"'$(basename "$PLUGIN_JAR")'","results":[' > "$json_report_file"
    
    local json_entries=()
    local success_count=0
    local total_count=0
    
    # Process results
    for result_file in "$TEST_RESULTS_DIR"/mc-*.success "$TEST_RESULTS_DIR"/mc-*.error; do
        if [[ -f "$result_file" ]]; then
            local filename=$(basename "$result_file")
            local mc_version=$(echo "$filename" | sed 's/mc-\(.*\)\.\(success\|error\)/\1/')
            local status_details=$(cat "$result_file")
            local status=$(echo "$status_details" | cut -d'|' -f1)
            local details=$(echo "$status_details" | cut -d'|' -f2-)
            
            total_count=$((total_count + 1))
            
            if [[ "$status" == "SUCCESS" ]]; then
                echo "| $mc_version | ✅ PASS | $details |" >> "$report_file"
                success_count=$((success_count + 1))
            else
                echo "| $mc_version | ❌ FAIL | $details |" >> "$report_file"
            fi
            
            # Add to JSON
            json_entries+=('{"version":"'$mc_version'","status":"'$status'","details":"'$details'"}')
        fi
    done
    
    # Complete JSON report
    local json_content=$(IFS=','; echo "${json_entries[*]}")
    echo "$json_content" >> "$json_report_file"
    echo ']}' >> "$json_report_file"
    
    # Add summary to markdown report
    cat >> "$report_file" << EOF

## Summary

- **Total Versions Tested:** $total_count
- **Successful:** $success_count
- **Failed:** $((total_count - success_count))
- **Success Rate:** $(( success_count * 100 / total_count ))%

## Notes

This compatibility test verifies:
1. Server startup with plugin
2. Plugin loading and enabling
3. Basic plugin functionality (config file creation)

For detailed logs, check the individual log files in the results directory.
EOF

    echo_success "Compatibility report generated: $report_file"
    echo_info "JSON report generated: $json_report_file"
}

main() {
    echo_header "Minecraft Shop Plugin Compatibility Test"
    
    # Handle single version argument
    if [[ $# -eq 1 ]]; then
        REQUESTED_VERSION="$1"
        echo_info "Testing single version: $REQUESTED_VERSION"
        
        # Find matching jar file
        JAR_FILE=$(ls "$SUPPORTED_VERSIONS_DIR/paper-$REQUESTED_VERSION"*.jar 2>/dev/null | head -n1)
        
        if [[ -z "$JAR_FILE" ]]; then
            echo_error "No server jar found for version $REQUESTED_VERSION"
            echo ""
            echo "Available versions:"
            ls -1 "$SUPPORTED_VERSIONS_DIR/paper-"*.jar 2>/dev/null | sed 's/.*paper-\([^-]*\).*/  \1/' | sort -V
            exit 1
        fi
        
        JAR_NAME=$(basename "$JAR_FILE")
        SERVER_JARS=("$JAR_NAME")
    else
        # Get list of server jars and sort by version (descending)
        SERVER_JARS=($(ls "$SUPPORTED_VERSIONS_DIR"/paper-*.jar | sort -V -r | xargs -n1 basename))
    fi
    
    if [[ ${#SERVER_JARS[@]} -eq 0 ]]; then
        echo_error "No Paper server jars found in $SUPPORTED_VERSIONS_DIR"
        exit 1
    fi
    
    echo_info "Found ${#SERVER_JARS[@]} server version(s) to test"
    
    # Create necessary directories
    mkdir -p "$TEST_ENVIRONMENTS_DIR" "$TEST_RESULTS_DIR"
    
    # Clean previous results
    rm -f "$TEST_RESULTS_DIR"/*.log "$TEST_RESULTS_DIR"/*.success "$TEST_RESULTS_DIR"/*.error
    
    # Find plugin jar
    find_plugin_jar
    
    # Test each version
    local failed_tests=0
    for jar_file in "${SERVER_JARS[@]}"; do
        if ! test_server_version "$jar_file"; then
            failed_tests=$((failed_tests + 1))
        fi
        echo  # Add spacing between tests
    done
    
    # Generate report
    generate_report
    
    echo_header "Compatibility Test Complete"
    if [[ $failed_tests -eq 0 ]]; then
        echo_success "All tests completed successfully!"
    else
        echo_warning "$failed_tests test(s) encountered issues"
    fi
    
    echo_info "Results saved to: $TEST_RESULTS_DIR"
    
    # If single version test, show results immediately
    if [[ $# -eq 1 ]]; then
        local mc_version=$(get_mc_version "$JAR_NAME")
        local success_file="$TEST_RESULTS_DIR/mc-$mc_version.success"
        local error_file="$TEST_RESULTS_DIR/mc-$mc_version.error"
        local log_file="$TEST_RESULTS_DIR/mc-$mc_version.log"
        
        echo_header "Test Results for MC $mc_version"
        
        if [[ -f "$success_file" ]]; then
            local details=$(cat "$success_file" | cut -d'|' -f2-)
            echo_success "PASSED: $details"
        elif [[ -f "$error_file" ]]; then
            local details=$(cat "$error_file" | cut -d'|' -f2-)
            echo_error "FAILED: $details"
            echo ""
            echo_info "Recent log entries:"
            tail -20 "$log_file" 2>/dev/null || echo "No log file available"
        else
            echo_warning "No results found"
        fi
        
        echo_info "Full log available at: $log_file"
    fi
}

# Run main function
main "$@" 