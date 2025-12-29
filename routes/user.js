const express = require('express');
const router = express.Router();
const pool = require('../database/init');
const { generateTodos } = require('../ai/todoGenerator');

// 인증 미들웨어
const requireAuth = (req, res, next) => {
  if (!req.session.user) return res.redirect('/auth/login');
  next();
};

// 🔹 목표 데이터 정규화 함수
const normalizeGoals = (goalRow) => {
  if (!goalRow) return null;

  return {
    korean: goalRow.korean,
    math: goalRow.math,
    english: goalRow.english,
    social: goalRow.social,
    science: goalRow.science,
    history: goalRow.history,
    study_period: goalRow.study_period
  };
};

// ==============================
// 마이페이지
// GET /user/mypage
// ==============================
router.get('/mypage', requireAuth, async (req, res) => {
  const userId = req.session.user.id;

  try {
    const goalResult = await pool.query(
      'SELECT * FROM goals WHERE user_id = $1 ORDER BY id DESC LIMIT 1',
      [userId]
    );

    const goals = goalResult.rows[0]
      ? normalizeGoals(goalResult.rows[0])
      : null;

    res.render('mypage', {
      user: req.session.user,
      goals,
      csrfToken: req.csrfToken()
    });
  } catch (err) {
    console.error('마이페이지 로딩 오류:', err);
    res.render('mypage', {
      user: req.session.user,
      goals: null,
      csrfToken: req.csrfToken()
    });
  }
});

// ==============================
// 목표 수정 + AI Todo 생성
// POST /user/update-goals
// ==============================
router.post('/update-goals', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const {
    korean,
    math,
    social,
    science,
    english,
    history,
    studyPeriod
  } = req.body;

  try {
    // 기존 목표 확인
    const existing = await pool.query(
      'SELECT id FROM goals WHERE user_id = $1 ORDER BY id DESC LIMIT 1',
      [userId]
    );

    let goalId;

    if (existing.rows.length > 0) {
      goalId = existing.rows[0].id;
      await pool.query(
        `UPDATE goals
         SET korean=$1, math=$2, social=$3, science=$4,
             english=$5, history=$6, study_period=$7
         WHERE id=$8`,
        [
          korean,
          math,
          social,
          science,
          english,
          history,
          studyPeriod,
          goalId
        ]
      );
    } else {
      const insertRes = await pool.query(
        `INSERT INTO goals
         (user_id, korean, math, social, science, english, history, study_period)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id`,
        [
          userId,
          korean,
          math,
          social,
          science,
          english,
          history,
          studyPeriod
        ]
      );
      goalId = insertRes.rows[0].id;
    }

    // 세션 업데이트
    req.session.user.goals = {
      korean,
      math,
      english,
      social,
      science,
      history,
      study_period: studyPeriod
    };
    req.session.save();

    // ==============================
    // AI Todo 생성
    // ==============================
    const existingTodosResult = await pool.query(
      'SELECT subject, task, date FROM todos WHERE user_id=$1 AND date>=CURRENT_DATE',
      [userId]
    );

    const aiTodos = await generateTodos(
      { korean, math, english, social, science, history },
      parseInt(studyPeriod),
      userId,
      existingTodosResult.rows
    );

    // DB 저장 (goal_id 포함)
    for (const todo of aiTodos) {
      await pool.query(
        `INSERT INTO todos (user_id, goal_id, subject, task, difficulty, date, completed)
         VALUES ($1,$2,$3,$4,$5,$6,false)`,
        [userId, goalId, todo.subject, todo.task, todo.difficulty, todo.date]
      );
    }

    // 목표 저장 후 홈으로 리다이렉트
    res.redirect('/home');
  } catch (err) {
    console.error('목표 업데이트 오류:', err);
    res.status(500).send('목표 등급 업데이트 또는 AI Todo 생성 중 오류 발생');
  }
});

module.exports = router;
