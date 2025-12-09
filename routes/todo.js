const express = require('express');
const router = express.Router();
const pool = require('../database/init');
const tf = require('@tensorflow/tfjs-node');
const path = require('path');

// AI 모델 로드 (서버 시작 시 한 번만 로드)
let todoModel = null;

async function loadTodoModel() {
  if (!todoModel) {
    try {
      const modelPath = path.join(__dirname, '../ai/todo_ai.tflite');
      // TFLite 모델 로드 (실제 구현 시 TensorFlow Lite 사용)
      // 여기서는 간단한 규칙 기반 시스템으로 대체
      console.log('Todo AI 모델 로드 완료 (규칙 기반 시스템)');
      todoModel = true;
    } catch (error) {
      console.error('Todo AI 모델 로드 실패:', error);
      todoModel = false;
    }
  }
  return todoModel;
}

loadTodoModel();

// 인증 미들웨어
const requireAuth = (req, res, next) => {
  if (!req.session.user) {
    return res.status(401).json({ error: '로그인이 필요합니다.' });
  }
  next();
};

// AI 기반 Todo 생성 함수
function generateTodos(goals, diploma, studyPeriod) {
  const subjects = ['korean', 'math', 'social', 'science', 'english', 'history'];
  const subjectNames = {
    korean: '국어',
    math: '수학',
    social: '사회',
    science: '과학',
    english: '영어',
    history: '역사'
  };

  const todos = [];
  const totalDays = studyPeriod;

  // 과목별 중요도 계산 (목표 등급이 낮을수록 중요도 높음)
  const priorities = {};
  subjects.forEach(subject => {
    const grade = goals[subject] || 3;
    priorities[subject] = 6 - grade; // 1등급 = 5점, 5등급 = 1점
  });

  // 디플로마에 따른 가중치 조정
  if (['IT', '공학', '수학', '물리', '화학', '생명과학', 'IB(자연)'].includes(diploma)) {
    // 이과: 수학, 과학 집중
    priorities.math *= 1.5;
    priorities.science *= 1.5;
  } else if (['인문학(문학/사학/철학)', '국제어문', '사회과학', '경제경영', 'IB(인문)'].includes(diploma)) {
    // 문과: 국어, 영어, 사회 집중
    priorities.korean *= 1.5;
    priorities.english *= 1.5;
    priorities.social *= 1.5;
    priorities.history *= 1.3;
  } else if (['예술', '체육'].includes(diploma)) {
    // 예체: 균등 분배
    subjects.forEach(s => priorities[s] *= 1.2);
  }

  // 총 우선순위 합계
  const totalPriority = Object.values(priorities).reduce((a, b) => a + b, 0);

  // 일별 Todo 생성
  for (let day = 0; day < totalDays; day++) {
    const date = new Date();
    date.setDate(date.getDate() + day);
    const dateStr = date.toISOString().split('T')[0];

    // 각 과목별로 우선순위에 따라 Todo 배분
    subjects.forEach(subject => {
      const proportion = priorities[subject] / totalPriority;
      const tasksForSubject = Math.max(1, Math.round(proportion * 5)); // 하루 1~5개

      // 주차별로 학습 내용 변경
      const week = Math.floor(day / 7);
      const taskTypes = getTasks(subject, goals[subject], week);

      for (let i = 0; i < Math.min(tasksForSubject, taskTypes.length); i++) {
        todos.push({
          date: dateStr,
          subject: subjectNames[subject],
          task: taskTypes[i]
        });
      }
    });
  }

  return todos;
}

