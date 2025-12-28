const express = require('express');
const router = express.Router();
const pool = require('../database/init');

const requireAuth = (req, res, next) => {
  if (!req.session.user) {
    return res.redirect('/login');
  }
  next();
};

/**
 * 문제 목록 페이지
 * GET /problem
 */
router.get('/', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { subject, difficulty, type } = req.query;

  try {
    /* ===============================
       1. 문제 목록 조회
    =============================== */
    let query = `
      SELECT 
        p.id,
        p.title,
        p.subject,
        p.difficulty,
        p.type,
        p.base_points AS score,
        ROW_NUMBER() OVER (ORDER BY p.created_at ASC) AS number,
        CASE 
          WHEN ps.id IS NOT NULL THEN TRUE
          ELSE FALSE
        END AS solved
      FROM problems p
      LEFT JOIN problem_submissions ps
        ON ps.problem_id = p.id
       AND ps.user_id = $1
      JOIN seasons s ON p.season_id = s.id
      WHERE s.is_active = TRUE
    `;

    const params = [userId];

    if (subject) {
      params.push(subject);
      query += ` AND p.subject = $${params.length}`;
    }

    if (difficulty) {
      params.push(difficulty);
      query += ` AND p.difficulty = $${params.length}`;
    }

    if (type) {
      params.push(type);
      query += ` AND p.type = $${params.length}`;
    }

    query += ` ORDER BY p.created_at DESC`;

    const problemsResult = await pool.query(query, params);

    /* ===============================
       2. 통계 계산
    =============================== */

    // 총 푼 문제 수
    const solvedResult = await pool.query(
      `SELECT COUNT(*) FROM problem_submissions WHERE user_id = $1`,
      [userId]
    );

    const totalSolved = Number(solvedResult.rows[0].count);

    // 총 점수
    const scoreResult = await pool.query(
      `SELECT COALESCE(SUM(points_earned), 0) AS total
       FROM problem_submissions
       WHERE user_id = $1 AND is_correct = TRUE`,
      [userId]
    );

    const totalScore = Number(scoreResult.rows[0].total);

    // 정확도
    const totalSubmitResult = await pool.query(
      `SELECT COUNT(*) FROM problem_submissions WHERE user_id = $1`,
      [userId]
    );

    const totalSubmissions = Number(totalSubmitResult.rows[0].count);
    const accuracy =
      totalSubmissions === 0
        ? 0
        : Math.round((totalSolved / totalSubmissions) * 100);

    /* ===============================
       3. 렌더링
    =============================== */
    res.render('problem', {
      problems: problemsResult.rows,
      stats: {
        totalSolved,
        totalScore,
        accuracy
      }
    });
  } catch (err) {
    console.error('문제 목록 조회 오류:', err);
    res.status(500).send('문제 목록을 불러오는 중 오류가 발생했습니다.');
  }
});

/**
 * 문제 상세 페이지
 * GET /problem/:id
 */
router.get('/:id', requireAuth, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await pool.query(
      `SELECT id, title, content, subject, difficulty, type
       FROM problems
       WHERE id = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).send('문제를 찾을 수 없습니다.');
    }

    res.render('problem_detail', {
      problem: result.rows[0]
    });
  } catch (err) {
    console.error('문제 상세 조회 오류:', err);
    res.status(500).send('문제 조회 중 오류 발생');
  }
});

/**
 * 문제 풀이 페이지
 * GET /problem/:id/solve
 */
router.get('/:id/solve', requireAuth, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await pool.query(
      `SELECT id, title, content, choices
       FROM problems
       WHERE id = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).send('문제를 찾을 수 없습니다.');
    }

    res.render('problem_solve', {
      problem: result.rows[0]
    });
  } catch (err) {
    console.error('문제 풀이 페이지 오류:', err);
    res.status(500).send('문제 로딩 실패');
  }
});

module.exports = router;
