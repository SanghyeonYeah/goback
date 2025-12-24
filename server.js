const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const { body, validationResult } = require('express-validator');
const pool = require('../database/init');
const csrf = require('csurf');
const fetch = require('node-fetch');
const { OAuth2Client } = require('google-auth-library');

const client = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// CSRF 보호 미들웨어 (선택적 적용)
const csrfProtection = csrf({ cookie: false });

/* ================================
   로그인 페이지
================================ */
router.get('/login', (req, res) => {
  if (req.session.user) {
    return res.redirect('/home');
  }

  res.render('login', {
    error: null,
    csrfToken: null // GET 페이지는 CSRF 불필요
  });
});

/* ================================
   회원가입 페이지
================================ */
router.get('/register', (req, res) => {
  res.render('register', {
    error: null,
    csrfToken: null
  });
});

/* ================================
   로그아웃
================================ */
router.get('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/auth/login');
  });
});

/* ================================
   일반 로그인 (CSRF 적용)
================================ */
router.post('/login', csrfProtection, [
  body('username').trim().notEmpty().withMessage('아이디를 입력하세요'),
  body('password').notEmpty().withMessage('비밀번호를 입력하세요')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.json({
      success: false,
      error: errors.array()[0].msg
    });
  }

  const { username, password } = req.body;

  try {
    const result = await pool.query(
      `SELECT id, username, email, password_hash, student_id, diploma, grade 
       FROM users 
       WHERE username = $1`,
      [username]
    );

    if (result.rows.length === 0) {
      return res.json({
        success: false,
        error: '아이디 또는 비밀번호가 올바르지 않습니다.'
      });
    }

    const user = result.rows[0];
    const isValid = await bcrypt.compare(password, user.password_hash);

    if (!isValid) {
      return res.json({
        success: false,
        error: '아이디 또는 비밀번호가 올바르지 않습니다.'
      });
    }

    req.session.user = user;

    await pool.query(
      'UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = $1',
      [user.id]
    );

    res.json({
      success: true,
      redirect: '/home'
    });
  } catch (err) {
    console.error(err);
    res.json({
      success: false,
      error: '로그인 중 오류가 발생했습니다.'
    });
  }
});

/* ================================
   GOOGLE OAUTH2 — 인증 페이지 이동 (CSRF 제외)
================================ */
router.get('/google', (req, res) => {
  const redirectUri =
    process.env.GOOGLE_REDIRECT_URI ||
    'http://localhost:3000/auth/google/callback';

  // state 파라미터로 CSRF 보호
  const state = Math.random().toString(36).substring(7);
  req.session.oauthState = state;

  const authUrl =
    `https://accounts.google.com/o/oauth2/v2/auth?` +
    `client_id=${process.env.GOOGLE_CLIENT_ID}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&response_type=code` +
    `&scope=openid%20email%20profile` +
    `&hd=cnsa.hs.kr` +
    `&state=${state}`;

  res.redirect(authUrl);
});

/* ================================
   GOOGLE OAUTH2 — 콜백 (CSRF 제외)
================================ */
router.get('/google/callback', async (req, res) => {
  const { code, state } = req.query;

  // State 검증으로 CSRF 보호
  if (!code || !state || state !== req.session.oauthState) {
    return res.redirect('/auth/login?error=invalid_state');
  }

  delete req.session.oauthState;

  try {
    const redirectUri =
      process.env.GOOGLE_REDIRECT_URI ||
      'http://localhost:3000/auth/google/callback';

    // 구글 토큰 교환
    const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        code,
        client_id: process.env.GOOGLE_CLIENT_ID,
        client_secret: process.env.GOOGLE_CLIENT_SECRET,
        redirect_uri: redirectUri,
        grant_type: 'authorization_code'
      })
    });

    const tokens = await tokenRes.json();

    if (tokens.error) {
      console.error('Google OAuth Error:', tokens);
      return res.redirect('/auth/login?error=oauth_failed');
    }

    // ID Token 검증
    const ticket = await client.verifyIdToken({
      idToken: tokens.id_token,
      audience: process.env.GOOGLE_CLIENT_ID
    });

    const payload = ticket.getPayload();
    const email = payload.email;
    const googleId = payload.sub;

    // 도메인 확인
    if (!email.endsWith('@cnsa.hs.kr')) {
      return res.redirect('/auth/login?error=invalid_domain');
    }

    // 기존 유저 확인
    const userCheck = await pool.query(
      `SELECT id, username, email, student_id, diploma, grade 
       FROM users WHERE google_id=$1 OR email=$2`,
      [googleId, email]
    );

    // 이미 존재 → 바로 로그인
    if (userCheck.rows.length > 0) {
      const user = userCheck.rows[0];

      req.session.user = user;

      await pool.query(
        'UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id=$1',
        [user.id]
      );

      return res.redirect('/home');
    }

    // 신규 사용자 → 구글 정보 저장 후 추가정보 입력 페이지로
    req.session.pendingGoogleAuth = {
      email,
      googleId,
      name: payload.name
    };

    return res.redirect('/auth/complete-registration');
  } catch (err) {
    console.error('Google 인증 오류:', err);
    res.redirect('/auth/login?error=server_error');
  }
});

