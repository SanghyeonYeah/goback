CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    google_id VARCHAR(255) UNIQUE NOT NULL, -- OAuth2 식별자
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    grade INT NOT NULL CHECK (grade IN (1, 2, 3)),
    diploma_type VARCHAR(50) NOT NULL, -- 'STEM_IT', 'HUMAN_LIT', 'ART_PHY', etc.
    school_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 과목별 목표 등급 (사용자별 1:N)
CREATE TABLE user_targets (
    target_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    subject_code VARCHAR(20) NOT NULL, -- 'KOR', 'MATH', 'ENG', 'PHY1', etc.
    target_grade INT NOT NULL CHECK (target_grade BETWEEN 1 AND 9),
    is_taking BOOLEAN DEFAULT TRUE, -- 2학년 선택 과목 수강 여부
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE (user_id, subject_code)
);

-- 3. 학습 시간 설정 (면학/비면학 패턴)
CREATE TABLE study_configs (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    day_type VARCHAR(10) NOT NULL, -- 'WEEKDAY_SCHOOL', 'WEEKDAY_HOME', 'WEEKEND'
    available_start_time TIME,
    available_end_time TIME,
    is_night_study BOOLEAN DEFAULT FALSE, -- 연장 면학 여부
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 4. 데일리 학습 상태 (달력 '완'/'실' 기록용)
CREATE TABLE daily_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(10) CHECK (status IN ('WAN', 'SIL', 'NONE')), -- 완, 실, 미입력
    bonus_applied BOOLEAN DEFAULT FALSE, -- 다음날 점수 +10% 적용 여부
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE (user_id, date)
);

-- 5. 문제 테이블 (모의고사/문제집)
CREATE TABLE problems (
    problem_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_code VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL, -- 'MOCK_EXAM', 'WORKBOOK'
    content TEXT NOT NULL, -- 문제 내용 (또는 이미지 URL)
    answer VARCHAR(255) NOT NULL,
    base_score INT NOT NULL DEFAULT 2, -- 기본 배점 (2~5)
    season_id INT NOT NULL -- 시즌별 문제 관리
);

-- 6. 문제 풀이 기록 (랭킹 산정용)
CREATE TABLE submissions (
    sub_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    problem_id BIGINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    score_earned DECIMAL(5, 2) DEFAULT 0, -- 디플로마 보정 점수 반영
    solved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id)
);

-- 7. 랭킹 (집계 테이블)
CREATE TABLE rankings (
    ranking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    total_score DECIMAL(10, 2) DEFAULT 0,
    ranking_type VARCHAR(10) NOT NULL, -- 'DAILY', 'SEASON'
    record_date DATE, -- 일일 랭킹용
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 8. PVP 매치 기록
CREATE TABLE pvp_matches (
    match_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    problem_id BIGINT NOT NULL,
    winner_id INT, -- NULL이면 무승부 혹은 진행중
    status VARCHAR(20) DEFAULT 'MATCHING', -- 'MATCHING', 'IN_PROGRESS', 'FINISHED'
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(user_id),
    FOREIGN KEY (player2_id) REFERENCES users(user_id),
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id)
);