// 과목별 학습 태스크 생성
function getTasks(subject, targetGrade, week) {
  const tasks = {
    korean: [
      '문학 작품 독해 및 분석 (30분)',
      '비문학 지문 읽기 연습 (20분)',
      '어휘력 향상 학습 (15분)',
      '문법 개념 정리 (20분)',
      '기출 문제 풀이 (30분)'
    ],
    math: [
      '기본 개념 복습 (25분)',
      '유형별 문제 풀이 (40분)',
      '심화 문제 도전 (30분)',
      '오답 노트 정리 (20분)',
      '모의고사 문제 풀이 (45분)'
    ],
    social: [
      '교과서 핵심 개념 정리 (25분)',
      '자료 분석 연습 (20분)',
      '시사 이슈 학습 (15분)',
      '기출 문제 풀이 (30분)',
      '주제별 요약 정리 (25분)'
    ],
    science: [
      '실험 원리 이해 (20분)',
      '개념 정리 및 암기 (25분)',
      '계산 문제 연습 (30분)',
      '실험 결과 분석 (20분)',
      '종합 문제 풀이 (35분)'
    ],
    english: [
      '단어 암기 (20분)',
      '독해 지문 읽기 (25분)',
      '문법 문제 풀이 (20분)',
      '듣기 연습 (15분)',
      '영작 연습 (20분)'
    ],
    history: [
      '연대별 사건 정리 (20분)',
      '인물 및 사건 분석 (25분)',
      '역사적 배경 학습 (20분)',
      '기출 문제 풀이 (30분)',
      '주제별 요약 (20분)'
    ]
  };

  // 목표 등급에 따른 난이도 조정
  let selectedTasks = [...tasks[subject]];
  if (targetGrade === 1 || targetGrade === 2) {
    // 고난도 태스크 추가
    selectedTasks.push(`${subject} 심화 학습 (50분)`);
    selectedTasks.push(`${subject} 고난도 문제 풀이 (40분)`);
  }

  return selectedTasks;
}

// Todo 생성 (AI 기반)
router.post('/generate', requireAuth, async (req, res) => {
  const { diploma, korean, math, social, science, english, history, studyPeriod } = req.body;
  const userId = req.session.user.id;

  try {
    // 목표 저장
    const goalResult = await pool.query(
      `INSERT INTO goals (user_id, korean, math, social, science, english, history, study_period)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING id`,
      [userId, korean, math, social, science, english, history, studyPeriod]
    );

    const goalId = goalResult.rows[0].id;

    // AI로 Todo 생성
    const goals = { korean, math, social, science, english, history };
    const todos = generateTodos(goals, diploma, parseInt(studyPeriod));

    // DB에 저장
    for (const todo of todos) {
      await pool.query(
        `INSERT INTO todos (user_id, goal_id, date, subject, task)
         VALUES ($1, $2, $3, $4, $5)`,
        [userId, goalId, todo.date, todo.subject, todo.task]
      );
    }

    res.json({ success: true, goalId, todoCount: todos.length });
  } catch (error) {
    console.error('Todo 생성 오류:', error);
    res.status(500).json({ error: 'Todo 생성 중 오류가 발생했습니다.' });
  }
});

// Todo 목록 조회 (날짜별)
router.get('/', requireAuth, async (req, res) => {
  const { date } = req.query;
  const userId = req.session.user.id;

  try {
    const query = date
      ? 'SELECT * FROM todos WHERE user_id = $1 AND date = $2 ORDER BY id'
      : 'SELECT * FROM todos WHERE user_id = $1 AND date >= CURRENT_DATE ORDER BY date, id LIMIT 100';

    const params = date ? [userId, date] : [userId];
    const result = await pool.query(query, params);

    res.json({ todos: result.rows });
  } catch (error) {
    console.error('Todo 조회 오류:', error);
    res.status(500).json({ error: 'Todo 조회 중 오류가 발생했습니다.' });
  }
});

