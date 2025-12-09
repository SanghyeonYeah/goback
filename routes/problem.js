const express = require('express');
const router = express.Router();
const pool = require('../database/init');

const requireAuth = (req, res, next) => {
  if (!req.session.user) {
    return res.status(401).json({ error: '로그인이 필요합니다.' });
  }
  next();
};

// 문제 목록 조회
router.get('/', requireAuth, async (req, res) => {
  const { subject, seasonId } = req.query;

  try {
    let query = `
      SELECT p.*, s.name as season_name, s.is_active
      FROM problems p
      LEFT JOIN seasons s ON p.season_id = s.id
      WHERE 1=1
    `;
    const params = [];

    if (subject) {
      params.push(subject);
      query += ` AND p.subject = $${params.length}`;
    }

    if (seasonId) {
      params.push(seasonId);
      query += ` AND p.season_id = $${params.length}`;
    } else {
      // 기본적으로 활성 시즌의 문제만
      query += ` AND s.is_active = TRUE`;
    }

    query += ' ORDER BY p.created_at DESC';

    const result = await pool.query(query, params);
    res.json({ problems: result.rows });
  } catch (error) {
    console.error('문제 조회 오류:', error);
    res.status(500).json({ error: '문제 조회 중 오류가 발생했습니다.' });
  }
});

// 특정 문제 조회
router.get('/:id', requireAuth, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await pool.query(
      `SELECT p.*, s.name as season_name
       FROM problems p
       LEFT JOIN seasons s ON p.season_id = s.id
       WHERE p.id = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: '문제를 찾을 수 없습니다.' });
    }

    // 답은 제외하고 반환
    const problem = result.rows[0];
    delete problem.answer;

    res.json({ problem });
  } catch (error) {
    console.error('문제 조회 오류:', error);
    res.status(500).json({ error: '문제 조회 중 오류가 발생했습니다.' });
  }
});

// 문제 풀이 제출
router.post('/:id/submit', requireAuth, async (req, res) => {
  const { id } = req.params;
  const { answer } = req.body;
  const userId = req.session.user.id;

  try {
    // 문제 정보 조회
    const problemResult = await pool.query(
      `SELECT p.*, s.id as season_id
       FROM problems p
       JOIN seasons s ON p.season_id = s.id
       WHERE p.id = $1 AND s.is_active = TRUE`,
      [id]
    );

    if (problemResult.rows.length === 0) {
      return res.status(404).json({ error: '문제를 찾을 수 없거나 시즌이 종료되었습니다.' });
    }

    const problem = problemResult.rows[0];

    // 중복 제출 확인
    const duplicateCheck = await pool.query(
      'SELECT id FROM problem_submissions WHERE user_id = $1 AND problem_id = $2',
      [userId, id]
    );

    if (duplicateCheck.rows.length > 0) {
      return res.status(400).json({ error: '이미 제출한 문제입니다.' });
    }

    // 정답 확인
    const isCorrect = answer.trim().toLowerCase() === problem.answer.trim().toLowerCase();

    // 점수 계산
    let pointsEarned = 0;
    if (isCorrect) {
      // 전날 완료 보너스 확인
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().split('T')[0];

      const result = await pool.query(
        `SELECT calculate_problem_points($1, $2, $3, $4, $5) as points`,
        [problem.base_points, problem.subject, req.session.user.diploma, yesterdayStr, userId]
      );

      pointsEarned = result.rows[0].points;
    }

    // 제출 기록 저장
    await pool.query(
      `INSERT INTO problem_submissions (user_id, problem_id, answer, is_correct, points_earned)
       VALUES ($1, $2, $3, $4, $5)`,
      [userId, id, answer, isCorrect, pointsEarned]
    );

    // 점수 업데이트
    if (isCorrect && pointsEarned > 0) {
      const today = new Date().toISOString().split('T')[0];

      await pool.query(
        `INSERT INTO scores (user_id, season_id, date, total_score, daily_score)
         VALUES ($1, $2, $3, $4, $4)
         ON CONFLICT (user_id, season_id, date)
         DO UPDATE SET
           total_score = scores.total_score + $4,
           daily_score = scores.daily_score + $4,
           updated_at = CURRENT_TIMESTAMP`,
        [userId, problem.season_id, today, pointsEarned]
      );
    }

    res.json({
      success: true,
      isCorrect,
      pointsEarned,
      correctAnswer: isCorrect ? null : problem.answer
    });
  } catch (error) {
    console.error('문제 제출 오류:', error);
    res.status(500).json({ error: '문제 제출 중 오류가 발생했습니다.' });
  }
});

// 내 풀이 기록 조회
router.get('/my/submissions', requireAuth, async (req, res) => {
  const userId = req.session.user.id;

  try {
    const result = await pool.query(
      `SELECT ps.*, p.title, p.subject, p.base_points
       FROM problem_submissions ps
       JOIN problems p ON ps.problem_id = p.id
       WHERE ps.user_id = $1
       ORDER BY ps.submitted_at DESC
       LIMIT 50`,
      [userId]
    );

    res.json({ submissions: result.rows });
  } catch (error) {
    console.error('풀이 기록 조회 오류:', error);
    res.status(500).json({ error: '풀이 기록 조회 중 오류가 발생했습니다.' });
  }
});

// PVP용 랜덤 문제 조회
router.get('/random/pvp', requireAuth, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, title, content, subject, choices
       FROM problems
       WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
       ORDER BY RANDOM()
       LIMIT 1`
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: '사용 가능한 문제가 없습니다.' });
    }

    res.json({ problem: result.rows[0] });
  } catch (error) {
    console.error('랜덤 문제 조회 오류:', error);
    res.status(500).json({ error: '랜덤 문제 조회 중 오류가 발생했습니다.' });
  }
});

module.exports = router;