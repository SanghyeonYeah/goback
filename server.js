const express = require('express');
const path = require('path');
const session = require('express-session');
const helmet = require('helmet');
const compression = require('compression');
const cookieParser = require('cookie-parser');
const csrf = require('csurf');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
require('dotenv').config();

const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

const app = express();

/* ===== 보안 설정 ===== */
app.set('trust proxy', 1);
app.use(helmet());
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
    sameSite: 'lax'
  }
}));

/* ===== CSRF ===== */
const csrfProtection = csrf({ cookie: true });

// CSRF 보호 적용
app.use((req, res, next) => {
  csrfProtection(req, res, (err) => {
    if (err) return next(err);

    // GET은 검사 없이 토큰 생성만
    return next();
  });
});

// EJS에서 csrfToken 변수를 사용할 수 있게 설정
app.use((req, res, next) => {
  try {
    if (req.csrfToken) {
      res.locals.csrfToken = req.csrfToken();
    }
  } catch (e) {
    res.locals.csrfToken = null;
  }

  res.locals.user = req.session.user || null;
  next();
});

/* ===== Rate Limit ===== */
app.use('/api/', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100
}));

app.use('/auth/login', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5
}));

/* ===== Routes ===== */
app.use('/auth', require('./routes/auth'));
app.use('/api/todo', require('./routes/todo'));
app.use('/api/problem', require('./routes/problem'));
app.use('/api/ranking', require('./routes/ranking'));
app.use('/api/pvp', require('./routes/pvp'));
app.use('/admin', require('./routes/admin'));
app.use('/user', require('./routes/user'));

/* ===== 메인 페이지 ===== */
app.get('/', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.redirect('/home');
});

app.get('/home', (req, res) => {
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

    res.render('todo', { todos: result.rows });

  } catch (err) {
    console.error(err);
    res.render('todo', { todos: [] });
  }
});

/* ===== Calendar ===== */
app.get('/calendar', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');

  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${now.getMonth() + 1}`;

  res.render('calendar', {
    user: req.session.user,
    currentMonth
  });
});

/* ===== Ranking ===== */
app.get('/ranking', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.render('ranking', { user: req.session.user });
});

/* ===== Problem ===== */
app.get('/problem', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');

  res.render('problem', {
    user: req.session.user,
    stats: { totalSolved: 0, correctRate: 0, streak: 0 }
  });
});

/* ===== PVP ===== */
app.get('/pvp', (req, res) => {
  if (!req.session.user) return res.redirect('/auth/login');
  res.render('pvp', { user: req.session.user, match: null });
});

/* ===== 404 ===== */
app.use((req, res) => {
  res.status(404).send('페이지를 찾을 수 없습니다.');
});

/* ===== Error Handler ===== */
app.use((err, req, res, next) => {
  console.error(err.stack);

  if (err.code === 'EBADCSRFTOKEN') {
    return res.status(403).send('세션이 만료되었습니다.');
  }

  res.status(500).send('서버 오류');
});

/* ===== Start Server ===== */
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Railway 서버 실행 중: ${PORT}`);
});

module.exports = { app, pool };