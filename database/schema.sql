-- Study Planner Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS study_planner CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study_planner;

-- 사용자 테이블
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    student_number VARCHAR(20) UNIQUE NOT NULL,
    diploma ENUM('IT', '공학', '수학', '물리', '화학', '생명과학', 'IB(자연)', 
                 '인문학', '국제어문', '사회과학', '경제경영', 'IB(인문)', '예술', '체육') NOT NULL,
    grade TINYINT DEFAULT 1,
    email VARCHAR(100) UNIQUE,
    google_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_student_number (student_number)
);

-- 세션 테이블
CREATE TABLE sessions (
    session_id VARCHAR(128) PRIMARY KEY,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);

-- 시즌 테이블
CREATE TABLE seasons (
    season_id INT PRIMARY KEY AUTO_INCREMENT,
    season_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_dates (start_date, end_date)
);

-- 목표 테이블
CREATE TABLE goals (
    goal_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    target_grade TINYINT NOT NULL CHECK (target_grade BETWEEN 1 AND 5),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    INDEX idx_user_season (user_id, season_id),
    INDEX idx_subject (subject)
);

-- Todo 테이블
CREATE TABLE todos (
    todo_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    goal_id INT,
    subject VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    activity_type ENUM('문제풀이', '개념학습', '복습', '모의고사') NOT NULL,
    difficulty ENUM('쉬움', '보통', '어려움') NOT NULL,
    estimated_time INT NOT NULL COMMENT '분 단위',
    actual_time INT COMMENT '분 단위',
    scheduled_date DATE NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES goals(goal_id) ON DELETE SET NULL,
    INDEX idx_user_date (user_id, scheduled_date),
    INDEX idx_completed (completed)
);

-- 학습 데이터 테이블 (AI 학습용)
CREATE TABLE study_data (
    study_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    activity_type ENUM('문제풀이', '개념학습', '복습', '모의고사') NOT NULL,
    difficulty ENUM('쉬움', '보통', '어려움') NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    estimated_time INT NOT NULL COMMENT '분 단위',
    actual_time INT NOT NULL COMMENT '분 단위',
    performance_score DECIMAL(5,2) COMMENT '성취도 점수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_subject (user_id, subject),
    INDEX idx_date (start_time)
);

-- 문제 테이블
CREATE TABLE problems (
    problem_id INT PRIMARY KEY AUTO_INCREMENT,
    season_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    question_image_url VARCHAR(500),
    option1 TEXT NOT NULL,
    option2 TEXT NOT NULL,
    option3 TEXT,
    option4 TEXT,
    option5 TEXT,
    correct_answer TINYINT NOT NULL CHECK (correct_answer BETWEEN 1 AND 5),
    difficulty ENUM('쉬움', '보통', '어려움') NOT NULL,
    base_points TINYINT NOT NULL CHECK (base_points BETWEEN 2 AND 5),
    problem_type ENUM('모의고사', '문제집') DEFAULT '모의고사',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    INDEX idx_season_subject (season_id, subject),
    INDEX idx_difficulty (difficulty)
);

-- 문제 풀이 기록
CREATE TABLE problem_attempts (
    attempt_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    problem_id INT NOT NULL,
    selected_answer TINYINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    points_earned DECIMAL(5,2) NOT NULL,
    time_spent INT COMMENT '초 단위',
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE,
    INDEX idx_user_problem (user_id, problem_id),
    INDEX idx_attempted_at (attempted_at)
);

-- 랭킹 테이블 (시즌별)
CREATE TABLE season_rankings (
    ranking_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    total_points DECIMAL(10,2) DEFAULT 0,
    daily_points DECIMAL(10,2) DEFAULT 0,
    rank_position INT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_season (user_id, season_id),
    INDEX idx_season_points (season_id, total_points DESC),
    INDEX idx_season_daily (season_id, daily_points DESC)
);

-- 일일 랭킹 스냅샷
CREATE TABLE daily_rankings (
    daily_ranking_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    ranking_date DATE NOT NULL,
    daily_points DECIMAL(10,2) DEFAULT 0,
    rank_position INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, ranking_date),
    INDEX idx_date_points (ranking_date, daily_points DESC)
);