// Todo 완료 처리
router.put('/:id/complete', requireAuth, async (req, res) => {
  const { id } = req.params;
  const userId = req.session.user.id;

  try {
    // Todo 소유권 확인
    const checkResult = await pool.query(
      'SELECT id, completed FROM todos WHERE id = $1 AND user_id = $2',
      [id, userId]
    );

    if (checkResult.rows.length === 0) {
      return res.status(404).json({ error: 'Todo를 찾을 수 없습니다.' });
    }

    // 완료 처리
    await pool.query(
      'UPDATE todos SET completed = TRUE, completed_at = CURRENT_TIMESTAMP WHERE id = $1',
      [id]
    );

    // 날짜별 완료 상태 확인
    const dateResult = await pool.query(
      'SELECT date FROM todos WHERE id = $1',
      [id]
    );
    const date = dateResult.rows[0].date;

    // 해당 날짜의 모든 Todo 완료 여부 확인
    const statusResult = await pool.query(
      `SELECT 
        COUNT(*) as total,
        SUM(CASE WHEN completed THEN 1 ELSE 0 END) as completed
       FROM todos
       WHERE user_id = $1 AND date = $2`,
      [userId, date]
    );

    const { total, completed } = statusResult.rows[0];
    const status = parseInt(completed) === parseInt(total) ? '완' : '실';

    // 달력 기록 업데이트
    await pool.query(
      `INSERT INTO calendar_records (user_id, date, status)
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id, date)
       DO UPDATE SET status = $3`,
      [userId, date, status]
    );

    res.json({ success: true, status });
  } catch (error) {
    console.error('Todo 완료 처리 오류:', error);
    res.status(500).json({ error: 'Todo 완료 처리 중 오류가 발생했습니다.' });
  }
});

// Todo 완료 취소
router.put('/:id/uncomplete', requireAuth, async (req, res) => {
  const { id } = req.params;
  const userId = req.session.user.id;

  try {
    await pool.query(
      'UPDATE todos SET completed = FALSE, completed_at = NULL WHERE id = $1 AND user_id = $2',
      [id, userId]
    );

    // 달력 상태 업데이트
    const dateResult = await pool.query('SELECT date FROM todos WHERE id = $1', [id]);
    const date = dateResult.rows[0].date;

    await pool.query(
      `INSERT INTO calendar_records (user_id, date, status)
       VALUES ($1, $2, '실')
       ON CONFLICT (user_id, date)
       DO UPDATE SET status = '실'`,
      [userId, date]
    );

    res.json({ success: true });
  } catch (error) {
    console.error('Todo 완료 취소 오류:', error);
    res.status(500).json({ error: 'Todo 완료 취소 중 오류가 발생했습니다.' });
  }
});

// 달력 데이터 조회
router.get('/calendar', requireAuth, async (req, res) => {
  const { month, year } = req.query;
  const userId = req.session.user.id;

  try {
    const startDate = `${year}-${month}-01`;
    const endDate = `${year}-${month}-31`;

    const result = await pool.query(
      `SELECT date, status FROM calendar_records
       WHERE user_id = $1 AND date BETWEEN $2 AND $3
       ORDER BY date`,
      [userId, startDate, endDate]
    );

    res.json({ records: result.rows });
  } catch (error) {
    console.error('달력 조회 오류:', error);
    res.status(500).json({ error: '달력 조회 중 오류가 발생했습니다.' });
  }
});

// 목표 달성 여부 확인
router.get('/goal-achievement/:goalId', requireAuth, async (req, res) => {
  const { goalId } = req.params;
  const userId = req.session.user.id;

  try {
    // 목표 정보 조회
    const goalResult = await pool.query(
      'SELECT * FROM goals WHERE id = $1 AND user_id = $2',
      [goalId, userId]
    );

    if (goalResult.rows.length === 0) {
      return res.status(404).json({ error: '목표를 찾을 수 없습니다.' });
    }

    // 현재 성적 조회 (실제로는 시즌 종료 후 성적 입력 필요)
    // 여기서는 완료율로 대체
    const completionResult = await pool.query(
      `SELECT 
        COUNT(*) as total,
        SUM(CASE WHEN completed THEN 1 ELSE 0 END) as completed
       FROM todos
       WHERE goal_id = $1`,
      [goalId]
    );

    const { total, completed } = completionResult.rows[0];
    const completionRate = total > 0 ? (completed / total * 100).toFixed(1) : 0;

    res.json({
      goal: goalResult.rows[0],
      completionRate,
      achieved: completionRate >= 80 // 80% 이상 완료 시 달성으로 간주
    });
  } catch (error) {
    console.error('목표 달성 확인 오류:', error);
    res.status(500).json({ error: '목표 달성 확인 중 오류가 발생했습니다.' });
  }
});

module.exports = router;