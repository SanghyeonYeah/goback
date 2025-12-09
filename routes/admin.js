const express = require('express');
const router = express.Router();
const pool = require('../database/init');
const { body, validationResult } = require('express-validator');

// 관리자 인증 미들웨어
const requireAdmin = async (req, res, next) => {
  if (!req.session.user) {
    return res.redirect('/auth/login');
  }

  try {
    const result = await pool.query(
      'SELECT * FROM admin_users WHERE user_id = $1',
      [req.session.user.id]
    );

    if (result.rows.length === 0) {
      return res.status(403).send('관리자 권한이 필요합니다.');
    }

    req.adminPermissions = result.rows[0].permissions;
    next();
  } catch (error) {
    console.error('관리자 확인 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
};

// 관리자 대시보드
router.get('/', requireAdmin, (req, res) => {
  res.render('admin/dashboard', { user: req.session.user });
});

// 문제 등록 페이지
router.get('/problem', requireAdmin, async (req, res) => {
  try {
    const seasons = await pool.query(
      'SELECT * FROM seasons ORDER BY created_at DESC'
    );

    res.render('admin/problem', {
      user: req.session.user,
      seasons: seasons.rows,
      error: null
    });
  } catch (error) {
    console.error('문제 등록 페이지 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
});

// 문제 등록 처리
router.post('/problem', requireAdmin, [
  body('title').trim().notEmpty().withMessage('제목을 입력하세요'),
  body('content').trim().notEmpty().withMessage('문제 내용을 입력하세요'),
  body('subject').trim().notEmpty().withMessage('과목을 선택하세요'),
  body('type').trim().notEmpty().withMessage('문제 유형을 선택하세요'),
  body('base_points').isInt({ min: 2, max: 5 }).withMessage('배점은 2-5점 사이여야 합니다'),
  body('answer').trim().notEmpty().withMessage('정답을 입력하세요'),
  body('season_id').isInt().withMessage('시즌을 선택하세요')
], async (req, res) => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    const seasons = await pool.query('SELECT * FROM seasons ORDER BY created_at DESC');
    return res.render('admin/problem', {
      user: req.session.user,
      seasons: seasons.rows,
      error: errors.array()[0].msg
    });
  }

  const { title, content, subject, type, base_points, answer, choices, season_id } = req.body;

  try {
    // 선택지 JSON 처리
    let choicesJson = null;
    if (choices) {
      try {
        choicesJson = JSON.parse(choices);
      } catch {
        choicesJson = null;
      }
    }

    await pool.query(
      `INSERT INTO problems (title, content, subject, type, base_points, answer, choices, season_id, created_by)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [title, content, subject, type, base_points, answer, choicesJson, season_id, req.session.user.id]
    );

    res.redirect('/admin/problem?success=true');
  } catch (error) {
    console.error('문제 등록 오류:', error);
    const seasons = await pool.query('SELECT * FROM seasons ORDER BY created_at DESC');
    res.render('admin/problem', {
      user: req.session.user,
      seasons: seasons.rows,
      error: '문제 등록 중 오류가 발생했습니다.'
    });
  }
});

// 문제 목록 조회 (관리자)
router.get('/problems', requireAdmin, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT p.*, s.name as season_name, u.username as creator_name
       FROM problems p
       LEFT JOIN seasons s ON p.season_id = s.id
       LEFT JOIN users u ON p.created_by = u.id
       ORDER BY p.created_at DESC`
    );

    res.render('admin/problems', {
      user: req.session.user,
      problems: result.rows
    });
  } catch (error) {
    console.error('문제 목록 조회 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
});

// 문제 삭제
router.delete('/problem/:id', requireAdmin, async (req, res) => {
  const { id } = req.params;

  try {
    await pool.query('DELETE FROM problems WHERE id = $1', [id]);
    res.json({ success: true });
  } catch (error) {
    console.error('문제 삭제 오류:', error);
    res.status(500).json({ error: '문제 삭제 중 오류가 발생했습니다.' });
  }
});

// 시즌 등록 페이지
router.get('/season', requireAdmin, async (req, res) => {
  try {
    const seasons = await pool.query(
      'SELECT * FROM seasons ORDER BY start_date DESC'
    );

    res.render('admin/season', {
      user: req.session.user,
      seasons: seasons.rows,
      error: null
    });
  } catch (error) {
    console.error('시즌 페이지 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
});

// 시즌 등록 처리
router.post('/season', requireAdmin, [
  body('name').trim().notEmpty().withMessage('시즌 이름을 입력하세요'),
  body('start_date').isDate().withMessage('시작 날짜를 입력하세요'),
  body('end_date').isDate().withMessage('종료 날짜를 입력하세요')
], async (req, res) => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    const seasons = await pool.query('SELECT * FROM seasons ORDER BY start_date DESC');
    return res.render('admin/season', {
      user: req.session.user,
      seasons: seasons.rows,
      error: errors.array()[0].msg
    });
  }

  const { name, start_date, end_date, reward } = req.body;

  try {
    // 날짜 검증
    if (new Date(end_date) <= new Date(start_date)) {
      const seasons = await pool.query('SELECT * FROM seasons ORDER BY start_date DESC');
      return res.render('admin/season', {
        user: req.session.user,
        seasons: seasons.rows,
        error: '종료 날짜는 시작 날짜보다 이후여야 합니다.'
      });
    }

    await pool.query(
      `INSERT INTO seasons (name, start_date, end_date, reward, is_active)
       VALUES ($1, $2, $3, $4, TRUE)`,
      [name, start_date, end_date, reward || null]
    );

    res.redirect('/admin/season?success=true');
  } catch (error) {
    console.error('시즌 등록 오류:', error);
    const seasons = await pool.query('SELECT * FROM seasons ORDER BY start_date DESC');
    res.render('admin/season', {
      user: req.session.user,
      seasons: seasons.rows,
      error: '시즌 등록 중 오류가 발생했습니다.'
    });
  }
});

// 시즌 활성화/비활성화
router.put('/season/:id/toggle', requireAdmin, async (req, res) => {
  const { id } = req.params;

  try {
    await pool.query(
      'UPDATE seasons SET is_active = NOT is_active WHERE id = $1',
      [id]
    );

    res.json({ success: true });
  } catch (error) {
    console.error('시즌 토글 오류:', error);
    res.status(500).json({ error: '시즌 상태 변경 중 오류가 발생했습니다.' });
  }
});

// 시즌 삭제
router.delete('/season/:id', requireAdmin, async (req, res) => {
  const { id } = req.params;

  try {
    await pool.query('DELETE FROM seasons WHERE id = $1', [id]);
    res.json({ success: true });
  } catch (error) {
    console.error('시즌 삭제 오류:', error);
    res.status(500).json({ error: '시즌 삭제 중 오류가 발생했습니다.' });
  }
});

// 통계 대시보드
router.get('/stats', requireAdmin, async (req, res) => {
  try {
    // 전체 사용자 수
    const userCount = await pool.query('SELECT COUNT(*) as count FROM users');

    // 전체 문제 수
    const problemCount = await pool.query('SELECT COUNT(*) as count FROM problems');

    // 오늘 제출된 답안 수
    const todaySubmissions = await pool.query(
      `SELECT COUNT(*) as count FROM problem_submissions
       WHERE submitted_at >= CURRENT_DATE`
    );

    // 활성 시즌 수
    const activeSeason = await pool.query(
      'SELECT COUNT(*) as count FROM seasons WHERE is_active = TRUE'
    );

    // 과목별 문제 분포
    const subjectDistribution = await pool.query(
      `SELECT subject, COUNT(*) as count
       FROM problems
       GROUP BY subject
       ORDER BY count DESC`
    );

    // 최근 가입자
    const recentUsers = await pool.query(
      `SELECT username, student_id, created_at
       FROM users
       ORDER BY created_at DESC
       LIMIT 10`
    );

    res.render('admin/stats', {
      user: req.session.user,
      stats: {
        userCount: userCount.rows[0].count,
        problemCount: problemCount.rows[0].count,
        todaySubmissions: todaySubmissions.rows[0].count,
        activeSeason: activeSeason.rows[0].count,
        subjectDistribution: subjectDistribution.rows,
        recentUsers: recentUsers.rows
      }
    });
  } catch (error) {
    console.error('통계 조회 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
});

// 사용자 관리
router.get('/users', requireAdmin, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT u.id, u.username, u.email, u.student_id, u.diploma, u.grade,
              u.created_at, u.last_login,
              COALESCE(s.total_score, 0) as total_score
       FROM users u
       LEFT JOIN (
         SELECT user_id, SUM(total_score) as total_score
         FROM scores
         GROUP BY user_id
       ) s ON u.id = s.user_id
       ORDER BY u.created_at DESC`
    );

    res.render('admin/users', {
      user: req.session.user,
      users: result.rows
    });
  } catch (error) {
    console.error('사용자 목록 조회 오류:', error);
    res.status(500).send('서버 오류가 발생했습니다.');
  }
});

// 사용자 삭제
router.delete('/user/:id', requireAdmin, async (req, res) => {
  const { id } = req.params;

  // 관리자 본인은 삭제 불가
  if (parseInt(id) === req.session.user.id) {
    return res.status(400).json({ error: '본인 계정은 삭제할 수 없습니다.' });
  }

  try {
    await pool.query('DELETE FROM users WHERE id = $1', [id]);
    res.json({ success: true });
  } catch (error) {
    console.error('사용자 삭제 오류:', error);
    res.status(500).json({ error: '사용자 삭제 중 오류가 발생했습니다.' });
  }
});

module.exports = router;