-- PVP 매치 테이블
CREATE TABLE pvp_matches (
    match_id INT PRIMARY KEY AUTO_INCREMENT,
    season_id INT NOT NULL,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    problem_id INT NOT NULL,
    winner_id INT COMMENT 'NULL은 무승부',
    player1_answer TINYINT,
    player2_answer TINYINT,
    player1_time INT COMMENT '초 단위',
    player2_time INT COMMENT '초 단위',
    match_status ENUM('waiting', 'in_progress', 'completed') DEFAULT 'waiting',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE,
    INDEX idx_players (player1_id, player2_id),
    INDEX idx_status (match_status)
);

-- 달력 완료 기록
CREATE TABLE calendar_records (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    record_date DATE NOT NULL,
    all_completed BOOLEAN NOT NULL,
    completion_rate DECIMAL(5,2) NOT NULL,
    total_tasks INT NOT NULL,
    completed_tasks INT NOT NULL,
    bonus_applied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, record_date),
    INDEX idx_date (record_date)
);

-- 시즌 리포트
CREATE TABLE season_reports (
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    goal_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    target_grade TINYINT NOT NULL,
    achieved_grade TINYINT,
    goal_achieved BOOLEAN DEFAULT FALSE,
    total_study_time INT COMMENT '분 단위',
    total_problems_solved INT DEFAULT 0,
    average_accuracy DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES goals(goal_id) ON DELETE CASCADE,
    INDEX idx_user_season (user_id, season_id)
);

-- 관리자 권한 테이블
CREATE TABLE admin_users (
    admin_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_admin_user (user_id)
);

-- 알림 테이블
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    notification_type ENUM('goal', 'ranking', 'pvp', 'system') NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_unread (user_id, is_read)
);

-- 디플로마별 과목 정보
CREATE TABLE diploma_subjects (
    id INT PRIMARY KEY AUTO_INCREMENT,
    diploma VARCHAR(50) NOT NULL,
    required_subjects JSON NOT NULL COMMENT '["국어", "수학", "영어", "역사", "통합과학", "사회"]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 초기 디플로마-과목 매핑 데이터
INSERT INTO diploma_subjects (diploma, required_subjects) VALUES
('IT', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('공학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('수학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('물리', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('화학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('생명과학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('IB(자연)', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('인문학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('국제어문', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('사회과학', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('경제경영', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('IB(인문)', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('예술', '["국어", "수학", "영어", "역사", "통합과학", "사회"]'),
('체육', '["국어", "수학", "영어", "역사", "통합과학", "사회"]');

-- 관리자 계정 생성 (비밀번호: admin)
-- 실제 운영시 반드시 변경 필요
INSERT INTO users (username, password_hash, salt, student_number, diploma, grade, email)
VALUES ('admin', 
        '$2a$10$N9qo8uLOickgx2ZMRZoMye', 
        'randomsalt123456',
        'ADMIN001', 
        'IT', 
        1, 
        'admin@cnsa.hs.kr');

INSERT INTO admin_users (user_id) VALUES (1);

-- 인덱스 최적화
CREATE INDEX idx_todos_user_date_completed ON todos(user_id, scheduled_date, completed);
CREATE INDEX idx_study_data_user_time ON study_data(user_id, start_time);
CREATE INDEX idx_problem_attempts_user_time ON problem_attempts(user_id, attempted_at);

-- 뷰: 현재 시즌 활성 사용자 랭킹
CREATE VIEW v_current_season_ranking AS
SELECT 
    sr.ranking_id,
    sr.user_id,
    u.username,
    u.diploma,
    sr.total_points,
    sr.daily_points,
    sr.rank_position,
    s.season_name,
    sr.last_updated
FROM season_rankings sr
JOIN users u ON sr.user_id = u.user_id
JOIN seasons s ON sr.season_id = s.season_id
WHERE s.is_active = TRUE
ORDER BY sr.total_points DESC;

-- 뷰: 사용자별 목표 달성률
CREATE VIEW v_user_goal_achievement AS
SELECT 
    g.user_id,
    u.username,
    g.season_id,
    s.season_name,
    COUNT(*) as total_goals,
    SUM(CASE WHEN g.achieved THEN 1 ELSE 0 END) as achieved_goals,
    ROUND(SUM(CASE WHEN g.achieved THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as achievement_rate
FROM goals g
JOIN users u ON g.user_id = u.user_id
JOIN seasons s ON g.season_id = s.season_id
GROUP BY g.user_id, u.username, g.season_id, s.season_name;
