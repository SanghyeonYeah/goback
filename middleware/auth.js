const User = require('../models/user');

/* =========================
   인증 필수 미들웨어
========================= */
const authMiddleware = (req, res, next) => {
  if (!req.session || !req.session.user) {
    if (req.xhr || req.headers.accept?.includes('json')) {
      return res.status(401).json({ 
        error: '로그인이 필요합니다.',
        redirectTo: '/auth/login'
      });
    }
    return res.redirect('/auth/login');
  }
  next();
};

/* =========================
   관리자 권한 필수
========================= */
const requireAdmin = async (req, res, next) => {
  if (!req.session || !req.session.user) {
    if (req.xhr || req.headers.accept?.includes('json')) {
      return res.status(401).json({ 
        error: '로그인이 필요합니다.',
        redirectTo: '/auth/login'
      });
    }
    return res.redirect('/auth/login');
  }

  try {
    const isAdmin = await User.isAdmin(req.session.user.id);

    if (!isAdmin) {
      if (req.xhr || req.headers.accept?.includes('json')) {
        return res.status(403).json({ error: '관리자 권한이 필요합니다.' });
      }
      return res.status(403).send('관리자 권한이 필요합니다.');
    }

    next();
  } catch (error) {
    console.error('관리자 확인 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
};

/* =========================
   로그인 상태 체크 (선택)
========================= */
const checkAuth = (req, res, next) => {
  req.isAuthenticated = !!(req.session && req.session.user);
  req.currentUser = req.session?.user || null;
  next();
};

/* =========================
   이미 로그인한 경우 리디렉션
========================= */
const redirectIfAuthenticated = (req, res, next) => {
  if (req.session && req.session.user) {
    return res.redirect('/home');
  }
  next();
};

/* =========================
   세션 만료 갱신
========================= */
const refreshSession = (req, res, next) => {
  if (req.session && req.session.user) {
    req.session.touch();
  }
  next();
};

/* =========================
   사용자 소유권 확인
========================= */
const checkOwnership = (userIdParam = 'userId') => {
  return (req, res, next) => {
    if (!req.session || !req.session.user) {
      return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    const requestedUserId = parseInt(
      req.params[userIdParam] || req.body[userIdParam],
      10
    );

    if (requestedUserId !== req.session.user.id) {
      return res.status(403).json({ error: '권한이 없습니다.' });
    }

    next();
  };
};

/* =========================
   API 키 인증
========================= */
const requireApiKey = (req, res, next) => {
  const apiKey = req.headers['x-api-key'] || req.query.apiKey;

  if (!apiKey) {
    return res.status(401).json({ error: 'API 키가 필요합니다.' });
  }

  if (apiKey !== process.env.API_KEY) {
    return res.status(403).json({ error: '유효하지 않은 API 키입니다.' });
  }

  next();
};

/* =========================
   관리자 세부 권한 체크
========================= */
const checkPermission = (permission) => {
  return async (req, res, next) => {
    if (!req.session || !req.session.user) {
      return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    try {
      const pool = require('../database/init');
      const result = await pool.query(
        'SELECT permissions FROM admin_users WHERE user_id = $1',
        [req.session.user.id]
      );

      if (!result.rows.length || !result.rows[0].permissions[permission]) {
        return res.status(403).json({ 
          error: `${permission} 권한이 없습니다.` 
        });
      }

      next();
    } catch (error) {
      console.error('권한 확인 오류:', error);
      res.status(500).json({ error: '서버 오류가 발생했습니다.' });
    }
  };
};

/* =========================
   학교 이메일 도메인 체크
========================= */
const checkSchoolDomain = (req, res, next) => {
  const email = req.body.email;
  const allowedDomain = process.env.ALLOWED_DOMAIN || 'cnsa.hs.kr';

  if (!email || !email.endsWith(`@${allowedDomain}`)) {
    return res.status(403).json({ 
      error: `학교 이메일(@${allowedDomain})만 사용할 수 있습니다.` 
    });
  }

  next();
};

/* =========================
   활동 로깅
========================= */
const logActivity = (req, res, next) => {
  if (req.session && req.session.user) {
    req.session.lastActivity = new Date();
    console.log(
      `[활동] ${req.session.user.username} | ${req.method} ${req.path}`
    );
  }
  next();
};

/* =========================
   세션 타임아웃 체크
========================= */
const checkSessionTimeout = (timeoutMinutes = 60) => {
  return (req, res, next) => {
    if (req.session?.user && req.session.lastActivity) {
      const diff =
        (Date.now() - new Date(req.session.lastActivity)) / 1000 / 60;

      if (diff > timeoutMinutes) {
        req.session.destroy();
        if (req.xhr || req.headers.accept?.includes('json')) {
          return res.status(401).json({
            error: '세션이 만료되었습니다.',
            redirectTo: '/auth/login'
          });
        }
        return res.redirect('/auth/login');
      }
    }
    next();
  };
};

/* =========================
   다중 세션 방지
========================= */
const preventMultipleSessions = async (req, res, next) => {
  if (!req.session?.user) return next();

  try {
    const pool = require('../database/init');
    const { rows } = await pool.query(
      'SELECT last_session_id FROM users WHERE id = $1',
      [req.session.user.id]
    );

    if (rows[0]?.last_session_id && rows[0].last_session_id !== req.sessionID) {
      req.session.destroy();
      return res.status(401).json({
        error: '다른 기기에서 로그인되어 세션이 종료되었습니다.',
        redirectTo: '/auth/login'
      });
    }

    await pool.query(
      'UPDATE users SET last_session_id = $1 WHERE id = $2',
      [req.sessionID, req.session.user.id]
    );
  } catch (error) {
    console.error('세션 확인 오류:', error);
  }

  next();
};

/* =========================
   EXPORT
========================= */
module.exports = {
  authMiddleware,      // ⭐ 핵심: server.js에서 쓰는 이름
  requireAdmin,
  checkAuth,
  redirectIfAuthenticated,
  refreshSession,
  checkOwnership,
  requireApiKey,
  checkPermission,
  checkSchoolDomain,
  logActivity,
  checkSessionTimeout,
  preventMultipleSessions
};
