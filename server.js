const express = require('express');
const path = require('path');
const session = require('express-session');
const helmet = require('helmet');
const compression = require('compression');
const cookieParser = require('cookie-parser');
const csrf = require('csurf');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
const bcrypt = require('bcrypt');
require('dotenv').config();

const { Pool } = require('pg');

const app = express();

// PostgreSQL 연결
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

pool.on('error', (err) => {
  console.error('PostgreSQL 연결 오류:', err);
});

/* ===== 보안 설정 ===== */
app.set('trust proxy', 1);
app.use(helmet({
  contentSecurityPolicy: false
}));
app.use(compression());
app.use(morgan('combined'));

/* ===== Body Parser ===== */
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(cookieParser());

/* ===== 정적 파일 ===== */
app.use(express.static(path.join(__dirname, 'public')));

/* ===== View Engine ===== */
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

/* ===== Session ===== */
app.use(session({
  name: 'studyplanner.sid',
  secret: process.env.SESSION_SECRET || 'railway-secret-change-this',
  resave: false,
  saveUninitialized: false,
  cookie: {
    secure: true, // 프로덕션 환경 (HTTPS)
    httpOnly: true,
    sameSite: 'lax',
    maxAge: 24 * 60 * 60 * 1000
  }
}));

/* ===== 템플릿 전역 변수 ===== */
app.use((req, res, next) => {
  res.locals.user = req.session.user || null;
  next();
});

/* ===== Rate Limit ===== */
const apiLimiter = rateLimit({ 
  windowMs: 15 * 60 * 1000, 
  max: 100 
});

const loginLimiter = rateLimit({ 
  windowMs: 15 * 60 * 1000, 
  max: 5 
});

/* ===== CSRF Protection (POST/PUT/DELETE만) ===== */
const csrfProtection = csrf({ cookie: false });

// CSRF 토큰 생성 헬퍼 (GET 페이지용)
const generateCsrfToken = (req) => {
  try {
    if (req.session && req.session.user) {
      return req.csrfToken();
    }
    return null;
  } catch (err) {
    return null;
  }
};

/* ===== Helper Functions ===== */
async function getUserByUsername(username) {
  try {
    const result = await pool.query(
      'SELECT * FROM users WHERE username = $1',
      [username]
    );
    return result.rows[0];
  } catch (err) {
    console.error('DB 조회 오류:', err);
    throw err;
  }
}

