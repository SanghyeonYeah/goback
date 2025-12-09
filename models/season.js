const db = require('../database/init');

module.exports = {
    createSeason: async (name, start, end) => {
        const query = `
            INSERT INTO seasons (season_name, start_date, end_date)
            VALUES ($1, $2, $3) RETURNING *;
        `;
        const result = await db.query(query, [name, start, end]);
        return result.rows[0];
    },

    getSeasons: async () => {
        const result = await db.query('SELECT * FROM seasons ORDER BY start_date DESC');
        return result.rows;
    },

    getCurrentSeason: async () => {
        const query = `
            SELECT * FROM seasons
            WHERE NOW() BETWEEN start_date AND end_date
            LIMIT 1
        `;
        const result = await db.query(query);
        return result.rows[0];
    }
};
