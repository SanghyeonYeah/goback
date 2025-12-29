// 환경변수 로드
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config();
}

const express = require('express');
const path = require('path');
const session = require('express-session');
const helmet = require('helmet');
const compression = require('compression');
const cookieParser = require('cookie-parser');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
const csurf = require('csurf');
const pool = require('./database/init');
const { generateTodos } = require('./ai/todoGenerator'); // AI 모듈 import

const { authMiddleware } = require('./middleware/auth');

const app = express();

/* ===== 보안 ===== */
app.set('trust proxy', 1);
app.use(helmet({ contentSecurityPolicy: false }));
app.use(compression());
app.use(morgan('combined'));

/* ===== Parser ===== */
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(cookieParser());

/* ===== Static ===== */
app.use(express.static(path.join(__dirname, 'public')));

/* ===== View ===== */
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

/* ===== Session ===== */
app.use(session({
  name: 'studyplanner.sid',
  secret: process.env.SESSION_SECRET || 'dev-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    secure: process.env.NODE_ENV === 'production',
    httpOnly: true,
    sameSite: 'lax',
    maxAge: 24 * 60 * 60 * 1000
  }
}));

/* ===== CSRF (Auth 라우터 제외) ===== */
const csrfProtection = csurf({ cookie: false });

app.use((req, res, next) => {
  if (req.path.startsWith('/auth')) {
    return next();
  }
  csrfProtection(req, res, next);
});

/* ===== 템플릿 전역 ===== */
app.use((req, res, next) => {
  res.locals.user = req.session.user || null;
  res.locals.csrfToken = req.csrfToken ? req.csrfToken() : '';
  next();
});

/* ===== Rate Limit ===== */
const apiLimiter = rateLimit({ 
  windowMs: 15 * 60 * 1000, 
  max: 100,
  message: 'Too many requests'
});

/* ===== Auth Router ===== */
app.use('/auth', require('./routes/auth'));

app.get('/', (req, res) => {
  if (req.session.user) return res.redirect('/home');
  res.redirect('/auth/login');
});

