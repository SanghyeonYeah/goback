const express = require('express');
const path = require('path');
const session = require('express-session');
const helmet = require('helmet');
const compression = require('compression');
const cookieParser = require('cookie-parser');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
require('dotenv').config();

const pool = require('./database/init');

const app = express();

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
    secure: process.env.NODE_ENV === 'production',
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

/* ===== 라우터 연결 ===== */
const authRouter = require('./routes/auth');
app.use('/auth', authRouter);

/* ===== 메인 페이지 ===== */
app.get('/', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.redirect('/home');
});

app.get('/home', async (req, res) => {
  try {
    if (!req.session.user) return res.redirect('/auth/login');
    
    res.render('home', { 
      user: req.session.user,
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
    
    const result = await pool.query(
      `SELECT id, subject, task, completed 
       FROM todos 
       WHERE user_id = $1 AND date = CURRENT_DATE 
       ORDER BY created_at ASC`,
      [userId]
    );
    
    res.render('todo', { 
      todos: result.rows
    });
  } catch (err) {
    console.error('Todo 페이지 오류:', err);
    res.render('todo', { 
      todos: []
    });
  }
});

app.post('/todo', apiLimiter, async (req, res) => {
  try {
    if (!req.session.user) return res.status(401).json({ error: '인증 필요' });
    
    // Todo 생성 로직
    res.json({ success: true });
  } catch (err) {
    console.error('Todo 생성 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

app.put('/todo/:id', apiLimiter, async (req, res) => {
  try {
    if (!req.session.user) return res.status(401).json({ error: '인증 필요' });
    
    // Todo 수정 로직
    res.json({ success: true });
  } catch (err) {
    console.error('Todo 수정 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

app.delete('/todo/:id', apiLimiter, async (req, res) => {
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
    
    res.render('calendar', { 
      user: req.session.user,
      currentMonth
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
    
    res.render('ranking', { 
      user: req.session.user
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
    
    res.render('problem', { 
      user: req.session.user,
      stats: { totalSolved: 0, correctRate: 0, streak: 0 }
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
    
    res.render('pvp', { 
      user: req.session.user,
      match: null
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
const PORT = process.env.PORT || 3000;

async function startServer() {
  try {
    // DB 연결 테스트 (타임아웃 추가)
    const timeout = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('DB 연결 타임아웃')), 5000)
    );
    
    await Promise.race([
      pool.query('SELECT NOW()'),
      timeout
    ]);
    
    console.log('✅ DB 연결 성공');
  } catch (err) {
    console.error('⚠️ DB 연결 실패:', err.message);
    console.log('⚠️ DB 없이 서버 시작 (일부 기능 제한)');
  }
  
  // 서버 시작 - DB 연결 실패해도 서버는 시작
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`✅ 서버 실행 중: 포트 ${PORT}`);
    console.log(`🔍 환경: ${process.env.NODE_ENV || 'development'}`);
  });
}

startServer();

module.exports = { app, pool };