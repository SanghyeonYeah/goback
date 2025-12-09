const express = require('express');
const router = express.Router();
const pool = require('../database/init');

// 인증 미들웨어
const requireAuth = (req, res, next) => {
  if (!req.session.user) return res.redirect('/auth/login');
  next();
};

// 마이페이지 렌더
router.get('/mypage', requireAuth, async (req, res) => {
  const userId = req.session.user.id;

  try {
    const goalResult = await pool.query(
      'SELECT * FROM goals WHERE user_id = $1 ORDER BY id DESC LIMIT 1',
      [userId]
    );

    const goals = goalResult.rows[0] || null;

    res.render('mypage', { user: req.session.user, goals });
  } catch (err) {
    console.error(err);
    res.render('mypage', { user: req.session.user, goals: null });
  }
});

// 목표 등급 수정/재설정
router.post('/update-goals', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { korean, math, social, science, english, history, studyPeriod } = req.body;

  try {
    // 기존 목표가 있으면 update, 없으면 insert
    const existing = await pool.query(
      'SELECT * FROM goals WHERE user_id = $1 ORDER BY id DESC LIMIT 1',
      [userId]
    );

    if (existing.rows.length > 0) {
      await pool.query(
        `UPDATE goals 
         SET korean=$1, math=$2, social=$3, science=$4, english=$5, history=$6, study_period=$7
         WHERE id=$8`,
        [korean, math, social, science, english, history, studyPeriod, existing.rows[0].id]
      );
    } else {
      await pool.query(
        `INSERT INTO goals (user_id, korean, math, social, science, english, history, study_period)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
        [userId, korean, math, social, science, english, history, studyPeriod]
      );
    }

    // 세션 갱신
    req.session.user.goals = { korean, math, social, science, english, history, studyPeriod };
    req.session.save();

    res.redirect('/user/mypage');
  } catch (err) {
    console.error(err);
    res.status(500).send('목표 등급 업데이트 중 오류 발생');
  }
});

module.exports = router;
