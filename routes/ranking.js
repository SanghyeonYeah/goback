const express = require('express');
const router = express.Router();
const pool = require('../database/init');

const requireAuth = (req, res, next) => {
  if (!req.session.user) {
    return res.status(401).json({ error: '로그인이 필요합니다.' });
  }
  next();
};

// 일일 랭킹 조회
router.get('/daily', requireAuth, async (req, res) => {
  const { date } = req.query;
  const targetDate = date || new Date().toISOString().split('T')[0];

  try {
    const result = await pool.query(
      `SELECT u.id, u.username, u.student_id, s.daily_score, s.date,
              ROW_NUMBER() OVER (ORDER BY s.daily_score DESC) as rank
       FROM users u
       JOIN scores s ON u.id = s.user_id
       WHERE s.date = $1 AND s.daily_score > 0
       ORDER BY s.daily_score DESC
       LIMIT 100`,
      [targetDate]
    );

    res.json({ ranking: result.rows, date: targetDate });
  } catch (error) {
    console.error('일일 랭킹 조회 오류:', error);
    res.status(500).json({ error: '일일 랭킹 조회 중 오류가 발생했습니다.' });
  }
});

// 시즌 랭킹 조회
router.get('/season', requireAuth, async (req, res) => {
  const { seasonId } = req.query;

  try {
    let query;
    let params = [];

    if (seasonId) {
      query = `
        SELECT u.id, u.username, u.student_id, s.total_score, s.season_id,
               ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
        FROM users u
        JOIN (
          SELECT user_id, season_id, SUM(total_score) as total_score
          FROM scores
          WHERE season_id = $1
          GROUP BY user_id, season_id
        ) s ON u.id = s.user_id
        WHERE s.total_score > 0
        ORDER BY s.total_score DESC
        LIMIT 100
      `;
      params = [seasonId];
    } else {
      query = `
        SELECT u.id, u.username, u.student_id, s.total_score, s.season_id, se.name as season_name,
               ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
        FROM users u
        JOIN (
          SELECT user_id, season_id, SUM(total_score) as total_score
          FROM scores
          WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
          GROUP BY user_id, season_id
        ) s ON u.id = s.user_id
        JOIN seasons se ON s.season_id = se.id
        WHERE s.total_score > 0
        ORDER BY s.total_score DESC
        LIMIT 100
      `;
    }

    const result = await pool.query(query, params);
    res.json({ ranking: result.rows });
  } catch (error) {
    console.error('시즌 랭킹 조회 오류:', error);
    res.status(500).json({ error: '시즌 랭킹 조회 중 오류가 발생했습니다.' });
  }
});

// 내 랭킹 조회
router.get('/my', requireAuth, async (req, res) => {
  const userId = req.session.user.id;

  try {
    // 오늘 일일 랭킹
    const dailyResult = await pool.query(
      `WITH ranked AS (
        SELECT user_id, daily_score,
               ROW_NUMBER() OVER (ORDER BY daily_score DESC) as rank
        FROM scores
        WHERE date = CURRENT_DATE
      )
      SELECT rank, daily_score FROM ranked WHERE user_id = $1`,
      [userId]
    );

    // 현재 시즌 랭킹
    const seasonResult = await pool.query(
      `WITH ranked AS (
        SELECT s.user_id, SUM(s.total_score) as total_score,
               ROW_NUMBER() OVER (ORDER BY SUM(s.total_score) DESC) as rank
        FROM scores s
        WHERE s.season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
        GROUP BY s.user_id
      )
      SELECT rank, total_score FROM ranked WHERE user_id = $1`,
      [userId]
    );

    res.json({
      daily: dailyResult.rows[0] || { rank: null, daily_score: 0 },
      season: seasonResult.rows[0] || { rank: null, total_score: 0 }
    });
  } catch (error) {
    console.error('내 랭킹 조회 오류:', error);
    res.status(500).json({ error: '내 랭킹 조회 중 오류가 발생했습니다.' });
  }
});

module.exports = router;