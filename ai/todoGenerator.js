const { spawnSync } = require('child_process');

/**
 * 목표 → Todo 자동 생성
 */
function generateTodos(goals, studyPeriod, userId, existingTodos = []) {
  const pyProcess = spawnSync('python3', ['ai/predict_todos_opt.py'], {
    input: JSON.stringify({ goals, studyPeriod, userId }),
    encoding: 'utf-8'
  });

  if (pyProcess.error) throw pyProcess.error;
  if (pyProcess.status !== 0) throw new Error(pyProcess.stderr);

  let todos;
  try {
    todos = JSON.parse(pyProcess.stdout);
  } catch (err) {
    console.error('Python 결과 파싱 실패:', err);
    throw err;
  }

  // 기존 Todo와 중복 제거
  return todos.filter(todo => 
    !existingTodos.some(et =>
      et.subject === todo.subject &&
      et.task === todo.task &&
      et.date === todo.date
    )
  );
}

module.exports = { generateTodos };