/* ================================
   구글 OAuth 후 추가 회원가입 페이지
================================ */
router.get('/complete-registration', (req, res) => {
  if (!req.session.pendingGoogleAuth) {
    return res.redirect('/auth/register');
  }

  res.render('complete-registration', {
    email: req.session.pendingGoogleAuth.email,
    error: null,
    csrfToken: null
  });
});

/* ================================
   구글 OAuth 후 회원가입 처리 (CSRF 적용)
================================ */
router.post(
  '/complete-registration',
  csrfProtection,
  [
    body('username')
      .trim()
      .isLength({ min: 3, max: 20 })
      .withMessage('아이디는 3~20자입니다.'),
    body('password')
      .isLength({ min: 8 })
      .withMessage('비밀번호는 최소 8자 이상입니다.'),
    body('student_id').notEmpty().withMessage('학번을 입력하세요'),
    body('diploma').notEmpty().withMessage('디플로마를 선택하세요')
  ],
  async (req, res) => {
    if (!req.session.pendingGoogleAuth) {
      return res.json({
        success: false,
        error: '세션이 만료되었습니다.'
      });
    }

    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.json({
        success: false,
        error: errors.array()[0].msg
      });
    }

    const { username, password, student_id, diploma } = req.body;
    const { email, googleId } = req.session.pendingGoogleAuth;

    try {
      // 중복 검사
      const dupe = await pool.query(
        `SELECT id FROM users WHERE username=$1 OR student_id=$2`,
        [username, student_id]
      );

      if (dupe.rows.length > 0) {
        return res.json({
          success: false,
          error: '이미 존재하는 아이디 또는 학번입니다.'
        });
      }

      const passwordHash = await bcrypt.hash(password, 10);

      const insert = await pool.query(
        `INSERT INTO users (username, email, password_hash, student_id, diploma, google_id, grade)
         VALUES ($1, $2, $3, $4, $5, $6, 1)
         RETURNING id, username, email, student_id, diploma, grade`,
        [username, email, passwordHash, student_id, diploma, googleId]
      );

      const user = insert.rows[0];

      req.session.user = user;

      delete req.session.pendingGoogleAuth;

      res.json({
        success: true,
        redirect: '/home'
      });
    } catch (err) {
      console.error(err);
      res.json({
        success: false,
        error: '회원가입 중 오류 발생'
      });
    }
  }
);

/* ================================
   일반 회원가입 (CSRF 적용)
================================ */
router.post('/register', csrfProtection, [
  body('username').trim().isLength({ min: 3, max: 20 }).withMessage('아이디는 3-20자여야 합니다'),
  body('email').isEmail().withMessage('올바른 이메일을 입력하세요'),
  body('password').isLength({ min: 8 }).withMessage('비밀번호는 최소 8자 이상이어야 합니다'),
  body('student_id').notEmpty().withMessage('학번을 입력하세요'),
  body('diploma').notEmpty().withMessage('디플로마를 선택하세요')
], async (req, res) => {

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.json({
      success: false,
      error: errors.array()[0].msg
    });
  }

  const { username, email, password, student_id, diploma } = req.body;

  if (!email.endsWith('@cnsa.hs.kr')) {
    return res.json({
      success: false,
      error: '학교 이메일(@cnsa.hs.kr)만 사용할 수 있습니다.'
    });
  }

  try {
    const dupe = await pool.query(
      `SELECT id FROM users WHERE username=$1 OR email=$2 OR student_id=$3`,
      [username, email, student_id]
    );

    if (dupe.rows.length > 0) {
      return res.json({
        success: false,
        error: '이미 존재하는 아이디, 이메일 또는 학번입니다.'
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    await pool.query(
      `INSERT INTO users (username, email, password_hash, student_id, diploma, grade)
       VALUES ($1, $2, $3, $4, $5, 1)`,
      [username, email, passwordHash, student_id, diploma]
    );

    res.json({
      success: true,
      redirect: '/auth/login'
    });
  } catch (err) {
    console.error(err);
    res.json({
      success: false,
      error: '회원가입 중 오류 발생'
    });
  }
});

module.exports = router;