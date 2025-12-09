-- 사용자 테이블 (salt 컬럼 제거, google_id는 선택사항으로 유지)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    student_id VARCHAR(20) UNIQUE NOT NULL,
    diploma VARCHAR(50) NOT NULL,
    grade INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CHECK (email LIKE '%@cnsa.hs.kr')
);

-- 세션 테이블
CREATE TABLE sessions (
    sid VARCHAR(255) PRIMARY KEY,
    sess JSON NOT NULL,
    expire TIMESTAMP NOT NULL
);

CREATE INDEX sessions_expire_idx ON sessions(expire);

-- 목표 테이블
CREATE TABLE goals (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    season_id INTEGER,
    korean INTEGER CHECK (korean BETWEEN 1 AND 5),
    math INTEGER CHECK (math BETWEEN 1 AND 5),
    social INTEGER CHECK (social BETWEEN 1 AND 5),
    science INTEGER CHECK (science BETWEEN 1 AND 5),
    english INTEGER CHECK (english BETWEEN 1 AND 5),
    history INTEGER CHECK (history BETWEEN 1 AND 5),
    physics INTEGER CHECK (physics BETWEEN 1 AND 5),
    chemistry INTEGER CHECK (chemistry BETWEEN 1 AND 5),
    biology INTEGER CHECK (biology BETWEEN 1 AND 5),
    study_period INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Todo 테이블
CREATE TABLE todos (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    goal_id INTEGER REFERENCES goals(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    subject VARCHAR(50) NOT NULL,
    task TEXT NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX todos_user_date_idx ON todos(user_id, date);

-- 달력 완료 기록 테이블
CREATE TABLE calendar_records (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    status VARCHAR(10) CHECK (status IN ('완', '실')),
    bonus_applied BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, date)
);

-- 시즌 테이블
CREATE TABLE seasons (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    reward TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 문제 테이블
CREATE TABLE problems (
    id SERIAL PRIMARY KEY,
    season_id INTEGER REFERENCES seasons(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    subject VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    base_points INTEGER CHECK (base_points BETWEEN 2 AND 5),
    answer TEXT NOT NULL,
    choices JSONB,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 문제 풀이 기록 테이블
CREATE TABLE problem_submissions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    problem_id INTEGER REFERENCES problems(id) ON DELETE CASCADE,
    answer TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    points_earned INTEGER NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX submissions_user_idx ON problem_submissions(user_id);

-- 점수 테이블 (랭킹용)
CREATE TABLE scores (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    season_id INTEGER REFERENCES seasons(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_score INTEGER DEFAULT 0,
    daily_score INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, season_id, date)
);

CREATE INDEX scores_season_total_idx ON scores(season_id, total_score DESC);
CREATE INDEX scores_date_daily_idx ON scores(date, daily_score DESC);

-- PVP 매치 테이블
CREATE TABLE pvp_matches (
    id SERIAL PRIMARY KEY,
    player1_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    player2_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    problem_id INTEGER REFERENCES problems(id),
    player1_answer TEXT,
    player2_answer TEXT,
    player1_time INTEGER,
    player2_time INTEGER,
    winner_id INTEGER REFERENCES users(id),
    result VARCHAR(20) CHECK (result IN ('player1_win', 'player2_win', 'draw', 'ongoing')),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

-- PVP 통계 테이블
CREATE TABLE pvp_stats (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    draws INTEGER DEFAULT 0,
    total_matches INTEGER DEFAULT 0
);

-- 관리자 권한 테이블
CREATE TABLE admin_users (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    permissions JSONB DEFAULT '{"can_add_problems": true, "can_manage_seasons": true}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 함수: 일일 Todo 완료 체크
CREATE OR REPLACE FUNCTION check_daily_completion(p_user_id INTEGER, p_date DATE)
RETURNS VARCHAR AS $$
DECLARE
    total_todos INTEGER;
    completed_todos INTEGER;
BEGIN
    SELECT COUNT(*), SUM(CASE WHEN completed THEN 1 ELSE 0 END)
    INTO total_todos, completed_todos
    FROM todos
    WHERE user_id = p_user_id AND date = p_date;
    
    IF total_todos = 0 THEN
        RETURN NULL;
    ELSIF completed_todos = total_todos THEN
        RETURN '완';
    ELSE
        RETURN '실';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 함수: 문제 점수 계산 (디플로마 보너스 적용)
CREATE OR REPLACE FUNCTION calculate_problem_points(
    p_base_points INTEGER,
    p_subject VARCHAR,
    p_diploma VARCHAR,
    p_bonus_date DATE,
    p_user_id INTEGER
)
RETURNS INTEGER AS $$
DECLARE
    final_points NUMERIC;
    date_bonus NUMERIC := 0;
    diploma_bonus NUMERIC := 0;
BEGIN
    final_points := p_base_points;
    
    -- 전날 완료 보너스 체크
    IF p_bonus_date IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM calendar_records
            WHERE user_id = p_user_id
            AND date = p_bonus_date
            AND status = '완'
        ) THEN
            date_bonus := 0.1;
        END IF;
    END IF;
    
    -- 디플로마 보너스
    IF p_diploma IN ('IT', '공학', '수학', '물리', '화학', '생명과학', 'IB(자연)') THEN
        -- 이과
        IF p_subject IN ('물리', '화학', '생명과학', '수학') THEN
            diploma_bonus := 0;
        ELSE
            diploma_bonus := -0.1;
        END IF;
    ELSIF p_diploma IN ('인문학(문학/사학/철학)', '국제어문', '사회과학', '경제경영', 'IB(인문)') THEN
        -- 문과
        IF p_subject IN ('국어', '영어', '사회', '역사') THEN
            diploma_bonus := 0;
        ELSE
            diploma_bonus := 0.1;
        END IF;
    ELSIF p_diploma IN ('예술', '체육') THEN
        -- 예체
        diploma_bonus := 0.1;
    END IF;
    
    final_points := final_points * (1 + date_bonus + diploma_bonus);
    
    RETURN ROUND(final_points);
END;
$$ LANGUAGE plpgsql;

-- 트리거: Todo 완료 시 달력 업데이트
CREATE OR REPLACE FUNCTION update_calendar_on_todo_complete()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.completed = TRUE AND OLD.completed = FALSE THEN
        INSERT INTO calendar_records (user_id, date, status)
        VALUES (NEW.user_id, NEW.date, check_daily_completion(NEW.user_id, NEW.date))
        ON CONFLICT (user_id, date)
        DO UPDATE SET status = check_daily_completion(NEW.user_id, NEW.date);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER todo_completion_trigger
AFTER UPDATE ON todos
FOR EACH ROW
EXECUTE FUNCTION update_calendar_on_todo_complete();

-- 인덱스 생성
CREATE INDEX users_email_idx ON users(email);
CREATE INDEX problems_season_idx ON problems(season_id);
CREATE INDEX todos_goal_idx ON todos(goal_id);
CREATE INDEX calendar_user_date_idx ON calendar_records(user_id, date);

-- 뷰: 현재 시즌 랭킹
CREATE OR REPLACE VIEW current_season_ranking AS
SELECT 
    u.id,
    u.username,
    u.student_id,
    s.total_score,
    s.season_id,
    ROW_NUMBER() OVER (PARTITION BY s.season_id ORDER BY s.total_score DESC) as rank
FROM users u
JOIN scores s ON u.id = s.user_id
JOIN seasons se ON s.season_id = se.id
WHERE se.is_active = TRUE;

-- 뷰: 일일 랭킹
CREATE OR REPLACE VIEW daily_ranking AS
SELECT 
    u.id,
    u.username,
    u.student_id,
    s.daily_score,
    s.date,
    ROW_NUMBER() OVER (PARTITION BY s.date ORDER BY s.daily_score DESC) as rank
FROM users u
JOIN scores s ON u.id = s.user_id
WHERE s.date = CURRENT_DATE;