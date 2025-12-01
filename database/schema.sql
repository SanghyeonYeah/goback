- Users Table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    diploma VARCHAR(50) NOT NULL,
    grade_level INT NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Session Table
CREATE TABLE sessions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    session_key VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Goals Table
CREATE TABLE goals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT,
    korean_target INT,
    math_target INT,
    social_target INT,
    science_target INT,
    english_target INT,
    history_target INT,
    physics_target INT,
    chemistry_target INT,
    biology_target INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Todo Table
CREATE TABLE todos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    goal_id INT,
    subject VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);

-- Calendar Plan Table
CREATE TABLE calendar_plans (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_date DATE NOT NULL,
    status VARCHAR(10),
    completion_rate DECIMAL(5,2),
    bonus_score_percentage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, plan_date)
);

-- Season Table
CREATE TABLE seasons (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    prize_description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_season_dates (start_date, end_date)
);

-- Problems Table
CREATE TABLE problems (
    id INT PRIMARY KEY AUTO_INCREMENT,
    season_id INT,
    subject VARCHAR(50) NOT NULL,
    source VARCHAR(100),
    content TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    explanation TEXT,
    score_point INT NOT NULL,
    difficulty_level INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE SET NULL
);

-- User Problem Results Table
CREATE TABLE user_problem_results (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    problem_id INT NOT NULL,
    season_id INT,
    user_answer VARCHAR(255),
    is_correct BOOLEAN,
    score_earned INT,
    diploma_adjusted_score INT,
    solved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE SET NULL
);

-- Daily Ranking Table
CREATE TABLE daily_rankings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    ranking_date DATE NOT NULL,
    total_score INT,
    rank INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_daily_rank (user_id, ranking_date)
);

-- Seasonal Ranking Table
CREATE TABLE seasonal_rankings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT NOT NULL,
    total_score INT,
    rank INT,
    goal_achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE,
    UNIQUE KEY unique_seasonal_rank (user_id, season_id)
);

-- PVP Match History Table
CREATE TABLE pvp_matches (
    id INT PRIMARY KEY AUTO_INCREMENT,
    season_id INT,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    problem_id INT,
    player1_answer VARCHAR(255),
    player2_answer VARCHAR(255),
    time_limit_seconds INT DEFAULT 300,
    player1_completed BOOLEAN,
    player2_completed BOOLEAN,
    winner_id INT,
    match_result VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE SET NULL,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE SET NULL
);

-- Supplementary Learning Plan Table
CREATE TABLE supplementary_plans (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    season_id INT,
    subject VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    plan_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE
);

-- Indexes for Performance
CREATE INDEX idx_user_goals ON goals(user_id);
CREATE INDEX idx_user_todos ON todos(user_id);
CREATE INDEX idx_user_problems ON user_problem_results(user_id);
CREATE INDEX idx_season_problems ON problems(season_id);
CREATE INDEX idx_daily_ranking_date ON daily_rankings(ranking_date);
CREATE INDEX idx_seasonal_ranking_season ON seasonal_rankings(season_id);
CREATE INDEX idx_pvp_season ON pvp_matches(season_id);