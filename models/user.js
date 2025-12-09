const pool = require('../database/init');
const bcrypt = require('bcrypt');

class User {
  // 사용자 생성
  static async create({ username, email, password, studentId, diploma, googleId = null }) {
    try {
      // 비밀번호 해싱
      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash(password + salt, 10);

      const result = await pool.query(
        `INSERT INTO users (username, email, password_hash, salt, student_id, diploma, google_id, grade)
         VALUES ($1, $2, $3, $4, $5, $6, $7, 1)
         RETURNING id, username, email, student_id, diploma, grade, created_at`,
        [username, email, passwordHash, salt, studentId, diploma, googleId]
      );

      // PVP 통계 초기화
      await pool.query('INSERT INTO pvp_stats (user_id) VALUES ($1)', [result.rows[0].id]);

      return result.rows[0];
    } catch (error) {
      throw error;
    }
  }

  // ID로 사용자 조회
  static async findById(id) {
    try {
      const result = await pool.query(
        `SELECT id, username, email, student_id, diploma, grade, created_at, last_login
         FROM users
         WHERE id = $1`,
        [id]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // 사용자명으로 사용자 조회 (로그인용)
  static async findByUsername(username) {
    try {
      const result = await pool.query(
        `SELECT id, username, email, password_hash, salt, student_id, diploma, grade
         FROM users
         WHERE username = $1`,
        [username]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // 이메일로 사용자 조회
  static async findByEmail(email) {
    try {
      const result = await pool.query(
        `SELECT id, username, email, student_id, diploma, grade, google_id
         FROM users
         WHERE email = $1`,
        [email]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // Google ID로 사용자 조회
  static async findByGoogleId(googleId) {
    try {
      const result = await pool.query(
        `SELECT id, username, email, student_id, diploma, grade, google_id
         FROM users
         WHERE google_id = $1`,
        [googleId]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // 학번으로 사용자 조회
  static async findByStudentId(studentId) {
    try {
      const result = await pool.query(
        `SELECT id, username, email, student_id, diploma, grade
         FROM users
         WHERE student_id = $1`,
        [studentId]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // 비밀번호 검증
  static async verifyPassword(user, password) {
    try {
      const hashedPassword = await bcrypt.hash(password + user.salt, 10);
      return hashedPassword === user.password_hash;
    } catch (error) {
      throw error;
    }
  }

  // 비밀번호 변경
  static async updatePassword(userId, newPassword) {
    try {
      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash(newPassword + salt, 10);

      await pool.query(
        'UPDATE users SET password_hash = $1, salt = $2 WHERE id = $3',
        [passwordHash, salt, userId]
      );

      return true;
    } catch (error) {
      throw error;
    }
  }

  // Google ID 연결
  static async linkGoogleId(userId, googleId) {
    try {
      await pool.query(
        'UPDATE users SET google_id = $1 WHERE id = $2',
        [googleId, userId]
      );

      return true;
    } catch (error) {
      throw error;
    }
  }

  // 마지막 로그인 시간 업데이트
  static async updateLastLogin(userId) {
    try {
      await pool.query(
        'UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = $1',
        [userId]
      );

      return true;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 정보 업데이트
  static async update(userId, updates) {
    try {
      const allowedFields = ['diploma', 'grade'];
      const updateFields = [];
      const values = [];
      let paramCount = 1;

      for (const [key, value] of Object.entries(updates)) {
        if (allowedFields.includes(key)) {
          updateFields.push(`${key} = $${paramCount}`);
          values.push(value);
          paramCount++;
        }
      }

      if (updateFields.length === 0) {
        return false;
      }

      values.push(userId);
      const query = `UPDATE users SET ${updateFields.join(', ')} WHERE id = $${paramCount}`;

      await pool.query(query, values);
      return true;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 삭제
  static async delete(userId) {
    try {
      await pool.query('DELETE FROM users WHERE id = $1', [userId]);
      return true;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 통계 조회
  static async getStats(userId) {
    try {
      const result = await pool.query(
        `SELECT 
          u.id,
          u.username,
          u.student_id,
          u.diploma,
          COALESCE(s.total_score, 0) as total_score,
          COALESCE(p.total_matches, 0) as total_pvp_matches,
          COALESCE(p.wins, 0) as pvp_wins,
          COUNT(DISTINCT ps.id) as problems_solved,
          COUNT(DISTINCT CASE WHEN ps.is_correct THEN ps.id END) as correct_answers
         FROM users u
         LEFT JOIN (
           SELECT user_id, SUM(total_score) as total_score
           FROM scores
           GROUP BY user_id
         ) s ON u.id = s.user_id
         LEFT JOIN pvp_stats p ON u.id = p.user_id
         LEFT JOIN problem_submissions ps ON u.id = ps.user_id
         WHERE u.id = $1
         GROUP BY u.id, u.username, u.student_id, u.diploma, s.total_score, p.total_matches, p.wins`,
        [userId]
      );

      return result.rows[0] || null;
    } catch (error) {
      throw error;
    }
  }

  // 전체 사용자 목록 조회 (관리자용)
  static async getAll({ limit = 50, offset = 0, orderBy = 'created_at', order = 'DESC' }) {
    try {
      const allowedOrderBy = ['created_at', 'username', 'student_id', 'last_login'];
      const allowedOrder = ['ASC', 'DESC'];

      if (!allowedOrderBy.includes(orderBy)) orderBy = 'created_at';
      if (!allowedOrder.includes(order.toUpperCase())) order = 'DESC';

      const result = await pool.query(
        `SELECT 
          u.id,
          u.username,
          u.email,
          u.student_id,
          u.diploma,
          u.grade,
          u.created_at,
          u.last_login,
          COALESCE(s.total_score, 0) as total_score
         FROM users u
         LEFT JOIN (
           SELECT user_id, SUM(total_score) as total_score
           FROM scores
           GROUP BY user_id
         ) s ON u.id = s.user_id
         ORDER BY u.${orderBy} ${order}
         LIMIT $1 OFFSET $2`,
        [limit, offset]
      );

      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 수 조회
  static async count() {
    try {
      const result = await pool.query('SELECT COUNT(*) as count FROM users');
      return parseInt(result.rows[0].count);
    } catch (error) {
      throw error;
    }
  }

  // 관리자 권한 확인
  static async isAdmin(userId) {
    try {
      const result = await pool.query(
        'SELECT user_id FROM admin_users WHERE user_id = $1',
        [userId]
      );

      return result.rows.length > 0;
    } catch (error) {
      throw error;
    }
  }

  // 관리자 권한 부여
  static async grantAdmin(userId, permissions = null) {
    try {
      const defaultPermissions = {
        can_add_problems: true,
        can_manage_seasons: true,
        can_manage_users: true
      };

      await pool.query(
        `INSERT INTO admin_users (user_id, permissions)
         VALUES ($1, $2)
         ON CONFLICT (user_id) DO UPDATE SET permissions = $2`,
        [userId, JSON.stringify(permissions || defaultPermissions)]
      );

      return true;
    } catch (error) {
      throw error;
    }
  }

  // 관리자 권한 제거
  static async revokeAdmin(userId) {
    try {
      await pool.query('DELETE FROM admin_users WHERE user_id = $1', [userId]);
      return true;
    } catch (error) {
      throw error;
    }
  }
}

module.exports = User;