#!/bin/bash

################################################################################
# StudyPlanner Build APK Script
# 스터디 플래너 Android APK 빌드 스크립트
#
# 사용법:
#   ./scripts/build_apk.sh                # 기본값(debug) 빌드
#   ./scripts/build_apk.sh debug          # 디버그 APK 빌드
#   ./scripts/build_apk.sh production     # 프로덕션 APK 빌드 (서명 포함)
#
# 요구사항:
#   - Android SDK (API 24 이상)
#   - Gradle 7.0+
#   - Java JDK 11+
#   - keystore.jks (프로덕션 빌드 시)
#
# 프로덕션 서명 설정:
#   export KEYSTORE_PASSWORD="your_password"
#   export KEY_PASSWORD="your_key_password"
#   export KEY_ALIAS="your_key_alias"
################################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 함수 정의
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[!]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_header() {
    echo ""
    echo -e "${CYAN}════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}════════════════════════════════════════${NC}"
    echo ""
}

# 기본값 설정
BUILD_TYPE="${1:-debug}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APP_DIR="$PROJECT_ROOT/app"
BUILD_OUTPUT="$APP_DIR/build/outputs"

print_header "StudyPlanner APK Builder - ${BUILD_TYPE^^}"

# 1. 환경 확인
print_header "Step 1: Environment Check"

log_info "Checking Java installation..."
if ! command -v java &> /dev/null; then
    log_error "Java is not installed"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1)
log_success "Java found: $JAVA_VERSION"

log_info "Checking Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    log_error "ANDROID_HOME environment variable is not set"
    log_warn "Set it: export ANDROID_HOME=/path/to/android/sdk"
    exit 1
fi
log_success "Android SDK found at: $ANDROID_HOME"

log_info "Checking Gradle..."
if ! command -v gradle &> /dev/null && ! [ -x "$APP_DIR/gradlew" ]; then
    log_error "Gradle wrapper not found at $APP_DIR/gradlew"
    exit 1
fi
log_success "Gradle wrapper found"

# 2. 빌드 준비
print_header "Step 2: Build Preparation"

cd "$APP_DIR"

log_info "Cleaning previous builds..."
./gradlew clean > /dev/null 2>&1
log_success "Build directory cleaned"

log_info "Downloading dependencies..."
./gradlew dependencies > /dev/null 2>&1
log_success "Dependencies downloaded"

# 3. 버전 정보 설정
print_header "Step 3: Version Configuration"

VERSION_CODE=$(date +%s)
VERSION_NAME="1.0.0"
BUILD_TIME=$(date "+%Y-%m-%d %H:%M:%S")

log_info "Version Code: $VERSION_CODE"
log_info "Version Name: $VERSION_NAME"
log_info "Build Time: $BUILD_TIME"

# 4. 빌드 실행
print_header "Step 4: Building APK"

