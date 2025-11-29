#!/bin/bash

################################################################################
# StudyPlanner Deploy Script
# 스터디 플래너 프로덕션 배포 스크립트
# 
# 사용법:
#   ./scripts/deploy.sh                 # 기본값(production) 배포
#   ./scripts/deploy.sh staging         # Staging 환경 배포
#   ./scripts/deploy.sh development     # 개발 환경 배포
#
# 요구사항:
#   - Node.js 14+
#   - npm 6+
#   - PostgreSQL 12+
#   - Android SDK (APK 빌드 시)
#   - PM2 (프로덕션 배포 시)
################################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo "======================================"
    echo "$1"
    echo "======================================"
    echo ""
}

# 기본값 설정
ENVIRONMENT="${1:-production}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DEPLOY_LOG="${PROJECT_ROOT}/deploy.log"

# 배포 로그 시작
{
    echo "=== Deploy Start ==="
    echo "Timestamp: $(date)"
    echo "Environment: $ENVIRONMENT"
    echo "Project Root: $PROJECT_ROOT"
} >> "$DEPLOY_LOG"

print_header "StudyPlanner Deploy Script - $ENVIRONMENT"

# 1. 환경 설정 검증
print_header "Step 1: Environment Validation"

log_info "Checking Node.js installation..."
if ! command -v node &> /dev/null; then
    log_error "Node.js is not installed"
    exit 1
fi
NODE_VERSION=$(node -v)
log_success "Node.js found: $NODE_VERSION"

log_info "Checking npm installation..."
if ! command -v npm &> /dev/null; then
    log_error "npm is not installed"
    exit 1
fi
NPM_VERSION=$(npm -v)
log_success "npm found: $NPM_VERSION"

# 환경 변수 파일 확인
if [ ! -f "$PROJECT_ROOT/server/.env.$ENVIRONMENT" ]; then
    log_error "Environment file not found: server/.env.$ENVIRONMENT"
    log_warn "Creating default .env file..."
    
    cat > "$PROJECT_ROOT/server/.env.$ENVIRONMENT" << 'EOF'
NODE_ENV=production
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_NAME=study_planner_prod
DB_USER=postgres
DB_PASSWORD=change_me
JWT_SECRET=change_me_with_random_secret
JWT_EXPIRE=7d
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
LOG_LEVEL=info
EOF
    
    log_warn "Please update server/.env.$ENVIRONMENT with your configuration"
    exit 1
fi

log_success "Environment file found: server/.env.$ENVIRONMENT"

# 2. 의존성 설치
print_header "Step 2: Installing Dependencies"

log_info "Installing npm packages for server..."
cd "$PROJECT_ROOT/server"
npm install --production
log_success "Server dependencies installed"

cd "$PROJECT_ROOT"

# 3. 데이터베이스 설정
print_header "Step 3: Database Configuration"

log_info "Loading database configuration from .env.$ENVIRONMENT..."
export $(cat "$PROJECT_ROOT/server/.env.$ENVIRONMENT" | grep -v '^#' | xargs)

if [ "$ENVIRONMENT" = "production" ]; then
    log_info "Checking PostgreSQL connection..."
    if ! command -v psql &> /dev/null; then
        log_error "PostgreSQL client (psql) not found"
        exit 1
    fi
    
    # DB 연결 테스트
    if psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
        log_success "Database connection successful"
        
        # 테이블 존재 확인
        if psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "SELECT to_regclass('public.users')" | grep -q users; then
            log_success "Database tables already exist"
        else
            log_info "Creating database tables..."
            psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -f "$PROJECT_ROOT/server/db_schema/create_tables.sql"
            
            if [ -f "$PROJECT_ROOT/server/db_schema/seed_data.sql" ]; then
                log_info "Seeding initial data..."
                psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -f "$PROJECT_ROOT/server/db_schema/seed_data.sql"
            fi
            
            log_success "Database initialized successfully"
        fi
    else
        log_error "Cannot connect to PostgreSQL at $DB_HOST:$DB_PORT"
        exit 1
    fi