/* ===== HOME ===== */
app.get('/home', authMiddleware, async (req, res) => {
  try {
    res.render('home', {
      user: req.session.user,
      dday: 0,
      season: null,
      todos: { total: 0, completed: 0 },
      seasonRanking: [],
      dailyRanking: [],
      todayTodos: [],
      goals: {},
      stats: {},
      match: null
    });
  } catch (err) {
    console.error('홈 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

/* ===== TODO ===== */
app.get('/todo', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, subject, task, difficulty, completed
       FROM todos
       WHERE user_id = $1 AND date = CURRENT_DATE`,
      [req.session.user.id]
    );
    const todos = result.rows || [];
    const completedCount = todos.filter(t => t.completed).length;

    // 목표 조회
    const goalsResult = await pool.query(
      `SELECT korean, math, english, social, science, history
       FROM goals
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT 1`,
      [req.session.user.id]
    );
    const goals = goalsResult.rows[0] || {};

    res.render('todo', {
      todos,
      progress: {
        total: todos.length,
        completed: completedCount,
        percentage: todos.length ? Math.round((completedCount / todos.length) * 100) : 0
      },
      goals
    });
  } catch (err) {
    console.error('TODO 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

app.post('/todo', authMiddleware, csrfProtection, apiLimiter, async (req, res) => {
  res.json({ success: true });
});

/* ===== TODO 토글 ===== */
app.post('/todos/:id/toggle', authMiddleware, csrfProtection, async (req, res) => {
  const userId = req.session.user.id;
  const todoId = req.params.id;

  try {
    // Todo 소유권 + 상태 확인
    const todoResult = await pool.query(
      `SELECT completed, date FROM todos WHERE id = $1 AND user_id = $2`,
      [todoId, userId]
    );

    if (todoResult.rows.length === 0) {
      return res.status(404).send('존재하지 않는 Todo');
    }

    const currentCompleted = todoResult.rows[0].completed;
    const date = todoResult.rows[0].date;

    // 상태 토글
    await pool.query(
      `UPDATE todos
       SET completed = $1,
           completed_at = CASE WHEN $1 THEN NOW() ELSE NULL END
       WHERE id = $2`,
      [!currentCompleted, todoId]
    );

    res.redirect('/todo');
  } catch (err) {
    console.error('Todo 토글 오류:', err);
    res.status(500).send('처리 실패');
  }
});

/* ===== CALENDAR ===== */
app.get('/calendar', authMiddleware, async (req, res) => {
  try {
    const now = new Date();
    const year = parseInt(req.query.year) || now.getFullYear();
    const month = parseInt(req.query.month) || now.getMonth() + 1;
    
    // 캘린더 데이터 생성
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    const startDayOfWeek = firstDay.getDay();
    const daysInMonth = lastDay.getDate();
    
    const calendarDays = [];
    
    // 이전 달 빈 칸
    for (let i = 0; i < startDayOfWeek; i++) {
      calendarDays.push({ isEmpty: true });
    }
    
    // 현재 달 날짜
    for (let date = 1; date <= daysInMonth; date++) {
      const currentDate = new Date(year, month - 1, date);
      const dayOfWeek = currentDate.getDay();
      
      calendarDays.push({
        date,
        isEmpty: false,
        isToday: currentDate.toDateString() === now.toDateString(),
        isSunday: dayOfWeek === 0,
        isSaturday: dayOfWeek === 6,
        status: null // 'complete', 'incomplete' 또는 null
      });
    }

    // 해당 월의 완료 기록 조회
    const recordsResult = await pool.query(
      `SELECT date, 
              CASE 
                WHEN COUNT(*) = COUNT(*) FILTER (WHERE completed = true) THEN 'complete'
                ELSE 'incomplete'
              END as status
       FROM todos
       WHERE user_id = $1 
       AND EXTRACT(YEAR FROM date) = $2
       AND EXTRACT(MONTH FROM date) = $3
       GROUP BY date`,
      [req.session.user.id, year, month]
    );

    // 기록을 캘린더에 반영
    recordsResult.rows.forEach(record => {
      const day = new Date(record.date).getDate();
      const dayIndex = startDayOfWeek + day - 1;
      if (calendarDays[dayIndex]) {
        calendarDays[dayIndex].status = record.status;
      }
    });

    // 통계 계산
    const statsResult = await pool.query(
      `SELECT 
        COUNT(DISTINCT date) FILTER (
          WHERE completed = true 
          AND EXTRACT(MONTH FROM date) = $2
        ) as completed_days,
        COUNT(DISTINCT date) as total_days
       FROM todos
       WHERE user_id = $1
       AND EXTRACT(YEAR FROM date) = $2
       AND EXTRACT(MONTH FROM date) = $3`,
      [req.session.user.id, year, month]
    );

    const stats = statsResult.rows[0] || { completed_days: 0, total_days: 0 };
    
    res.render('calendar', {
      user: req.session.user,
      currentMonth: `${year}년 ${month}월`,
      year,
      month,
      calendarDays,
      stats: {
        monthlyGoal: daysInMonth,
        completedDays: parseInt(stats.completed_days) || 0,
        streak: 0, // TODO: 연속 달성 계산
        achievementRate: stats.total_days > 0 
          ? Math.round((stats.completed_days / stats.total_days) * 100) 
          : 0
      }
    });
  } catch (err) {
    console.error('캘린더 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

/* ===== RANKING ===== */
app.get('/ranking', authMiddleware, async (req, res) => {
  try {
    // 현재 시즌 조회
    const seasonResult = await pool.query(
      `SELECT id, name, start_date, end_date 
       FROM seasons 
       WHERE CURRENT_DATE BETWEEN start_date AND end_date 
       ORDER BY start_date DESC 
       LIMIT 1`
    );
    
    const currentSeason = seasonResult.rows[0] || null;
    
    // 시즌 랭킹 조회
    let rankings = [];
    if (currentSeason) {
      const rankingResult = await pool.query(
        `SELECT u.id, u.username, u.diploma, 
                COALESCE(sr.total_score, 0) as total_score
         FROM users u
         LEFT JOIN season_rankings sr ON u.id = sr.user_id AND sr.season_id = $1
         ORDER BY total_score DESC
         LIMIT 10`,
        [currentSeason.id]
      );
      rankings = rankingResult.rows;
    }
    
    res.render('ranking', {
      user: req.session.user,
      session: req.session,
      currentSeason,
      rankings
    });
  } catch (err) {
    console.error('랭킹 오류:', err);
    res.render('ranking', {
      user: req.session.user,
      session: req.session,
      currentSeason: null,
      rankings: []
    });
  }
});

/* ===== PVP ===== */
app.get('/pvp', authMiddleware, (req, res) => {
  res.render('pvp', { user: req.session.user, match: null });
});

/* ===== PROBLEM ===== */
app.get('/problem', authMiddleware, async (req, res) => {
  try {
    const problemsResult = await pool.query(
      `SELECT id, title, difficulty, category, solved 
       FROM problems 
       ORDER BY id DESC 
       LIMIT 50`
    );
    
    const statsResult = await pool.query(
      `SELECT 
        COUNT(*) FILTER (WHERE solved = true) as total_solved,
        COUNT(*) as total_problems
       FROM problems`
    );
    
    const stats = statsResult.rows[0] || { total_solved: 0, total_problems: 0 };
    
    res.render('problem', { 
      user: req.session.user, 
      problems: problemsResult.rows,
      stats: {
        totalSolved: stats.total_solved || 0,
        totalProblems: stats.total_problems || 0,
        solvingRate: stats.total_problems > 0 
          ? Math.round((stats.total_solved / stats.total_problems) * 100) 
          : 0
      }
    });
  } catch (err) {
    console.error('문제 페이지 오류:', err);
    res.render('problem', { 
      user: req.session.user, 
      problems: [],
      stats: { totalSolved: 0, totalProblems: 0, solvingRate: 0 }
    });
  }
});

/* ===== MYPAGE ===== */
app.get('/mypage', authMiddleware, async (req, res) => {
  try {
    const statsResult = await pool.query(
      `SELECT 
        COUNT(*) FILTER (WHERE completed = true) as completed_total,
        COUNT(*) as total_todos
       FROM todos
       WHERE user_id = $1`,
      [req.session.user.id]
    );
    
    const goalsResult = await pool.query(
      `SELECT korean, math, english, science, social, history, study_period
       FROM goals 
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT 1`,
      [req.session.user.id]
    );
    
    const goals = goalsResult.rows[0] || {
      korean: 3,
      math: 3,
      english: 3,
      science: 3,
      social: 3,
      history: 3,
      study_period: 14
    };
    
    res.render('mypage', {
      user: req.session.user,
      stats: statsResult.rows[0] || { completed_total: 0, total_todos: 0 },
      goals
    });
  } catch (err) {
    console.error('마이페이지 오류:', err);
    res.render('mypage', {
      user: req.session.user,
      stats: { completed_total: 0, total_todos: 0 },
      goals: { korean: 3, math: 3, english: 3, science: 3, social: 3, history: 3, study_period: 14 }
    });
  }
});

/* ===== USER UPDATE GOALS (AI 통합) ===== */
app.post('/user/update-goals', authMiddleware, csrfProtection, async (req, res) => {
  const userId = req.session.user.id;
  const { korean, math, english, social, science, history, studyPeriod } = req.body;
  
  try {
    // 1. 기존 목표 확인
    const existingResult = await pool.query(
      `SELECT id FROM goals WHERE user_id=$1 ORDER BY created_at DESC LIMIT 1`,
      [userId]
    );
    
    if (existingResult.rows.length > 0) {
      // 업데이트
      await pool.query(
        `UPDATE goals 
         SET korean=$1, math=$2, english=$3, social=$4, science=$5, history=$6, study_period=$7, created_at=NOW()
         WHERE id=$8`,
        [korean, math, english, social, science, history, studyPeriod, existingResult.rows[0].id]
      );
    } else {
      // 새 목표 INSERT
      await pool.query(
        `INSERT INTO goals (user_id, korean, math, english, social, science, history, study_period, created_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,NOW())`,
        [userId, korean, math, english, social, science, history, studyPeriod]
      );
    }
    
    // 2. 세션 업데이트
    req.session.user.goals = { 
      korean, math, english, social, science, history, 
      study_period: studyPeriod 
    };
    req.session.save();
    
    // 3. 기존 Todo 삭제 (오늘 이후)
    await pool.query(
      `DELETE FROM todos WHERE user_id=$1 AND date >= CURRENT_DATE`,
      [userId]
    );
    
    // 4. 기존 Todo 조회 (오늘 이후) - AI에 컨텍스트 제공
    const existingTodosResult = await pool.query(
      `SELECT subject, task, date FROM todos WHERE user_id=$1 AND date >= CURRENT_DATE`,
      [userId]
    );
    
    // 5. AI Todo 생성
    const aiTodos = await generateTodos(
      { korean, math, english, social, science, history },
      parseInt(studyPeriod),
      userId,
      existingTodosResult.rows
    );
    
    // 6. Todo DB 저장
    for (const todo of aiTodos) {
      await pool.query(
        `INSERT INTO todos (user_id, subject, task, difficulty, date, completed)
         VALUES ($1,$2,$3,$4,$5,false)`,
        [userId, todo.subject, todo.task, todo.difficulty, todo.date]
      );
    }
    
    console.log(`✅ ${userId}번 사용자: AI Todo ${aiTodos.length}개 생성 완료`);
    res.redirect('/todo');
    
  } catch (err) {
    console.error('[GOALS UPDATE ERROR]', err);
    res.status(500).send('목표 업데이트 또는 AI Todo 생성 실패');
  }
});

/* ===== Health ===== */
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

/* ===== 404 ===== */
app.use((req, res) => {
  res.status(404).send('Not Found');
});

/* ===== Error ===== */
app.use((err, req, res, next) => {
  if (err.code === 'EBADCSRFTOKEN') {
    console.error('CSRF 토큰 오류:', req.path);
    return res.status(403).send('CSRF 토큰 오류');
  }
  console.error('서버 오류:', err.stack);
  res.status(500).send('Server Error');
});

/* ===== Start ===== */
const PORT = Number(process.env.PORT) || 3000;

pool.query('SELECT NOW()')
  .then(() => console.log('✅ DB 연결 성공'))
  .catch(e => console.error('❌ DB 오류', e.message));

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on ${PORT}`);
});

module.exports = app;