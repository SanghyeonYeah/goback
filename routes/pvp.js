const express = require('express');
const pvpRouter = express.Router();
const pool = require('../database/init');

const requireAuth = (req, res, next) => {
  if (!req.session.user) {
    return res.status(401).json({ error: '로그인이 필요합니다.' });
  }
  next();
};

// PVP 매칭 대기열 (메모리 저장)
const matchQueue = [];

// PVP 챌린지 (랭킹에서 도전)
pvpRouter.get('/challenge/:targetUserId', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { targetUserId } = req.params;

  try {
    // 대상 유저 확인
    const userCheck = await pool.query('SELECT id, username FROM users WHERE id = $1', [targetUserId]);
    if (userCheck.rows.length === 0) {
      return res.status(404).send('사용자를 찾을 수 없습니다.');
    }

    // 랜덤 문제 선택
    const problemResult = await pool.query(
      `SELECT id, title, content, choices, subject, answer FROM problems
       WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
       ORDER BY RANDOM()
       LIMIT 1`
    );

    if (problemResult.rows.length === 0) {
      return res.status(404).send('사용 가능한 문제가 없습니다.');
    }

    const problem = problemResult.rows[0];

    // 매치 생성
    const matchResult = await pool.query(
      `INSERT INTO pvp_matches (player1_id, player2_id, problem_id, result, started_at)
       VALUES ($1, $2, $3, 'ongoing', CURRENT_TIMESTAMP)
       RETURNING id`,
      [userId, targetUserId, problem.id]
    );

    // PVP 페이지로 리다이렉트
    res.redirect(`/pvp?matchId=${matchResult.rows[0].id}`);
  } catch (error) {
    console.error('PVP 챌린지 오류:', error);
    res.status(500).send('PVP 챌린지 중 오류가 발생했습니다.');
  }
});

// PVP 페이지 렌더링
pvpRouter.get('/', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { matchId } = req.query;

  try {
    if (matchId) {
      // 기존 매치 조회
      const result = await pool.query(
        `SELECT m.*, p.title, p.content, p.choices, p.subject, p.answer,
                u1.username as player1_name, u2.username as player2_name
         FROM pvp_matches m
         JOIN problems p ON m.problem_id = p.id
         JOIN users u1 ON m.player1_id = u1.id
         JOIN users u2 ON m.player2_id = u2.id
         WHERE m.id = $1 AND (m.player1_id = $2 OR m.player2_id = $2)`,
        [matchId, userId]
      );

      if (result.rows.length === 0) {
        return res.status(404).send('매치를 찾을 수 없습니다.');
      }

      const match = result.rows[0];
      const isPlayer1 = match.player1_id === userId;

      // choices를 파싱
      let choices = [];
      try {
        choices = typeof match.choices === 'string' ? JSON.parse(match.choices) : match.choices;
      } catch (e) {
        console.error('Choices 파싱 오류:', e);
        choices = [];
      }

      res.render('pvp', {
        user: req.session.user,
        match: {
          id: match.id,
          status: match.result === 'ongoing' ? 'in_progress' : 'finished',
          winner_id: match.winner_id,
          result: match.result
        },
        opponent: {
          id: isPlayer1 ? match.player2_id : match.player1_id,
          username: isPlayer1 ? match.player2_name : match.player1_name
        },
        problem: {
          question: match.content,
          option1: choices[0] || '',
          option2: choices[1] || '',
          option3: choices[2] || '',
          option4: choices[3] || '',
          option5: choices[4] || '',
          answer: match.answer
        },
        myAnswer: isPlayer1 ? match.player1_answer : match.player2_answer,
        opponentAnswer: isPlayer1 ? match.player2_answer : match.player1_answer,
        csrfToken: req.csrfToken ? req.csrfToken() : ''
      });
    } else {
      // 매치 없이 접근 - 대기 화면
      res.render('pvp', {
        user: req.session.user,
        match: null,
        opponent: null,
        problem: null,
        myAnswer: null,
        opponentAnswer: null,
        csrfToken: req.csrfToken ? req.csrfToken() : ''
      });
    }
  } catch (error) {
    console.error('PVP 페이지 로드 오류:', error);
    res.status(500).send('PVP 페이지 로드 중 오류가 발생했습니다.');
  }
});

