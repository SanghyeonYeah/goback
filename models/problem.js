const db = require('../database/init');

module.exports = {
    addProblem: async (title, content, answer, score) => {
        const query = `
            INSERT INTO problems (title, content, answer, score)
            VALUES ($1, $2, $3, $4) RETURNING *;
        `;
        const result = await db.query(query, [title, content, answer, score]);
        return result.rows[0];
    },

    getRandomProblem: async () => {
        const result = await db.query(`
            SELECT * FROM problems ORDER BY RANDOM() LIMIT 1
        `);
        return result.rows[0];
    },

    getAllProblems: async () => {
        const result = await db.query('SELECT * FROM problems ORDER BY id DESC');
        return result.rows;
    }
};
