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

/* ===== 템플릿 전역 ===== */
app.use((req, res, next) => {
  res.locals.user = req.session.user || null;
  next();
});

/* ===== Rate Limit ===== */
const apiLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 100 });

/* ===== Auth Router ===== */
app.use('/auth', require('./routes/auth'));

/* ===== HOME (단 하나) ===== */
app.get('/home', authMiddleware, async (req, res) => {
  try {
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
    console.error('홈 오류:', err);
    res.status(500).send('페이지 오류');
  }
});

/* ===== TODO ===== */
app.get('/todo', authMiddleware, async (req, res) => {
  const result = await pool.query(
    `SELECT id, subject, task, completed
     FROM todos
     WHERE user_id = $1 AND date = CURRENT_DATE`,
    [req.session.user.id]
  );
  res.render('todo', { todos: result.rows });
});

app.post('/todo', authMiddleware, apiLimiter, async (req, res) => {
  res.json({ success: true });
});

/* ===== 기타 페이지 ===== */
app.get('/calendar', authMiddleware, (req, res) => {
  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${now.getMonth() + 1}`;
  res.render('calendar', { user: req.session.user, currentMonth });
});

app.get('/ranking', authMiddleware, (req, res) => {
  res.render('ranking', { user: req.session.user });
});

app.get('/pvp', authMiddleware, (req, res) => {
  res.render('pvp', { user: req.session.user });
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
  console.error(err.stack);
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
