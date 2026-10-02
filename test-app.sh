#!/usr/bin/env bash
# ==============================================================================
# Spring Cloud Showcase - Test Application & Swagger Verification Suite
# ==============================================================================

set -e

# Color helpers
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${CYAN}================================================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}================================================================${NC}"
}

run_maven_tests() {
    print_header "Running Automated Unit & Integration Tests (Maven)"
    log_info "Executing: mvn test"
    mvn test
    log_success "All unit, integration, and OpenAPI specification tests passed!"
}

run_swagger_tests() {
    print_header "Verifying Swagger / OpenAPI 3 Specifications"
    log_info "Testing OpenAPI document generation on Inventory Service..."
    mvn test -pl inventory-service -Dtest=InventoryOpenApiTest
    log_success "Inventory Service OpenAPI & Swagger UI verified successfully!"

    log_info "Testing OpenAPI document generation on Order Service..."
    mvn test -pl order-service -Dtest=OrderOpenApiTest
    log_success "Order Service OpenAPI & Swagger UI verified successfully!"

    log_info "Testing OpenAPI aggregation & Swagger UI on API Gateway..."
    mvn test -pl api-gateway -Dtest=GatewayOpenApiTest
    log_success "API Gateway Swagger UI Aggregation verified successfully!"
}

run_logging_tests() {
    print_header "Verifying File Logging Mechanisms & Log Outputs"
    log_info "Running OrderLoggingTest to verify file logging..."
    mvn test -pl order-service -Dtest=OrderLoggingTest
    if [ -f "order-service/logs/order-service.log" ]; then
        log_success "Log file order-service/logs/order-service.log verified! Size: $(wc -c < order-service/logs/order-service.log) bytes"
        log_info "Last 3 lines of order-service.log:"
        tail -n 3 order-service/logs/order-service.log
    else
        log_error "order-service/logs/order-service.log was not generated!"
        exit 1
    fi
}

check_endpoint() {
    local name="$1"
    local url="$2"
    local expected_code="${3:-200}"

    log_info "Pinging $name at $url ..."
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" || echo "000")

    if [ "$http_code" -eq "$expected_code" ]; then
        log_success "$name responded with HTTP $http_code (Expected: $expected_code)"
        return 0
    else
        log_error "$name responded with HTTP $http_code (Expected: $expected_code)"
        return 1
    fi
}

run_live_tests() {
    print_header "Testing Live Microservices & Swagger Endpoints"

    log_info "Checking if services are currently listening..."

    # Check Swagger endpoints if services are running
    echo ""
    echo "--- 1. Swagger UI & OpenAPI Specification Endpoints ---"
    check_endpoint "Inventory Service OpenAPI Docs" "http://localhost:8082/v3/api-docs" 200 || true
    check_endpoint "Inventory Service Swagger UI" "http://localhost:8082/swagger-ui/index.html" 200 || true

    check_endpoint "Order Service OpenAPI Docs" "http://localhost:8081/v3/api-docs" 200 || true
    check_endpoint "Order Service Swagger UI" "http://localhost:8081/swagger-ui/index.html" 200 || true

    check_endpoint "Gateway Swagger UI Aggregator" "http://localhost:8080/swagger-ui.html" 302 || true
    check_endpoint "Gateway Swagger Config" "http://localhost:8080/v3/api-docs/swagger-config" 200 || true
    check_endpoint "Gateway Routed Order Docs" "http://localhost:8080/order-service/v3/api-docs" 200 || true
    check_endpoint "Gateway Routed Inventory Docs" "http://localhost:8080/inventory-service/v3/api-docs" 200 || true

    echo ""
    echo "--- 2. Functional Business Endpoints ---"
    log_info "Querying Inventory for SKU 'IPHONE15'..."
    curl -s http://localhost:8082/api/inventory/IPHONE15 | jq . || curl -s http://localhost:8082/api/inventory/IPHONE15 || true

    echo ""
    log_info "Placing test order via Order Service..."
    curl -s -X POST http://localhost:8081/api/orders \
        -H "Content-Type: application/json" \
        -d '{"skuCode":"IPHONE15","quantity":1,"price":999.00}' | jq . || true

    echo ""
    log_info "Testing Circuit Breaker fallback on Order Service..."
    curl -s -X POST "http://localhost:8081/api/orders/simulate-circuit-breaker?skuCode=IPHONE15&mode=fail" | jq . || true
}

show_help() {
    echo "Spring Cloud Showcase Test App"
    echo ""
    echo "Usage: ./test-app.sh [OPTION]"
    echo ""
    echo "Options:"
    echo "  --all        (Default) Run complete test suite (Maven tests + OpenAPI + Logging)"
    echo "  --logging    Run file logging verification tests only"
    echo "  --swagger    Run OpenAPI and Swagger validation tests only"
    echo "  --unit       Run unit and integration test suite only"
    echo "  --live       Probe live running services and Swagger endpoints over HTTP"
    echo "  --help       Show this help message"
}

# Main command dispatcher
ACTION="${1:---all}"

case "$ACTION" in
    --all)
        print_header "Spring Cloud Showcase - Comprehensive Test Suite"
        run_maven_tests
        run_logging_tests
        echo ""
        log_success "All test suites completed successfully!"
        ;;
    --logging)
        run_logging_tests
        ;;
    --swagger)
        run_swagger_tests
        ;;
    --unit)
        run_maven_tests
        ;;
    --live)
        run_live_tests
        ;;
    --help|-h)
        show_help
        ;;
    *)
        log_error "Unknown option: $ACTION"
        show_help
        exit 1
        ;;
esac
