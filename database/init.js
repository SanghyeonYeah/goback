const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: { rejectUnauthorized: false },
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

// 연결 테스트
pool.on('connect', () => {
  console.log('✅ 데이터베이스 연결 성공');
});

pool.on('error', (err) => {
  console.error('❌ 데이터베이스 연결 오류:', err);
  process.exit(-1);
});

// 초기화 스크립트 (관리자 비밀번호 설정)
async function initializeAdmin() {
  const bcrypt = require('bcrypt');
  try {
    const result = await pool.query(
      "SELECT id FROM users WHERE username = 'admin'"
    );

    if (result.rows.length > 0) {
      const passwordHash = await bcrypt.hash('admin', 10);
      await pool.query(
        'UPDATE users SET password_hash = $1 WHERE username = $2',
        [passwordHash, 'admin']
      );
      console.log('✅ 관리자 계정 비밀번호 설정 완료 (admin/admin)');
    }
  } catch (error) {
    console.error('❌ 관리자 초기화 오류:', error);
  }
}

// 서버 시작 시 초기화
if (require.main === module) {
  initializeAdmin().then(() => {
    console.log('데이터베이스 초기화 완료');
    process.exit(0);
  });
}

module.exports = pool;
