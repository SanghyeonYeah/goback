const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: process.env.NODE_ENV === 'production' 
    ? { rejectUnauthorized: false } 
    : false,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

// 연결 에러는 로그만 찍고 서버는 계속 실행
pool.on('connect', () => {
  console.log('✅ 데이터베이스 연결 성공');
});

pool.on('error', (err) => {
  console.error('⚠️ 데이터베이스 연결 오류:', err.message);
  // process.exit 제거 - 서버가 죽지 않도록
});

module.exports = pool;