else
    log_warn "Skipping database setup for $ENVIRONMENT environment"
fi

# 4. 환경 변수 복사
print_header "Step 4: Configuring Environment Variables"

log_info "Setting up .env for server..."
cp "$PROJECT_ROOT/server/.env.$ENVIRONMENT" "$PROJECT_ROOT/server/.env"
log_success "Environment variables configured"

# 5. PM2 설정 (프로덕션만)
if [ "$ENVIRONMENT" = "production" ]; then
    print_header "Step 5: PM2 Setup"
    
    if ! command -v pm2 &> /dev/null; then
        log_info "Installing PM2 globally..."
        npm install -g pm2
    fi
    
    log_success "PM2 found: $(pm2 -v)"
    
    # PM2 기존 프로세스 중지
    log_info "Stopping existing Study Planner processes..."
    pm2 delete study-planner-api 2>/dev/null || true
    pm2 delete ranking-batch 2>/dev/null || true
    pm2 delete season-reset 2>/dev/null || true
    
    log_info "Starting API server with PM2..."
    pm2 start "$PROJECT_ROOT/server/api/index.js" \
        --name "study-planner-api" \
        --instances max \
        --exec-mode cluster \
        --env production \
        --log-date-format "YYYY-MM-DD HH:mm:ss Z" \
        --merge-logs \
        --max-memory-restart 500M
    
    log_info "Starting ranking batch job..."
    pm2 start "$PROJECT_ROOT/server/jobs/rankingBatch.js" \
        --name "ranking-batch" \
        --instances 1 \
        --env production
    
    log_info "Starting season reset job..."
    pm2 start "$PROJECT_ROOT/server/jobs/seasonReset.js" \
        --name "season-reset" \
        --instances 1 \
        --env production
    
    # PM2 설정 저장
    pm2 save
    pm2 startup
    
    log_success "PM2 processes started successfully"
    
    log_info "PM2 status:"
    pm2 status
    
    log_info "View logs: pm2 logs study-planner-api"
fi

# 6. Android APK 빌드
print_header "Step 6: Building Android APK"

if [ -f "$PROJECT_ROOT/scripts/build_apk.sh" ]; then
    log_info "Starting Android APK build..."
    
    if [ "$ENVIRONMENT" = "production" ]; then
        bash "$PROJECT_ROOT/scripts/build_apk.sh" production
    else
        bash "$PROJECT_ROOT/scripts/build_apk.sh" debug
    fi
    
    log_success "Android APK built successfully"
else
    log_warn "build_apk.sh not found, skipping APK build"
fi

# 7. 헬스 체크
if [ "$ENVIRONMENT" = "production" ]; then
    print_header "Step 7: Health Check"
    
    log_info "Waiting for server to start..."
    sleep 5
    
    for i in {1..10}; do
        if curl -s http://localhost:3000/health > /dev/null 2>&1; then
            log_success "Server is running and healthy"
            break
        fi
        
        if [ $i -eq 10 ]; then
            log_error "Server health check failed after 10 attempts"
            log_warn "Please check PM2 logs: pm2 logs study-planner-api"
            exit 1
        fi
        
        log_info "Health check attempt $i/10..."
        sleep 2
    done
fi

# 8. 배포 완료
print_header "Deployment Complete!"

log_success "StudyPlanner deployment completed successfully"
log_info "Environment: $ENVIRONMENT"
log_info "Timestamp: $(date)"

if [ "$ENVIRONMENT" = "production" ]; then
    log_info "API Server: http://localhost:3000"
    log_info "View logs: pm2 logs study-planner-api"
    log_info "Monitor processes: pm2 monit"
fi

# 배포 로그 종료
{
    echo "=== Deploy End ==="
    echo "Status: SUCCESS"
    echo "Timestamp: $(date)"
    echo ""
} >> "$DEPLOY_LOG"

echo -e "${GREEN}"
echo "╔════════════════════════════════════════╗"
echo "║   StudyPlanner Deployed Successfully   ║"
echo "╚════════════════════════════════════════╝"
echo -e "${NC}"

exit 0
