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
      `SELECT id, subject, task, completed
       FROM todos
       WHERE user_id = $1 AND date = CURRENT_DATE`,
      [req.session.user.id]
    );
    const todos = result.rows || [];
    const completedCount = todos.filter(t => t.completed).length;

    res.render('todo', {
      todos,
      progress: {
        total: todos.length,
        completed: completedCount,
        percentage: todos.length ? Math.round((completedCount / todos.length) * 100) : 0
      },
      goals: {}
    });
  } catch (err) {
    console.error('TODO 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

app.post('/todo', authMiddleware, csrfProtection, apiLimiter, async (req, res) => {
  res.json({ success: true });
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
        todos: []
      });
    }
    
    res.render('calendar', {
      user: req.session.user,
      currentMonth: `${year}-${month}`,
      year,
      month,
      calendarDays,
      stats: {}
    });
  } catch (err) {
    console.error('캘린더 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

/* ===== RANKING ===== */
app.get('/ranking', authMiddleware, async (req, res) => {
  try {
    // 현재 시즌 조회 (description 컬럼 제외)
    const seasonResult = await pool.query(
      `SELECT id, name, start_date, end_date 
       FROM seasons 
       WHERE CURRENT_DATE BETWEEN start_date AND end_date 
       ORDER BY start_date DESC 
       LIMIT 1`
    );
    
    const currentSeason = seasonResult.rows[0] || null;
    
    // 시즌 랭킹 조회
    let seasonRanking = [];
    if (currentSeason) {
      const rankingResult = await pool.query(
        `SELECT u.username, sr.total_points, sr.rank
         FROM season_rankings sr
         JOIN users u ON sr.user_id = u.id
         WHERE sr.season_id = $1
         ORDER BY sr.rank
         LIMIT 10`,
        [currentSeason.id]
      );
      seasonRanking = rankingResult.rows;
    }
    
    // 일일 랭킹 조회
    const dailyResult = await pool.query(
      `SELECT u.username, COUNT(*) as completed_count
       FROM todos t
       JOIN users u ON t.user_id = u.id
       WHERE t.date = CURRENT_DATE AND t.completed = true
       GROUP BY u.id, u.username
       ORDER BY completed_count DESC
       LIMIT 10`
    );
    
    res.render('ranking', {
      user: req.session.user,
      currentSeason,
      seasonRanking,
      dailyRanking: dailyResult.rows
    });
  } catch (err) {
    console.error('랭킹 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

/* ===== PVP ===== */
app.get('/pvp', authMiddleware, (req, res) => {
  res.render('pvp', { user: req.session.user, match: null });
});

/* ===== PROBLEM (추가) ===== */
app.get('/problem', authMiddleware, async (req, res) => {
  try {
    // 문제 목록 조회
    const problemsResult = await pool.query(
      `SELECT id, title, difficulty, category, solved 
       FROM problems 
       ORDER BY id DESC 
       LIMIT 50`
    );
    
    // 사용자 통계 조회
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
    // 테이블이 없는 경우 기본값으로 렌더링
    res.render('problem', { 
      user: req.session.user, 
      problems: [],
      stats: {
        totalSolved: 0,
        totalProblems: 0,
        solvingRate: 0
      }
    });
  }
});

/* ===== MYPAGE (추가) ===== */
app.get('/mypage', authMiddleware, async (req, res) => {
  try {
    // 사용자 통계 조회
    const statsResult = await pool.query(
      `SELECT 
        COUNT(*) FILTER (WHERE completed = true) as completed_total,
        COUNT(*) as total_todos
       FROM todos
       WHERE user_id = $1`,
      [req.session.user.id]
    );
    
    // 목표 조회
    const goalsResult = await pool.query(
      `SELECT korean, math, english, science, social 
       FROM goals 
       WHERE user_id = $1 
       LIMIT 1`,
      [req.session.user.id]
    );
    
    const goals = goalsResult.rows[0] || {
      korean: 3,
      math: 3,
      english: 3,
      science: 3,
      social: 3
    };
    
    res.render('mypage', {
      user: req.session.user,
      stats: statsResult.rows[0] || { completed_total: 0, total_todos: 0 },
      goals
    });
  } catch (err) {
    console.error('마이페이지 오류:', err);
    // 에러 발생 시 기본값으로 렌더링
    res.render('mypage', {
      user: req.session.user,
      stats: { completed_total: 0, total_todos: 0 },
      goals: {
        korean: 3,
        math: 3,
        english: 3,
        science: 3,
        social: 3
      }
    });
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