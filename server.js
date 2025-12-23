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
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

const app = express();

/* ===== 보안 설정 ===== */
app.set('trust proxy', 1);
app.use(helmet({
  contentSecurityPolicy: false // EJS 사용시 필요
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
  secret: process.env.SESSION_SECRET || 'railway-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    secure: process.env.NODE_ENV === 'production',
    httpOnly: true,
    sameSite: process.env.NODE_ENV === 'production' ? 'none' : 'lax',
    maxAge: 24 * 60 * 60 * 1000 // 24시간
  }
}));

/* ===== CSRF ===== */
const csrfProtection = csrf({ cookie: false }); // 세션 기반으로 변경

// 템플릿 전역 변수
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

/* ===== Helper Functions ===== */
async function getUserByUsername(username) {
  const result = await pool.query(
    'SELECT * FROM users WHERE username = $1',
    [username]
  );
  return result.rows[0];
}

/* ===== Auth Routes (인라인) ===== */
app.get('/auth/login', csrfProtection, (req, res) => {
  if (req.session.user) return res.redirect('/home');
  res.render('login', { csrfToken: req.csrfToken() });
});

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
    console.error(err);
    return res.json({ error: '로그인 중 서버 오류가 발생했습니다.' });
  }
});

app.get('/auth/logout', (req, res) => {
  req.session.destroy((err) => {
    if (err) console.error(err);
    res.redirect('/auth/login');
  });
});

/* ===== Routes (외부 라우터가 있다면 활성화) ===== */
// app.use('/api/todo', apiLimiter, require('./routes/todo'));
// app.use('/api/problem', apiLimiter, require('./routes/problem'));
// app.use('/api/ranking', apiLimiter, require('./routes/ranking'));
// app.use('/api/pvp', apiLimiter, require('./routes/pvp'));
// app.use('/admin', require('./routes/admin'));
// app.use('/user', require('./routes/user'));

/* ===== 메인 페이지 ===== */
app.get('/', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.redirect('/home');
});

app.get('/home', csrfProtection, (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  
  res.render('home', { 
    user: req.session.user,
    csrfToken: req.csrfToken(),
    dday: 0,
    season: null,
    todos: { total: 0, completed: 0 },
    seasonRanking: [],
    dailyRanking: [],
    todayTodos: []
  });
});

/* ===== Todo ===== */
app.get('/todo', csrfProtection, async (req, res) => {
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
      todos: result.rows,
      csrfToken: req.csrfToken()
    });
  } catch (err) {
    console.error(err);
    res.render('todo', { 
      todos: [],
      csrfToken: req.csrfToken()
    });
  }
});

/* ===== Calendar ===== */
app.get('/calendar', csrfProtection, (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  
  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  
  res.render('calendar', { 
    user: req.session.user,
    currentMonth,
    csrfToken: req.csrfToken()
  });
});

/* ===== Ranking ===== */
app.get('/ranking', csrfProtection, (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  
  res.render('ranking', { 
    user: req.session.user,
    csrfToken: req.csrfToken()
  });
});

/* ===== Problem ===== */
app.get('/problem', csrfProtection, (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  
  res.render('problem', { 
    user: req.session.user,
    stats: { totalSolved: 0, correctRate: 0, streak: 0 },
    csrfToken: req.csrfToken()
  });
});

/* ===== PVP ===== */
app.get('/pvp', csrfProtection, (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  
  res.render('pvp', { 
    user: req.session.user,
    match: null,
    csrfToken: req.csrfToken()
  });
});

/* ===== 404 ===== */
app.use((req, res) => {
  res.status(404).send('페이지를 찾을 수 없습니다.');
});

/* ===== Error Handler ===== */
app.use((err, req, res, next) => {
  console.error(err.stack);
  
  if (err.code === 'EBADCSRFTOKEN') {
    return res.status(403).send('세션이 만료되었습니다. 페이지를 새로고침해주세요.');
  }
  
  res.status(500).send('서버 오류가 발생했습니다.');
});

/* ===== Start Server ===== */
const PORT = process.env.PORT || 8080;

app.listen(PORT, () => {
  console.log(`✅ 서버 실행 중: 포트 ${PORT}`);
});

module.exports = { app, pool };