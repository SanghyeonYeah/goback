const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  host: 'postgresql://postgres:nTdazFWkZOeezcSnhGwuvMZXnzORNQwL@postgres.railway.internal:5432/railway',
  port: 5432,
  database: process.env.DB_NAME || 'study_planner',
  user: 'railway',
  password: 'nTdazFWkZOeezcSnhGwuvMZXnzORNQwL',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

// 연결 테스트
pool.on('connect', () => {
  console.log('데이터베이스 연결 성공');
});

pool.on('error', (err) => {
  console.error('데이터베이스 연결 오류:', err);
  process.exit(-1);
});

// 초기화 스크립트 (관리자 비밀번호 설정)
async function initializeAdmin() {
  const bcrypt = require('bcrypt');
  
  try {
    // 관리자 계정 확인
    const result = await pool.query(
      "SELECT id FROM users WHERE username = 'admin'"
    );

    if (result.rows.length > 0) {
      // 관리자 비밀번호 업데이트 (admin/admin)
      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash('admin' + salt, 10);

      await pool.query(
        'UPDATE users SET password_hash = $1, salt = $2 WHERE username = $3',
        [passwordHash, salt, 'admin']
      );

      console.log('✅ 관리자 계정 비밀번호 설정 완료 (admin/admin)');
    }
  } catch (error) {
    console.error('관리자 초기화 오류:', error);
  }
}

// 서버 시작 시 관리자 초기화
if (require.main === module) {
  initializeAdmin().then(() => {
    console.log('데이터베이스 초기화 완료');
    process.exit(0);
  });
}

module.exports =  pool ;