// PVP 답안 제출
pvpRouter.post('/submit', requireAuth, async (req, res) => {
  const { match_id, answer } = req.body;
  const userId = req.session.user.id;

  try {
    // 매치 정보 조회
    const matchResult = await pool.query(
      `SELECT m.*, p.answer as correct_answer
       FROM pvp_matches m
       JOIN problems p ON m.problem_id = p.id
       WHERE m.id = $1 AND (m.player1_id = $2 OR m.player2_id = $2) AND m.result = 'ongoing'`,
      [match_id, userId]
    );

    if (matchResult.rows.length === 0) {
      return res.status(404).send('매치를 찾을 수 없거나 이미 종료되었습니다.');
    }

    const match = matchResult.rows[0];
    const isPlayer1 = match.player1_id === userId;
    const submissionTime = Math.floor((Date.now() - new Date(match.started_at)) / 1000);

    // 시간 초과 체크 (5분)
    if (submissionTime > 300) {
      return res.status(400).send('제한 시간이 초과되었습니다.');
    }

    // 답안 저장
    if (isPlayer1) {
      await pool.query(
        'UPDATE pvp_matches SET player1_answer = $1, player1_time = $2 WHERE id = $3',
        [answer, submissionTime, match_id]
      );
    } else {
      await pool.query(
        'UPDATE pvp_matches SET player2_answer = $1, player2_time = $2 WHERE id = $3',
        [answer, submissionTime, match_id]
      );
    }

    // 두 플레이어 모두 제출했는지 확인
    const updatedMatch = await pool.query(
      'SELECT * FROM pvp_matches WHERE id = $1',
      [match_id]
    );

    const updated = updatedMatch.rows[0];

    if (updated.player1_answer && updated.player2_answer) {
      // 결과 판정
      const p1Correct = String(updated.player1_answer).trim() === String(match.correct_answer).trim();
      const p2Correct = String(updated.player2_answer).trim() === String(match.correct_answer).trim();

      let result;
      let winnerId = null;

      if (p1Correct && !p2Correct) {
        result = 'player1_win';
        winnerId = updated.player1_id;
      } else if (!p1Correct && p2Correct) {
        result = 'player2_win';
        winnerId = updated.player2_id;
      } else if (p1Correct && p2Correct) {
        // 둘 다 맞춤 - 시간으로 판정
        if (updated.player1_time < updated.player2_time) {
          result = 'player1_win';
          winnerId = updated.player1_id;
        } else if (updated.player2_time < updated.player1_time) {
          result = 'player2_win';
          winnerId = updated.player2_id;
        } else {
          result = 'draw';
        }
      } else {
        result = 'draw';
      }

      // 매치 결과 업데이트
      await pool.query(
        'UPDATE pvp_matches SET result = $1, winner_id = $2, ended_at = CURRENT_TIMESTAMP WHERE id = $3',
        [result, winnerId, match_id]
      );

      // PVP 통계 업데이트
      if (result !== 'draw') {
        const loserId = winnerId === updated.player1_id ? updated.player2_id : updated.player1_id;

        await pool.query(
          `INSERT INTO pvp_stats (user_id, wins, total_matches) VALUES ($1, 1, 1)
           ON CONFLICT (user_id) DO UPDATE SET wins = pvp_stats.wins + 1, total_matches = pvp_stats.total_matches + 1`,
          [winnerId]
        );

        await pool.query(
          `INSERT INTO pvp_stats (user_id, losses, total_matches) VALUES ($1, 1, 1)
           ON CONFLICT (user_id) DO UPDATE SET losses = pvp_stats.losses + 1, total_matches = pvp_stats.total_matches + 1`,
          [loserId]
        );
      } else {
        await pool.query(
          `INSERT INTO pvp_stats (user_id, draws, total_matches) VALUES ($1, 1, 1)
           ON CONFLICT (user_id) DO UPDATE SET draws = pvp_stats.draws + 1, total_matches = pvp_stats.total_matches + 1`,
          [updated.player1_id]
        );
        await pool.query(
          `INSERT INTO pvp_stats (user_id, draws, total_matches) VALUES ($1, 1, 1)
           ON CONFLICT (user_id) DO UPDATE SET draws = pvp_stats.draws + 1, total_matches = pvp_stats.total_matches + 1`,
          [updated.player2_id]
        );
      }
    }

    // 결과 페이지로 리다이렉트
    res.redirect(`/pvp?matchId=${match_id}`);
  } catch (error) {
    console.error('PVP 답안 제출 오류:', error);
    res.status(500).send('PVP 답안 제출 중 오류가 발생했습니다.');
  }
});

// PVP 통계 조회
pvpRouter.get('/stats', requireAuth, async (req, res) => {
  const userId = req.session.user.id;

  try {
    const result = await pool.query(
      'SELECT * FROM pvp_stats WHERE user_id = $1',
      [userId]
    );

    if (result.rows.length === 0) {
      return res.json({ wins: 0, losses: 0, draws: 0, total_matches: 0 });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error('PVP 통계 조회 오류:', error);
    res.status(500).json({ error: 'PVP 통계 조회 중 오류가 발생했습니다.' });
  }
});

module.exports = pvpRouter;