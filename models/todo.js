const db = require('../database/init');

module.exports = {
    createTodo: async (userId, date, subject, text, completed = false) => {
        const query = `
            INSERT INTO todos (user_id, date, subject, text, completed)
            VALUES ($1, $2, $3, $4, $5) RETURNING *;
        `;
        const values = [userId, date, subject, text, completed];
        const result = await db.query(query, values);
        return result.rows[0];
    },

    getTodosByUser: async (userId) => {
        const result = await db.query(
            'SELECT * FROM todos WHERE user_id = $1 ORDER BY date ASC',
            [userId]
        );
        return result.rows;
    },

    updateTodoStatus: async (todoId, completed) => {
        const result = await db.query(
            'UPDATE todos SET completed = $1 WHERE id = $2 RETURNING *',
            [completed, todoId]
        );
        return result.rows[0];
    }
};
