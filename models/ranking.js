const pool = require('../database/init');

class Ranking {
  // 일일 랭킹 조회
  static async getDailyRanking(date = null, { limit = 100, offset = 0 } = {}) {
    try {
      const targetDate = date || new Date().toISOString().split('T')[0];

      const result = await pool.query(
        `SELECT 
          u.id,
          u.username,
          u.student_id,
          u.diploma,
          s.daily_score,
          s.date,
          ROW_NUMBER() OVER (ORDER BY s.daily_score DESC) as rank
         FROM users u
         JOIN scores s ON u.id = s.user_id
         WHERE s.date = $1 AND s.daily_score > 0
         ORDER BY s.daily_score DESC
         LIMIT $2 OFFSET $3`,
        [targetDate, limit, offset]
      );

      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 시즌 랭킹 조회
  static async getSeasonRanking(seasonId = null, { limit = 100, offset = 0 } = {}) {
    try {
      let query, params;

      if (seasonId) {
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            u.diploma,
            s.total_score,
            s.season_id,
            se.name as season_name,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, season_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id = $1
            GROUP BY user_id, season_id
          ) s ON u.id = s.user_id
          JOIN seasons se ON s.season_id = se.id
          WHERE s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $2 OFFSET $3
        `;
        params = [seasonId, limit, offset];
      } else {
        // 활성 시즌의 랭킹
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            u.diploma,
            s.total_score,
            s.season_id,
            se.name as season_name,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, season_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
            GROUP BY user_id, season_id
          ) s ON u.id = s.user_id
          JOIN seasons se ON s.season_id = se.id
          WHERE s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $1 OFFSET $2
        `;
        params = [limit, offset];
      }

      const result = await pool.query(query, params);
      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 특정 사용자의 랭킹 조회
  static async getUserRanking(userId) {
    try {
      // 일일 랭킹
      const dailyResult = await pool.query(
        `WITH ranked AS (
          SELECT 
            user_id, 
            daily_score,
            ROW_NUMBER() OVER (ORDER BY daily_score DESC) as rank
          FROM scores
          WHERE date = CURRENT_DATE
        )
        SELECT rank, daily_score 
        FROM ranked 
        WHERE user_id = $1`,
        [userId]
      );

      // 시즌 랭킹
      const seasonResult = await pool.query(
        `WITH ranked AS (
          SELECT 
            s.user_id, 
            SUM(s.total_score) as total_score,
            ROW_NUMBER() OVER (ORDER BY SUM(s.total_score) DESC) as rank
          FROM scores s
          WHERE s.season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
          GROUP BY s.user_id
        )
        SELECT rank, total_score 
        FROM ranked 
        WHERE user_id = $1`,
        [userId]
      );

      return {
        daily: dailyResult.rows[0] || { rank: null, daily_score: 0 },
        season: seasonResult.rows[0] || { rank: null, total_score: 0 }
      };
    } catch (error) {
      throw error;
    }
  }

  // 디플로마별 랭킹
  static async getRankingByDiploma(diploma, seasonId = null, { limit = 50, offset = 0 } = {}) {
    try {
      let query, params;

      if (seasonId) {
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            u.diploma,
            s.total_score,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id = $1
            GROUP BY user_id
          ) s ON u.id = s.user_id
          WHERE u.diploma = $2 AND s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $3 OFFSET $4
        `;
        params = [seasonId, diploma, limit, offset];
      } else {
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            u.diploma,
            s.total_score,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
            GROUP BY user_id
          ) s ON u.id = s.user_id
          WHERE u.diploma = $1 AND s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $2 OFFSET $3
        `;
        params = [diploma, limit, offset];
      }

      const result = await pool.query(query, params);
      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 특정 사용자 주변 랭킹 조회 (±5등)
  static async getSurroundingRanking(userId, seasonId = null, range = 5) {
    try {
      // 사용자의 현재 순위 조회
      const userRanking = seasonId 
        ? await this.getUserSeasonRank(userId, seasonId)
        : await this.getUserRanking(userId);

      const userRank = seasonId ? userRanking.rank : userRanking.season.rank;

      if (!userRank) {
        return [];
      }

      const startRank = Math.max(1, userRank - range);
      const endRank = userRank + range;

      let query;
      if (seasonId) {
        query = `
          WITH ranked AS (
            SELECT 
              u.id,
              u.username,
              u.student_id,
              s.total_score,
              ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
            FROM users u
            JOIN (
              SELECT user_id, SUM(total_score) as total_score
              FROM scores
              WHERE season_id = $1
              GROUP BY user_id
            ) s ON u.id = s.user_id
            WHERE s.total_score > 0
          )
          SELECT * FROM ranked
          WHERE rank BETWEEN $2 AND $3
          ORDER BY rank ASC
        `;
        var params = [seasonId, startRank, endRank];
      } else {
        query = `
          WITH ranked AS (
            SELECT 
              u.id,
              u.username,
              u.student_id,
              s.total_score,
              ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
            FROM users u
            JOIN (
              SELECT user_id, SUM(total_score) as total_score
              FROM scores
              WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
              GROUP BY user_id
            ) s ON u.id = s.user_id
            WHERE s.total_score > 0
          )
          SELECT * FROM ranked
          WHERE rank BETWEEN $1 AND $2
          ORDER BY rank ASC
        `;
        var params = [startRank, endRank];
      }

      const result = await pool.query(query, params);
      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 특정 시즌의 사용자 순위
  static async getUserSeasonRank(userId, seasonId) {
    try {
      const result = await pool.query(
        `WITH ranked AS (
          SELECT 
            s.user_id,
            SUM(s.total_score) as total_score,
            ROW_NUMBER() OVER (ORDER BY SUM(s.total_score) DESC) as rank
          FROM scores s
          WHERE s.season_id = $1
          GROUP BY s.user_id
        )
        SELECT rank, total_score
        FROM ranked
        WHERE user_id = $2`,
        [seasonId, userId]
      );

      return result.rows[0] || { rank: null, total_score: 0 };
    } catch (error) {
      throw error;
    }
  }

  // 점수 업데이트 (내부 함수)
  static async updateScore(userId, seasonId, points) {
    try {
      const today = new Date().toISOString().split('T')[0];

      await pool.query(
        `INSERT INTO scores (user_id, season_id, date, total_score, daily_score)
         VALUES ($1, $2, $3, $4, $4)
         ON CONFLICT (user_id, season_id, date)
         DO UPDATE SET
           total_score = scores.total_score + $4,
           daily_score = scores.daily_score + $4,
           updated_at = CURRENT_TIMESTAMP`,
        [userId, seasonId, today, points]
      );

      return true;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 점수 조회
  static async getUserScore(userId, seasonId = null, date = null) {
    try {
      let query, params;

      if (date) {
        query = `
          SELECT daily_score, total_score, date
          FROM scores
          WHERE user_id = $1 AND date = $2
        `;
        params = [userId, date];
      } else if (seasonId) {
        query = `
          SELECT SUM(total_score) as total_score, season_id
          FROM scores
          WHERE user_id = $1 AND season_id = $2
          GROUP BY season_id
        `;
        params = [userId, seasonId];
      } else {
        query = `
          SELECT SUM(total_score) as total_score
          FROM scores
          WHERE user_id = $1 AND season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
        `;
        params = [userId];
      }

      const result = await pool.query(query, params);
      return result.rows[0] || { total_score: 0 };
    } catch (error) {
      throw error;
    }
  }

  // 전체 랭킹 통계
  static async getStatistics(seasonId = null) {
    try {
      let query, params;

      if (seasonId) {
        query = `
          SELECT 
            COUNT(DISTINCT user_id) as total_users,
            SUM(total_score) as total_points,
            AVG(total_score) as avg_points,
            MAX(total_score) as max_points
          FROM scores
          WHERE season_id = $1
        `;
        params = [seasonId];
      } else {
        query = `
          SELECT 
            COUNT(DISTINCT user_id) as total_users,
            SUM(total_score) as total_points,
            AVG(total_score) as avg_points,
            MAX(total_score) as max_points
          FROM scores
          WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
        `;
        params = [];
      }

      const result = await pool.query(query, params);
      return result.rows[0];
    } catch (error) {
      throw error;
    }
  }

  // 일일 Top N
  static async getDailyTopN(n = 10, date = null) {
    try {
      const targetDate = date || new Date().toISOString().split('T')[0];

      const result = await pool.query(
        `SELECT 
          u.id,
          u.username,
          u.student_id,
          s.daily_score,
          ROW_NUMBER() OVER (ORDER BY s.daily_score DESC) as rank
         FROM users u
         JOIN scores s ON u.id = s.user_id
         WHERE s.date = $1 AND s.daily_score > 0
         ORDER BY s.daily_score DESC
         LIMIT $2`,
        [targetDate, n]
      );

      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 시즌 Top N
  static async getSeasonTopN(n = 10, seasonId = null) {
    try {
      let query, params;

      if (seasonId) {
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            s.total_score,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id = $1
            GROUP BY user_id
          ) s ON u.id = s.user_id
          WHERE s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $2
        `;
        params = [seasonId, n];
      } else {
        query = `
          SELECT 
            u.id,
            u.username,
            u.student_id,
            s.total_score,
            ROW_NUMBER() OVER (ORDER BY s.total_score DESC) as rank
          FROM users u
          JOIN (
            SELECT user_id, SUM(total_score) as total_score
            FROM scores
            WHERE season_id IN (SELECT id FROM seasons WHERE is_active = TRUE)
            GROUP BY user_id
          ) s ON u.id = s.user_id
          WHERE s.total_score > 0
          ORDER BY s.total_score DESC
          LIMIT $1
        `;
        params = [n];
      }

      const result = await pool.query(query, params);
      return result.rows;
    } catch (error) {
      throw error;
    }
  }

  // 사용자 점수 히스토리
  static async getUserScoreHistory(userId, seasonId = null, limit = 30) {
    try {
      let query, params;

      if (seasonId) {
        query = `
          SELECT date, daily_score, total_score
          FROM scores
          WHERE user_id = $1 AND season_id = $2
          ORDER BY date DESC
          LIMIT $3
        `;
        params = [userId, seasonId, limit];
      } else {
        query = `
          SELECT date, daily_score, total_score, season_id
          FROM scores
          WHERE user_id = $1
          ORDER BY date DESC
          LIMIT $2
        `;
        params = [userId, limit];
      }

      const result = await pool.query(query, params);
      return result.rows;
    } catch (error) {
      throw error;
    }
  }
}

module.exports = Ranking;