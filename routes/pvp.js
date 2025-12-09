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

// PVP 매칭 요청
pvpRouter.post('/match', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const { targetUserId } = req.body;

  try {
    if (targetUserId) {
      // 특정 유저와 매칭 (랭킹에서 클릭)
      const userCheck = await pool.query('SELECT id FROM users WHERE id = $1', [targetUserId]);
      if (userCheck.rows.length === 0) {
        return res.status(404).json({ error: '사용자를 찾을 수 없습니다.' });
      }

      // 랜덤 문제 선택
      const problemResult = await pool.query(
        `SELECT id FROM problems
         WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
         ORDER BY RANDOM()
         LIMIT 1`
      );

      if (problemResult.rows.length === 0) {
        return res.status(404).json({ error: '사용 가능한 문제가 없습니다.' });
      }

      // 매치 생성
      const matchResult = await pool.query(
        `INSERT INTO pvp_matches (player1_id, player2_id, problem_id, result)
         VALUES ($1, $2, $3, 'ongoing')
         RETURNING id`,
        [userId, targetUserId, problemResult.rows[0].id]
      );

      res.json({ matchId: matchResult.rows[0].id, targetUserId });
    } else {
      // 랜덤 매칭 (대기열 사용)
      const existingInQueue = matchQueue.find(q => q.userId === userId);
      if (existingInQueue) {
        return res.json({ waiting: true, message: '매칭을 기다리는 중입니다.' });
      }

      // 대기 중인 다른 유저 찾기
      const opponent = matchQueue.shift();

      if (opponent) {
        // 매칭 성공
        const problemResult = await pool.query(
          `SELECT id FROM problems
           WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
           ORDER BY RANDOM()
           LIMIT 1`
        );

        const matchResult = await pool.query(
          `INSERT INTO pvp_matches (player1_id, player2_id, problem_id, result)
           VALUES ($1, $2, $3, 'ongoing')
           RETURNING id`,
          [userId, opponent.userId, problemResult.rows[0].id]
        );

        res.json({ matchId: matchResult.rows[0].id, opponentId: opponent.userId });
      } else {
        // 대기열에 추가
        matchQueue.push({ userId, timestamp: Date.now() });
        res.json({ waiting: true, message: '매칭 상대를 찾는 중입니다.' });
      }
    }
  } catch (error) {
    console.error('PVP 매칭 오류:', error);
    res.status(500).json({ error: 'PVP 매칭 중 오류가 발생했습니다.' });
  }
});

// PVP 매치 정보 조회
pvpRouter.get('/match/:matchId', requireAuth, async (req, res) => {
  const { matchId } = req.params;
  const userId = req.session.user.id;

  try {
    const result = await pool.query(
      `SELECT m.*, p.title, p.content, p.choices, p.subject,
              u1.username as player1_name, u2.username as player2_name
       FROM pvp_matches m
       JOIN problems p ON m.problem_id = p.id
       JOIN users u1 ON m.player1_id = u1.id
       JOIN users u2 ON m.player2_id = u2.id
       WHERE m.id = $1 AND (m.player1_id = $2 OR m.player2_id = $2)`,
      [matchId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: '매치를 찾을 수 없습니다.' });
    }

    const match = result.rows[0];
    const isPlayer1 = match.player1_id === userId;

    res.json({
      matchId: match.id,
      problem: {
        title: match.title,
        content: match.content,
        choices: match.choices,
        subject: match.subject
      },
      opponent: isPlayer1 ? match.player2_name : match.player1_name,
      myAnswer: isPlayer1 ? match.player1_answer : match.player2_answer,
      opponentAnswer: isPlayer1 ? match.player2_answer : match.player1_answer,
      result: match.result,
      timeLimit: 300, // 5분
      startedAt: match.started_at
    });
  } catch (error) {
    console.error('PVP 매치 조회 오류:', error);
    res.status(500).json({ error: 'PVP 매치 조회 중 오류가 발생했습니다.' });
  }
});

// PVP 답안 제출
pvpRouter.post('/match/:matchId/submit', requireAuth, async (req, res) => {
  const { matchId } = req.params;
  const { answer } = req.body;
  const userId = req.session.user.id;

  try {
    // 매치 정보 조회
    const matchResult = await pool.query(
      `SELECT m.*, p.answer as correct_answer
       FROM pvp_matches m
       JOIN problems p ON m.problem_id = p.id
       WHERE m.id = $1 AND (m.player1_id = $2 OR m.player2_id = $2) AND m.result = 'ongoing'`,
      [matchId, userId]
    );

    if (matchResult.rows.length === 0) {
      return res.status(404).json({ error: '매치를 찾을 수 없거나 이미 종료되었습니다.' });
    }

    const match = matchResult.rows[0];
    const isPlayer1 = match.player1_id === userId;
    const submissionTime = Math.floor((Date.now() - new Date(match.started_at)) / 1000);

    // 시간 초과 체크 (5분)
    if (submissionTime > 300) {
      return res.status(400).json({ error: '제한 시간이 초과되었습니다.' });
    }

    // 답안 저장
    if (isPlayer1) {
      await pool.query(
        'UPDATE pvp_matches SET player1_answer = $1, player1_time = $2 WHERE id = $3',
        [answer, submissionTime, matchId]
      );
    } else {
      await pool.query(
        'UPDATE pvp_matches SET player2_answer = $1, player2_time = $2 WHERE id = $3',
        [answer, submissionTime, matchId]
      );
    }

    // 두 플레이어 모두 제출했는지 확인
    const updatedMatch = await pool.query(
      'SELECT * FROM pvp_matches WHERE id = $1',
      [matchId]
    );

    const updated = updatedMatch.rows[0];

    if (updated.player1_answer && updated.player2_answer) {
      // 결과 판정
      const p1Correct = updated.player1_answer.trim().toLowerCase() === match.correct_answer.trim().toLowerCase();
      const p2Correct = updated.player2_answer.trim().toLowerCase() === match.correct_answer.trim().toLowerCase();

      let result;
      let winnerId = null;

      if (p1Correct && !p2Correct) {
        result = 'player1_win';
        winnerId = updated.player1_id;
      } else if (!p1Correct && p2Correct) {
        result = 'player2_win';
        winnerId = updated.player2_id;
      } else {
        result = 'draw';
      }

      // 매치 결과 업데이트
      await pool.query(
        'UPDATE pvp_matches SET result = $1, winner_id = $2, ended_at = CURRENT_TIMESTAMP WHERE id = $3',
        [result, winnerId, matchId]
      );

      // PVP 통계 업데이트
      if (result !== 'draw') {
        const loserId = winnerId === updated.player1_id ? updated.player2_id : updated.player1_id;

        await pool.query(
          `UPDATE pvp_stats SET wins = wins + 1, total_matches = total_matches + 1 WHERE user_id = $1`,
          [winnerId]
        );

        await pool.query(
          `UPDATE pvp_stats SET losses = losses + 1, total_matches = total_matches + 1 WHERE user_id = $1`,
          [loserId]
        );
      } else {
        await pool.query(
          `UPDATE pvp_stats SET draws = draws + 1, total_matches = total_matches + 1 WHERE user_id IN ($1, $2)`,
          [updated.player1_id, updated.player2_id]
        );
      }

      res.json({ submitted: true, matchComplete: true, result, winnerId });
    } else {
      res.json({ submitted: true, matchComplete: false, waiting: true });
    }
  } catch (error) {
    console.error('PVP 답안 제출 오류:', error);
    res.status(500).json({ error: 'PVP 답안 제출 중 오류가 발생했습니다.' });
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