/* ===== Auth Routes ===== */
// GET: CSRF 미들웨어 없이 토큰만 생성
app.get('/auth/login', (req, res) => {
  try {
    if (req.session.user) return res.redirect('/home');
    
    // 로그인 페이지는 세션 없어도 토큰 필요 (POST 요청용)
    const csrfToken = generateCsrfToken(req);
    res.render('login', { csrfToken });
  } catch (err) {
    console.error('로그인 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

// POST: CSRF 검증 적용
app.post('/auth/login', loginLimiter, csrfProtection, async (req, res) => {
  const { username, password } = req.body;
  
  try {
    const user = await getUserByUsername(username);
    
    if (!user) {
      return res.json({ error: '사용자를 찾을 수 없습니다.' });
    }
    
    const match = await bcrypt.compare(password, user.password);
    
    if (!match) {
      return res.json({ error: '비밀번호가 틀렸습니다.' });
    }
    
    req.session.user = { 
      id: user.id, 
      name: user.username 
    };
    
    return res.json({ success: true, redirect: '/home' });
  } catch (err) {
    console.error('로그인 오류:', err);
    return res.json({ error: '로그인 중 서버 오류가 발생했습니다.' });
  }
});

app.get('/auth/logout', (req, res) => {
  req.session.destroy((err) => {
    if (err) console.error('로그아웃 오류:', err);
    res.redirect('/auth/login');
  });
});

/* ===== 메인 페이지 ===== */
app.get('/', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.redirect('/home');
});

app.get('/home', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const csrfToken = generateCsrfToken(req);
    
    res.render('home', { 
      user: req.session.user,
      csrfToken,
      dday: 0,
      season: null,
      todos: { total: 0, completed: 0 },
      seasonRanking: [],
      dailyRanking: [],
      todayTodos: []
    });
  } catch (err) {
    console.error('홈 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

/* ===== Todo ===== */
app.get('/todo', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const userId = req.session.user.id;
    const csrfToken = generateCsrfToken(req);
    
    const result = await pool.query(
      `SELECT id, subject, task, completed 
       FROM todos 
       WHERE user_id = $1 AND date = CURRENT_DATE 
       ORDER BY created_at ASC`,
      [userId]
    );
    
    res.render('todo', { 
      todos: result.rows,
      csrfToken
    });
  } catch (err) {
    console.error('Todo 페이지 오류:', err);
    const csrfToken = generateCsrfToken(req);
    res.render('todo', { 
      todos: [],
      csrfToken
    });
  }
});

app.post('/api/todo', apiLimiter, csrfProtection, async (req, res) => {
  try {
    if (!req.session.user) return res.status(401).json({ error: '인증 필요' });
    
    // Todo 생성 로직
    res.json({ success: true });
  } catch (err) {
    console.error('Todo 생성 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

app.put('/api/todo/:id', apiLimiter, csrfProtection, async (req, res) => {
  try {
    if (!req.session.user) return res.status(401).json({ error: '인증 필요' });
    
    // Todo 수정 로직
    res.json({ success: true });
  } catch (err) {
    console.error('Todo 수정 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

app.delete('/api/todo/:id', apiLimiter, csrfProtection, async (req, res) => {
  try {
    if (!req.session.user) return res.status(401).json({ error: '인증 필요' });
    
    // Todo 삭제 로직
    res.json({ success: true });
  } catch (err) {
    console.error('Todo 삭제 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

/* ===== Calendar ===== */
app.get('/calendar', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const now = new Date();
    const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const csrfToken = generateCsrfToken(req);
    
    res.render('calendar', { 
      user: req.session.user,
      currentMonth,
      csrfToken
    });
  } catch (err) {
    console.error('캘린더 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

/* ===== Ranking ===== */
app.get('/ranking', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const csrfToken = generateCsrfToken(req);
    
    res.render('ranking', { 
      user: req.session.user,
      csrfToken
    });
  } catch (err) {
    console.error('랭킹 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

/* ===== Problem ===== */
app.get('/problem', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const csrfToken = generateCsrfToken(req);
    
    res.render('problem', { 
      user: req.session.user,
      stats: { totalSolved: 0, correctRate: 0, streak: 0 },
      csrfToken
    });
  } catch (err) {
    console.error('문제 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

/* ===== PVP ===== */
app.get('/pvp', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    const csrfToken = generateCsrfToken(req);
    
    res.render('pvp', { 
      user: req.session.user,
      match: null,
      csrfToken
    });
  } catch (err) {
    console.error('PVP 페이지 오류:', err);
    res.status(500).send('페이지 로드 오류');
  }
});

/* ===== Health Check (Railway용) ===== */
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok' });
});

/* ===== 404 ===== */
app.use((req, res) => {
  res.status(404).send('페이지를 찾을 수 없습니다.');
});

/* ===== Error Handler ===== */
app.use((err, req, res, next) => {
  console.error('서버 오류:', err.stack);
  
  if (err.code === 'EBADCSRFTOKEN') {
    return res.status(403).send('세션이 만료되었습니다. 페이지를 새로고침해주세요.');
  }
  
  res.status(500).send('서버 오류가 발생했습니다.');
});

/* ===== DB 연결 테스트 후 서버 시작 ===== */
const PORT = process.env.PORT || 8080;

async function startServer() {
  try {
    // DB 연결 테스트
    await pool.query('SELECT NOW()');
    console.log('✅ DB 연결 성공');
    
    // 서버 시작
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`✅ 서버 실행 중: 포트 ${PORT}`);
      console.log(`📍 환경: ${process.env.NODE_ENV || 'development'}`);
    });
  } catch (err) {
    console.error('❌ 서버 시작 실패:', err);
    process.exit(1);
  }
}

startServer();

module.exports = { app, pool };