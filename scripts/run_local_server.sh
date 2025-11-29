#!/bin/bash

################################################################################
# StudyPlanner Local Development Server Script
# 스터디 플래너 로컬 개발 서버 실행 스크립트
#
# 사용법:
#   ./scripts/run_local_server.sh            # 기본 설정으로 서버 시작
#   ./scripts/run_local_server.sh --reset    # DB 초기화 후 시작
#   ./scripts/run_local_server.sh --seed     # 샘플 데이터 삽입 후 시작
#
# 요구사항:
#   - Node.js 14+
#   - npm 6+
#   - PostgreSQL 12+
#   - psql (PostgreSQL client)
#
# 환경 변수 설정:
#   export POSTGRES_USER=postgres
#   export POSTGRES_PASSWORD=password
#   export POSTGRES_HOST=localhost
#   export POSTGRES_PORT=5432
#   export DB_NAME=study_planner_dev
################################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 함수 정의
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_debug() {
    echo -e "${MAGENTA}→${NC} $1"
}

print_header() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  $1"
    echo -e "${CYAN}╚════════════════════════════════════════╝${NC}"
    echo ""
}

print_separator() {
    echo -e "${CYAN}────────────────────────────────────────${NC}"
}

# 기본값 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SERVER_DIR="$PROJECT_ROOT/server"
RESET_DB=${RESET_DB:-false}
SEED_DATA=${SEED_DATA:-false}

# 옵션 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        --reset)
            RESET_DB=true
            shift
            ;;
        --seed)
            SEED_DATA=true
            shift
            ;;
        --help)
            echo "Usage: ./scripts/run_local_server.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --reset    Initialize database (drop all tables)"
            echo "  --seed     Load sample data"
            echo "  --help     Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_header "StudyPlanner Local Development Server"

# 1. 환경 확인
print_header "Step 1: Environment Check"

log_info "Checking Node.js..."
if ! command -v node &> /dev/null; then
    log_error "Node.js is not installed"
    log_warn "Install Node.js from https://nodejs.org"
    exit 1
fi
NODE_VERSION=$(node -v)
log_success "Node.js: $NODE_VERSION"

log_info "Checking npm..."
if ! command -v npm &> /dev/null; then
    log_error "npm is not installed"
    exit 1
fi
NPM_VERSION=$(npm -v)
log_success "npm: $NPM_VERSION"

# 2. PostgreSQL 환경 변수 설정
print_header "Step 2: PostgreSQL Configuration"

DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${DB_NAME:-study_planner_dev}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-password}"

log_info "Database Configuration:"
log_debug "Host: $DB_HOST:$DB_PORT"
log_debug "Database: $DB_NAME"
log_debug "User: $DB_USER"

# 3. PostgreSQL 연결 확인
print_header "Step 3: PostgreSQL Connection Check"

log_info "Checking PostgreSQL installation..."
if ! command -v psql &> /dev/null; then
    log_error "PostgreSQL client (psql) is not installed"
    log_warn "Install PostgreSQL from https://www.postgresql.org/download"
    exit 1
fi
PSQL_VERSION=$(psql --version)
log_success "$PSQL_VERSION"

log_info "Connecting to PostgreSQL..."
if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d postgres -c "SELECT 1" > /dev/null 2>&1; then
    log_success "PostgreSQL connection successful"
else
    log_error "Cannot connect to PostgreSQL"
    log_warn "Ensure PostgreSQL is running at $DB_HOST:$DB_PORT"
    log_warn "Check credentials: USER=$DB_USER"
    exit 1
fi

# 4. 데이터베이스 생성
print_header "Step 4: Database Setup"

log_info "Checking if database exists..."
if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d postgres -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
    log_success "Database already exists: $DB_NAME"
    
    if [ "$RESET_DB" = true ]; then
        log_warn "Resetting database..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" > /dev/null 2>&1
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" > /dev/null 2>&1
        log_success "Database reset"
    fi
else
    log_info "Creating database: $DB_NAME"
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" > /dev/null 2>&1
    log_success "Database created"
fi

# 5. 테이블 생성
print_header "Step 5: Database Schema Setup"

if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "SELECT to_regclass('public.users')" | grep -q users; then
    log_success "Tables already exist"
else
    log_info "Creating tables..."
    if [ ! -f "$SERVER_DIR/db_schema/create_tables.sql" ]; then
        log_error "Schema file not found: $SERVER_DIR/db_schema/create_tables.sql"
        exit 1
    fi
    
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -f "$SERVER_DIR/db_schema/create_tables.sql" > /dev/null 2>&1
    log_success "Tables created"
fi

# 6. 샘플 데이터 로드
if [ "$SEED_DATA" = true ] || [ "$RESET_DB" = true ]; then
    print_header "Step 6: Loading Sample Data"
    
    if [ -f "$SERVER_DIR/db_schema/seed_data.sql" ]; then
        log_info "Loading sample data..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -f "$SERVER_DIR/db_schema/seed_data.sql" > /dev/null 2>&1
        log_success "Sample data loaded"
    else
        log_warn "Seed data file not found, skipping"
    fi
fi

# 7. 환경 파일 설정
print_header "Step 7: Environment Configuration"

ENV_FILE="$SERVER_DIR/.env.local"

if [ ! -f "$ENV_FILE" ]; then
    log_info "Creating .env.local..."
    cat > "$ENV_FILE" << EOF
NODE_ENV=development
PORT=3000
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=dev_jwt_secret_key_change_in_production
JWT_EXPIRE=7d
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
LOG_LEVEL=debug
API_BASE_URL=http://localhost:3000
EOF
    log_success ".env.local created"
    log_warn "Update API credentials in .env.local"
else
    log_success ".env.local already exists"
fi

# 8. 의존성 설치
print_header "Step 8: Installing Dependencies"

cd "$SERVER_DIR"

if [ ! -d "node_modules" ]; then
    log_info "Installing npm packages..."
    npm install
    log_success "Dependencies installed"
else
    log_success "Dependencies already installed"
    log_info "Run 'npm install' to update packages"
fi

# 9. 서버 시작
print_header "Step 9: Starting Development Server"

log_info "Using environment file: $ENV_FILE"
export $(cat "$ENV_FILE" | grep -v '^#' | xargs)

log_info "Server starting on http://localhost:$PORT"
log_info "API Base URL: http://localhost:$PORT/api/v1"
print_separator

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN} StudyPlanner Development Server Ready${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo "Development URLs:"
echo "  API: http://localhost:$PORT"
echo "  Health: http://localhost:$PORT/health"
echo "  Logs: press Ctrl+C to stop"
echo ""
echo "Useful Commands:"
echo "  Test API: curl http://localhost:$PORT/health"
echo "  Database: PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME"
echo ""

# nodemon 또는 pm2 dev로 서버 시작
if command -v pm2 &> /dev/null; then
    log_info "Starting with PM2..."
    pm2 start api/index.js \
        --name "study-planner-dev" \
        --watch \
        --ignore-watch="node_modules" \
        --env development \
        --watch-delay 1000
    pm2 logs study-planner-dev
else
    log_info "Starting with Node.js directly..."
    log_warn "Install nodemon globally for hot reload:"
    log_warn "  npm install -g nodemon"
    echo ""
    node api/index.js
fi

cd "$PROJECT_ROOT"

exit 0
