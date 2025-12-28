const express = require('express');
const router = express.Router();
const pool = require('../database/init');

/* =========================
   AUTH MIDDLEWARE
========================= */
function requireAuth(req, res, next) {
  if (!req.session.user) {
    return res.redirect('/login');
  }
  next();
}

/* =========================
   GET /todo
   오늘의 Todo 페이지 렌더링
========================= */
router.get('/', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const today = new Date().toISOString().split('T')[0];

  try {
    /* 오늘의 Todo 목록 */
    const todosResult = await pool.query(
      `
      SELECT id, content, completed
      FROM todos
      WHERE user_id = $1 AND date = $2
      ORDER BY id ASC
      `,
      [userId, today]
    );

    /* 최근 목표 1개 */
    const goalResult = await pool.query(
      `
      SELECT *
      FROM goals
      WHERE user_id = $1
      ORDER BY id DESC
      LIMIT 1
      `,
      [userId]
    );

    const todos = todosResult.rows;
    const goals = goalResult.rows[0] || null;

    /* 진행률 계산 */
    const total = todos.length;
    const completed = todos.filter(t => t.completed).length;
    const percentage = total === 0 ? 0 : Math.round((completed / total) * 100);

    res.render('todo', {
      todos,
      goals,
      progress: {
        total,
        completed,
        percentage
      },
      csrfToken: req.csrfToken()
    });

  } catch (err) {
    console.error('[TODO PAGE ERROR]', err);
    res.status(500).send('Todo 페이지 로딩 실패');
  }
});

/* =========================
   POST /todos/:id/toggle
   Todo 완료 / 취소
========================= */
router.post('/todos/:id/toggle', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const todoId = req.params.id;

  try {
    /* Todo 소유권 + 상태 확인 */
    const todoResult = await pool.query(
      `
      SELECT completed, date
      FROM todos
      WHERE id = $1 AND user_id = $2
      `,
      [todoId, userId]
    );

    if (todoResult.rows.length === 0) {
      return res.status(404).send('존재하지 않는 Todo');
    }

    const currentCompleted = todoResult.rows[0].completed;
    const date = todoResult.rows[0].date;

    /* 상태 토글 */
    await pool.query(
      `
      UPDATE todos
      SET completed = $1,
          completed_at = CASE WHEN $1 THEN NOW() ELSE NULL END
      WHERE id = $2
      `,
      [!currentCompleted, todoId]
    );

    /* 해당 날짜의 Todo 상태 계산 */
    const statusResult = await pool.query(
      `
      SELECT COUNT(*) AS total,
             SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completed
      FROM todos
      WHERE user_id = $1 AND date = $2
      `,
      [userId, date]
    );

    const total = Number(statusResult.rows[0].total);
    const completed = Number(statusResult.rows[0].completed);
    const status = total === completed ? '완' : '실';

    /* 캘린더 기록 업서트 */
    await pool.query(
      `
      INSERT INTO calendar_records (user_id, date, status)
      VALUES ($1, $2, $3)
      ON CONFLICT (user_id, date)
      DO UPDATE SET status = $3
      `,
      [userId, date, status]
    );

    res.redirect('/todo');

  } catch (err) {
    console.error('[TODO TOGGLE ERROR]', err);
    res.status(500).send('Todo 처리 실패');
  }
});

/* =========================
   POST /todo/add
   Todo 추가 (선택)
========================= */
router.post('/add', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { content } = req.body;
  const today = new Date().toISOString().split('T')[0];

  if (!content || content.trim().length === 0) {
    return res.redirect('/todo');
  }

  try {
    await pool.query(
      `
      INSERT INTO todos (user_id, content, date)
      VALUES ($1, $2, $3)
      `,
      [userId, content.trim(), today]
    );

    res.redirect('/todo');

  } catch (err) {
    console.error('[TODO ADD ERROR]', err);
    res.status(500).send('Todo 추가 실패');
  }
});

/* =========================
   POST /todo/delete/:id
   Todo 삭제 (선택)
========================= */
router.post('/delete/:id', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const todoId = req.params.id;

  try {
    await pool.query(
      `
      DELETE FROM todos
      WHERE id = $1 AND user_id = $2
      `,
      [todoId, userId]
    );

    res.redirect('/todo');

  } catch (err) {
    console.error('[TODO DELETE ERROR]', err);
    res.status(500).send('Todo 삭제 실패');
  }
});

module.exports = router;