if [ "$BUILD_TYPE" = "production" ]; then
    log_info "Building production APK (optimized with shrinking)..."
    
    ./gradlew assembleRelease \
        -PversionCode=$VERSION_CODE \
        -PversionName=$VERSION_NAME \
        -Dorg.gradle.jvmargs="-Xmx2g"
    
    log_success "Release build completed"
    
    UNSIGNED_APK="$BUILD_OUTPUT/apk/release/app-release-unsigned.apk"
    SIGNED_APK="$BUILD_OUTPUT/apk/release/app-release.apk"
    ALIGNED_APK="$BUILD_OUTPUT/apk/release/app-release-aligned.apk"
    
    # 5. 프로덕션: APK 서명
    print_header "Step 5: Signing Release APK"
    
    KEYSTORE_FILE="$PROJECT_ROOT/keystore.jks"
    
    if [ ! -f "$KEYSTORE_FILE" ]; then
        log_error "Keystore file not found: $KEYSTORE_FILE"
        log_warn "Generate keystore with:"
        log_warn "  keytool -genkey -v -keystore keystore.jks"
        log_warn "  -keyalg RSA -keysize 2048 -validity 10000"
        exit 1
    fi
    
    if [ -z "$KEYSTORE_PASSWORD" ]; then
        log_error "KEYSTORE_PASSWORD environment variable not set"
        exit 1
    fi
    
    if [ -z "$KEY_PASSWORD" ]; then
        log_error "KEY_PASSWORD environment variable not set"
        exit 1
    fi
    
    if [ -z "$KEY_ALIAS" ]; then
        log_warn "KEY_ALIAS not set, using 'studyplanner-key'"
        KEY_ALIAS="studyplanner-key"
    fi
    
    log_info "Signing APK with keystore..."
    jarsigner -verbose \
        -sigalg SHA256withRSA \
        -digestalg SHA-256 \
        -keystore "$KEYSTORE_FILE" \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        "$UNSIGNED_APK" \
        "$KEY_ALIAS" > /dev/null 2>&1
    
    log_success "APK signed successfully"
    
    # 6. ZIP align
    print_header "Step 6: ZIP Alignment"
    
    log_info "Aligning APK..."
    if command -v zipalign &> /dev/null; then
        zipalign -v 4 "$UNSIGNED_APK" "$ALIGNED_APK" > /dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            mv "$ALIGNED_APK" "$SIGNED_APK"
            log_success "APK aligned successfully"
        else
            log_warn "ZIP alignment failed, using signed APK as-is"
            mv "$UNSIGNED_APK" "$SIGNED_APK"
        fi
    else
        log_warn "zipalign not found, using signed APK as-is"
        mv "$UNSIGNED_APK" "$SIGNED_APK"
    fi
    
    # 7. 검증
    print_header "Step 7: APK Verification"
    
    if [ -f "$SIGNED_APK" ]; then
        SIZE=$(du -h "$SIGNED_APK" | cut -f1)
        MD5=$(md5sum "$SIGNED_APK" | awk '{print $1}')
        
        log_success "APK created successfully"
        log_info "File: $SIGNED_APK"
        log_info "Size: $SIZE"
        log_info "MD5: $MD5"
        
        # jarsigner로 서명 검증
        log_info "Verifying signature..."
        jarsigner -verify -verbose -certs "$SIGNED_APK" > /dev/null 2>&1
        log_success "Signature verified"
    else
        log_error "APK file not found"
        exit 1
    fi
    
else
    # 디버그 빌드
    log_info "Building debug APK..."
    
    ./gradlew assembleDebug \
        -PversionCode=$VERSION_CODE \
        -PversionName=$VERSION_NAME \
        -Dorg.gradle.jvmargs="-Xmx2g"
    
    log_success "Debug build completed"
    
    DEBUG_APK="$BUILD_OUTPUT/apk/debug/app-debug.apk"
    
    if [ -f "$DEBUG_APK" ]; then
        SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        MD5=$(md5sum "$DEBUG_APK" | awk '{print $1}')
        
        log_success "Debug APK created successfully"
        log_info "File: $DEBUG_APK"
        log_info "Size: $SIZE"
        log_info "MD5: $MD5"
    else
        log_error "Debug APK not found"
        exit 1
    fi
fi

cd "$PROJECT_ROOT"

# 8. 빌드 완료
print_header "Build Complete"

if [ "$BUILD_TYPE" = "production" ]; then
    APK_FILE="$BUILD_OUTPUT/apk/release/app-release.apk"
    INSTALL_INSTRUCTION="Push to Play Store or distribute directly"
else
    APK_FILE="$BUILD_OUTPUT/apk/debug/app-debug.apk"
    INSTALL_INSTRUCTION="adb install \"$APK_FILE\""
fi

echo -e "${GREEN}"
echo "╔════════════════════════════════════════════════════╗"
echo "║        APK Build Completed Successfully            ║"
echo "╠════════════════════════════════════════════════════╣"
echo "║ Build Type:  ${BUILD_TYPE^^}"
echo "║ Output:      ${APK_FILE}"
echo "║ Install:     $INSTALL_INSTRUCTION"
echo "╚════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 추가 정보
echo ""
echo "Build Information:"
echo "  Version: $VERSION_NAME (Code: $VERSION_CODE)"
echo "  Build Time: $BUILD_TIME"
echo ""

if [ "$BUILD_TYPE" = "debug" ]; then
    echo "To install on connected device:"
    echo "  adb install \"$APK_FILE\""
    echo ""
    echo "To uninstall:"
    echo "  adb uninstall com.yourorg.studyplanner"
fi

exit 0
